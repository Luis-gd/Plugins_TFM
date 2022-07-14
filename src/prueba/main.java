package prueba;

import prueba.NSGAIII;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.File;  // Import the File class
import java.io.IOException;  // Import the IOException class to handle errors


public class main {
    private static Map<Map<List<String>, Integer>,List<Double>> mejoresValoresConSolucion(Map<Map<List<String>, Integer>,
            List<Double>> frontera, int objetivo){
        Map<Map<List<String>, Integer>,List<Double>> solucion = new HashMap<>();
        List<Double> valores = new ArrayList<>();
        Double minVal = 1.0;
        Map<List<String>, Integer> k = new HashMap<>();
        for (Map<List<String>, Integer> key: frontera.keySet()) {
            if (minVal > frontera.get(key).get(objetivo)) {
                minVal = frontera.get(key).get(objetivo);
                k = key;
            }
        }
        valores.add(NSGAIII.funcionObjetivoRiesgo(k));
        valores.add(NSGAIII.funcionObjetivoPasajeros(k));
        valores.add(NSGAIII.funcionObjetivoDineroMedioLinea(k));
        valores.add(NSGAIII.funcionObjetivoPasajerosCompanya(k));
        valores.add(NSGAIII.funcionObjetivoHomogeneidadPerdidas(k));
        valores.add(NSGAIII.funcionObjevitoConectividad(k));
        solucion.put(k, valores);
        return solucion;
    }

    public static void main(String args[])  //static method
    {
        

        List<String> AeropuertosEspanyoles = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/" +
                    "src/prueba/datos/aeropuertos_entradas.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                AeropuertosEspanyoles.add(scanner.next());
            }
            // Closing the scanner
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println("El path del documento AeropuertosEspanyoles no está bien especificado");
        //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        NSGAIII.AeropuertosEspanyoles = AeropuertosEspanyoles;

