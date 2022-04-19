package earlywarn.etl;

import earlywarn.definiciones.ETLOperationRequiredException;
import earlywarn.definiciones.Propiedad;
import earlywarn.main.Consultas;
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
	 * Añade las conexiones entre aeropuertos y países que faltan en la BD.
	 * Estas conexiones se obtienen buscando aeropuertos situados en un ProvinceState pero que no están relacionados
	 * con Country.
	 * Fija la propiedad {@link Propiedad#ETL_AEROPUERTO_PAÍS} a true en la BD.
	 */
	@Procedure(mode = Mode.WRITE)
	public void añadirConexionesAeropuertoPaís() {
		try (Transaction tx = db.beginTx()) {
			tx.execute(
				"MATCH (a:Airport)-[]-(ps:ProvinceState)-[]-(c:Country) " +
				"MERGE (a)<-[:INFLUENCE_ZONE]-(c)");
			tx.commit();
			new Propiedades(db).setBool(Propiedad.ETL_AEROPUERTO_PAÍS, true);
		}
	}

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

	/**
	 * Añade el ratio de turistas por país (o región) y fecha (año y mes).
	 * Los datos se obtienen de un CSV importado con el nombre indicado. El formato esperado para el CSV es
	 * "countryID,provinceStateId,año,mes,porcentaje", donde:
	 * 		- countryID es el identificador del país, igual que está almacenado en la BD
	 * 		- provinceStateId es el identificador de la región, igual que está almacenado en la BD. Debe ir entre
	 * 		comillas dobles ya que contiene una coma. Si el dato es para el país completo, este campo debe estar vacío.
	 * 		- año es el año del dato
	 * 		- mes es el mes del dato
	 * 		- porcentaje es un valor entre 0 y 1 indicando el ratio de visitantes por motivos turísticos para este
	 * 		territorio y mes. El separador de decimales debe ser un punto.
	 * El CSV no debe contener una cabecera.
	 * Fija la propiedad {@link Propiedad#ETL_RATIO_TURISTAS} a true en la BD.
	 * @param rutaFichero Ruta al fichero CSV, relativa a la carpeta de import definida en la configuración de Neo4J.
	 */
	@Procedure(mode = Mode.WRITE)
	public void añadirRatioTuristas(@Name("rutaFichero") String rutaFichero) {
		try (Transaction tx = db.beginTx()) {
			tx.execute(
				"LOAD CSV FROM 'file:///" + rutaFichero + "' AS line " +
				"CALL apoc.do.when(line[1] IS NULL, \"" +
					"MATCH (c:Country {countryId: line[0]}) " +
					"MERGE (c)-[:TURIST_RATIO]->" +
					"(tr:TuristRatio {year: toInteger(line[2]), month: toInteger(line[3])}) " +
					"SET tr.ratio = toFloat(line[4])" +
				"\", \"" +
					"MATCH (:Country {countryId: line[0]})<-[:BELONGS_TO]-(ps:ProvinceState {provinceStateId: line[1]}) " +
					"MERGE (ps)-[:TURIST_RATIO]->" +
					"(tr:TuristRatio {year: toInteger(line[2]), month: toInteger(line[3])}) " +
					"SET tr.ratio = toFloat(line[4])" +
				"\", {line:line}) YIELD value RETURN value");
			tx.commit();
			new Propiedades(db).setBool(Propiedad.ETL_RATIO_TURISTAS, true);
		}
	}

	/**
	 * Añade una estimación del número de turistas a bordo de cada vuelo de llegada usando los datos de turismo.
	 * Requiere que se hayan ejecutado las operaciones ETL que añaden el ratio de turistas por región y el número
	 * de pasajeros por vuelo, así como la operación ETL que convierte las fechas de los vuelos a tipo date y la
	 * que añade las relaciones faltantes entre aeropuerto y país.
	 * Fija la propiedad {@link Propiedad#ETL_TURISTAS_VUELO} a true en la BD.
	 * @param mismaFecha Si es true, para cada vuelo se intentará buscar datos de turismo en su fecha de llegada.
	 *                   Útil si se está trabajando con vuelos pasados y se sabe que se dispone de datos de turismo
	 *                   para los mismos.
	 * @param aproximarFaltantes Si es true, los vuelos que no tengan datos asignados (porque no había datos en su
	 *                           fecha o porque mismaFecha es false) calcularán su número de turistas usando los datos
	 *                           de turismo del año más reciente de su mes de llegada. Útil si se trabaja con vuelos
	 *                           futuros y se sabe que no se dispone de datos de turismo para los mismos.
	 * @throws ETLOperationRequiredException Si no se ha ejecutado alguna de las siguientes operaciones ETL:
	 * {@link Modificar#convertirRelacionesAOD()}, {@link Modificar#calcularNúmeroPasajeros()},
	 * {@link Modificar#convertirFechasVuelos()} o {@link Añadir#añadirConexionesAeropuertoPaís()}.
	 * @throws IllegalArgumentException Si mismaFecha y aproximarFaltantes son ambos false.
	 */
	@Procedure(mode = Mode.WRITE)
	public void añadirTuristasVuelo(@Name("mismaFecha") Boolean mismaFecha,
									@Name("aproximarFaltantes") Boolean aproximarFaltantes) {
		if (!mismaFecha && !aproximarFaltantes) {
			throw new IllegalArgumentException("No tiene sentido llamar a añadirTuristasVuelo() si no se quiere usar " +
				"ni los datos de turismo presentes ni aproximar los futuros, ya que entonces el método no hace nada.");
		}

		Propiedades p = new Propiedades(db);
		if (p.getBool(Propiedad.ETL_RATIO_TURISTAS) && p.getBool(Propiedad.ETL_PASAJEROS) &&
				p.getBool(Propiedad.ETL_CONVERTIR_FECHAS_VUELOS) && p.getBool(Propiedad.ETL_AEROPUERTO_PAÍS)) {
			try (Transaction tx = db.beginTx()) {
				/*
				 * Antes de nada, limpiar los datos de número de turistas que pudiera haber de antes, ya que este
				 * código necesita saber qué vuelos han recibido datos y qué vuelos no durante la ejecución
				 */
				tx.execute("MATCH (f:FLIGHT) SET f.turists = null");

				if (mismaFecha) {
					/*
					 * Primero fijamos el valor para los vuelos que llegan a un aeropuerto cuya región tiene datos
					 * de turismo en esas fechas
					 */
					tx.execute(
						"MATCH (f:FLIGHT) " +
						"CALL { " +
							"WITH f " +
							"MATCH (f)-[]->(:AirportOperationDay)<-[]-(:Airport)<-[]-(:ProvinceState)-[]->(tr:TuristRatio) " +
							"WHERE tr.year = f.dateOfArrival.year AND tr.month = f.dateOfArrival.month " +
							"RETURN tr.ratio as ratio " +
						"} " +
						"SET f.turists = f.passengers * ratio");

					/*
					 * Habrá vuelos que se habrán quedado sin asignar porque su región no tenía datos. Para esos vuelos,
					 * probamos a usar los datos a nivel nacional.
					 */
					tx.execute(
						"MATCH (f:FLIGHT) " +
						"WHERE f.turists IS NULL " +
						"CALL { " +
							"WITH f " +
							"MATCH (f)-[]->(:AirportOperationDay)<-[]-(:Airport)<-[]-(:Country)-[]->(tr:TuristRatio) " +
							"WHERE tr.year = f.dateOfArrival.year AND tr.month = f.dateOfArrival.month " +
							"RETURN tr.ratio as ratio " +
						"} " +
						"SET f.turists = f.passengers * ratio");
				}

				/*
				 * Ahora tenemos que ver qué hacemos con los vuelos que aún no tienen datos, que serán aquellos para
				 * los que no hay datos de turismo en su país para su fecha de destino (o todos si mismaFecha era
				 * false).
				 */
				if (aproximarFaltantes) {
					/*
					 * Para cada vuelo, tenemos que buscar los datos de turismo del mismo mes más recientes
					 * que tengamos.
					 */
					Consultas consultas = new Consultas(db);
					int primerAño = consultas.getPrimerAñoDatosTurismo();
					int últimoAño = consultas.getÚltimoAñoDatosTurismo();

					for (int añoActual = últimoAño; añoActual >= primerAño; añoActual--) {
						// Probar con los datos de la región primero
						tx.execute(
							"MATCH (f:FLIGHT) " +
							"WHERE f.turists IS NULL " +
							"CALL { " +
								"WITH f " +
								"MATCH (f)-[]->(:AirportOperationDay)<-[]-(:Airport)<-[]-(:ProvinceState)-[]->(tr:TuristRatio) " +
								"WHERE tr.year = " + añoActual + " AND tr.month = f.dateOfArrival.month " +
								"RETURN tr.ratio as ratio " +
							"} " +
							"SET f.turists = f.passengers * ratio");

						// Si no hay datos, usar los del país
						tx.execute(
							"MATCH (f:FLIGHT) " +
							"WHERE f.turists IS NULL " +
							"CALL { " +
								"WITH f " +
								"MATCH (f)-[]->(:AirportOperationDay)<-[]-(:Airport)<-[]-(:Country)-[]->(tr:TuristRatio) " +
								"WHERE tr.year = " + añoActual + " AND tr.month = f.dateOfArrival.month " +
								"RETURN tr.ratio as ratio " +
							"} " +
							"SET f.turists = f.passengers * ratio");
					}
				}

				/*
				 * Llegados a este punto, no hay nada más que hacer. Los vuelos que aún no tengan un valor de turistas
				 * no tienen datos disponibles que puedan usar.
				 * Fijamos su número de turistas a 0.
				 */
				tx.execute(
					"MATCH (f:FLIGHT) " +
					"WHERE f.turists IS NULL " +
					"SET f.turists = 0");

				tx.commit();
				new Propiedades(db).setBool(Propiedad.ETL_TURISTAS_VUELO, true);
			}
		} else {
			throw new ETLOperationRequiredException("Esta operación requiere que se haya ejecutado la operación ETL " +
				"que añade los datos de turismo, la operación ETL que calcula los pasajeros de cada vuelo, la " +
				"operación ETL que convierte las fechas de vuelos a tipo date y la operación ETL que añade conexiones " +
				"faltantes entre aeropuertos y países antes de ejecutarla.");
		}
	}
}
