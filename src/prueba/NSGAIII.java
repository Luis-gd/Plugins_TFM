package prueba;

import java.util.*;


public class NSGAIII {
    // El algoritmo solo minimiza, las máximizaciones se hacen cambiando el signo a la función objetivo

    // Inicialización aleatoria del tamanyo de la poblacion
    public static int m = 6; //M = NumeroObjetivos
    public static int p = 4; //Numero de divisiones usado para distribuir direcciones de referencia en la frontera
    public static int h = factorial(m+p-1)/(factorial(p)*factorial(m-1)); //Numero de direcciones de referencia

    private static int funcN (){
        int aux = h+1;
        while (aux%4 != 0 ){ aux++;}
        return aux;
    }
    public static int n = funcN() ; //Tamanyo de población

    static Double probMutacion = 0.1;
    static Map<List<String>, Double> riesgos; // Riesgos importados por conexion (salida, entrada)

    static Map<String, Double> conectividadesAeropuertosOrigen; //Conjunto de aeropuertos origen

    static Map<List<String>, List<Double>> conectividadYvuelosExterioresAEspanya;//entrada y salida de una conexion,
    //los vuelos totales que tiene el aeropuerto de salida, la cantidad de esos vuelos que vienen a españa y la conectividad del aeropuerto de salida

    static Map<Map<List<String>, Integer>, List<Double>> valoresPrimeraFronteraDePareto; // Los valores de las funciones objetivo de la primera frontera de pareto

    static Map<List<String>, Integer> conexiones; // Estado de las conexiones; (salida, entrada)

    static Map<String, Set<String>> listaConexionesPorAeropuertoEspanyol; // Aeropuerto español y lista de conexiones del mismo

    static Map<List<String>, Integer> pasajeros; // Numero de pasajeros por linea (salida, entrada)

    static Map<List<String>, Integer> pasajerosCompanyias; // Numero de pasajeros por linea y companyia (salida, entrada, companyia)

    static Map<List<String>, Double> dineroMedio; // Dinero medio por linea (salida, entrada)

    static List<String> companyias; // Lista de companyias
    static Map<List<String>, Integer> vuelos; // Vuelos entre aeropuertos (salida, entrada)

    static List<String> AeropuertosEspanyoles; // Lista de aeropuertos espanyoles

    static List<Map<List<String>, Integer>> padres = new ArrayList<>(); // Lista de los padres

    static List<Map<List<String>, Integer>> hijos = new ArrayList<>(); // Lista de los hijos

    static List<List<Map<List<String>, Integer>>> fronterasDePareto; // Lista con las F de pareto

    static Map<List<Double>, List<Double>> distanciasAsignacionSolucionesConPuntosReferencia; // distancias entre los puntos de referencia y las soluciones
    static Map<List<Double>, List<Map<List<String>, Integer>>> asignacionSolucionesConPuntosReferencia; // asignación de las soluciones a los puntos de ref

    static Map<Map<List<String>, Integer> ,List<Double>> valoresFuncionesObjetivo;


    // Funciones del algoritmo
    public static int factorial(int n)
    {
        if (n == 0)
            return 1;

        return n*factorial(n-1);
    }

    public static void poblacionInicial(){
        int numeroIndividuos = 0;
        while (numeroIndividuos < n){
            Map<List<String>, Integer> padre = new HashMap<>();
            for (List<String> conexion:conexiones.keySet()) {
                int valor;
                if (Math.random() < 0.5){
                    valor = 0;
                }else{
                    valor = 1;
                }
                padre.put(conexion, valor);
            }
            padres.add(padre);
            numeroIndividuos ++;
        }

    }

