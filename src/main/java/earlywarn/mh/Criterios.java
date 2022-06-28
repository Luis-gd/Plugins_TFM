package earlywarn.mh;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;

public class Criterios{
    //Número de conexiones con las que trabajamos
    static int numDimensions=0;
    //Número de compañias aéreas con las que trabajamos
    static int numCompanyias=0;
    //TODO: Añadir la opción de meter las restricciones por parámetros
    //TODO: Más adelante modificar el código para que se puedan introducir estos valores por parámetros
    //Restricción del impacto económico de los pasajeros perdidos, en porcentaje
    final static float maxPorcentajePasajerosPerdidos=0.2f;
    //Restricción de la homogeneidad en el porcentaje de pasajeros que pierden las aerolíneas
    final static float maxPorcentajeDesviacionMediaPasajerosPerdidosPorCompanyia=0.2f;
    //Restricción de la homogeneidad en el porcentaje de pérdida de ingresos por turismo en los destinos.
    final static float maxPorcentajeDesviacionMediaIngresosDestinos=0.2f;
    //Restricción sobre la conectividad perdida en los destinos.
    final static float maxPorcentajeConectividadPerdida=0.2f;
    //Restricción del porcentaje de pérdida de ingresos por turismo en los destinos
    final static float maxPorcentajeDineroPerdidoRegion = 0.2f;
    //TODO: Calcular un valor correcto para la penalización
    //Valor que se añade a la función objetivo cuando una solución no cumple una restricción
    final static int penalizacionRestriccion=1000000;
    //Valor del peso asociado los objetivos epidemiológicos, en el caso de que no se usen como restricción
    final static double pesosEpidemiologicos = 0.6;
    //Valor del peso asociado los objetivos económicos, en el caso de que no se usen como restricción, este valor se
    //reparte entre los distintos objetivos
    final static double pesosEconomicos = 0.2;
    //Valor del peso asociado los objetivos sociales, en el caso de que no se usen como restricción
    final static double pesosSociales = 0.2;
    //La solución con la que trabajamos (Si es 1 la conexión está abierta, si es 0 cerrada), se modifica en evaluate fitness
    static List<Boolean> solucion = new ArrayList<>();
    //Contiene el nombre de los aeropuertos de entrada no repetidos, en principio serán los de España
    static List<String> nombreAeropuertosEntradaEspanya = new ArrayList<>();
    //Contiene el nombre de los aeropuertos de salida no repetidos
    static List<String> nombreAeropuertosSalida = new ArrayList<>();
    //Contiene el nombre de las compañías aéreas no repetidos
    static List<String> nombreCompanyias = new ArrayList<>();
    //Listado de vuelos que hay entre los aeropuertos, no hay repetidos
    static List<Conexion> listaConexiones = new ArrayList<>();
    //Valores de infectados del SIR en las conexiones
    static Map<Conexion,Double> listaRiesgosEspanyoles = new HashMap<>();
    //Número de pasajeros en las conexiones
    static Map<Conexion,Integer> listaPasajeros = new HashMap<>();
    //Dinero que ganan los destinos asociados a su conexión
    static Map<Conexion,Double> listaDineroTurismoConexion = new HashMap<>();
    //Conectividad de los aeropuertos salida hacia aeropuertos españoles
    static Map<String,Double> listaConectividadAeropuerto = new HashMap<>();
    //Conectividad de los aeropuertos salida hacia aeropuertos españoles
    static Map<Conexion,Integer> listaNumeroVuelosConexion = new HashMap<>();
    //Número de pasajeros en las conexiones dependiendo de la compañía
    static Map<ConexionyCompanyia,Integer> listaPasajerosCompanyia = new HashMap<>();
    //Los caracteres que se utilizan para separar los CSV
    static String COMMA_DELIMITER=",";

