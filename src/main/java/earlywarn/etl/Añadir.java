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
}
