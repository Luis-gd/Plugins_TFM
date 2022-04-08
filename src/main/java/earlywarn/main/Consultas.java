package earlywarn.main;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Clase usada para realizar consultas sencillas a la base de datos
 */
public class Consultas {
	/*
	 * La instancia de la base de datos.
	 * Debe ser obtenida usando la anotación @Context en un procedimiento o función
	 */
	private final GraphDatabaseService db;

	public Consultas(GraphDatabaseService db) {
		this.db = db;
	}

	/**
	 * Devuelve el número de vuelos que salen del aeropuerto indicado en el día indicado
	 * @param idAeropuerto Código IATA del aeropuerto
	 * @param día Día en el que buscar vuelos
	 * @return Número de vuelos que salen del aeropuerto en el día indicado
	 */
	public long getVuelosSalidaAeropuerto(String idAeropuerto, LocalDate día) {
		String fechaStr = día.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		try (Transaction tx = db.beginTx()) {
			try (Result res = tx.execute(
				"MATCH (a:Airport)-[:`" + fechaStr + "`]->(:AirportOperationDay)-[]->(f:FLIGHT) WHERE a.iata = \"" +
					idAeropuerto + "\" RETURN count(f)")) {
				Map<String, Object> row = res.next();
				return (long) row.get(res.columns().get(0));
			}
		}
	}
}
