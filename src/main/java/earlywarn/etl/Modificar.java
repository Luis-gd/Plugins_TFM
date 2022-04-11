package earlywarn.etl;

import earlywarn.main.Propiedad;
import earlywarn.main.Propiedades;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

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
		}
		new Propiedades(db).setBool(Propiedad.ETL_RELACIONES_AOD, true);
	}
}
