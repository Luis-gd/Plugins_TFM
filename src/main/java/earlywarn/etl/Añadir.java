package earlywarn.etl;

import earlywarn.definiciones.Propiedad;
import earlywarn.main.Propiedades;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

/**
 * En esta clase se encuentran los procedimientos encargados de añadir datos a la BD antes de empezar a trabajar
 * con la misma.
 */
public class Añadir {
	@Context
	public GraphDatabaseService db;

	/**
	 * Añade el valor de conectividad a los diferentes aeropuertos.
	 * Los datos se obtienen de un CSV importado con el nombre indicado. El formato esperado para el CSV es
	 * "códigoIATAAeropuerto,conectividad". El CSV no debe contener una cabecera. El valor de conectividad es un entero
	 * sin separador de miles.
	 * Fija la propiedad {@link Propiedad#ETL_CONECTIVIDAD} a true en la BD.
	 * @param rutaFichero Ruta al fichero CSV, relativa a la carpeta de import definida en la configuración de Neo4J.
	 */
	@Procedure(mode = Mode.WRITE)
	public void añadirConectividad(@Name("rutaFichero") String rutaFichero) {
		try (Transaction tx = db.beginTx()) {
			tx.execute(
				"LOAD CSV FROM 'file:///" + rutaFichero + "' AS line " +
					"MATCH (a:Airport) " +
					"WHERE a.iata = line[0] " +
					"SET a.connectivity = toInteger(line[1])");
			tx.commit();
			new Propiedades(db).setBool(Propiedad.ETL_CONECTIVIDAD, true);
		}
	}
}
