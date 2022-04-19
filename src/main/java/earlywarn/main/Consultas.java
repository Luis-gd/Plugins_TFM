package earlywarn.main;

import earlywarn.definiciones.ETLOperationRequiredException;
import earlywarn.definiciones.Propiedad;
import earlywarn.definiciones.SentidoVuelo;
import earlywarn.etl.Modificar;
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

	// Primer año del que se tienen datos de turismo. Null si aún no se ha consultado la BD para obtener el valor.
	private Integer primerAñoDatosTurismo;
	// Último año del que se tienen datos de turismo. Null si aún no se ha consultado la BD para obtener el valor.
	private Integer últimoAñoDatosTurismo;

	public Consultas(GraphDatabaseService db) {
		this.db = db;
		primerAñoDatosTurismo = null;
		últimoAñoDatosTurismo = null;
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
	 * {@link Modificar#convertirRelacionesAOD()}.
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

	/**
	 * Devuelve el año más antiguo del que se tienen datos acerca del porcentaje de visitantes por motivos turísticos.
	 * El valor está cacheado, por lo que una instancia de esta clase siempre devolverá el mismo. La instancia actual
	 * solo consultará la base de datos la primera vez que se llame a este método.
	 * @return Año más antiguo del que hay registros en la BD acerca del porcentaje de turistas
	 */
	public int getPrimerAñoDatosTurismo() {
		if (últimoAñoDatosTurismo == null) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute("MATCH (tr:TuristRatio) RETURN min(tr.year)")) {
					Map<String, Object> row = res.next();
					últimoAñoDatosTurismo = Math.toIntExact((Long) row.get(res.columns().get(0)));
				}
			}
		}
		return primerAñoDatosTurismo;
	}

	/**
	 * Devuelve el año más reciente del que se tienen datos acerca del porcentaje de visitantes por motivos turísticos.
	 * El valor está cacheado, por lo que una instancia de esta clase siempre devolverá el mismo. La instancia actual
	 * solo consultará la base de datos la primera vez que se llame a este método.
	 * @return Año más reciente del que hay registros en la BD acerca del porcentaje de turistas
	 */
	public int getÚltimoAñoDatosTurismo() {
		if (últimoAñoDatosTurismo == null) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute("MATCH (tr:TuristRatio) RETURN max(tr.year)")) {
					Map<String, Object> row = res.next();
					últimoAñoDatosTurismo = Math.toIntExact((Long) row.get(res.columns().get(0)));
				}
			}
		}
		return últimoAñoDatosTurismo;
	}

	/**
	 * Dado una fecha de inicio, una fecha de fin y un país devuelve el valor de riesgo importado para el país.
	 * Requiere que se haya llevado a cabo la operación ETL que elimina vuelos sin datos SIR.
	 * Ejemplo: earlywarn.main.SIR_por_pais(date("2019-06-01"), date({year: 2019, month: 7, day: 1}), "Spain")
	 * @param pais Nombre del país tal y como aparece en la base de datos
	 * @param diaInicio Primer día a tener en cuenta
	 * @param diaFin Último día a tener en cuenta
	 * @return Valor del riesgo importado (SIR total) para el país indicado teniendo en cuenta todos los vuelos entrantes
	 * en el periodo especificado
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Modificar#borrarVuelosSinSIR()}.
	 */
	public Double getSIRPorPais(String pais, LocalDate diaInicio, LocalDate diaFin) {
		// Las formas de escribir las fechas en neo4j son como entrada: date("2019-06-01") y date({year: 2019, month: 7}
		String diaInicioStr = diaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String diaFinStr = diaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		if (new Propiedades(db).getBool(Propiedad.ETL_BORRAR_VUELOS_SIN_SIR)) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute(
					"MATCH (c:Country)<-[:BELONGS_TO]-(:ProvinceState)-[:INFLUENCE_ZONE]->(:Airport)" +
						"-[]->(:AirportOperationDay)<-[]-(f:FLIGHT) " +
						"WHERE c.countryName=\"" + pais + "\" " +
						"AND date(\"" + diaInicioStr + "\") <= date(f.dateOfDeparture) <= date(\"" + diaFinStr + "\")" +
						"RETURN SUM( f.flightIfinal * " +
						"(duration.inSeconds(datetime(f.instantOfDeparture), datetime(f.instantOfArrival)).seconds/60))")) {
					Map<String, Object> row = res.next();
					return (Double) row.get(res.columns().get(0));
				}
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
				"que elimina los vuelos sin datos SIR antes de ejecutarla.");
		}
	}
}
