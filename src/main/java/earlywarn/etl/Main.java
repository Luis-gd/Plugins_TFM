package earlywarn.etl;

import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

/**
 * Clase que contiene el método principal con el que se inician las operaciones ETL sobre la BD.
 */
public class Main {
	/**
	 * Ejecuta todas las operaciones ETL.
	 * @param rutaFicheroConectividad Ruta al fichero CSV que contiene los datos de conectividad de aeropuertos,
	 *                                relativa a la carpeta de import definida en la configuración de Neo4J.
	 * @param rutaFicheroTurismo Ruta al fichero CSV que contiene los datos del ratio de turistas de las diferentes
	 *                           regiones, relativa a la carpeta de import definida en la configuración de Neo4J.
	 */
	@Procedure(mode = Mode.WRITE)
	public void mainETL(@Name("rutaFicheroConectividad") String rutaFicheroConectividad,
						@Name("rutaFicheroTurismo") String rutaFicheroTurismo) {
		Modificar modificar = new Modificar();
		modificar.convertirRelacionesAOD();
		modificar.borrarVuelosSinSIR();
		modificar.convertirFechasVuelos();
		modificar.calcularNúmeroPasajeros();

		Añadir añadir = new Añadir();
		añadir.añadirConexionesAeropuertoPaís();
		añadir.añadirConectividad(rutaFicheroConectividad);
		añadir.añadirRatioTuristas(rutaFicheroTurismo);
	}
}