    /**
     * Calcula el fitness de una partícula, comprueba todos los objetivos/restricciones
     * @param positions La posición de una partícula
     * @return Devuelve su fitness
     */
    //TODO: Cambiar solucion, de momento todo a 1 probando cosas
    public static double evaluateFitness(List<Boolean> positions) {
        //solucion=positions;
        //De momento lo dejamos así debido a que el número de conexiones es 569
        for(int i=0;i<569;i++){
            solucion.add(true);
        }
        return calculoRiesgoImportado()*pesosEpidemiologicos+calculoEconomicoPerdidaPasajeros()*pesosEconomicos/4+
                calculoPerdidaIngresosDestinos()*pesosEconomicos/4+calculoHomogeneidadPasajerosAerolineas()*
                pesosEconomicos/4+calculoHomogeneidadIngresosTurismoDestinos()*pesosEconomicos/4+
                calculoConectividadDestinos()*pesosSociales;
    }
    //TODO: Hay que cargar todos los datos, ahora mismo los estoy dando por hecho, cada dimensión una conexión
    /**
     * Calcula el fitness del riesgo importado dada una solución
     * @return Devuelve el riesgo de las conexiones abiertas
     */
    //TODO: Comprobar las escalas de riesgo
    //TODO: Comprobar si debería estar en porcentaje para normalizar los distintos objetivos
    private static double calculoRiesgoImportado(){
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
    private static double calculoEconomicoPerdidaPasajeros(){
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
    private static double calculoPerdidaIngresosDestinos(){
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

    //TODO: Modificar para que no se comparen los vuelos con compañía aérea UNKNNOWN
    /**
     * Se comprueba la homogeneidad de perdida de pasajeros entre distintas compañías aéreas, para ello hacemos la desviación
     * media de los pasajeros perdidos por cada compañía y vemos si esta debajo de un porcentaje, implementado como restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static double calculoHomogeneidadPasajerosAerolineas(){
        int i;
        //int[][] pasajerosConexion=new int[numDimensions][numCompanyias];
        int[] totalPasajeros=new int[nombreCompanyias.size()];
        int[] totalPasajerosConexiones=new int[nombreCompanyias.size()];
        double[] porcentajePerdido = new double[nombreCompanyias.size()];
        double porcentajePerdidoMedia = 0.0;
        double porcentajePerdidoDesviacionMedia = 0.0;
        for(int j=0;j<nombreCompanyias.size();j++){
            porcentajePerdido[j]=0.0;
            for(i=0;i<listaConexiones.size();i++){
                if(listaPasajerosCompanyia.get(new ConexionyCompanyia(listaConexiones.get(i),nombreCompanyias.get(j)))!=null){
                    totalPasajeros[j]=totalPasajeros[j]+listaPasajerosCompanyia.get(new ConexionyCompanyia(
                            listaConexiones.get(i),nombreCompanyias.get(j)));
                    if(solucion.get(i)){
                        totalPasajerosConexiones[j]=totalPasajerosConexiones[j]+listaPasajerosCompanyia.get(new
                                ConexionyCompanyia(listaConexiones.get(i),nombreCompanyias.get(j)));
                    }
                }
            }
            if(totalPasajeros[j]!=0){
                porcentajePerdido[j]=1-(double)totalPasajerosConexiones[j]/totalPasajeros[j];
            }else{
                porcentajePerdido[j]=0;
            }
            porcentajePerdidoMedia = porcentajePerdidoMedia + porcentajePerdido[j];
        }
        porcentajePerdidoMedia = porcentajePerdidoMedia / nombreCompanyias.size();
        for(i=0;i<nombreCompanyias.size();i++){
            porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia +
                                                Math.abs(porcentajePerdido[i]-porcentajePerdidoMedia);
        }
        porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia / nombreCompanyias.size();
        return porcentajePerdidoDesviacionMedia;
        /*Usar esto si queremos restricción
        if(porcentajePerdidoDesviacionMedia>maxPorcentajeDesviacionMediaPasajerosPerdidosPorCompanyia) {
            return penalizacionRestriccion;
        }
        return 0;
         */
    }

    /**
     * Se comprueba la homogeneidad de perdida de ingresos por turismo entre los diferentes destinos, para ello hacemos la
     * desviación media de los pasajeros perdidos por cada compañía y vemos si esta debajo de un porcentaje, implementado
     * como restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static double calculoHomogeneidadIngresosTurismoDestinos(){
        int[] numeroVuelos = new int[numDimensions];
        String[] aeropuertosEntradaRepetidos = new String[numDimensions];
        java.util.Map<String, Integer> numVuelosAeropuerto = new java.util.HashMap<>();
        java.util.Map<String, Integer> numVuelosAeropuertoConexiones = new java.util.HashMap<>();
        List<Double> porcentajePerdido = new ArrayList<>();
        double mediaPorcentajeVuelosPerdidos=0.0;
        double porcentajePerdidoDesviacionMedia = 0.0;
        int i;
        for(i=0;i<listaConexiones.size();i++){
            if(numVuelosAeropuerto.get(aeropuertosEntradaRepetidos[i])!=null){
                numVuelosAeropuerto.put(aeropuertosEntradaRepetidos[i], numVuelosAeropuerto.get(aeropuertosEntradaRepetidos[i])+
                        numeroVuelos[i]);
            }else{
                numVuelosAeropuerto.put(aeropuertosEntradaRepetidos[i], numeroVuelos[i]);
            }
            if(solucion.get(i)){
                if(numVuelosAeropuertoConexiones.get(aeropuertosEntradaRepetidos[i])!=null){
                    numVuelosAeropuertoConexiones.put(aeropuertosEntradaRepetidos[i],
                            numVuelosAeropuertoConexiones.get(aeropuertosEntradaRepetidos[i])+numeroVuelos[i]);
                }else{
                    numVuelosAeropuertoConexiones.put(aeropuertosEntradaRepetidos[i], numeroVuelos[i]);
                }
            }
        }
        i=0;
        for (String key:numVuelosAeropuerto.keySet()) {
            if(numVuelosAeropuertoConexiones.get(key)!=null){
                porcentajePerdido.add((double)numVuelosAeropuertoConexiones.get(key)/numVuelosAeropuerto.get(key));
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
        /*
        if(porcentajePerdidoDesviacionMedia>maxPorcentajeDesviacionMediaIngresosDestinos) {
            return penalizacionRestriccion;
        }
        return 0;
         */
    }
    //TODO: Pasar a función auxiliar el código compartido entre calculoConectividadDestinos y calculoHomogeneidadIngresosTurismoDestinos
    //TODO: Cambiar como funciona la función de conectividad
    /**
     * Comprueba que la conectividad perdida en los destinos no sea mayor que un porcentaje, implementada como una restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static int calculoConectividadDestinos(){
        /*
        int[] numeroVuelos = new int[numDimensions];
        String[] aeropuertosEntradaRepetidos = new String[numDimensions];
        java.util.Map<String, Integer> conectividadAeropuerto = new java.util.HashMap<>();
        java.util.Map<String, Integer> numVuelosAeropuerto = new java.util.HashMap<>();
        java.util.Map<String, Integer> numVuelosAeropuertoConexiones = new java.util.HashMap<>();
        List<Float> porcentajePerdido = new ArrayList<>();
        int conectividadTotal=0;
        float sumaConectividadConexiones=0.0f;
        float porcentajeConectividadPerdida;
        int i;
        for(i=0;i<numDimensions;i++){
            if(numVuelosAeropuerto.get(aeropuertosEntradaRepetidos[i])!=null){
                numVuelosAeropuerto.put(aeropuertosEntradaRepetidos[i], numVuelosAeropuerto.get(aeropuertosEntradaRepetidos[i])+
                        numeroVuelos[i]);
            }else{
                numVuelosAeropuerto.put(aeropuertosEntradaRepetidos[i], numeroVuelos[i]);
            }
            if(solucion[i]){
                if(numVuelosAeropuertoConexiones.get(aeropuertosEntradaRepetidos[i])!=null){
                    numVuelosAeropuertoConexiones.put(aeropuertosEntradaRepetidos[i],
                            numVuelosAeropuertoConexiones.get(aeropuertosEntradaRepetidos[i])+numeroVuelos[i]);
                }else{
                    numVuelosAeropuertoConexiones.put(aeropuertosEntradaRepetidos[i], numeroVuelos[i]);
                }
            }
        }
        i=0;
        for (String key:numVuelosAeropuerto.keySet()) {
            if(numVuelosAeropuertoConexiones.get(key)!=null){
                porcentajePerdido.add(conectividadAeropuerto.get(i)*
                        (float)numVuelosAeropuertoConexiones.get(key)/numVuelosAeropuerto.get(key));
            }else{
                porcentajePerdido.add(0.0f);
            }
            i++;
        }
        for(i=0;i<porcentajePerdido.size();i++){
            sumaConectividadConexiones=sumaConectividadConexiones+porcentajePerdido.get(i);
            conectividadTotal=conectividadTotal+conectividadAeropuerto.get(i);
        }
        porcentajeConectividadPerdida=1-sumaConectividadConexiones/conectividadTotal;
        if(porcentajeConectividadPerdida>maxPorcentajeConectividadPerdida) {
            return penalizacionRestriccion;
        }
        */
        return 0;
    }

