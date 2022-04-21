package earlywarn.main;

import earlywarn.definiciones.ETLOperationRequiredException;
import earlywarn.definiciones.Propiedad;
import earlywarn.definiciones.SentidoVuelo;
import earlywarn.etl.Añadir;
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
	// Primer año del que se tienen datos de gasto turístico. Null si aún no se ha consultado la BD para obtener el valor.
	private Integer primerAñoDatosGastoTurístico;
	// Último año del que se tienen datos de gasto turístico. Null si aún no se ha consultado la BD para obtener el valor.
	private Integer últimoAñoDatosGastoTurístico;

	public Consultas(GraphDatabaseService db) {
		this.db = db;
		primerAñoDatosTurismo = null;
		últimoAñoDatosTurismo = null;
		primerAñoDatosGastoTurístico = null;
		últimoAñoDatosGastoTurístico = null;
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
		if (primerAñoDatosTurismo == null) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute("MATCH (tr:TuristRatio) RETURN min(tr.year)")) {
					Map<String, Object> row = res.next();
					primerAñoDatosTurismo = Math.toIntExact((Long) row.get(res.columns().get(0)));
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
	 * Devuelve el año más antiguo del que se tienen datos acerca del gasto turístico por persona y país de origen.
	 * El valor está cacheado, por lo que una instancia de esta clase siempre devolverá el mismo. La instancia actual
	 * solo consultará la base de datos la primera vez que se llame a este método.
	 * @return Año más antiguo del que hay registros en la BD acerca del gasto turístico
	 */
	public int getPrimerAñoDatosGastoTurístico() {
		if (primerAñoDatosGastoTurístico == null) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute("MATCH (te:TuristExpense) RETURN min(te.year)")) {
					Map<String, Object> row = res.next();
					primerAñoDatosGastoTurístico = Math.toIntExact((Long) row.get(res.columns().get(0)));
				}
			}
		}
		return primerAñoDatosGastoTurístico;
	}

	/**
	 * Devuelve el año más reciente del que se tienen datos acerca del gasto turístico por persona y país de origen.
	 * El valor está cacheado, por lo que una instancia de esta clase siempre devolverá el mismo. La instancia actual
	 * solo consultará la base de datos la primera vez que se llame a este método.
	 * @return Año más reciente del que hay registros en la BD acerca del gasto turístico
	 */
	public int getÚltimoAñoDatosGastoTurístico() {
		if (últimoAñoDatosGastoTurístico == null) {
			try (Transaction tx = db.beginTx()) {
				try (Result res = tx.execute("MATCH (te:TuristExpense) RETURN max(te.year)")) {
					Map<String, Object> row = res.next();
					últimoAñoDatosGastoTurístico = Math.toIntExact((Long) row.get(res.columns().get(0)));
				}
			}
		}
		return últimoAñoDatosGastoTurístico;
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
	public Double getRiesgoPorPais(String pais, LocalDate diaInicio, LocalDate diaFin) {
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
					"RETURN SUM(f.flightIfinal)")) {
					Map<String, Object> row = res.next();
					return (Double) row.get(res.columns().get(0));
				}
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
				"que elimina los vuelos sin datos SIR antes de ejecutarla.");
		}
	}

	/**
	 * Obtiene el número total de pasajeros que viajan en un rango de fechas,
	 * opcionalmente filtrando por país de destino.
	 * Requiere que se haya ejecutado la operación ETL que calcula el número de pasajeros a bordo de cada vuelo y la
	 * operación ETL que añade las relaciones faltantes entre país y aeropuerto.
	 * @param díaInicio Primer día a tener en cuenta
	 * @param díaFin Último día a tener en cuenta
	 * @param idPaís Solo se tendrán en cuenta los vuelos que tienen este país como destino, o todos
	 *               si se deja en blanco.
	 * @return Número total de pasajeros en el rango de fechas indicado.
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Añadir#calcularNúmeroPasajeros()} o la operación ETL {@link Añadir#añadirConexionesAeropuertoPaís()}.
	 */
	public int getPasajerosTotales(LocalDate díaInicio, LocalDate díaFin, String idPaís) {
		String díaInicioStr = díaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String díaFinStr = díaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Propiedades propiedades = new Propiedades(db);

		if (propiedades.getBool(Propiedad.ETL_PASAJEROS) && propiedades.getBool(Propiedad.ETL_AEROPUERTO_PAÍS)) {
			try (Transaction tx = db.beginTx()) {
				if (idPaís.isEmpty()) {
					try (Result res = tx.execute(
						"MATCH (f:FLIGHT) WHERE date(\"" + díaInicioStr + "\") <= date(f.dateOfDeparture) <= " +
						"date(\"" + díaFinStr + "\") RETURN sum(f.passengers)")) {
						Map<String, Object> row = res.next();
						return Math.toIntExact((Long) row.get(res.columns().get(0)));
					}
				} else {
					try (Result res = tx.execute(
						"MATCH (f:FLIGHT)-[]->(:AirportOperationDay)-[]-(:Airport)-[]-(c:Country) " +
						"WHERE c.countryId = \"" + idPaís + "\" AND date(\"" + díaInicioStr + "\") <= " +
						"date(f.dateOfDeparture) <= date(\"" + díaFinStr + "\") RETURN sum(f.passengers)")) {
						Map<String, Object> row = res.next();
						return Math.toIntExact((Long) row.get(res.columns().get(0)));
					}
				}
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
				"que calcula el número de pasajeros de cada vuelo y la operación ETL que añade las relaciones " +
				"faltantes entre aeropuerto y país antes de ejecutarla.");
		}
	}
	/**
	 * @see #getPasajerosTotales(LocalDate, LocalDate, String)
	 */
	public int getPasajerosTotales(LocalDate díaInicio, LocalDate díaFin) {
		return getPasajerosTotales(díaInicio, díaFin, "");
	}

	/**
	 * Obtiene los ingresos turísticos totales en un rango de fechas, opcionalmente filtrando por país de destino.
	 * Requiere que se haya ejecutado la operación ETL que calcula los ingresos por turismo de cada vuelo y la
	 * operación ETL que añade las relaciones faltantes entre país y aeropuerto.
	 * @param díaInicio Primer día a tener en cuenta
	 * @param díaFin Último día a tener en cuenta
	 * @param idPaís Solo se tendrán en cuenta los vuelos que tienen este país como destino, o todos
	 *               si se deja en blanco.
	 * @return Ingresos totales (en euros) entre todos los vuelos en el periodo indicado
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Añadir#añadirIngresosVuelo(Boolean, Boolean)} o la operación ETL
	 * {@link Añadir#añadirConexionesAeropuertoPaís()}.
	 */
	public double getIngresosTurísticosTotales(LocalDate díaInicio, LocalDate díaFin, String idPaís) {
		String díaInicioStr = díaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String díaFinStr = díaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Propiedades propiedades = new Propiedades(db);

		if (propiedades.getBool(Propiedad.ETL_INGRESOS_VUELO) && propiedades.getBool(Propiedad.ETL_AEROPUERTO_PAÍS)) {
			try (Transaction tx = db.beginTx()) {
				if (idPaís.isEmpty()) {
					try (Result res = tx.execute(
						"MATCH (f:FLIGHT) WHERE date(\"" + díaInicioStr + "\") <= date(f.dateOfDeparture) <= " +
						"date(\"" + díaFinStr + "\") RETURN sum(f.incomeFromTurism)")) {
						Map<String, Object> row = res.next();
						return (double) row.get(res.columns().get(0));
					}
				} else {
					try (Result res = tx.execute(
						"MATCH (f:FLIGHT)-[]->(:AirportOperationDay)-[]-(:Airport)-[]-(c:Country) " +
						"WHERE c.countryId = \"" + idPaís + "\" AND date(\"" + díaInicioStr + "\") <= " +
						"date(f.dateOfDeparture) <= date(\"" + díaFinStr + "\") RETURN sum(f.incomeFromTurism)")) {
						Map<String, Object> row = res.next();
						return (double) row.get(res.columns().get(0));
					}
				}
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
				"que calcula los ingresos por turismo de cada vuelo y la operación ETL que añade las relaciones " +
				"faltantes entre aeropuerto y país antes de ejecutarla.");
		}
	}
	/**
	 * @see #getIngresosTurísticosTotales(LocalDate, LocalDate, String)
	 */
	public double getIngresosTurísticosTotales(LocalDate díaInicio, LocalDate díaFin) {
		return getIngresosTurísticosTotales(díaInicio, díaFin, "");
	}

	/**
	 * Obtiene el valor total de conectividad entre todos los aeropuertos, opcionalmente filtrando por país.
	 * Requiere que se haya ejecutado la operación ETL que carga los datos de la conectividad de cada aeropuerto y la
	 * operación ETL que añade las relaciones faltantes entre país y aeropuerto.
	 * @param idPaís Solo se tendrán en cuenta los aeropuertos que tienen este país como destino, o todos
	 *               si se deja en blanco.
	 * @return Conectividad total entre todos los aeropuertos
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Añadir#añadirConectividad(String)} o la operación ETL {@link Añadir#añadirConexionesAeropuertoPaís()}.
	 */
	public int getConectividadTotal(String idPaís) {
		Propiedades propiedades = new Propiedades(db);

		if (propiedades.getBool(Propiedad.ETL_CONECTIVIDAD) && propiedades.getBool(Propiedad.ETL_AEROPUERTO_PAÍS)) {
			try (Transaction tx = db.beginTx()) {
				if (idPaís.isEmpty()) {
					try (Result res = tx.execute(
						"MATCH (a:Airport) RETURN sum(a.connectivity)")) {
						Map<String, Object> row = res.next();
						return Math.toIntExact((Long) row.get(res.columns().get(0)));
					}
				} else {
					try (Result res = tx.execute(
						"MATCH (a:Airport)-[]-(c:Country) " +
						"WHERE c.countryId = \"" + idPaís + "\" RETURN sum(a.connectivity)")) {
						Map<String, Object> row = res.next();
						return Math.toIntExact((Long) row.get(res.columns().get(0)));
					}
				}
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
				"que carga los datos de la conectividad de cada aeropuerto y la operación ETL que añade las relaciones " +
				"faltantes entre aeropuerto y país antes de ejecutarla.");
		}
	}
	/**
	 * @see #getConectividadTotal(String)
	 */
	public int getConectividadTotal() {
		return getConectividadTotal("");
	}
}