        List<String> companyias = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/" +
                    "src/prueba/datos/companyias.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                companyias.add(str);
            }
            // Closing the scanner
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println("El path del documento companyias no está bien especificado");
            //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        companyias.remove("UNKNOWN");
        NSGAIII.companyias = companyias;

        Map<List<String>, Double> dineroMedio = new HashMap<>();
        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/" +
                    "src/prueba/datos/dinero_por_vuelo.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                String split[] = str.split(",");
                dineroMedio.put(List.of(split[2], split[1]), Double.parseDouble(split[0]));
            }
            // Closing the scanner
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println("El path del documento dineroMedio no está bien especificado");
            //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        NSGAIII.dineroMedio = dineroMedio;

        Map<List<String>, Integer> vuelos = new HashMap<>();
        Map<List<String>, List<Double>> conectividadYvuelosExterioresAEspanya = new HashMap<>();
        Map<String, Double> conectividadesAeropuertosOrigen = new HashMap<>();

        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/src/prueba" +
                    "/datos/conectividad_por_aeropuerto.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                String split[] = str.split(",");
                conectividadYvuelosExterioresAEspanya.put(List.of(split[1], split[0]),
                        List.of(Double.parseDouble(split[2]),Double.parseDouble(split[3])));
                conectividadesAeropuertosOrigen.put(split[0], Double.parseDouble(split[4]));

                if (vuelos.keySet().contains(List.of(split[1], split[0]))){
                    vuelos.put(List.of(split[1], split[0]), vuelos.get(List.of(split[1], split[0])) + Integer.parseInt(split[2]));

                }else{
                    vuelos.put(List.of(split[1], split[0]), Integer.parseInt(split[2]));
                }
            }
            // Closing the scanner
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println("El path del documento conectividad_por_aeropuerto no está bien especificado");
            //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        NSGAIII.conectividadYvuelosExterioresAEspanya = conectividadYvuelosExterioresAEspanya;
        NSGAIII.conectividadesAeropuertosOrigen = conectividadesAeropuertosOrigen;
        NSGAIII.vuelos = vuelos;


        Map<List<String>, Integer> pasajeros = new HashMap<>();
        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/" +
                    "src/prueba/datos/pasajeros_por_vuelo.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                String split[] = str.split(",");
                if (pasajeros.keySet().contains(List.of(split[2], split[1]))){
                    pasajeros.put(List.of(split[2], split[1]), pasajeros.get(List.of(split[2], split[1])) + (int) Double.parseDouble(split[0]));
                }else{
                    pasajeros.put(List.of(split[2], split[1]), (int) Double.parseDouble(split[0]));
                }
            }
            // Closing the scanner
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println("El path del documento pasajeros no está bien especificado");
            //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        NSGAIII.pasajeros = pasajeros;

        Map<List<String>, Integer> pasajerosCompanyia = new HashMap<>();
        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/" +
                    "src/prueba/datos/pasajeros_por_vuelo_y_companyias.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                String split[] = str.split(",");
                if(pasajerosCompanyia.keySet().contains(List.of(split[3],split[2], split[1]))){
                    pasajerosCompanyia.put(List.of(split[3], split[2], split[1]),
                            pasajerosCompanyia.get(List.of(split[3], split[2], split[1])) + (int) Double.parseDouble(split[0]));

                }else{
                    pasajerosCompanyia.put(List.of(split[3], split[2], split[1]), (int) Double.parseDouble(split[0]));
                }
            }
            // Closing the scanner
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println("El path del documento no está bien especificado");
            //do something with e, or handle this case
        }
        NSGAIII.pasajerosCompanyias = pasajerosCompanyia;

        Map<List<String>, Integer> conexiones = new HashMap<>();
        Map<List<String>, Double> riesgos = new HashMap<>();
        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/" +
                    "src/prueba/datos/sir.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                String split[] = str.split(",");
                conexiones.put(List.of(split[2], split[1]), 0);
                if (riesgos.containsKey(List.of(split[2], split[1]))){
                    riesgos.put(List.of(split[2], split[1]), Double.parseDouble(split[0]) + riesgos.get(List.of(split[2], split[1])));
                }else{
                    riesgos.put(List.of(split[2], split[1]), Double.parseDouble(split[0]));
                }
            }
            // Closing the scanner
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println("El path del documento conexiones no está bien especificado");
            //do something with e, or handle this case
        }
        NSGAIII.conexiones = conexiones;
        NSGAIII.riesgos = riesgos;

        Map<String, Set<String>> listaConexionesPorAeropuertoEspanyol = new HashMap<>();
       // listaConexionesPorAeropuertoEspanyol.put("Adolfo Suárez Madrid–Barajas Airport", new HashSet<>(List.of("A Coruña Airport", "Abu Dhabi International Airport")));
       // System.out.println(listaConexionesPorAeropuertoEspanyol.get(""));
        for (String aeropuerto:AeropuertosEspanyoles) {
            for ( List<String> vuelo:vuelos.keySet()) {
                if (vuelo.get(1).equals(aeropuerto)){
                    if (listaConexionesPorAeropuertoEspanyol.keySet().contains(aeropuerto)){
                        Set<String> valor = new HashSet<>(listaConexionesPorAeropuertoEspanyol.get(aeropuerto));
                        List<String> aux = new ArrayList<>(vuelo);
                        aux.remove(aeropuerto);
                        String otroAeropuerto = aux.get(0);
                        valor.add(otroAeropuerto);
                        listaConexionesPorAeropuertoEspanyol.put(aeropuerto, valor);
                    }else {
                        List<String> aux = new ArrayList<>(vuelo);
                        aux.remove(aeropuerto);
                        String otroAeropuerto = aux.get(0);
                        Set<String> valor = Set.of(otroAeropuerto);
                        listaConexionesPorAeropuertoEspanyol.put(aeropuerto, valor);
                    }
                }
            }
        }

        NSGAIII.listaConexionesPorAeropuertoEspanyol = listaConexionesPorAeropuertoEspanyol;

        // Pruebas del código tras la carga de datos
        NSGAIII.poblacionInicial();
        //System.out.println(NSGAIII.funcionObjevitoConectividad(NSGAIII.padres.get(0)));
        List<List<Map<List<String>, Integer>>> ultimasFronteras = new ArrayList<>();
        Boolean condicionDeParada = true;
        Integer iteraciones = 0;
        while (condicionDeParada && iteraciones < 1000){
            List<List<Map<List<String>, Integer>>> fronterasDePareto = NSGAIII.nivelesNoDominados();
            if (ultimasFronteras.size() < 10){
                ultimasFronteras.add(fronterasDePareto.get(0));
            }else if (ultimasFronteras.size() == 10){
                Integer sum = 0;
                for (List<Map<List<String>, Integer>> fronterasPrevias: ultimasFronteras) {
                    if(fronterasPrevias.equals(fronterasDePareto.get(0))){
                        sum++;
                    }
                }
                ultimasFronteras.remove(0);
                ultimasFronteras.add(fronterasDePareto.get(0));
                if (sum == 10){
                    condicionDeParada = false;
                }
            }
           // System.out.println("Numero de fronteras de pareto"  + fronterasDePareto.size());
            //System.out.println("Tamaño de la primera frontera de pareto " + fronterasDePareto.get(0).size());


            try {
                File myObj = new File("resultados.txt");
                if (myObj.createNewFile()) {
                    System.out.println("Fichero Creado: " + myObj.getName());
                } else {
                    System.out.println("El fichero ya existe.");
                }
            } catch (IOException e) {
                System.out.println("Hubo un error.");
                e.printStackTrace();
                }

            try {
                FileWriter myWriter = new FileWriter("resultados.txt", true);
                myWriter.write("Mejor valor del SIR: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 0).keySet().toArray()[0]+ "\n");
                myWriter.write("Solucion asociada al SIR: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 0).get(mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 0).keySet().toArray()[0])+ "\n");
                myWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ \n");

                myWriter.write("Mejor valor del objetivo de minimización de pérdidas de pasajeros: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 1).keySet().toArray()[0]+ "\n");
                myWriter.write("Solucion asociada al valor del objetivo de minimización de pérdidas de pasajeros: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 1).get(mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 1).keySet().toArray()[0])+ "\n");
                myWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ \n");

                myWriter.write("Mejor valor del objetivo de minimización de pérdidas del dinero medio por líneas: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 2).keySet().toArray()[0]+ "\n");
                myWriter.write("Solucion asociada al valor del objetivo de minimización de pérdidas del dinero medio por líneas: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 2).get(mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 2).keySet().toArray()[0])+ "\n");
                myWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ \n");

                myWriter.write("Mejor valor del objetivo de minimización de pasajeros perdidos por compañía: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 3).keySet().toArray()[0]+ "\n");
                myWriter.write("Solucion asociada al valor del objetivo de minimización de pasajeros perdidos por compañía: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 3).get(mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 3).keySet().toArray()[0])+ "\n");
                myWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ \n");

                myWriter.write("Mejor valor del objetivo de homogeneidad de pérdidas de las aerolíneas: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 4).keySet().toArray()[0]+ "\n");
                myWriter.write("Solucion asociada al valor del objetivo de homogeneidad de pérdidas de las aerolíneas: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 4).get(mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 4).keySet().toArray()[0])+ "\n");
                myWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ \n");

                myWriter.write("Mejor valor de la minimización de pérdida de la conectividad: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 5).keySet().toArray()[0]+ "\n");
                myWriter.write("Solucion asociada al valor de la minimización de pérdida de la conectividad: " +
                        mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 5).get(mejoresValoresConSolucion(NSGAIII.valoresPrimeraFronteraDePareto, 5).keySet().toArray()[0])+ "\n");
                myWriter.write("-------------------------------------------------------------------------------------------------------- \n");
                myWriter.close();
                System.out.println("Se escribió en el fichero.");
            } catch (IOException e) {
                System.out.println("Hubo un problema.");
                e.printStackTrace();
            }
            iteraciones++;
        }
    }


}
