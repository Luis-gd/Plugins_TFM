package earlywarn.main;

import earlywarn.definiciones.ETLOperationRequiredException;
import earlywarn.definiciones.Propiedad;
import earlywarn.etl.Añadir;
import earlywarn.etl.Modificar;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Representa una línea de conexión entre dos aeropuertos que engloba todos los vuelos entre ellos en un cierto
 * rango de fechas.
 * Esta clase permite consultar ciertas propiedades de la línea (como el número total de pasajeros o de vuelos)
 * minimizando el número de accesos a la BD (solo se accede la primera vez que se consulta un dato, después el dato
 * se cachea).
 */
public class Línea {
	private final GraphDatabaseService db;
	public final String id;
	public final String idAeropuertoOrigen;
	public final String idAeropuertoDestino;
	private final String díaInicio;
	private final String díaFin;
	private final Propiedades propiedades;

	// Valores cacheados
	private Long pasajeros;
	private Double ingresosTurísticos;
	private Map<String, Long> pasajerosPorAerolínea;
	private Long numVuelos;
	private Double riesgoImportado;

	/**
	 * Crea una instancia de la clase
	 * @param id Identificador de la línea. Formado por el código IATA del aeropuerto de origen, un guión y el
	 *           código IATA del aerpuerto destino.
	 * @param díaInicio Primer día a tener en cuenta al obtener datos de vuelos que viajan por esta línea
	 * @param díaFin Último día a tener en cuenta al obtener datos de vuelos que viajan por esta línea
	 * @param db Conexión a la BD
	 */
	public Línea(String id, LocalDate díaInicio, LocalDate díaFin, GraphDatabaseService db) {
		this.id = id;
		this.db = db;
		this.díaInicio = díaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		this.díaFin = díaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		propiedades = new Propiedades(db);

		String[] split = id.split("-");
		idAeropuertoOrigen = split[0];
		idAeropuertoDestino = split[1];
	}

