package earlywarn.mh.vnsrs;

import earlywarn.definiciones.IDCriterio;
import earlywarn.definiciones.IRecocidoSimulado;
import earlywarn.definiciones.IllegalOperationException;
import earlywarn.definiciones.OperaciónLínea;
import earlywarn.main.Consultas;
import earlywarn.main.GestorLíneas;
import earlywarn.main.GestorLíneasBuilder;
import earlywarn.main.Utils;
import earlywarn.main.modelo.FitnessPorPesos;
import earlywarn.main.modelo.RegistroAeropuertos;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	private Estadísticas estadísticas;

	/*
	 * Número forzado de iteraciones a realizar. Si se fija, el algoritmo siempre terminará exactamente tras este
	 * número de iteraciones, ignorando la condición de parada habitual. Si es < 0, no tiene efecto alguno.
	 */
	private int numFijoIteraciones;
	// Número de soluciones consideradas que eran peor que la actual
	private int solucionesPeores;
	// Número de soluciones que eran peores que la actual y fueron aceptadas
	private int solucionesPeoresAceptadas;
	// Mejor solución encontrada hasta ahora (como array de booleanos) y su fitness
	private boolean[] mejorSolución;
	private double fitnessMejorSolución;
	// Número de iteración actual
	private int iter;

	public VnsRs(Config config, GraphDatabaseService db, Log log) {
		this.db = db;
		this.log = log;
		this.config = config;
		numFijoIteraciones = -1;
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
		String mensaje = "Fin ejecución VNS + RS. Mejor solución (fitness " + fitnessMejorSolución + "):\n" +
			"Líneas abiertas: " +
			Utils.listaLíneasAString(conversorLíneas.getAbiertas(mejorSolución), true) +
			"\nLíneas cerradas: " +
			Utils.listaLíneasAString(conversorLíneas.getCerradas(mejorSolución), true);
		log.info(mensaje);
	}

	/**
	 * Almacena la mejor solución encontrada en un fichero.
	 * El fichero contendrá 3 líneas:
	 * - La primera tendrá el valor de fitness de la solución
	 * - La segunda tendrá los IDs de todas las líneas abiertas separados con comas
	 * - La tercera tendrá los IDs de todas las líneas cerradas separadas con comas
	 * Requiere que se haya ejecutado el algoritmo con anterioridad.
	 * @param rutaFichero Ruta al fichero de salida
	 * @throws IllegalOperationException Si la metaheurística aún no se ha ejecutado
	 */
	public void guardarResultado(String rutaFichero) {
		if (mejorSolución == null) {
			throw new IllegalOperationException("No se puede almacenar el resultado de la metaheurística si " +
				"ésta no se ha ejecutado aún");
		} else {
			try {
				Files.createDirectory(Paths.get(rutaFichero).getParent());
			} catch (FileAlreadyExistsException e) {
				// OK
			} catch (IOException e) {
				log.warn("No se ha podido crear el directorio para almacenar el resultado de la metaheurística.\n" + e);
				return;
			}

			try (FileWriter fSalida = new FileWriter(rutaFichero)) {
				fSalida.write(fitnessMejorSolución + "\n");
				fSalida.write(Utils.listaLíneasAString(conversorLíneas.getAbiertas(mejorSolución), false) + "\n");
				fSalida.write(Utils.listaLíneasAString(conversorLíneas.getCerradas(mejorSolución), false) + "\n");
			} catch (IOException e) {
				log.warn("No se ha podido guardar el resultado de la metaheurística.\n" + e);
			}
		}
	}

	/**
	 * Almacena las estadísticas de la ejecución a un fichero CSV.
	 * Requiere que se haya ejecutado el algoritmo con anterioridad.
	 * @param rutaFichero Ruta al fichero de salida
	 * @throws IllegalOperationException Si la metaheurística aún no se ha ejecutado
	 */
	public void guardarEstadísticas(String rutaFichero) {
		if (estadísticas == null) {
			throw new IllegalOperationException("No se puede almacenar las estadísticas de la metaheurística si " +
				"ésta no se ha ejecutado aún");
		} else {
			estadísticas.toCsv(rutaFichero);
		}
	}

	@Override
	public float calcularPorcentajeAceptadas(float tInicial, int numIteraciones) {
		numFijoIteraciones = numIteraciones;
		/*
		 * Creamos la instancia de recocido simulado con datos de configuración basados en los parámetros indicados en
		 * lugar de usar los de la configuración. Alfa se fija a 1 para que la temperatura no disminuya.
		 */
		ConfigRS configRS = new ConfigRS();
		configRS.tInicial = tInicial;
		configRS.alfa = 1;
		configRS.itReducciónT = config.configRS.itReducciónT;
		rs = new RecocidoSimulado(configRS);
		config.configRS = configRS;

		init();
		_ejecutar();

		return (float) solucionesPeoresAceptadas / solucionesPeores;
	}

	/**
	 * Inicializa las variables necesarias para ejecutar el algoritmo
	 */
	private void init() {
		CriterioFactory fCriterios = new CriterioFactory(consultas, config, registroAeropuertos);
		gEntornos = new GestorEntornos(config.configVNS, conversorLíneas, líneas.size(), config.configRS.tInicial);
		gLíneas = new GestorLíneasBuilder(líneas, conversorLíneas, config.díaInicio, config.díaFin, db)
			.añadirCriterio(fCriterios.criterio(IDCriterio.RIESGO_IMPORTADO))
			.añadirCriterio(fCriterios.criterio(IDCriterio.NÚMERO_PASAJEROS))
			.añadirCriterio(fCriterios.criterio(IDCriterio.INGRESOS_TURÍSTICOS))
			.añadirCriterio(fCriterios.criterio(IDCriterio.HOMOGENEIDAD_AEROLÍNEAS))
			.añadirCriterio(fCriterios.criterio(IDCriterio.HOMOGENEIDAD_AEROPUERTOS))
			.añadirCriterio(fCriterios.criterio(IDCriterio.CONECTIVIDAD))
			.añadirCriteriosRestricciones(config, fCriterios)
			.añadirCálculoFitness(new FitnessPorPesos(config.pesos))
			.build();
		estadísticas = new Estadísticas(log);

		solucionesPeores = 0;
		solucionesPeoresAceptadas = 0;
		mejorSolución = null;
		fitnessMejorSolución = -1;
		iter = 0;
	}

	/**
	 * Ejecuta la metaheurística una vez que ésta está inicializada
	 */
	private void _ejecutar() {
		double fitnessActual = gLíneas.getFitness();
		mejorSolución = gLíneas.getLíneasBool();
		fitnessMejorSolución = fitnessActual;

		// Registrar estadísticas del estado inicial
		estadísticas.registrarIteración(new EstadísticasIteración(-1, gLíneas.getNumAbiertas(),
			new EntornoVNS(gEntornos.getEntorno()), rs.temperatura, fitnessActual, fitnessMejorSolución, 1));

		while (continuar()) {
			EntornoVNS entorno = gEntornos.getEntorno();
			log.info("Inicio iter " + iter + ". Abiertas: " + gLíneas.getNumAbiertas() + ", fitness actual: " +
				fitnessActual + ", entorno: " + entorno.operación + " " + entorno.getNumLíneas() +
				", T: " + rs.temperatura);

			List<String> líneasAVariar = getLíneasAVariar(entorno);
			int numAbiertas = gLíneas.getNumAbiertas();
			gLíneas.abrirCerrarLíneas(líneasAVariar, entorno.operación);
			double nuevoFitness = gLíneas.getFitness();

			// Verificar restricciones
			boolean factible = config.restricciones.cumple(gLíneas.getCriterios());
			if (!factible) {
				nuevoFitness = rs.penalizarFitness(fitnessActual, nuevoFitness);
			}

			// Insertar un nuevo caso en la memoria indicando la operación realizada y si hubo una mejora en el fitness
			gEntornos.registrarCasoX(
				new CasoEntornoX(numAbiertas, entorno.operación == OperaciónLínea.ABRIR, nuevoFitness > fitnessActual));

			// Comprobar si esta solución es el nuevo máximo global
			if (factible && nuevoFitness > fitnessMejorSolución) {
				fitnessMejorSolución = nuevoFitness;
				mejorSolución = gLíneas.getLíneasBool();
			}

			boolean esPeorSolución = nuevoFitness < fitnessActual;
			if (esPeorSolución) {
				solucionesPeores++;
			}
			// Comprobar si aceptamos esta nueva solución o si nos quedamos con la anterior
			double probAceptación = rs.probabilidadAceptación(fitnessActual, nuevoFitness);
			if (rs.considerarSolución(fitnessActual, nuevoFitness)) {
				fitnessActual = nuevoFitness;
				gEntornos.registrarEstadoY(líneasAVariar);
				if (esPeorSolución) {
					solucionesPeoresAceptadas++;
				}
			} else {
				gLíneas.abrirCerrarLíneas(líneasAVariar, entorno.operación.invertir());
				// Indicar que nos mantenemos en el mismo estado, es decir, no se ha variado ninguna línea
				gEntornos.registrarEstadoY(new ArrayList<>());
			}

			// Registrar estadísticas de esta iteración
			estadísticas.registrarIteración(new EstadísticasIteración(iter, gLíneas.getNumAbiertas(),
				new EntornoVNS(gEntornos.getEntorno()), rs.temperatura, fitnessActual, fitnessMejorSolución,
				probAceptación));

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
		if (numFijoIteraciones >= 0) {
			return iter < numFijoIteraciones;
		} else {
			if (iter < config.itParada) {
				return true;
			} else {
				double fitnessHaceItParadaIteraciones =
					estadísticas.listaEstadísticas.get(iter - config.itParada).fitnessMejor;
				if (config.porcentMejora == 0) {
					return fitnessMejorSolución > fitnessHaceItParadaIteraciones;
				} else {
					float porcentajeMejora = (float) (fitnessMejorSolución / fitnessHaceItParadaIteraciones);
					return porcentajeMejora - 1 >= config.porcentMejora;
				}
			}
		}
	}
}