    public static void cruce( Map<List<String>, Integer> padre1,Map<List<String>, Integer> padre2){
        List<List<String>> keysPadres = new ArrayList<>(padre1.keySet());
        Random r = new Random();
        int i = r.nextInt(keysPadres.size());
        Map<List<String>, Integer> hijo12 = new HashMap<>();
        Map<List<String>, Integer> hijo21 = new HashMap<>();
        for (int j = 0; j < keysPadres.size(); j++){
            if (j < i){
                hijo12.put(keysPadres.get(j), padre1.get(keysPadres.get(j)));
                hijo21.put(keysPadres.get(j), padre2.get(keysPadres.get(j)));
            }else{
                hijo12.put(keysPadres.get(j), padre2.get(keysPadres.get(j)));
                hijo21.put(keysPadres.get(j), padre1.get(keysPadres.get(j)));
            }
        }
        hijos.add(hijo12);
        hijos.add(hijo21);
    }

    public static void mutacion(Map<List<String>, Integer> individuo){
        Random r = new Random();
        List<List<String>> keys= new ArrayList<>(individuo.keySet());
        int i = r.nextInt(keys.size());
        Integer valor = individuo.get(keys.get(i));
        if (valor == 0){
            individuo.put(keys.get(i), 1);
        }else{
            individuo.put(keys.get(i), 0);
        }
    }
    public static Integer dominancia(List<Double> Vcandidato, List<Double> VpuntoF){
        Integer solucion =0;
        for (int j = 0; j < m; j++){
            double candidato =  Vcandidato.get(j);
            double actual = VpuntoF.get(j);
            if (candidato < actual ){
                solucion++;
            }
        }
        return solucion;
    }

    // B. Calculo de los puntos de referencia en el hiperplano
    public static List<List<Double>> calculoPuntosReferencia(){
        // Numero de puntos igual a h que ya está calculado
        List<List<Double>> listaPuntosRefecia = new ArrayList<>();
        Double sigma = 1/(double) p;
        for (int i = 0; i <= p; i++){
            List<Double> punto = new ArrayList<>();
            punto.add(i*sigma);
            listaPuntosRefecia.add(punto);
        }
        while (listaPuntosRefecia.get(0).size()<m-1) {
            List<List<Double>> listaPuntosRefeciaActualizada = new ArrayList<>();
            for (List<Double> punto: listaPuntosRefecia) {
                Double sum = 0.0;
                for (Double val: punto) {
                    sum += val/sigma;
                }
                Double valorMax = p-sum;
                for (int i = 0; i <= valorMax; i++){
                    List<Double> puntoMod = new ArrayList<>(punto);
                    puntoMod.add(i*sigma);
                    listaPuntosRefeciaActualizada.add(puntoMod);
                }
            }
            listaPuntosRefecia = new ArrayList<>(listaPuntosRefeciaActualizada);
        }
        for (List<Double> punto: listaPuntosRefecia) {
            Double sum = 0.0;
            for (Double val: punto) {
                sum += val;
            }
            punto.add(1-sum);
        }

        return listaPuntosRefecia;
    }


    // C. Normalizacion adaptativa de la población.
    public static Map<Map<List<String>, Integer> ,List<Double>> valoresTrasladados(){
        List<Map<List<String>, Integer>> F1 = fronterasDePareto.get(0);
        List<Double> puntoIdeal = new ArrayList<>();
        for (Map<List<String>, Integer> individuo: F1) {
            if (puntoIdeal.size() == 0) {
                puntoIdeal.addAll(valoresFuncionesObjetivo.get(individuo));
            } else {
                for (int i = 0; i < puntoIdeal.size(); i++) {
                    if (puntoIdeal.get(i) > valoresFuncionesObjetivo.get(individuo).get(i)) {
                        puntoIdeal.set(i, valoresFuncionesObjetivo.get(individuo).get(i));
                    }
                }
            }
        }

        List<Map<List<String>, Integer>> Fl = fronterasDePareto.get(fronterasDePareto.size()-1);
        List<Double> peorPunto = new ArrayList<>();
        for (Map<List<String>, Integer> individuo: Fl) {
            if (peorPunto.size()==0){
                peorPunto.addAll(valoresFuncionesObjetivo.get(individuo));
            }else{
                for(int i =0; i < puntoIdeal.size(); i++){
                    if (peorPunto.get(i) < valoresFuncionesObjetivo.get(individuo).get(i)){
                        peorPunto.set(i, valoresFuncionesObjetivo.get(individuo).get(i));
                    }
                }
            }
        }

        Map<Map<List<String>, Integer> ,List<Double>> valoresFuncionesObjetivoTrasladados =
                new HashMap<>(valoresFuncionesObjetivo);
        valoresFuncionesObjetivoTrasladados.forEach((k,v)-> {
            for (int i = 0; i < v.size(); i++) {
                v.set(i, (v.get(i) - puntoIdeal.get(i))/peorPunto.get(i));
            }
        });

        return valoresFuncionesObjetivoTrasladados;
    }

