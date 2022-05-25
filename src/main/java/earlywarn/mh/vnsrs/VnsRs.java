package earlywarn.mh.vnsrs;

import earlywarn.definiciones.IRecocidoSimulado;
import earlywarn.definiciones.IllegalOperationException;
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

import java.util.ArrayList;
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
	private GestorLíneas gLíneas;
	private final RegistroAeropuertos registroAeropuertos;
	private final Consultas consultas;
	private final List<String> líneas;
	private final ConversorLíneas conversorLíneas;

	// Número máximo de iteraciones. La ejecución siempre terminará si se alcanza. -1 si no hay límite.
	private int iterMax;
	// Número total de posibles soluciones aceptadas
	private int solucionesAceptadas;
	// Mejor solución encontrada hasta ahora (como array de booleanos) y su fitness
	private boolean[] mejorSolución;
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
		iterMax = Integer.MAX_VALUE;
		registroAeropuertos = new RegistroAeropuertos(config.díaInicio, config.díaFin, db);
		consultas = new Consultas(db);
		líneas = consultas.getLíneas(config.díaInicio, config.díaFin, config.país);
		conversorLíneas = new ConversorLíneas(líneas);
	}

	/**
	 * Ejecuta la metaheurística con funcionamiento y parámetros estándar
	 */
	public void ejecutar() {
		rs = new RecocidoSimulado(config.configRS);
		init();
		_ejecutar();
	}

	/**
	 * Printea la lista de líneas abiertas y cerradas de la mejor solución encontrada tras la ejecución del algoritmo.
	 * Requiere que se haya ejecutado el algoritmo con anterioridad.
	 * @throws IllegalOperationException Si la metaheurística aún no se ha ejecutado
	 */
	public void printResultado() {
		if (mejorSolución == null) {
			throw new IllegalOperationException("No se puede printear el resultado de la metaheurística si ésta no " +
				"se ha ejecutado aún");
		}
		String mensaje = "Fin ejecución VNS + RS. Mejor solución:\n" +
			"Líneas abiertas: " +
			Utils.listaLíneasAString(conversorLíneas.getAbiertas(mejorSolución)) +
			"\nLíneas cerradas: " +
			Utils.listaLíneasAString(conversorLíneas.getCerradas(mejorSolución)) + "\n";
		log.info(mensaje);
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
		gEntornos = new GestorEntornos(config.configVNS, conversorLíneas, líneas.size(), config.configRS.tInicial);
		gLíneas = new GestorLíneasBuilder(líneas, conversorLíneas, config.díaInicio, config.díaFin, db)
			.añadirCriterio(new RiesgoImportado(
				consultas.getRiesgoPorPaís(config.díaInicio, config.díaFin, config.país)))
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
		double fitnessActual = gLíneas.getFitness();

		while (continuar()) {
			EntornoVNS entorno = gEntornos.getEntorno();
			List<String> líneasAVariar = getLíneasAVariar(entorno);
			int numAbiertas = gLíneas.getNumAbiertas();
			gLíneas.abrirCerrarLíneas(líneasAVariar, entorno.operación);
			double nuevoFitness = gLíneas.getFitness();

			// Insertar un nuevo caso en la memoria indicando la operación realizada y si hubo una mejora en el fitness
			gEntornos.registrarCasoX(
				new CasoEntornoX(numAbiertas, entorno.operación == OperaciónLínea.ABRIR, nuevoFitness > fitnessActual));

			// Comprobar si esta solución es el nuevo máximo global
			if (nuevoFitness > fitnessMejorSolución) {
				fitnessMejorSolución = nuevoFitness;
				mejorSolución = gLíneas.getLíneasBool();
				iteracionesSinMejora = 0;
			} else {
				iteracionesSinMejora++;
			}

			// Comprobar si aceptamos esta nueva solución o si nos quedamos con la anterior
			if (rs.considerarSolución(fitnessActual, nuevoFitness)) {
				fitnessActual = nuevoFitness;
				solucionesAceptadas++;
				gEntornos.registrarEstadoY(líneasAVariar);
			} else {
				gLíneas.abrirCerrarLíneas(líneasAVariar, entorno.operación.invertir());
				// Indicar que nos mantenemos en el mismo estado, es decir, no se ha variado ninguna línea
				gEntornos.registrarEstadoY(new ArrayList<>());
			}

			iter++;
			rs.sigIter();
			gEntornos.sigIter(gLíneas.getNumAbiertas(), rs.temperatura);
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
			numLíneasPosibles = gLíneas.getNumCerradas();
		} else {
			numLíneasPosibles = gLíneas.getNumAbiertas();
		}
		// Posiciones al azar en la lista de líneas que identifican las líneas a abrir o cerrar
		List<Integer> posiciones = Utils.múltiplesAleatorios(numLíneasPosibles, entorno.getNumLíneas());
		return gLíneas.getPorPosiciónYEstado(posiciones, entorno.operación == OperaciónLínea.CERRAR);
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
