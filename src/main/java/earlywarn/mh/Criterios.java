package earlywarn.mh;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;

public class Criterios{
    //TODO: Añadir la opción de meter las restricciones por parámetros
    //Restricción del impacto económico de los pasajeros perdidos, en porcentaje
    final float maxPorcentajePasajerosPerdidos=0.2f;
    //Restricción de la homogeneidad en el porcentaje de pasajeros que pierden las aerolíneas
    final float maxPorcentajeDesviacionMediaPasajerosPerdidosPorCompanyia=0.2f;
    //Restricción de la homogeneidad en el porcentaje de pérdida de ingresos por turismo en los destinos.
    final float maxPorcentajeDesviacionMediaIngresosDestinos=0.2f;
    //Restricción sobre la conectividad perdida en los destinos.
    final float maxPorcentajeConectividadPerdida=0.2f;
    //Restricción del porcentaje de pérdida de ingresos por turismo en los destinos
    final float maxPorcentajeDineroPerdidoRegion = 0.2f;
    //TODO: Calcular un valor correcto para la penalización
    //Valor que se añade a la función objetivo cuando una solución no cumple una restricción
    final int penalizacionRestriccion=1000000;
    //Valor del peso asociado los objetivos epidemiológicos, en el caso de que no se usen como restricción
    final double pesosEpidemiologicos;
    //Valor del peso asociado los objetivos económicos, en el caso de que no se usen como restricción, este valor se
    //reparte entre los distintos objetivos
    final double pesosEconomicos;
    //Número de criterios económicos, se utiliza para repartir el peso económico entre los distintos criterios de manera
    //equitativa
    final int numCriteriosEconomicos = 4;
    //Valor del peso asociado los objetivos sociales, en el caso de que no se usen como restricción
    final double pesosSociales;
    //La solución con la que trabajamos (Si es 1 la conexión está abierta, si es 0 cerrada), se modifica en evaluate fitness
    List<Boolean> solucion = new ArrayList<>();
    //Contiene el nombre de los aeropuertos de entrada no repetidos, en principio serán los de España
    List<String> nombreAeropuertosEntradaEspanya = new ArrayList<>();
    //Contiene el nombre de los aeropuertos de salida no repetidos
    List<String> nombreAeropuertosSalida = new ArrayList<>();
    //Contiene el nombre de las compañías aéreas no repetidos
    List<String> nombreCompanyias = new ArrayList<>();
    //Listado de conexiones entre los aeropuertos, no hay repetidos
    List<Conexion> listaConexiones = new ArrayList<>();
    //Valores de infectados del SIR en las conexiones
    Map<Conexion,Double> listaRiesgosEspanyoles = new HashMap<>();
    //Número de pasajeros en las conexiones
    Map<Conexion,Integer> listaPasajeros = new HashMap<>();
    //Dinero que ganan los destinos asociados a su conexión
    Map<Conexion,Double> listaDineroTurismoConexion = new HashMap<>();
    //Conectividad de los aeropuertos salida hacia aeropuertos españoles
    Map<String,Double> listaConectividadAeropuerto = new HashMap<>();
    //Número de vuelos desde un aeropuerto cualquiera a un aeropuerto español
    Map<Conexion,Integer> listaNumeroVuelosConexion = new HashMap<>();
    //Número de pasajeros en las conexiones dependiendo de la compañía
    Map<ConexionyCompanyia,Integer> listaPasajerosCompanyia = new HashMap<>();
    //Los caracteres que se utilizan para separar los CSV
    String COMMA_DELIMITER=",";

    /**
     * Constructor de la clase, desde aquí se cargarán todos los datos
     */
    public Criterios(double pesosEpidemiologicos, double pesosEconomicos, double pesosSociales){
        this.pesosEpidemiologicos = pesosEpidemiologicos;
        this.pesosEconomicos = pesosEconomicos;
        this.pesosSociales = pesosSociales;
        cargaAeropuertosEntrada();
        cargaAeropuertosSalida();
        cargaCompanyias();
        cargaVuelos();
        cargaDatosSIR();
        cargaListaPasajeros();
        cargaListaPasajerosCompanyia();
        cargaDineroVuelos();
        cargaConectividad();
    }