    // D. Operador de asociación
    public static void asociacion(
            List<List<Double>> listaPuntosRefecia,
            Map<Map<List<String>, Integer> ,List<Double>> valoresFuncionesObjetivoTrasladados){
        Map<List<Double>, List<Double>> AUXdistanciasAsignacionSolucionesConPuntosReferencia = new HashMap<>();
        Map<List<Double>, List<Map<List<String>, Integer>>> AUXasignacionSolucionesConPuntosReferencia = new HashMap<>();
        valoresFuncionesObjetivoTrasladados.forEach((k,v)-> {
            List<Double> distancias = new ArrayList<>();
            for (List<Double> punto: listaPuntosRefecia) {
                List<Double> nuevoVector = new ArrayList<>();
                Double modV1 = 0.0;
                Double modD = 0.0;
                for (int i = 0; i < v.size(); i++) {
                    nuevoVector.add(punto.get(i) - v.get(i));
                    modV1 += (punto.get(i) - v.get(i))*(punto.get(i) - v.get(i));
                    modD += punto.get(i)*punto.get(i);
                }
                modV1 = Math.sqrt(modV1);
                modD = Math.sqrt(modD);
                Double productoDot = 0.0;
                for (int i = 0; i < punto.size(); i++) {
                    productoDot += punto.get(i)*nuevoVector.get(i);
                }
                Double coseno = productoDot/(modV1*modD);
                Double seno = Math.sqrt(1-coseno*coseno);
                Double distancia = modV1*modD*seno/modD;
                distancias.add(distancia);
            }
            Double distMin = distancias.stream().min(Double::compare).get();
            int indicePuntoReferencia = distancias.indexOf(distMin);
            List<Double> puntoRefMenorDistancia = listaPuntosRefecia.get(indicePuntoReferencia);
            if (!AUXasignacionSolucionesConPuntosReferencia.keySet().contains(puntoRefMenorDistancia)){
                AUXasignacionSolucionesConPuntosReferencia.put(puntoRefMenorDistancia,new ArrayList<>());
                AUXdistanciasAsignacionSolucionesConPuntosReferencia.put(puntoRefMenorDistancia,new ArrayList<>());

            }
            AUXasignacionSolucionesConPuntosReferencia.get(puntoRefMenorDistancia).add(k);
            AUXdistanciasAsignacionSolucionesConPuntosReferencia.get(puntoRefMenorDistancia).add(distMin);
        });
        asignacionSolucionesConPuntosReferencia = AUXasignacionSolucionesConPuntosReferencia;
        distanciasAsignacionSolucionesConPuntosReferencia = AUXdistanciasAsignacionSolucionesConPuntosReferencia;
    }

    //E. Niche-Preservation Operation

