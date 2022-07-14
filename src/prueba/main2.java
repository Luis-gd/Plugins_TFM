package prueba;
import java.io.File;
import java.io.FileNotFoundException;
import prueba.NSGAIII;
import java.util.*;
public class main2 {

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
        } catch (FileNotFoundException e) {
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
        } catch (FileNotFoundException e) {
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
        } catch (FileNotFoundException e) {
            System.out.println("El path del documento dineroMedio no está bien especificado");
            //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        NSGAIII.dineroMedio = dineroMedio;

        Map<List<String>, Integer> vuelos = new HashMap<>();
        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/" +
                    "src/prueba/datos/numero_vuelos.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                String split[] = str.split(",");
                if (vuelos.keySet().contains(List.of(split[2], split[1]))) {
                    vuelos.put(List.of(split[2], split[1]), vuelos.get(List.of(split[2], split[1])) + Integer.parseInt(split[0]));

                } else {
                    vuelos.put(List.of(split[2], split[1]), Integer.parseInt(split[0]));
                }

            }
            // Closing the scanner
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("El path del documento vuelos no está bien especificado");
            //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        NSGAIII.vuelos = vuelos;

        Map<List<String>, List<Double>> conectividadYvuelosExterioresAEspanya = new HashMap<>();
        Map<String, Double> conectividadesAeropuertosOrigen = new HashMap<>();

        try {
            Scanner scanner = new Scanner(new File("/home/Luis/IdeaProjects/PruebasNSGA-III/src/prueba" +
                    "/datos/datosParaConectividad.csv"));
            //Comma as a delimiter
            scanner.useDelimiter("\n");
            scanner.next();
            while (scanner.hasNext()) {
                String str = scanner.next();
                String split[] = str.split(",");
                conectividadYvuelosExterioresAEspanya.put(List.of(split[0], split[1]),
                        List.of(Double.parseDouble(split[2]), Double.parseDouble(split[3])));
                conectividadesAeropuertosOrigen.put(split[0], Double.parseDouble(split[4]));
            }
            // Closing the scanner
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("El path del documento datosParaConectividad no está bien especificado");
            //do something with e, or handle this case
        }
        //AeropuertosEspanyoles.remove(AeropuertosEspanyoles.get(0));
        NSGAIII.conectividadYvuelosExterioresAEspanya = conectividadYvuelosExterioresAEspanya;
        NSGAIII.conectividadesAeropuertosOrigen = conectividadesAeropuertosOrigen;

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
                if (pasajeros.keySet().contains(List.of(split[2], split[1]))) {
                    pasajeros.put(List.of(split[2], split[1]), pasajeros.get(List.of(split[2], split[1])) + (int) Double.parseDouble(split[0]));
                } else {
                    pasajeros.put(List.of(split[2], split[1]), (int) Double.parseDouble(split[0]));
                }
            }
            // Closing the scanner
            scanner.close();
        } catch (FileNotFoundException e) {
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
                if (pasajerosCompanyia.keySet().contains(List.of(split[3], split[2], split[1]))) {
                    pasajerosCompanyia.put(List.of(split[3], split[2], split[1]),
                            pasajerosCompanyia.get(List.of(split[3], split[2], split[1])) + (int) Double.parseDouble(split[0]));

                } else {
                    pasajerosCompanyia.put(List.of(split[3], split[2], split[1]), (int) Double.parseDouble(split[0]));
                }
            }
            // Closing the scanner
            scanner.close();
        } catch (FileNotFoundException e) {
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
        } catch (FileNotFoundException e) {
            System.out.println("El path del documento conexiones no está bien especificado");
            //do something with e, or handle this case
        }
        NSGAIII.conexiones = conexiones;
        NSGAIII.riesgos = riesgos;

        Map<String, Set<String>> listaConexionesPorAeropuertoEspanyol = new HashMap<>();
        // listaConexionesPorAeropuertoEspanyol.put("Adolfo Suárez Madrid–Barajas Airport", new HashSet<>(List.of("A Coruña Airport", "Abu Dhabi International Airport")));
        // System.out.println(listaConexionesPorAeropuertoEspanyol.get(""));
        for (String aeropuerto : AeropuertosEspanyoles) {
            for (List<String> vuelo : vuelos.keySet()) {
                if (vuelo.get(1).equals(aeropuerto)) {
                    if (listaConexionesPorAeropuertoEspanyol.keySet().contains(aeropuerto)) {
                        Set<String> valor = new HashSet<>(listaConexionesPorAeropuertoEspanyol.get(aeropuerto));
                        List<String> aux = new ArrayList<>(vuelo);
                        aux.remove(aeropuerto);
                        String otroAeropuerto = aux.get(0);
                        valor.add(otroAeropuerto);
                        listaConexionesPorAeropuertoEspanyol.put(aeropuerto, valor);
                    } else {
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
       // System.out.println(NSGAIII.valoresTrasladados());

        NSGAIII.nivelesNoDominados();
    }

}
