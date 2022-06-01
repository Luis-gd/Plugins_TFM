package earlywarn.mh.vnsrs;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;

/**
 * Clase desde la que se ejecuta la metaheurística de recocido simulado + VNS
 */
public class Main {
	/*
	 * Usado al determinar la temperatura inicial a usar para el recocido simulado. Se escogerá una
	 * temperatura que acepte al este porcentaje de soluciones, con una diferencia máxima menor o igual al
	 * valor de tolerancia.
	 */
	private static final float PORCENTAJE_ACEPTACIÓN_INICIAL = 0.9f;
	private static final float TOLERANCIA_ACEPTACIÓN_INICIAL = 0.005f;
	/*
	 * Número de iteraciones que se deberían realizar para obtener una estimación del porcentaje de soluciones
	 * aceptadas por cada valor de temperatura con el que se pruebe
	 */
	private static final int ITERACIONES_ACEPTACIÓN_INICIAL = 1000;

	private static final String RUTA_CONFIG = "import/config_vnsrs.xml";
	private static final String RUTA_RESULTADO = "export/resultado_vnsrs.txt";
	private static final String RUTA_ESTADÍSTICAS = "export/stats_vnsrs.csv";

	@Context
	public GraphDatabaseService db;
	@Context
	public Log log;

	@Procedure
	public void vnsRs() {
		log.info("Inicio metaheurística");
		Config config = new Config(RUTA_CONFIG);
		VnsRs vnsrs = new VnsRs(config, db, log);
		vnsrs.ejecutar();
		vnsrs.printResultado();
		vnsrs.guardarResultado(RUTA_RESULTADO);
		vnsrs.guardarEstadísticas(RUTA_ESTADÍSTICAS);
	}

	/**
	 * @return Temperatura inicial sugerida para obtener una tasa de aceptación de soluciones inicial cercana al
	 * valor especificado en la constante de esta clase.
	 */
	@UserFunction
	public Double calcularTInicial() {
		Config config = new Config(RUTA_CONFIG);
		VnsRs vnsrs = new VnsRs(config, db, log);
		CalculadoraTInicial calculadora = new CalculadoraTInicial(vnsrs);
		return ((Float) calculadora.determinarTInicial(PORCENTAJE_ACEPTACIÓN_INICIAL, TOLERANCIA_ACEPTACIÓN_INICIAL,
			ITERACIONES_ACEPTACIÓN_INICIAL)).doubleValue();
	}
}