    public static List<List<Map<List<String>, Integer>>> nichePreservarion(
            List<Map<List<String>, Integer>> Fl, int sobrante ){
        Map<List<Double>, List<Map<List<String>, Integer>>> puntosAsignadosFl = new HashMap<>();
        Map<List<Double>, List<Map<List<String>, Integer>>> puntosAsignadosNoFl = new HashMap<>();
        for (List<Double> puntoRef:asignacionSolucionesConPuntosReferencia.keySet()) {
            for (int i = 0; i < asignacionSolucionesConPuntosReferencia.get(puntoRef).size(); i++) {
                if (Fl.contains(asignacionSolucionesConPuntosReferencia.get(puntoRef).get(i))) {
                    if (!puntosAsignadosFl.keySet().contains(puntoRef)) {
                        puntosAsignadosFl.put(puntoRef, new ArrayList<>());
                    }
                    List<Map<List<String>, Integer>> aux = puntosAsignadosFl.get(puntoRef);
                    aux.add(asignacionSolucionesConPuntosReferencia.get(puntoRef).get(i));
                    puntosAsignadosFl.put(puntoRef, aux);
                } else {
                    if (!puntosAsignadosNoFl.keySet().contains(puntoRef)) {
                        puntosAsignadosNoFl.put(puntoRef, new ArrayList<>());
                    }
                    List<Map<List<String>, Integer>> aux2 = new ArrayList<>(puntosAsignadosNoFl.get(puntoRef));
                    aux2.add(asignacionSolucionesConPuntosReferencia.get(puntoRef).get(i));
                    puntosAsignadosNoFl.put(puntoRef, aux2);

                }
            }
        }
        int espacioPorCubrir = sobrante;
        List<Map<List<String>, Integer>> FlFinal = new ArrayList<>();
        while (espacioPorCubrir > 0) {
            Integer minimo = null;
            List<Double> puntoMenorAsignacion = new ArrayList<>();
            for (List<Double> puntoRef : puntosAsignadosNoFl.keySet()) {
                if (minimo == null) {
                    minimo = puntosAsignadosNoFl.get(puntoRef).size();
                    puntoMenorAsignacion = puntoRef;
                }
                if (puntosAsignadosNoFl.get(puntoRef).size() < minimo) {
                    minimo = puntosAsignadosNoFl.get(puntoRef).size();
                    puntoMenorAsignacion = puntoRef;
                }
            }
            if (puntosAsignadosFl.keySet().contains(puntoMenorAsignacion)){
                Map<List<String>, Integer> menorpuntoEspSoluciones = null;
                Double menorValorDeAsignacion = null;
                for (int i =0; i< distanciasAsignacionSolucionesConPuntosReferencia.get(puntoMenorAsignacion).size(); i++){
                    if (menorValorDeAsignacion == null) {
                        menorValorDeAsignacion = distanciasAsignacionSolucionesConPuntosReferencia.get(puntoMenorAsignacion).get(i);
                        menorpuntoEspSoluciones = asignacionSolucionesConPuntosReferencia.get(puntoMenorAsignacion).get(i);
                    }
                    if (distanciasAsignacionSolucionesConPuntosReferencia.get(puntoMenorAsignacion).get(i) < menorValorDeAsignacion) {
                        menorValorDeAsignacion = distanciasAsignacionSolucionesConPuntosReferencia.get(puntoMenorAsignacion).get(i);
                        menorpuntoEspSoluciones = asignacionSolucionesConPuntosReferencia.get(puntoMenorAsignacion).get(i);
                    }
                }
                FlFinal.add(menorpuntoEspSoluciones);
                espacioPorCubrir --;
            }
        }
        fronterasDePareto.remove(Fl);
        fronterasDePareto.add(FlFinal);
        return fronterasDePareto;
    }