    public int getNumConexiones(){
        System.out.println(listaConexiones.size());
        return listaConexiones.size();
    }

    /**
     * Calcula el fitness de una partícula, comprueba todos los objetivos/restricciones
     * @param positions La posición de una partícula
     * @return Devuelve su fitness
     */
    public double evaluateFitnessUniobjetivo(List<Boolean> positions) {
        return calculoRiesgoImportado(positions)+
                calculoEconomicoPerdidaPasajeros(positions)/numCriteriosEconomicos+
                calculoPerdidaIngresosDestinos(positions)/numCriteriosEconomicos+
                calculoHomogeneidadPasajerosAerolineas(positions)/numCriteriosEconomicos+
                calculoHomogeneidadIngresosTurismoAeropuertos(positions)/numCriteriosEconomicos+
                calculoConectividadDestinos(positions);
    }

    /**
     * Función que calcula si la solución esDominada es dominada por la solución domina
     * @param esDominada Fitness de la solución que se comprueba si es dominada
     * @param domina Fitness de la solución que se comprueba si domina
     * @return Devuelve true si la solución esDominada es dominada por la solución domina
     */
    public boolean isDominated(List<Double> esDominada, List<Double> domina){
        boolean resultado = true;
        for(int i = 0; i<esDominada.size(); i++){
            if (esDominada.get(i) <= domina.get(i)) {
                resultado = false;
                break;
            }
        }
        return resultado;
    }

    /**
     * Calcula el fitness de una partícula, comprueba todos los objetivos/restricciones
     * @param positions Una partícula
     * @return Devuelve su fitness
     */
    public List<Double> evaluateFitness(List<Boolean> positions) {
        List<Double> fitnessResultante = new ArrayList<>();

        fitnessResultante.add(fitnessCriteriosEpidemiologicos(positions));
        fitnessResultante.add(fitnessCriteriosEconomicos(positions));
        fitnessResultante.add(fitnessCriteriosSociales(positions));

        return  fitnessResultante;
    }

    public double fitnessCriteriosEpidemiologicos(List<Boolean> positions){
        return calculoRiesgoImportado(positions);
    }

    public double fitnessCriteriosEconomicos(List<Boolean> positions){
        return calculoEconomicoPerdidaPasajeros(positions)/numCriteriosEconomicos+
                calculoPerdidaIngresosDestinos(positions)/numCriteriosEconomicos+
                calculoHomogeneidadPasajerosAerolineas(positions)/numCriteriosEconomicos+
                calculoHomogeneidadIngresosTurismoAeropuertos(positions)/numCriteriosEconomicos;
    }

    public double fitnessCriteriosSociales(List<Boolean> positions){
        return calculoConectividadDestinos(positions);
    }

    /**
     * Calcula el fitness del riesgo importado dada una solución
     * @return Devuelve el riesgo de las conexiones abiertas
     */
    private double calculoRiesgoImportado(List<Boolean> solucion){
        double riesgo=0;
        double riesgoTotal=0;
        for(int i=0;i<listaConexiones.size();i++){
            if(solucion.get(i)){
                riesgo=riesgo+listaRiesgosEspanyoles.get(listaConexiones.get(i));
            }
            riesgoTotal=riesgoTotal+listaRiesgosEspanyoles.get(listaConexiones.get(i));
        }
        return riesgo/riesgoTotal;
    }