    //TODO:Implementando carga de datos en CSV, más adelante funcionará con llamadas a Neo4j

    /**
     * Función que carga los aeropuertos de entrada, estos valores no se repiten debido a que no hay duplicados
     * en el csv. Se da por hecho que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private static void cargaAeropuertosEntrada(){
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
        //System.out.println("Aeropuertos de entrada"+nombreAeropuertosEntradaEspanya);
    }

    /**
     * Función que carga los aeropuertos de salida, estos valores no se repiten debido a que no hay duplicados en el
     * csv. Se da por hecho que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private static void cargaAeropuertosSalida(){
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
        //System.out.println("Aeropuertos de salida"+nombreAeropuertosSalida);
    }

    /**
     * Función que carga el código de las compañías aéreas, estos valores no se repiten debido a que no hay duplicados
     * en el csv. Se da por hecho que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private static void cargaCompanyias(){
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
        //System.out.println("Compañías aéreas"+nombreCompanyias);
    }

    /**
     * Función que carga todos los vuelos que se van a realizar ese día no se repiten. Se da por hecho que la primera
     * línea no contiene datos y por tanto nos la saltamos.
     */
    //TODO: Comprobar el .contains funciona correctamente
    private static void cargaVuelos(){
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
        /*System.out.println("Conexiones(entrada,salida):");
        for(int i=0;i<listaConexiones.size();i++){
            System.out.print("("+listaConexiones.get(i).entrada+","+listaConexiones.get(i).salida+") ");
        }
        System.out.println("Número de conexiones: "+listaConexiones.size());
         */
        System.out.println("Número de conexiones: "+listaConexiones.size());
    }