    public static List<Map<List<String>, Integer>> nichePreservarionUnaSolaFrontera(
            List<Map<List<String>, Integer>> Fl){
        Map<List<Double>, Integer> cantidadPuntosAsignados = new HashMap<>();
        List<Map<List<String>, Integer>> fronteraDefinitiva = new ArrayList<>(Fl);
        Map<List<Double>, List<Double>> distanciasAux = new HashMap<>(distanciasAsignacionSolucionesConPuntosReferencia);
        // Cálculo de los puntos asignados a cada rayo
        for (List<Double> puntoRef:asignacionSolucionesConPuntosReferencia.keySet()) {
            cantidadPuntosAsignados.put(puntoRef, asignacionSolucionesConPuntosReferencia.get(puntoRef).size());
        }

        while (fronteraDefinitiva.size() > n) {
            List<Double> puntoRefMax = null;
            for (List<Double> puntoRef : cantidadPuntosAsignados.keySet()) {
                //System.out.println("PuntoRef: " + puntoRef);
                if (puntoRefMax == null) {
                    puntoRefMax = puntoRef;
                } else {
                    if (cantidadPuntosAsignados.get(puntoRefMax) < cantidadPuntosAsignados.get(puntoRef)) {
                        puntoRefMax = puntoRef;
                    }
                }
            }
            Double maxDistancia = 0.0;
            for (Double distancia: distanciasAux.get(puntoRefMax)) {
                if(maxDistancia < distancia){
                    maxDistancia = distancia;
                }
            }
            Integer indicePuntoMasLejano = distanciasAux.get(puntoRefMax).indexOf(maxDistancia);
            Map<List<String>,java.lang.Integer> puntoIndeseable = asignacionSolucionesConPuntosReferencia.get(puntoRefMax).get(indicePuntoMasLejano);
            cantidadPuntosAsignados.put(puntoRefMax, cantidadPuntosAsignados.get(puntoRefMax)-1);
            List<Double> nuevaListaDistancias = distanciasAux.get(puntoRefMax);
            nuevaListaDistancias.remove(maxDistancia);
            distanciasAux.put(puntoRefMax, nuevaListaDistancias);
            fronteraDefinitiva.remove(puntoIndeseable);

        }
        return fronteraDefinitiva;
    }


    // A. Classification of Population Into Nondominated Levels
    public static List<List<Map<List<String>, Integer>>> nivelesNoDominados(){
        List<List<Map<List<String>, Integer>>> fronterasDeParetoAux = new ArrayList<>();
        List<Map<List<String>, Integer>> poblacion = new ArrayList<>(padres);
        hijos.clear();
        // cruces
        for (int i =0; i<padres.size(); i = i+2){
            cruce(padres.get(i), padres.get(i+1));
        }
        // mutacion
        Random r = new Random();
        for (Map<List<String>, Integer> hijo: hijos) {
            if (r.nextDouble() < probMutacion){
                mutacion(hijo);
            }
        }
        poblacion.addAll(hijos);

        //Calculo de los valores
        valoresFuncionesObjetivo = new HashMap<>();
        for ( Map<List<String>, Integer> indiviuo: poblacion) {
            List<Double> valores = new ArrayList<>();
            valores.add(funcionObjetivoRiesgo(indiviuo));
            valores.add(funcionObjetivoPasajeros(indiviuo));
            valores.add(funcionObjetivoDineroMedioLinea(indiviuo));
            valores.add(funcionObjetivoPasajerosCompanya(indiviuo));
            valores.add(funcionObjetivoHomogeneidadPerdidas(indiviuo));
            valores.add(funcionObjevitoConectividad(indiviuo));
            valoresFuncionesObjetivo.put(indiviuo, valores);
            //System.out.println(valores.stream().mapToDouble(Double::valueOf).sum());
        }

        while (poblacion.size() > n) {
            Set<Map<List<String>, Integer>> frontera = new HashSet<>();
            Set<Map<List<String>, Integer>> seQuedan = new HashSet<>();
            Set<Map<List<String>, Integer>> seVan = new HashSet<>();
            frontera.add(poblacion.get(0));
            for (int i = 1; i < poblacion.size(); i++){
                for (Map<List<String>, Integer> c: frontera) {
                    Integer dominancia = dominancia(valoresFuncionesObjetivo.get(poblacion.get(i)), valoresFuncionesObjetivo.get(c));
                    if (dominancia == m){
                        seQuedan.add(poblacion.get(i));
                        seVan.add(c);
                        seQuedan.remove(c);
                    } else if (dominancia > 0) {
                        seQuedan.add(poblacion.get(i));
                    }
                }
                frontera.addAll(seQuedan);
                frontera.removeAll(seVan);
            }
            fronterasDeParetoAux.add(new ArrayList<>(frontera));
            poblacion.removeAll(frontera);
        }

        fronterasDePareto = new ArrayList<>(fronterasDeParetoAux);
        //System.out.println(fronterasDePareto.size());
        valoresFuncionesObjetivo.remove(poblacion);

        if (poblacion.size()< n && fronterasDePareto.size()>1){
            int sobrante = n - poblacion.size();
            List<Map<List<String>, Integer>> Fl = fronterasDePareto.get(fronterasDePareto.size()-1);
            // Uso de los puntos de referencia
            List<List<Double>> listaPuntosRefecia = calculoPuntosReferencia();
            Map<Map<List<String>, Integer> ,List<Double>> valoresFuncionesObjetivoTrasladados =
                    valoresTrasladados();
            asociacion(listaPuntosRefecia, valoresFuncionesObjetivoTrasladados);
            fronterasDePareto = nichePreservarion(Fl,sobrante);
        }else if(poblacion.size()< n && fronterasDePareto.size()==1){
            int porRellenar = n;
            List<Map<List<String>, Integer>> Fl = fronterasDePareto.get(fronterasDePareto.size()-1);
            // Uso de los puntos de referencia
            List<List<Double>> listaPuntosRefecia = calculoPuntosReferencia();
            Map<Map<List<String>, Integer> ,List<Double>> valoresFuncionesObjetivoTrasladados =
                    valoresTrasladados();
            asociacion(listaPuntosRefecia, valoresFuncionesObjetivoTrasladados);
            fronterasDePareto.remove(0);
            fronterasDePareto.add(nichePreservarionUnaSolaFrontera(Fl));
        }
        padres.clear();
        hijos.clear();
        for (List<Map<List<String>, Integer>> frontera : fronterasDePareto) {
            padres.addAll(frontera);
        }
        valoresPrimeraFronteraDePareto = new HashMap<>();
        for (Map<List<String>, Integer> p:fronterasDePareto.get(0)) {
            List<Double> aux = valoresFuncionesObjetivo.get(p);
            valoresPrimeraFronteraDePareto.put(p, aux);
        }
        return fronterasDePareto;
    }

