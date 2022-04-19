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
	 * @param mismaFechaTurismo True si al rellenar los datos de turistas en vuelos deberían buscarse datos de turismo
	 *                          en la misma fecha. Para más detalles, ver {@link Añadir#añadirTuristasVuelo}.
	 *                          Si este parámetro es false, aproximarFaltantesTuristas debe ser true.
	 * @param aproximarFaltantesTurismo True si al rellenar los datos de turistas en vuelos deberían aproximarse los
	 *                                  datos de turismo cuando no estén disponibles. Para más detalles, ver
	 *                                  {@link Añadir#añadirTuristasVuelo}.
	 *                                  Si este parámetro es false, mismaFechaTuristas debe ser true.
	 */
	@Procedure(mode = Mode.WRITE)
	public void mainETL(@Name("rutaFicheroConectividad") String rutaFicheroConectividad,
						@Name("rutaFicheroTurismo") String rutaFicheroTurismo,
						@Name("mismaFechaTurismo") Boolean mismaFechaTurismo,
						@Name("aproximarFaltantesTurismo") Boolean aproximarFaltantesTurismo) {
		Modificar modificar = new Modificar();
		modificar.convertirRelacionesAOD();
		modificar.borrarVuelosSinSIR();
		modificar.convertirFechasVuelos();

		Añadir añadir = new Añadir();
		añadir.añadirConexionesAeropuertoPaís();
		añadir.añadirConectividad(rutaFicheroConectividad);
		añadir.calcularNúmeroPasajeros();
		añadir.añadirRatioTuristas(rutaFicheroTurismo);
		añadir.añadirTuristasVuelo(mismaFechaTurismo, aproximarFaltantesTurismo);
	}
}