    /**
     * Función que carga los valores epidemiologicos en listaRiesgosEspanyoles. Se da por hecho que la primera línea no
     * contiene datos y por tanto nos la saltamos.
     */
    private static void cargaDatosSIR(){
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
        /*
        //System.out.println("Número infectados de las conexiones: "+listaRiesgosEspanyoles);
        System.out.println("Número de conexiones: "+listaRiesgosEspanyoles.size());
        Conexion bruh = new Conexion("ACE","DUB");
        System.out.println("Valor conexión(ACE,DUB): "+listaRiesgosEspanyoles.get(bruh));
         */

    }

    /**
     * Función que carga el número de pasajeros en una conexión. Se da por hecho que la primera línea no contiene datos
     * y por tanto nos la saltamos.
     */
    private static void cargaListaPasajeros(){
        String ubicacionArchivo = "datos/pasajerosPorVuelo.csv";
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
        /*
        //System.out.println("Número pasajeros en las conexiones: "+listaPasajeros);
        System.out.println("Número de conexiones: "+listaPasajeros.size());
        Conexion bruh = new Conexion("ACE","LPA");
        System.out.println("Número pasajeros en conexión(ACE,LPA): "+listaPasajeros.get(bruh));
         */
    }

    /**
     * Función que carga el número de pasajeros en las conexiones distinguiendo entre compañías aéreas. Se da por hecho
     * que la primera línea no contiene datos y por tanto nos la saltamos.
     */
    private static void cargaListaPasajerosCompanyia(){
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
        /*
        System.out.println("Número pasajeros en las conexiones: "+listaPasajerosCompanyia);
        System.out.println("Número de conexiones: "+listaPasajerosCompanyia.size());
        ConexionyCompanyia bruh = new ConexionyCompanyia(new Conexion("ACE","LPA"), "CNF");
        System.out.println("Número pasajeros en conexión(ACE,LPA): "+listaPasajerosCompanyia.get(bruh));
         */
    }