    // Funciones objetivo
    public static Double funcionObjetivoRiesgo(Map<List<String>, Integer>candidato){
        Double sumatorio = 0.0;
        Double sumatorioTotal = 0.0;
        List<List<String>> llaves = new ArrayList<>(riesgos.keySet());
        for (int i = 0; i < llaves.size(); i++){
            sumatorio += riesgos.get(llaves.get(i))* candidato.get(llaves.get(i));
            sumatorioTotal += riesgos.get(llaves.get(i));
        }
        return sumatorio/sumatorioTotal;
    }

    public static Double funcionObjetivoPasajeros(Map<List<String>, Integer> candidato){
        Integer sumatorio = 0;
        Integer totalPasajeros = 0;
        List<List<String>> llaves = new ArrayList<>(pasajeros.keySet());
        for (int i = 0; i < llaves.size(); i++){
            sumatorio += pasajeros.get(llaves.get(i))* candidato.get(llaves.get(i));
            totalPasajeros += pasajeros.get(llaves.get(i));
        }
        Double porcentaje = 1- sumatorio.doubleValue()/totalPasajeros;
        return porcentaje;
    }

    public static Double funcionObjetivoDineroMedioLinea(Map<List<String>, Integer> candidato){
        Double sumatorio = 0.0;
        Double totalDinero = 0.0;
        List<List<String>> llaves = new ArrayList<>(dineroMedio.keySet());
        for (int i = 0; i < llaves.size(); i++){
            sumatorio += dineroMedio.get(llaves.get(i))* candidato.get(llaves.get(i));
            totalDinero += dineroMedio.get(llaves.get(i));
        }
        Double porcentaje = 1 - sumatorio/totalDinero;
        return porcentaje;
    }

    // Función objetivo pasajeros companyia
    public static Double pasajerosCompanyia(String companyia, Map<List<String>, Integer> candidato){
        Double numPasajeros = 0.0;
        Double totalPasajeros = 0.0;
        List<List<String>> llaves = new ArrayList<>(pasajerosCompanyias.keySet());
        for (int i = 0; i < llaves.size(); i++){
            if (llaves.get(i).contains(companyia)) {
                numPasajeros += pasajerosCompanyias.get(llaves.get(i))* candidato.get(llaves.get(i).subList(0,2));
                totalPasajeros += pasajerosCompanyias.get(llaves.get(i));
            }
        }
        Double division = 0.0;
        if (totalPasajeros != 0.0){
            division= numPasajeros/totalPasajeros;
        }
        return 1-division;
    }

