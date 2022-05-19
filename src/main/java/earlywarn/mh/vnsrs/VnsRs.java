package earlywarn.mh.vnsrs;

import earlywarn.definiciones.IRecocidoSimulado;
import earlywarn.definiciones.OperaciónLínea;
import earlywarn.main.Consultas;
import earlywarn.main.GestorLíneas;
import earlywarn.main.GestorLíneasBuilder;
import earlywarn.main.Utils;
import earlywarn.main.modelo.FitnessPorPesos;
import earlywarn.main.modelo.RegistroAeropuertos;
import earlywarn.main.modelo.criterio.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.util.List;

/**
 * Clase que implementa la metaheurística de recocido simulado + VNS
 */
public class VnsRs implements IRecocidoSimulado {
	private final GraphDatabaseService db;
	private final Log log;
	private final Config config;
	private RecocidoSimulado rs;
	private GestorEntornos gEntornos;
	private GestorLíneas líneas;
	private final RegistroAeropuertos registroAeropuertos;
	private final Consultas consultas;

	// Número máximo de iteraciones. La ejecución siempre terminará si se alcanza. -1 si no hay límite.
	private int iterMax;
	// Número total de posibles soluciones aceptadas
	private int solucionesAceptadas;
	// Lista de líneas cerradas en la mejor solución encontrada hasta ahora y su fitness
	private List<String> mejorSolución;
	private double fitnessMejorSolución;
	// Número de iteración actual
	private int iter;
	// Número de iteraciones que hace que no se encuentra un nuevo óptimo global
	private int iteracionesSinMejora;

	// TODO: Estadísticas de la ejecución

	public VnsRs(Config config, GraphDatabaseService db, Log log) {
		this.db = db;
		this.log = log;
		this.config = config;
		iterMax = -1;
		registroAeropuertos = new RegistroAeropuertos(config.díaInicio, config.díaFin, db);
		consultas = new Consultas(db);
	}

	/**
	 * Ejecuta la metaheurística con funcionamiento y parámetros estándar
	 */
	public void ejecutar() {
		rs = new RecocidoSimulado(config.configRS);
		init();
		_ejecutar();
	}

	@Override
	public float calcularPorcentajeAceptadas(float tInicial, int iterMax) {
		this.iterMax = iterMax;
		/*
		 * Creamos la instancia de recocido simulado con datos de configuración basados en los parámetros indicados en
		 * lugar de usar los de la configuración. Alfa se fija a 1 para que la temperatura no disminuya.
		 */
		ConfigRS configRS = new ConfigRS();
		configRS.tInicial = tInicial;
		configRS.alfa = 1;
		configRS.itReducciónT = config.configRS.itReducciónT;
		rs = new RecocidoSimulado(configRS);

		init();
		_ejecutar();

		return (float) solucionesAceptadas / iterMax;
	}

	/**
	 * Inicializa las variables necesarias para ejecutar el algoritmo
	 */
	private void init() {
		gEntornos = new GestorEntornos(config.configVNS);
		líneas = new GestorLíneasBuilder(config.país, config.díaInicio, config.díaFin, db)
			.añadirCriterio(new RiesgoImportado(
				consultas.getRiesgoPorPais(config.díaInicio, config.díaFin, config.país)))
			.añadirCriterio(new NumPasajeros(
				consultas.getPasajerosTotales(config.díaInicio, config.díaFin, config.país)))
			.añadirCriterio(new IngresosTurísticos(
				consultas.getIngresosTurísticosTotales(config.díaInicio, config.díaFin, config.país)))
			.añadirCriterio(new HomogeneidadAerolíneas(
				consultas.getPasajerosPorAerolínea(config.díaInicio, config.díaFin, config.país)))
			.añadirCriterio(new HomogeneidadAeropuertos(
				consultas.getPasajerosPorAeropuerto(config.díaInicio, config.díaFin, config.país), config.país,
				registroAeropuertos))
			.añadirCriterio(new Conectividad(
				consultas.getConectividadPaís(config.díaInicio, config.díaFin, config.país), registroAeropuertos))
			.añadirCálculoFitness(new FitnessPorPesos())
			.build();

		solucionesAceptadas = 0;
		mejorSolución = null;
		fitnessMejorSolución = -1;
		iter = 0;
		iteracionesSinMejora = 0;
	}

	/**
	 * Ejecuta la metaheurística una vez que ésta está inicializada
	 */
	private void _ejecutar() {
		double fitnessActual = líneas.getFitness();

		while (continuar()) {
			EntornoVNS entorno = gEntornos.getEntorno();
			List<String> líneasAVariar = getLíneasAVariar(entorno);
			líneas.abrirCerrarLíneas(líneasAVariar, entorno.operación);
			double nuevoFitness = líneas.getFitness();

			// Comrpobar si esta solución es el nuevo máximo global
			if (nuevoFitness > fitnessMejorSolución) {
				fitnessMejorSolución = nuevoFitness;
				mejorSolución = líneas.getCerradas();
				iteracionesSinMejora = 0;
			} else {
				iteracionesSinMejora++;
			}

			// Comprobar si aceptamos esta nueva solución o si nos quedamos con la anterior
			if (rs.considerarSolución(fitnessActual, nuevoFitness)) {
				fitnessActual = nuevoFitness;
				solucionesAceptadas++;
			} else {
				líneas.abrirCerrarLíneas(líneasAVariar, entorno.operación.invertir());
			}

			iter++;
			rs.sigIter();
			gEntornos.sigIter(rs.temperatura);
		}
	}

	/**
	 * Obtiene una lista con las líneas a abrir o cerrrar en la iteración actual. Las líneas se determinan de forma
	 * aleatoria en base al entorno en el que nos encontremos.
	 * @param entorno Entorno VNS en el que nos encontramos ahora mismo
	 * @return Lista de líneas que deben ser abiertas o cerradas
	 */
	private List<String> getLíneasAVariar(EntornoVNS entorno) {
		int numLíneasPosibles;
		if (entorno.operación == OperaciónLínea.ABRIR) {
			numLíneasPosibles = líneas.getNumAbiertas();
		} else {
			numLíneasPosibles = líneas.getNumCerradas();
		}
		// Posiciones al azar en la lista de líneas que identifican las líneas a abrir o cerrar
		List<Integer> posiciones = Utils.múltiplesAleatorios(numLíneasPosibles, entorno.getNumLíneas());
		return líneas.getPorPosiciónYEstado(posiciones, entorno.operación == OperaciónLínea.CERRAR);
	}

	/**
	 * Comprueba si se cumple la condición de parada de la metaheurística o si por el contrario se debe continuar
	 * con la ejecución.
	 * @return True si la ejecución debe continuar, false si se cumple la condición de parada.
	 */
	private boolean continuar() {
		return iteracionesSinMejora < config.itParada && iter < iterMax;
	}
}