    /**
     * Función que carga el dinero por turismo a cada una de las conexiones. Se da por hecho que la primera línea no
     * contiene datos y por tanto nos la saltamos.
     */
    private static void cargaDineroVuelos() {
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
        /*
        System.out.println("Dinero ganado en las conexiones: "+listaDineroTurismoConexion);
        System.out.println("Número de conexiones: "+listaDineroTurismoConexion.size());
        Conexion bruh = new Conexion("ACE","LPA");
        System.out.println("Valor conexión(ACE,LPA): "+listaDineroTurismoConexion.get(bruh));
         */
    }

    /**
     * Función que carga la conectividad de los aeropuertos de salida. Se da por hecho que la primera línea no contiene
     * datos y por tanto nos la saltamos.
     */
    private static void cargaConectividad(){
        String ubicacionArchivo = "datos/conectividad_por_aeropuerto.csv";
        Map<String,Integer> numeroVuelosHaciaEspanya = new HashMap<>();
        Map<String,Integer> numeroVuelosHaciaFuera = new HashMap<>();
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
                    numeroVuelosHaciaFuera.put(values[1], Integer.parseInt(values[3]));
                    listaConectividadAeropuerto.put(values[1], Double.parseDouble(values[4]));
                }
                for (int i = 0; i < nombreAeropuertosSalida.size(); i++) {
                    String aeropuerto = nombreAeropuertosSalida.get(i);
                    if(listaConectividadAeropuerto.get(aeropuerto)!=null){
                        listaConectividadAeropuerto.put(aeropuerto, listaConectividadAeropuerto.get(aeropuerto) *
                                numeroVuelosHaciaEspanya.get(aeropuerto) / numeroVuelosHaciaFuera.get(aeropuerto));
                    }else{
                        listaConectividadAeropuerto.put(aeropuerto, 0.0);
                    }
                    /*
                    System.out.println(aeropuerto);
                    System.out.println(listaConectividadAeropuerto.get(aeropuerto));
                     */
                }
            }
        }catch (Exception e){
            System.out.println("Excepción cargaConectividad: "+e);
        }
    }

    //TODO: Comprobar si tengo que añadir ListaConexionesPorAeropuertoEspanyol o no, si se añaden solo número de vuelos
    //TODO:Modificar para que cargue todos los datos llamando a este método
    public static void initCriterios(){
        cargaAeropuertosEntrada();
        cargaAeropuertosSalida();
        cargaCompanyias();
        cargaVuelos();
        cargaDatosSIR();
        cargaListaPasajeros();
        cargaListaPasajerosCompanyia();
        cargaDineroVuelos();
        cargaConectividad();
        //TODO: Quitar esto, es solo para hacer pruebas
        for(int i=0;i<listaConexiones.size();i++){
            solucion.add(true);
        }
        System.out.println(calculoRiesgoImportado());
        System.out.println(calculoEconomicoPerdidaPasajeros());
        System.out.println(calculoPerdidaIngresosDestinos());
        System.out.println(calculoHomogeneidadPasajerosAerolineas());
    }
}