    public static Double media(Map<List<String>, Integer> candidato){
        Double suma = 0.0;
        for (int k =0; k < companyias.size(); k++){
            suma = suma + pasajerosCompanyia(companyias.get(k), candidato);
        }
        Double resultado = 0.0;
        if (companyias.size() != 0){
            resultado = suma/companyias.size();
        }
        return resultado;
    }
    public static Double funcionObjetivoPasajerosCompanya(Map<List<String>, Integer> candidato){
        Double suma = 0.0;
        for (int k = 0; k < companyias.size(); k++){
            suma = suma + Math.abs(pasajerosCompanyia(companyias.get(k), candidato) - media(candidato));
        }
        return suma/companyias.size();
    }
    // Iniciado a las 14:46 del 12 de marzo;
    ////////////////////////////////////////////////////////////////7
    public static Double proporcionVuelosAeropuerto(String AeropuertoEspanyol,
                                              Map<List<String>, Integer> candidato){
        Double suma = 0.0;
        Double sumaTotal = 0.0;
        List<List<String>> llaves = new ArrayList<>(vuelos.keySet());
        for (int i = 0; i < llaves.size(); i++){
            if (llaves.get(i).contains(AeropuertoEspanyol)) {
                suma += vuelos.get(llaves.get(i))* candidato.get(llaves.get(i));
                sumaTotal += vuelos.get(llaves.get(i));
            }
        }
        Double solucion = 0.0;
        if (sumaTotal!=0){
            solucion = suma/sumaTotal;
        }
        return solucion;
    }

    public static Double mediaAeropuertos(Map<List<String>, Integer> candidato){
        Double suma = 0.0;
        Double sumaTotal = 0.0;
        List<List<String>> llaves = new ArrayList<>(vuelos.keySet());
        for (int i = 0; i < llaves.size(); i++){
            suma = suma + candidato.get(llaves.get(i)) * vuelos.get(llaves.get(i));
            sumaTotal += vuelos.get(llaves.get(i));
        }
        Double solucion = 0.0;
        if (sumaTotal!=0){
            solucion = suma/sumaTotal;
        }
        return solucion;
    }

    public static Double funcionObjetivoHomogeneidadPerdidas(Map<List<String>, Integer> candidato){
        Double suma = 0.0;
        for (int j = 0; j < AeropuertosEspanyoles.size(); j++){
            suma = suma + Math.abs(proporcionVuelosAeropuerto(AeropuertosEspanyoles.get(j), candidato) -
                    mediaAeropuertos(candidato));
        }
        return suma/AeropuertosEspanyoles.size();
    }

    public static Double funcionObjevitoConectividad(Map<List<String>, Integer> candidato){
        Double sumConectividadesPorOrigen = 0.0;
        Double conectividadTotal= 0.0;
        for (String origen : conectividadesAeropuertosOrigen.keySet()) {
            Double sumNumVuelosPorEstado = 0.0;
            for (List<String> linea : conectividadYvuelosExterioresAEspanya.keySet()) {
                if (linea.get(0).equals(origen)){
                    Double proporcion = conectividadYvuelosExterioresAEspanya.get(linea).get(0) /
                            conectividadYvuelosExterioresAEspanya.get(linea).get(1);
                    sumNumVuelosPorEstado = sumNumVuelosPorEstado +
                            candidato.get(linea) * proporcion;
                }
            }
            sumConectividadesPorOrigen = sumConectividadesPorOrigen +
                    conectividadesAeropuertosOrigen.get(origen)*(1-sumNumVuelosPorEstado);
            conectividadTotal = conectividadTotal + conectividadesAeropuertosOrigen.get(origen);
        }
        return sumConectividadesPorOrigen/conectividadTotal;
    }

    public String hola_mundo(){
        return "Hola Mundo prueba.UNSGAIII";
    }
}