	/**
	 * Obtiene el número total de pasajeros que circulan por esta línea en el periodo establecido.
	 * Requiere que se haya ejecutado la operación ETL que calcula el número de pasajeros por vuelo y la operación
	 * ETL que convierte las fechas de vuelos a tipo date.
	 * @return Número de pasajeros que circulan por esta línea en el periodo establecido.
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Añadir#calcularNúmeroPasajeros()} o la operación ETL {@link Modificar#convertirFechasVuelos()}.
	 */
	public long getPasajeros() {
		if (pasajeros == null) {
			if (propiedades.getBool(Propiedad.ETL_PASAJEROS) &&
			propiedades.getBool(Propiedad.ETL_CONVERTIR_FECHAS_VUELOS)) {
				try (Transaction tx = db.beginTx()) {
					try (Result res = tx.execute(
						"MATCH (a1:Airport)-[]-(aod1:AirportOperationDay)-[]->(f:FLIGHT)-[]->" +
						"(aod2:AirportOperationDay)-[]-(a2:Airport) " +
						"WHERE a1.iata = \"" + idAeropuertoOrigen + "\" AND a2.iata = \"" + idAeropuertoDestino +
						"\" AND date(\"" + díaInicio + "\") <= f.dateOfDeparture <= date(\"" + díaFin + "\") " +
						"RETURN sum(f.passengers)")) {

						Map<String, Object> row = res.next();
						pasajeros = (Long) row.get(res.columns().get(0));
					}
				}
			} else {
				throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
					"que calcula el número de pasajeros de cada vuelo y la operación ETL que convierte las fechas de " +
					"vuelos a tipo date antes de ejecutarla.");
			}
		}
		return pasajeros;
	}

	/**
	 * Obtiene los ingresos totales por turismo derivados de los vuelos que circulan por esta línea en el periodo
	 * establecido.
	 * Requiere que se haya ejecutado la operación ETL que calcula los ingresos por turismo de cada vuelo y la operación
	 * ETL que convierte las fechas de vuelos a tipo date.
	 * @return Ingresos totales derivados del turismo de los vuelos de esta línea
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Añadir#añadirIngresosVuelo(Boolean, Boolean)} o la operación ETL {@link Modificar#convertirFechasVuelos()}.
	 */
	public double getIngresosTurísticos() {
		if (ingresosTurísticos == null) {
			if (propiedades.getBool(Propiedad.ETL_INGRESOS_VUELO) &&
				propiedades.getBool(Propiedad.ETL_CONVERTIR_FECHAS_VUELOS)) {
				try (Transaction tx = db.beginTx()) {
					try (Result res = tx.execute(
						"MATCH (a1:Airport)-[]-(aod1:AirportOperationDay)-[]->(f:FLIGHT)-[]->" +
						"(aod2:AirportOperationDay)-[]-(a2:Airport) " +
						"WHERE a1.iata = \"" + idAeropuertoOrigen + "\" AND a2.iata = \"" + idAeropuertoDestino +
						"\" AND date(\"" + díaInicio + "\") <= f.dateOfDeparture <= date(\"" + díaFin + "\") " +
						"RETURN sum(f.incomeFromTurism)")) {

						Map<String, Object> row = res.next();
						ingresosTurísticos = (Double) row.get(res.columns().get(0));
					}
				}
			} else {
				throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
					"que calcula los ingresos por turismo de cada vuelo y la operación ETL que convierte las fechas de " +
					"vuelos a tipo date antes de ejecutarla.");
			}
		}
		return ingresosTurísticos;
	}

	/**
	 * Obtiene el número total de vuelos que circulan por esta línea en el periodo establecido.
	 * Requiere que se haya ejecutado la operación ETL que convierte las fechas de vuelos a tipo date.
	 * @return Número de pasajeros que circulan por esta línea en el periodo establecido.
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Modificar#convertirFechasVuelos()}.
	 */
	public long getNumVuelos() {
		if (numVuelos == null) {
			if (propiedades.getBool(Propiedad.ETL_CONVERTIR_FECHAS_VUELOS)) {
				try (Transaction tx = db.beginTx()) {
					try (Result res = tx.execute(
						"MATCH (a1:Airport)-[]-(aod1:AirportOperationDay)-[]->(f:FLIGHT)-[]->" +
						"(aod2:AirportOperationDay)-[]-(a2:Airport) " +
						"WHERE a1.iata = \"" + idAeropuertoOrigen + "\" AND a2.iata = \"" + idAeropuertoDestino +
						"\" AND date(\"" + díaInicio + "\") <= f.dateOfDeparture <= date(\"" + díaFin + "\") " +
						"RETURN count(f)")) {

						Map<String, Object> row = res.next();
						numVuelos = (Long) row.get(res.columns().get(0));
					}
				}
			} else {
				throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
					"que convierte las fechas de vuelos a tipo date antes de ejecutarla.");
			}
		}
		return numVuelos;
	}

	/**
	 * Obtiene el riesgo importado total de los vuelos que circulan por esta línea en el periodo establecido.
	 * Requiere que se haya ejecutado la operación ETL que convierte las fechas de vuelos a tipo date.
	 * @return Riesgo importado total de los vuelos de esta línea
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Modificar#convertirFechasVuelos()}.
	 */
	public double getRiesgoImportado() {
		if (riesgoImportado == null) {
			if (propiedades.getBool(Propiedad.ETL_CONVERTIR_FECHAS_VUELOS)) {
				try (Transaction tx = db.beginTx()) {
					try (Result res = tx.execute(
						"MATCH (a1:Airport)-[]-(aod1:AirportOperationDay)-[]->(f:FLIGHT)-[]->" +
						"(aod2:AirportOperationDay)-[]-(a2:Airport) " +
						"WHERE a1.iata = \"" + idAeropuertoOrigen + "\" AND a2.iata = \"" + idAeropuertoDestino +
						"\" AND date(\"" + díaInicio + "\") <= f.dateOfDeparture <= date(\"" + díaFin + "\") " +
						"RETURN sum(f.flightIfinal)")) {

						Map<String, Object> row = res.next();
						riesgoImportado = (Double) row.get(res.columns().get(0));
					}
				}
			} else {
				throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
					"que convierte las fechas de vuelos a tipo date antes de ejecutarla.");
			}
		}
		return riesgoImportado;
	}

	/**
	 * Obtiene el número de pasajeros por aerolínea de los vuelos que circulan por esta línea en el periodo establecido.
	 * Requiere que se haya ejecutado la operación ETL que calcula el número de pasajeros a bordo de cada vuelo y la
	 * operación ETL que convierte las fechas de vuelos a tipo date.
	 * Se excluyen los pasajeros de vuelos cuya aerolínea se desconoce.
	 * @return Mapa que relaciona códigos de aerolíneas con el número de pasajeros que viajan con cada una
	 * en el rango de fechas establecido.
	 * @throws ETLOperationRequiredException Si no se ha ejecutado la operación ETL
	 * {@link Añadir#calcularNúmeroPasajeros()} o la operación ETL {@link Modificar#convertirFechasVuelos()}.
	 */
	public Map<String, Long> getPasajerosPorAerolínea() {
		if (pasajerosPorAerolínea == null) {
			if (propiedades.getBool(Propiedad.ETL_PASAJEROS) &&
			propiedades.getBool(Propiedad.ETL_CONVERTIR_FECHAS_VUELOS)) {
				try (Transaction tx = db.beginTx()) {
					try (Result res = tx.execute(
						"MATCH (a1:Airport)-[]-(aod1:AirportOperationDay)-[]->(f:FLIGHT)-[]->" +
						"(aod2:AirportOperationDay)-[]-(a2:Airport) " +
						"WHERE a1.iata = \"" + idAeropuertoOrigen + "\" AND a2.iata = \"" + idAeropuertoDestino +
						"\" AND date(\"" + díaInicio + "\") <= f.dateOfDeparture <= date(\"" + díaFin + "\") " +
						"RETURN distinct(f.operator), sum(f.passengers)")) {

						pasajerosPorAerolínea = new TreeMap<>();
						List<String> columnas = res.columns();
						while (res.hasNext()) {
							Map<String, Object> row = res.next();
							String aerolínea = (String) row.get(columnas.get(0));
							if (!aerolínea.equals(Consultas.AEROLÍNEA_DESCONOCIDA)) {
								pasajerosPorAerolínea.put(aerolínea, (Long) row.get(columnas.get(1)));
							}
						}
					}
				}
			} else {
				throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
					"que calcula el número de pasajeros de cada vuelo y la operación ETL que convierte las fechas de " +
					"vuelos a tipo date antes de ejecutarla.");
			}
		}
		return pasajerosPorAerolínea;
	}
}
