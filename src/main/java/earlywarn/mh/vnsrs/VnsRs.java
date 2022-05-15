package earlywarn.mh.vnsrs;

import earlywarn.definiciones.IRecocidoSimulado;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

/**
 * Clase que implementa la metaheurística de recocido simulado + VNS
 */
public class VnsRs implements IRecocidoSimulado {
	private final GraphDatabaseService db;
	private final Log log;
	private final Config config;

	// Número máximo de iteraciones. La ejecución siempre terminará si se alcanza. -1 si no hay límite.
	private int iterMax;
	// Número total de posibles soluciones aceptadas
	private int solucionesAceptadas;

	public VnsRs(Config config, GraphDatabaseService db, Log log) {
		this.db = db;
		this.log = log;
		this.config = config;
		iterMax = -1;
	}

	/**
	 * Ejecuta la metaheurística
	 */
	public void ejecutar() {
		init();
		// TODO
	}

	@Override
	public float calcularPorcentajeAceptadas(float tInicial, int iterMax) {
		config.tInicial = tInicial;
		this.iterMax = iterMax;
		// Fijar alfa a 1 para evitar que disminuya la temperatura
		config.alfa = 1;
		ejecutar();
		return (float) solucionesAceptadas / iterMax;
	}

	/**
	 * Inicializa las variables necesarias para ejecutar el algoritmo
	 */
	private void init() {
		solucionesAceptadas = 0;
	}
}
