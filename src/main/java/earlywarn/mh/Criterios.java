package earlywarn.mh;

import java.util.ArrayList;
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
    //La solución con la que trabajamos, se modifica en evaluate fitness
    static boolean[] solucion;
    //Contiene el nombre de los aeropuertos de entrada, en principio serán los de España
    static List<String> nombreAeropuertosEntradaEspanya = new ArrayList<>();
    //Contiene el nombre de los aeropuertos de salida
    static List<String> nombreAeropuertosSalida = new ArrayList<>();
    //Contiene el nombre de las compañías aéreas
    static List<String> nombreCompanyias = new ArrayList<>();
    //Listado de vuelos que hay entre los aeropuertos, puede haber más de un vuelo entre los mismos aeropuertos
    static List<conexion> listaVuelos = new ArrayList<>();
    //Los caracteres que se utilizan para separar los CSV
    static String COMMA_DELIMITER=",";

    public float[] riesgoConexion;

    /**
     * Calcula el fitness de una partícula, comprueba todos los objetivos/restricciones
     * @param positions La posición de una partícula
     * @return Devuelve su fitness
     */
    public static double evaluateFitness(boolean[] positions) {
        solucion=positions;
        return calculoRiesgoImportado()+calculoEconomicoPerdidaPasajeros()+calculoPerdidaIngresosDestinos()+
                calculoHomogeneidadPasajerosAerolineas()+calculoHomogeneidadIngresosTurismoDestinos()+
                calculoConectividadDestinos();
    }
    //TODO: Hay que cargar todos los datos, ahora mismo los estoy dando por hecho, cada dimensión una conexión
    /**
     * Calcula el fitness del riesgo importado dada una solución
     * @return Devuelve el riesgo de las conexiones abiertas
     */
    //TODO: Comprobar las escalas de riesgo
    //TODO: Comprobar si debería estar en porcentaje para normalizar los distintos objetivos
    private static float calculoRiesgoImportado(){
        float[] riesgoConexion=new float[numDimensions];
        float riesgo=0;
        for(int i=0;i<numDimensions;i++){
            if(solucion[i]){
                riesgo=riesgo+riesgoConexion[i];
            }
        }
        return riesgo;
    }

    /**
     * Impacto económico para las compañías derivado de la pérdida de pasajeros, está
     * implementado como una restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static int calculoEconomicoPerdidaPasajeros(){
        int[] pasajerosConexion=new int[numDimensions];
        int totalPasajeros=0;
        int totalPasajerosConexiones=0;
        for(int i=0;i<numDimensions;i++){
            totalPasajeros=totalPasajeros+pasajerosConexion[i];
            if(solucion[i]){
                totalPasajerosConexiones=totalPasajerosConexiones+pasajerosConexion[i];
            }
        }
        float porcentajePerdido=1-(float)totalPasajerosConexiones/totalPasajeros;
        if(porcentajePerdido>maxPorcentajePasajerosPerdidos){
            return penalizacionRestriccion;
        }
        return 0;
    }

    /**
     * Porcentaje de pérdida de ingresos por turismo en los destinos, implementado como una restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static int calculoPerdidaIngresosDestinos(){
        int[] ingresosDestinoConexion=new int[numDimensions];
        int totalIngresos=0;
        int totalIngresosConexion=0;
        for(int i=0;i<numDimensions;i++){
            totalIngresos=totalIngresos+ingresosDestinoConexion[i];
            if(solucion[i]){
                totalIngresosConexion=totalIngresosConexion+ingresosDestinoConexion[i];
            }
        }
        float porcentajePerdido=1-(float)totalIngresosConexion/totalIngresos;
        if(porcentajePerdido>maxPorcentajeDineroPerdidoRegion){
            return penalizacionRestriccion;
        }
        return 0;
    }

    /**
     * Se comprueba la homogeneidad de perdida de pasajeros entre distintas compañías aéreas, para ello hacemos la desviación
     * media de los pasajeros perdidos por cada compañía y vemos si esta debajo de un porcentaje, implementado como restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static int calculoHomogeneidadPasajerosAerolineas(){
        int i;
        int[][] pasajerosConexion=new int[numDimensions][numCompanyias];
        int[] totalPasajeros=new int[numCompanyias];
        int[] totalPasajerosConexiones=new int[numCompanyias];
        float[] porcentajePerdido = new float[numCompanyias];
        float porcentajePerdidoMedia = 0.0f;
        float porcentajePerdidoDesviacionMedia = 0.0f;
        for(int j=0;j<numCompanyias;j++){
            for(i=0;i<numDimensions;i++){
                totalPasajeros[j]=totalPasajeros[j]+pasajerosConexion[i][j];
                if(solucion[i]){
                    totalPasajerosConexiones[j]=totalPasajerosConexiones[j]+pasajerosConexion[i][j];
                }
            }
            porcentajePerdido[j]=1-(float)totalPasajerosConexiones[j]/totalPasajeros[j];
            porcentajePerdidoMedia = porcentajePerdidoMedia + porcentajePerdido[j];
        }
        porcentajePerdidoMedia = porcentajePerdidoMedia / numCompanyias;
        for(i=0;i<numCompanyias;i++){
            porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia +
                                                Math.abs(porcentajePerdido[i]-porcentajePerdidoMedia);
        }
        porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia / numCompanyias;
        if(porcentajePerdidoDesviacionMedia>maxPorcentajeDesviacionMediaPasajerosPerdidosPorCompanyia) {
            return penalizacionRestriccion;
        }
        return 0;
    }

    /**
     * Se comprueba la homogeneidad de perdida de ingresos por turismo entre los diferentes destinos, para ello hacemos la
     * desviación media de los pasajeros perdidos por cada compañía y vemos si esta debajo de un porcentaje, implementado
     * como restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static int calculoHomogeneidadIngresosTurismoDestinos(){
        int[] numeroVuelos = new int[numDimensions];
        String[] aeropuertosEntradaRepetidos = new String[numDimensions];
        java.util.Map<String, Integer> numVuelosAeropuerto = new java.util.HashMap<>();
        java.util.Map<String, Integer> numVuelosAeropuertoConexiones = new java.util.HashMap<>();
        List<Float> porcentajePerdido = new ArrayList<>();
        float mediaPorcentajeVuelosPerdidos=0.0f;
        float porcentajePerdidoDesviacionMedia = 0.0f;
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
                porcentajePerdido.add((float)numVuelosAeropuertoConexiones.get(key)/numVuelosAeropuerto.get(key));
                mediaPorcentajeVuelosPerdidos=mediaPorcentajeVuelosPerdidos+porcentajePerdido.get(i);
            }else{
                porcentajePerdido.add(0.0f);
            }
            i++;
        }
        mediaPorcentajeVuelosPerdidos = mediaPorcentajeVuelosPerdidos / porcentajePerdido.size();
        for(i=0;i<porcentajePerdido.size();i++){
            porcentajePerdidoDesviacionMedia=porcentajePerdidoDesviacionMedia+
                                                Math.abs(porcentajePerdido.get(i)-mediaPorcentajeVuelosPerdidos);
        }
        porcentajePerdidoDesviacionMedia = porcentajePerdidoDesviacionMedia / porcentajePerdido.size();
        if(porcentajePerdidoDesviacionMedia>maxPorcentajeDesviacionMediaIngresosDestinos) {
            return penalizacionRestriccion;
        }
        return 0;
    }
    //TODO: Pasar a función auxiliar el código compartido entre calculoConectividadDestinos y calculoHomogeneidadIngresosTurismoDestinos
    /**
     * Comprueba que la conectividad perdida en los destinos no sea mayor que un porcentaje, implementada como una restricción
     * @return Devuelve el valor de penalizaciónRestriccion si no se cumple la restricción, sino 0
     */
    private static int calculoConectividadDestinos(){
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
        return 0;
    }
    //TODO:Implementando carga de datos en CSV, más adelante funcionará con llamadas a Neo4j

    /**
     * Función que carga los aeropuertos de entrada
     */
    private static void cargaAeropuertosEntrada(){
        String ubicacionArchivo = "datos/aeropuertos_entradas.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                nombreAeropuertosEntradaEspanya.add(values[0]);
            }
        }catch (Exception e){
            System.out.println("No se encuentra el archivo en "+ubicacionArchivo);
        }
    }

    /**
     * Función que carga los aeropuertos de salida
     */
    private static void cargaAeropuertosSalida(){
        String ubicacionArchivo = "datos/aeropuertos_salida.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                nombreAeropuertosSalida.add(values[0]);
            }
        }catch (Exception e){
            System.out.println("No se encuentra el archivo en "+ubicacionArchivo);
        }
    }

    /**
     * Función que carga el código de las compañías aéreas
     */
    private static void cargaCompanyias(){
        String ubicacionArchivo = "datos/companyias.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                nombreCompanyias.add(values[0]);
            }
        }catch (Exception e){
            System.out.println("No se encuentra el archivo en "+ubicacionArchivo);
        }
    }

    /**
     * Función que carga todos los vuelos que se van a realizar ese día
     */
    private static void cargaVuelos(){
        String ubicacionArchivo = "datos/sir.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(ubicacionArchivo))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                conexion anyadir = new conexion(values[1],values[2]);
                listaVuelos.add(anyadir);
            }
        }catch (Exception e){
            System.out.println("No se encuentra el archivo en "+ubicacionArchivo);
        }
    }

    //TODO:Modificar para que cargue todos los datos llamando a este método
    public static void initCriterios(){
        cargaAeropuertosEntrada();
        cargaAeropuertosSalida();
        cargaCompanyias();
        cargaVuelos();
    }
}
