package earlywarn.etl;

import earlywarn.definiciones.Globales;
import earlywarn.definiciones.Propiedad;
import earlywarn.main.Propiedades;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

/**
 * En esta clase se encuentran los procedimientos encargados de modificar datos en la BD antes de empezar a trabajar
 * con la misma. Estas modificaciones puden cambiar el formato de los datos o la estructura de la BD.
 */
public class Modificar {
	@Context
	public GraphDatabaseService db;

	/**
	 * Requerido por Neo4J
	 * @deprecated Este constructor no debe utilizarse. Usar {@link Modificar#Modificar(GraphDatabaseService)} en su lugar.
	 */
	@Deprecated
	public Modificar() {

	}
	public Modificar(GraphDatabaseService db) {
		this.db = db;
	}

	/**
	 * Modifica el formato de las relaciones entre aeropuertos y días de operación. En lugar de almacenar la fecha
	 * usando el tipo de la relación, pasa dicha fecha a un campo llamado "date" en la relación y cambia el tipo
	 * de todas estas relaciones a "OPERATES_ON".
	 * Fija la propiedad {@link Propiedad#ETL_RELACIONES_AOD} a true en la BD.
	 */
	@Procedure(mode = Mode.WRITE)
	public void convertirRelacionesAOD() {
		try (Transaction tx = db.beginTx()) {
			tx.execute(
				"MATCH (a:Airport)-[r]->(aod:AirportOperationDay) " +
				"WHERE type(r) <> \"OPERATES_ON\" " +
				"CREATE (a)-[:OPERATES_ON {date: date(type(r))}]->(aod)");
			tx.execute(
				"MATCH (a:Airport)-[r]->(aod:AirportOperationDay) " +
				"WHERE type(r) <> \"OPERATES_ON\" " +
				"DELETE r");
			tx.commit();
			new Propiedades(db).setBool(Propiedad.ETL_RELACIONES_AOD, true);
		}
	}

	/**
	 * Borra de la base de datos todos los vuelos que no tengan calculado su valor final de SIR.
	 * Fija la propiedad {@link Propiedad#ETL_BORRAR_VUELOS_SIN_SIR} a true en la BD.
	 */
	@Procedure(mode = Mode.WRITE)
	public void borrarVuelosSinSIR() {
		try (Transaction tx = db.beginTx()) {
			tx.execute(
				"MATCH (f:FLIGHT) " +
				"WHERE f.flightIfinal IS NULL " +
				"DETACH DELETE f");
			tx.commit();
			new Propiedades(db).setBool(Propiedad.ETL_BORRAR_VUELOS_SIN_SIR, true);
		}
	}

	/**
	 * Convierte las fechas de llegada y salida de los vuelos a tipo date.
	 * Fija la propiedad {@link Propiedad#ETL_CONVERTIR_FECHAS_VUELOS} a true en la BD.
	 */
	@Procedure(mode = Mode.WRITE)
	public void convertirFechasVuelos() {
		try (Transaction tx = db.beginTx()) {
			tx.execute(
				"MATCH (f:FLIGHT) " +
				"SET f.dateOfDeparture = date(f.dateOfDeparture) " +
				"SET f.dateOfArrival = date(f.dateOfArrival)");
			tx.commit();
			new Propiedades(db).setBool(Propiedad.ETL_CONVERTIR_FECHAS_VUELOS, true);
		}
	}

	/**
	 * Función para actualizar el valor de recuperación (alpha) del virus por defecto.
	 * No hace cambios en la base de datos, sino en la variable global <<DEFAULT_ALPHA>>.
	 */
	@Procedure(mode = Mode.WRITE)
	public void actualizarIndiceRecuperacion(@Name("newAlpha") Number alpha){
		Globales.updateAlpha((double) alpha);
	}

	/**
	 * Función para actualizar el valor de transmisión (beta) del virus por defecto.
	 * No hace cambios en la base de datos, sino en la variable global <<DEFAULT_BETA>.
	 */
	@Procedure(mode = Mode.WRITE)
	public void actualizarIndiceTransmision(@Name("newBeta") Number beta){
		Globales.updateBeta((double) beta);
	}
}