    /**
     * Impacto económico para las compañías derivado de la pérdida de pasajeros, está
     * implementado como una restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private double calculoEconomicoPerdidaPasajeros(List<Boolean> solucion){
        int totalPasajeros=0;
        int totalPasajerosConexiones=0;
        for(int i=0;i<listaConexiones.size();i++){
            totalPasajeros=totalPasajeros+listaPasajeros.get(listaConexiones.get(i));
            if(solucion.get(i)){
                totalPasajerosConexiones=totalPasajerosConexiones+listaPasajeros.get(listaConexiones.get(i));
            }
        }
        double porcentajePerdido;
        if(totalPasajeros!=0){
            porcentajePerdido=1-(double)totalPasajerosConexiones/totalPasajeros;
        }else{
            porcentajePerdido=0;
        }

        return porcentajePerdido;
        /*Usar esto si queremos restricción
        if(porcentajePerdido>maxPorcentajePasajerosPerdidos){
            return penalizacionRestriccion;
        }
        return 0;
         */
    }

    /**
     * Porcentaje de pérdida de ingresos por turismo en los destinos, implementado como una restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private double calculoPerdidaIngresosDestinos(List<Boolean> solucion){
        double totalIngresos=0;
        double totalIngresosConexion=0;
        for(int i=0;i<listaConexiones.size();i++){
            totalIngresos=totalIngresos+listaDineroTurismoConexion.get(listaConexiones.get(i));
            if(solucion.get(i)){
                totalIngresosConexion=totalIngresosConexion+listaDineroTurismoConexion.get(listaConexiones.get(i));
            }
        }
        double porcentajePerdido;
        if(totalIngresos!=0){
            porcentajePerdido=1-totalIngresosConexion/totalIngresos;
        }else{
            porcentajePerdido=0;
        }
        return porcentajePerdido;
        /*Usar esto si queremos restricción
        if(porcentajePerdido>maxPorcentajeDineroPerdidoRegion){
            return penalizacionRestriccion;
        }
        return 0;
         */
    }

    /**
     * Se comprueba la homogeneidad de perdida de pasajeros entre distintas compañías aéreas, para ello hacemos la desviación
     * media de los pasajeros perdidos por cada compañía y vemos si esta debajo de un porcentaje, implementado como restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private double calculoHomogeneidadPasajerosAerolineas(List<Boolean> solucion){
        int i;
        int[] totalPasajerosCompanyias=new int[nombreCompanyias.size()];
        int[] totalPasajerosConexiones=new int[nombreCompanyias.size()];
        List<Double> porcentajePerdido = new ArrayList<>();
        double porcentajePerdidoMedia = 0.0;
        double porcentajePerdidoDesviacionMedia = 0.0;
        for(int j=0;j<nombreCompanyias.size();j++){
            if(!nombreCompanyias.get(j).equals("UNKNOWN")){
                for(i=0;i<listaConexiones.size();i++){
                    if(listaPasajerosCompanyia.get(new ConexionyCompanyia(listaConexiones.get(i),
                            nombreCompanyias.get(j)))!=null){
                        totalPasajerosCompanyias[j]=totalPasajerosCompanyias[j]+listaPasajerosCompanyia.
                                get(new ConexionyCompanyia(listaConexiones.get(i),nombreCompanyias.get(j)));
                        if(solucion.get(i)){
                            totalPasajerosConexiones[j]=totalPasajerosConexiones[j]+listaPasajerosCompanyia.
                                    get(new ConexionyCompanyia(listaConexiones.get(i),nombreCompanyias.get(j)));
                        }
                    }
                }
                if(totalPasajerosCompanyias[j]!=0){
                    porcentajePerdido.add(1-(double)totalPasajerosConexiones[j]/totalPasajerosCompanyias[j]);
                    porcentajePerdidoMedia = porcentajePerdidoMedia +
                            1-(double)totalPasajerosConexiones[j]/totalPasajerosCompanyias[j];
                }
            }
        }
        porcentajePerdidoMedia = porcentajePerdidoMedia / porcentajePerdido.size();
        for(i=0;i<porcentajePerdido.size();i++){
            porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia +
                    Math.abs(porcentajePerdido.get(i)-porcentajePerdidoMedia);
        }
        porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia / porcentajePerdido.size();
        return porcentajePerdidoDesviacionMedia;
        /*Usar esto si queremos restricción
        if(porcentajePerdidoDesviacionMedia>maxPorcentajeDesviacionMediaPasajerosPerdidosPorCompanyia) {
            return penalizacionRestriccion;
        }
        return 0;
         */
    }

    /**
     * Se comprueba la homogeneidad de perdida de ingresos por turismo entre los diferentes aeropuertos, para ello
     * hacemos la desviación media de los pasajeros perdidos por cada compañía y vemos si esta debajo de un porcentaje,
     * implementado como restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private double calculoHomogeneidadIngresosTurismoAeropuertos(List<Boolean> solucion){
        java.util.Map<String, Integer> numPasajerosAeropuerto = new java.util.HashMap<>();
        java.util.Map<String, Integer> numPasajerosAeropuertoConexiones = new java.util.HashMap<>();
        List<Double> porcentajePerdido = new ArrayList<>();
        double mediaPorcentajeVuelosPerdidos=0.0;
        double porcentajePerdidoDesviacionMedia = 0.0;
        int i;
        for(i=0;i<listaConexiones.size();i++){
            if(numPasajerosAeropuerto.get(listaConexiones.get(i).entrada)!=null){
                numPasajerosAeropuerto.put(listaConexiones.get(i).entrada, numPasajerosAeropuerto.get(
                        listaConexiones.get(i).entrada)+listaPasajeros.get(listaConexiones.get(i)));
            }else{
                numPasajerosAeropuerto.put(listaConexiones.get(i).entrada, listaPasajeros.get(listaConexiones.get(i)));
            }
            if(solucion.get(i)){
                if(numPasajerosAeropuertoConexiones.get(listaConexiones.get(i).entrada)!=null){
                    numPasajerosAeropuertoConexiones.put(listaConexiones.get(i).entrada,numPasajerosAeropuertoConexiones.get(
                            listaConexiones.get(i).entrada)+listaPasajeros.get(listaConexiones.get(i)));
                }else{
                    numPasajerosAeropuertoConexiones.put(listaConexiones.get(i).entrada, listaPasajeros.get(listaConexiones.get(i)));
                }
            }
        }
        i=0;
        for (String key:numPasajerosAeropuerto.keySet()) {
            if(numPasajerosAeropuertoConexiones.get(key)!=null){
                porcentajePerdido.add((double)numPasajerosAeropuertoConexiones.get(key)/numPasajerosAeropuerto.get(key));
                mediaPorcentajeVuelosPerdidos=mediaPorcentajeVuelosPerdidos+porcentajePerdido.get(i);
            }else{
                porcentajePerdido.add(0.0);
            }
            i++;
        }
        mediaPorcentajeVuelosPerdidos = mediaPorcentajeVuelosPerdidos / porcentajePerdido.size();
        for(i=0;i<porcentajePerdido.size();i++){
            porcentajePerdidoDesviacionMedia=porcentajePerdidoDesviacionMedia+
                                                Math.abs(porcentajePerdido.get(i)-mediaPorcentajeVuelosPerdidos);
        }
        porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia / porcentajePerdido.size();
        return porcentajePerdidoDesviacionMedia;
    }
    /**
     * Comprueba que la conectividad perdida en los destinos no sea mayor que un porcentaje, implementada como una restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private double calculoConectividadDestinos(List<Boolean> solucion){
        int i;
        java.util.Map<String, Double> numVuelosAeropuerto = new java.util.HashMap<>();
        java.util.Map<String, Double> numVuelosAeropuertoConexiones = new java.util.HashMap<>();
        double sumatorioConectividadPerdida=0.0;
        double sumatorioConectividad=0.0;
        for(i=0;i<listaConexiones.size();i++){
            if(listaNumeroVuelosConexion.get(listaConexiones.get(i))!=null){
                if(numVuelosAeropuerto.get(listaConexiones.get(i).salida)!=null){
                    numVuelosAeropuerto.put(listaConexiones.get(i).salida,numVuelosAeropuerto.
                            get(listaConexiones.get(i).salida)+listaNumeroVuelosConexion.get(listaConexiones.get(i)));
                }else{
                    numVuelosAeropuerto.put(listaConexiones.get(i).salida,
                            Double.valueOf(listaNumeroVuelosConexion.get(listaConexiones.get(i))));
                }
                if(solucion.get(i)){
                    if(numVuelosAeropuertoConexiones.get(listaConexiones.get(i).salida)!=null){
                        numVuelosAeropuertoConexiones.put(listaConexiones.get(i).salida,numVuelosAeropuertoConexiones.
                                get(listaConexiones.get(i).salida)+listaNumeroVuelosConexion.get(listaConexiones.get(i)));
                    }else{
                        numVuelosAeropuertoConexiones.put(listaConexiones.get(i).salida,
                                Double.valueOf(listaNumeroVuelosConexion.get(listaConexiones.get(i))));
                    }
                }

            }
        }
        for(String key:numVuelosAeropuerto.keySet()){
            if(numVuelosAeropuertoConexiones.get(key)!=null){
                sumatorioConectividadPerdida=sumatorioConectividadPerdida+listaConectividadAeropuerto.get(key)*(
                        (numVuelosAeropuertoConexiones.get(key)/numVuelosAeropuerto.get(key)));
            }
            sumatorioConectividad=sumatorioConectividad+listaConectividadAeropuerto.get(key);
        }
        return 1-sumatorioConectividadPerdida/sumatorioConectividad;
    }

    //TODO:Implementando carga de datos en CSV, más adelante funcionará con llamadas a Neo4j
    /**
     * Función que carga los aeropuertos de entrada, estos valores no se repiten debido a que no hay duplicados
     * en el csv. Se da por hecho que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private void cargaAeropuertosEntrada(){
        String ubicacionArchivo = "datos/aeropuertos_entradas.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(COMMA_DELIMITER);
                    nombreAeropuertosEntradaEspanya.add(values[0]);
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaAeropuertosEntrada: "+e);
        }
    }

    /**
     * Función que carga los aeropuertos de salida, estos valores no se repiten debido a que no hay duplicados en el
     * csv. Se da por hecho que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private void cargaAeropuertosSalida(){
        String ubicacionArchivo = "datos/aeropuertos_salida.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(COMMA_DELIMITER);
                    nombreAeropuertosSalida.add(values[0]);
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaAeropuertosSalida: "+e);
        }
    }

    /**
     * Función que carga el código de las compañías aéreas, estos valores no se repiten debido a que no hay duplicados
     * en el csv. Se da por hecho que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private void cargaCompanyias(){
        String ubicacionArchivo = "datos/companyias.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(COMMA_DELIMITER);
                    nombreCompanyias.add(values[0]);
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaCompanyias: "+e);
        }
    }

    /**
     * Función que carga todos los vuelos que se van a realizar ese día no se repiten. Se da por hecho que la primera
     * línea no contiene datos y por tanto nos la saltamos.
     */
    private void cargaVuelos(){
        String ubicacionArchivo = "datos/sir.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            String[] values;
            Conexion anyadir;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    values = line.split(COMMA_DELIMITER);
                    anyadir = new Conexion(values[1], values[2]);
                    if (!listaConexiones.contains(anyadir)) {
                        listaConexiones.add(anyadir);
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaVuelos: "+e);
        }
        System.out.println("Número de conexiones: "+listaConexiones.size());
    }

    /**
     * Función que carga los valores epidemiologicos en listaRiesgosEspanyoles. Se da por hecho que la primera línea no
     * contiene datos y por tanto nos la saltamos.
     */
    private void cargaDatosSIR(){
        String ubicacionArchivo = "datos/sir.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            String[] values;
            Conexion indice;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    values = line.split(COMMA_DELIMITER);
                    indice = new Conexion(values[1], values[2]);
                    if (listaRiesgosEspanyoles.get(indice) != null) {
                        listaRiesgosEspanyoles.put(indice, listaRiesgosEspanyoles.get(indice) + Double.parseDouble(values[0]));
                    } else {
                        listaRiesgosEspanyoles.put(indice, Double.parseDouble(values[0]));
                    }
                }

            }
        }catch (Exception e){
            System.out.println("Excepción cargaDatosSIR: "+e);
        }
    }

    /**
     * Función que carga el número de pasajeros en una conexión. Se da por hecho que la primera línea no contiene datos
     * y por tanto nos la saltamos.
     */
    private void cargaListaPasajeros(){
        String ubicacionArchivo = "datos/pasajeros_por_vuelo.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            String[] values;
            Conexion indice;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    values = line.split(COMMA_DELIMITER);
                    indice = new Conexion(values[1], values[2]);
                    if (listaPasajeros.get(indice) != null) {
                        listaPasajeros.put(indice, listaPasajeros.get(indice) + (int)Double.parseDouble(values[0]));
                    } else {
                        listaPasajeros.put(indice, (int)Double.parseDouble(values[0]));
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaListaPasajeros: "+e);
        }
    }

    /**
     * Función que carga el número de pasajeros en las conexiones distinguiendo entre compañías aéreas. Se da por hecho
     * que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private void cargaListaPasajerosCompanyia(){
        String ubicacionArchivo = "datos/pasajeros_por_vuelo_y_companyias.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            String[] values;
            ConexionyCompanyia indice;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    values = line.split(COMMA_DELIMITER);
                    indice = new ConexionyCompanyia(new Conexion(values[2], values[3]), values[1]);
                    if (listaPasajerosCompanyia.get(indice) != null) {
                        listaPasajerosCompanyia.put(indice, listaPasajerosCompanyia.get(indice) +
                                (int)Double.parseDouble(values[0]));
                    } else {
                        listaPasajerosCompanyia.put(indice, (int)Double.parseDouble(values[0]));
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaListaPasajerosCompanyia: "+e);
        }
    }

    /**
     * Función que carga el dinero por turismo a cada una de las conexiones. Se da por hecho que la primera línea no
     * contiene datos y por tanto nos la saltamos.
     */
    private void cargaDineroVuelos() {
        String ubicacionArchivo = "datos/dinero_por_vuelo.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            String[] values;
            Conexion indice;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    values = line.split(COMMA_DELIMITER);
                    indice = new Conexion(values[1], values[2]);
                    if (listaDineroTurismoConexion.get(indice) != null) {
                        listaDineroTurismoConexion.put(indice, listaDineroTurismoConexion.get(indice) + Double.parseDouble(values[0]));
                    } else {
                        listaDineroTurismoConexion.put(indice, Double.parseDouble(values[0]));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Excepción cargaDineroVuelos: "+e);
        }
    }

    /**
     * Función que carga la conectividad de los aeropuertos de salida. Esta conectividad corresponde a la conectividad
     * del aeropuerto solo contando los vuelos hacia España. Se da por hecho que la primera línea no contiene datos y
     * por tanto nos la saltamos.
     */
    private void cargaConectividad(){
        String ubicacionArchivo = "datos/conectividad_por_aeropuerto.csv";
        Map<String,Integer> numeroVuelosHaciaEspanya = new HashMap<>();
        Map<String,Integer> numeroVuelosHaciaFueraTotales = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            String[] values;
            Conexion indice;
            if((br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    values = line.split(COMMA_DELIMITER);
                    indice = new Conexion(values[0], values[1]);
                    listaNumeroVuelosConexion.put(indice, Integer.parseInt(values[2]));
                    if (numeroVuelosHaciaEspanya.get(values[1]) != null) {
                        numeroVuelosHaciaEspanya.put(values[1], numeroVuelosHaciaEspanya.get(values[1]) + Integer.parseInt(values[2]));
                    } else {
                        numeroVuelosHaciaEspanya.put(values[1], Integer.parseInt(values[2]));
                    }
                    numeroVuelosHaciaFueraTotales.put(values[1], Integer.parseInt(values[3]));
                    listaConectividadAeropuerto.put(values[1], Double.parseDouble(values[4]));
                }
                for(String a:nombreAeropuertosSalida){
                    if(listaConectividadAeropuerto.get(a)!=null){
                        listaConectividadAeropuerto.put(a, listaConectividadAeropuerto.get(a) *
                                numeroVuelosHaciaEspanya.get(a) / numeroVuelosHaciaFueraTotales.get(a));
                    }else{
                        listaConectividadAeropuerto.put(a, 0.0);
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaConectividad: "+e);
        }
    }

    /**
     * Función que carga una solución dada por cplex, se utiliza para realizar pruebas. Para que funcione correctamente
     * hay que borrar "Solución:" al principio del txt generado por cplex. También suponemos que la solución dada por
     * cplex tiene el mismo orden que el dado en los datos del csv.
     */
    public List<Boolean> cargaSolucion(){
        String ubicacionArchivo = "datos/Solucion.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            String[] values;
            int contador;
            while ((line = br.readLine()) != null) {
                line=line.replaceAll("[^a-zA-Z0-9]",",");
                values = line.split(COMMA_DELIMITER);
                contador=0;
                for(String a:values){
                    if(!a.equals("")){
                        contador++;
                        if(contador%3==0){
                            if(a.equals("1")){
                                solucion.add(true);
                            }else{
                                solucion.add(false);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaSolucion: "+e);
        }
        return solucion;
    }
}
