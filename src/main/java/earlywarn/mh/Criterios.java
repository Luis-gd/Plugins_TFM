package earlywarn.mh;

import java.util.ArrayList;
import java.util.List;

public class Criterios{
    //Número de conexiones con las que trabajamos
    private static int numDimensions=0;
    //Número de compañias aéreas con las que trabajamos
    private static int numCompanyias=0;
    //TODO: Añadir la opción de meter las restricciones por parámetros
    //TODO: Más adelante modificar el código para que se puedan introducir estos valores por parámetros
    //Restricción del impacto económico de los pasajeros perdidos, en porcentaje
    private final static float maxPorcentajePasajerosPerdidos=0.2f;
    //Restricción de la homogeneidad en el porcentaje de pasajeros que pierden las aerolíneas
    private final static float maxPorcentajeDesviacionMediaPasajerosPerdidosPorCompanyia=0.2f;
    //Restricción de la homogeneidad en el porcentaje de pérdida de ingresos por turismo en los destinos.
    private final static float maxPorcentajeDesviacionMediaIngresosDestinos=0.2f;
    //Restricción sobre la conectividad perdida en los destinos.
    private final static float maxPorcentajeConectividadPerdida=0.2f;
    //Restricción del porcentaje de pérdida de ingresos por turismo en los destinos
    private final static float maxPorcentajeDineroPerdidoRegion = 0.2f;
    //TODO: Calcular un valor correcto para la penalización
    //Valor que se añade a la función objetivo cuando una solución no cumple una restricción
    private final static int penalizacionRestriccion=1000000;
    //La solución con la que trabajamos, se modifica en evaluate fitness
    private static boolean[] solucion;

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
    /**
     * Método para inicializar el número de dimensiones, hay que llamar a esta función primero para
     * empezar a trabajar
     * @param numDimensions2 El número de dimensiones que tendrá el problema, se equivale al número de conexiones
     *                  con las que trabajamos
     * @param numCompanyias2 El número de compañías aéreas que tenemos
     */
    public static void initCriterios(int numDimensions2,int numCompanyias2){
        numDimensions=numDimensions2;
        numCompanyias=numCompanyias2;
    }
}
