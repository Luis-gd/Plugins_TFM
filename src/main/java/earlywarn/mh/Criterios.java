package earlywarn.mh;

public class Criterios{
    //Número de conexiones con las que trabajamos
    private static int numDimensions=0;
    //Número de compañias aéreas con las que trabajamos
    private static int numCompanyias=0;
    //TODO: Añadir la opción de meter las restricciones por parámetros
    //Restricción del impacto económico de los pasajeros perdidos, en porcentaje
    private static float maxPorcentajePasajerosPerdidos=0.2f;
    //Restricción del porcentaje de pérdida de ingresos por turismo en los destinos
    private static float maxPorcentajeDineroPerdidoRegion = 0.2f;
    //TODO: Calcular un valor correcto para la penalización
    //Valor que se añade a la función objetivo cuando una solución no cumple una restricción
    private static int penalizacionRestriccion=1000;
    //La solución con la que trabajamos, se modifica en evaluate fitness
    private static boolean[] solucion;
    //TODO: Hacer la función evaluteFitness
    //TODO: Hay que cargar todos los datos, ahora mismo los estoy dando por hecho
    /**
     * Calcula el fitness del riesgo importado dada una solución
     * @return Devuelve el riesgo de las conexiones abiertas
     */
    //TODO: Comprobar las escalas de riesgo
    //TODO: Comprobar si debería estar en porcentaje para normalizarlos distintos objetivos
    private static float calculoRiesgoImportado(){
        float[] riesgoConexion=new float[numDimensions];
        float riesgo=0;
        for(int i=0;i<numDimensions;i++){
            if(solucion[i]==true){
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
            if(solucion[i]==true){
                totalPasajerosConexiones=totalPasajerosConexiones+pasajerosConexion[i];
            }
        }
        float porcentajePerdido=1-totalPasajerosConexiones/totalPasajeros;
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
            if(solucion[i]==true){
                totalIngresosConexion=totalIngresosConexion+ingresosDestinoConexion[i];
            }
        }
        float porcentajePerdido=1-totalIngresosConexion/totalIngresos;
        if(porcentajePerdido>maxPorcentajeDineroPerdidoRegion){
            return penalizacionRestriccion;
        }
        return 0;
    }
    //TODO: Arreglar la condición, liandola mirando todas las compañías
    private static int calculoHomogeneidadPasajerosAerolineas(){
        int[][] pasajerosConexion=new int[numDimensions][numCompanyias];
        int totalPasajeros[]=new int[numCompanyias];
        int totalPasajerosConexiones[]=new int[numCompanyias];
        for(int j=0;j<numCompanyias;j++){
            for(int i=0;i<numDimensions;i++){
                totalPasajeros[j]=totalPasajeros[j]+pasajerosConexion[i][j];
                if(solucion[i]==true){
                    totalPasajerosConexiones[j]=totalPasajerosConexiones[j]+pasajerosConexion[i][j];
                }
            }
        }
        float porcentajePerdido=1-totalPasajerosConexiones/totalPasajeros;
        if(porcentajePerdido>maxPorcentajePasajerosPerdidos){
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
