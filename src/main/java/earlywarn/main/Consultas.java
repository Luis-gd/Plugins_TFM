package earlywarn.main;

import earlywarn.definiciones.ETLOperationRequiredException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

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
//patata
	/**
	 * Devuelve el número de vuelos que salen del aeropuerto indicado en el rango de días indicados.
	 * Requiere que se haya llevado a cabo la operación ETL que convierte las relaciones entre Airport y AOD.
	 * @param idAeropuerto Código IATA del aeropuerto
	 * @param diaInicio Primer día en el que buscar vuelos (inclusivo)
	 * @param diaFin Último día en el que buscar vuelos (inclusivo)
	 * @return Número de vuelos que salen del aeropuerto en el día indicado
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link earlywarn.etl.Modificar#convertirRelacionesAOD}.
	 */
	public long getVuelosSalidaAeropuerto(String idAeropuerto, LocalDate diaInicio, LocalDate diaFin) {
		String diaInicioStr = diaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String diaFinStr = diaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		if (new Propiedades(db).getBool(Propiedad.ETL_RELACIONES_AOD)) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute(
					"MATCH (a:Airport)-[o:OPERATES_ON]->(:AirportOperationDay)-[]->(f:FLIGHT) " +
						"WHERE a.iata = \"" + idAeropuerto + "\" " +
						"AND date(\"" + diaInicioStr + "\") <= o.date <= date(\"" + diaFinStr + "\") " +
						"RETURN count(f)")) {
					Map<String, Object> row = res.next();
					return (long) row.get(res.columns().get(0));
				}
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
				"de conversión de relaciones AOD antes de ejecutarla.");
		}
	}

	//@UserFunction
	//@Description("Dado una fecha de inicio, una fecha de fin y un país devuelve el valor de riesgo importado para el " +
	//		"país. Ejemplo: earlywarn.main.SIR_por_pais(date(\"2019-06-01\"), date({year: 2019, month: 7, day: 1})," +
	//		" \"España\")" )
	public Double getSIRPorPais(String idAeropuerto, LocalDate diaInicio, LocalDate diaFin ){
		// Las formas de escribir las fechas en neo4j son como entrada: date("2019-06-01") y date({year: 2019, month: 7}
		//TODO: Luis
		String diaInicioStr = diaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String diaFinStr = diaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		if (new Propiedades(db).getBool(Propiedad.ETL_RELACIONES_AOD)) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute(
						"MATCH (a:Airport)-[o:OPERATES_ON]->(:AirportOperationDay)-[]->(f:FLIGHT) " +
								"WHERE a.iata = \"" + idAeropuerto + "\" " +
								"AND date(\"" + diaInicioStr + "\") <= o.date <= date(\"" + diaFinStr + "\") " +
								"RETURN count(f)")) {
					Map<String, Object> row = res.next();
					return (Double) row.get(res.columns().get(0));
				}
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
					"de conversión de relaciones AOD antes de ejecutarla.");
		}

	}
}
