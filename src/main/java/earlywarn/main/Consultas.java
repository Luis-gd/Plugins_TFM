package earlywarn.main;

import earlywarn.definiciones.ETLOperationRequiredException;
import earlywarn.definiciones.Propiedad;
import earlywarn.definiciones.SentidoVuelo;
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
	 * Devuelve el número de vuelos que entran y/o salen del aeropuerto indicado en el rango de días indicados.
	 * Requiere que se haya llevado a cabo la operación ETL que convierte las relaciones entre Airport y AOD.
	 * @param idAeropuerto Código IATA del aeropuerto

	 * @param díaInicio Primer día en el que buscar vuelos (inclusivo)
	 * @param díaFin Último día en el que buscar vuelos (inclusivo)
	 * @param sentido Sentido de los vuelos a considerar

	 * @return Número de vuelos que salen del aeropuerto en el día indicado
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link earlywarn.etl.Modificar#convertirRelacionesAOD}.
	 */

	public long getVuelosAeropuerto(String idAeropuerto, LocalDate díaInicio, LocalDate díaFin, SentidoVuelo sentido) {
		String díaInicioStr = díaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String díaFinStr = díaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		if (new Propiedades(db).getBool(Propiedad.ETL_RELACIONES_AOD)) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute(
					"MATCH (a:Airport)-[o:OPERATES_ON]->(:AirportOperationDay)" +
						sentido.operadorAODVuelo("") + "(f:FLIGHT) " +
						"WHERE a.iata = \"" + idAeropuerto + "\" " +
						"AND date(\"" + díaInicioStr + "\") <= o.date <= date(\"" + díaFinStr + "\") " +
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


	//Dado una fecha de inicio, una fecha de fin y un país devuelve el valor de riesgo importado para el " +
	//		"país. Ejemplo: earlywarn.main.SIR_por_pais(date(\"2019-06-01\"), date({year: 2019, month: 7, day: 1})," +
	//		" \"Spain\")" )
	public Double getSIRPorPais(String pais, LocalDate diaInicio, LocalDate diaFin ){
		// Las formas de escribir las fechas en neo4j son como entrada: date("2019-06-01") y date({year: 2019, month: 7}
		//TODO: Eliminar el filtro del exists cuando se eliminen los nodos que no tienen el SIR
		String diaInicioStr = diaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String diaFinStr = diaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Transaction tx = db.beginTx();
		try (Result res = tx.execute(
						"MATCH (c:Country)<-[:BELONGS_TO]-(:ProvinceState)-[:INFLUENCE_ZONE]->(:Airport)" +
								"-[]->(:AirportOperationDay)<-[]-(f:FLIGHT) " +
								"WHERE c.countryName=\"" + pais + "\" " +
								"AND EXISTS(f.flightIfinal) " +
								"AND date(\"" + diaInicioStr + "\") <= date(f.dateOfDeparture) <= date(\"" + diaFinStr + "\")"  +
								"RETURN SUM( f.flightIfinal * " +
								"(duration.inSeconds(datetime(f.instantOfDeparture), datetime(f.instantOfArrival)).seconds/60))")){
		Map<String, Object> row = res.next();
		return (Double) row.get(res.columns().get(0));}
	}
}
