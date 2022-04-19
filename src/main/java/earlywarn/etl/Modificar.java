package earlywarn.etl;

import earlywarn.definiciones.Propiedad;
import earlywarn.main.Propiedades;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

import java.util.Map;

/**
 * En esta clase se encuentran los procedimientos encargados de modificar datos en la BD antes de empezar a trabajar
 * con la misma. Estas modificaciones puden cambiar el formato de los datos o la estructura de la BD.
 */
public class Modificar {
	@Context
	public GraphDatabaseService db;

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
	 * Añade un campo a cada vuelo que incluye su número de pasajeros. El valor se calcula el número de asientos y
	 * el porcentaje de ocupación del vuelo.
	 * Los valores faltantes del número de asientos y el porcentaje de ocupación se rellenarán con la media de todo
	 * el dataset.
	 * Fija la propiedad {@link Propiedad#ETL_PASAJEROS} a true en la BD.
	 */
	// TODO: Mover a etl.Añadir
	@Procedure(mode = Mode.WRITE)
	public void calcularNúmeroPasajeros() {
		try (Transaction tx = db.beginTx()) {
			int mediaAsientos;
			double mediaOcupación;
			try (Result res = tx.execute("MATCH (f:FLIGHT) RETURN avg(f.seatsCapacity)")) {
				Map<String, Object> row = res.next();
				mediaAsientos = (int) Math.round((double) row.get(res.columns().get(0)));
			}
			try (Result res = tx.execute("MATCH (f:FLIGHT) RETURN avg(f.occupancyPercentage)")) {
				Map<String, Object> row = res.next();
				mediaOcupación = (double) row.get(res.columns().get(0));
			}

			// Rellenar valores faltantes
			tx.execute(
				"MATCH (f:FLIGHT) " +
				"WHERE f.seatsCapacity IS NULL " +
				"SET f.seatsCapacity = " + mediaAsientos);
			tx.execute(
				"MATCH (f:FLIGHT) " +
					"WHERE f.occupancyPercentage IS NULL " +
					"SET f.occupancyPercentage = " + mediaOcupación);

			// Insertar número de pasajeros
			tx.execute(
				"MATCH (f:FLIGHT) " +
					"SET f.passengers = toInteger(round(f.seatsCapacity * f.occupancyPercentage / 100))");

			tx.commit();
			new Propiedades(db).setBool(Propiedad.ETL_PASAJEROS, true);
		}
	}
}
