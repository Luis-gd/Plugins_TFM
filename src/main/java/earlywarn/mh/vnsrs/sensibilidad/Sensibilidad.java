package earlywarn.mh.vnsrs.sensibilidad;

import earlywarn.main.Consultas;
import earlywarn.main.modelo.datoid.Aeropuerto;
import earlywarn.main.modelo.ListaSoluciones;
import earlywarn.main.modelo.datoid.Línea;
import earlywarn.main.modelo.datoid.AeropuertoFactory;
import earlywarn.main.modelo.datoid.LíneaFactory;
import earlywarn.main.modelo.datoid.RegistroDatoID;
import earlywarn.mh.vnsrs.ConversorLíneas;
import earlywarn.mh.vnsrs.CriterioFactory;
import earlywarn.mh.vnsrs.config.Config;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que implementa el análisis de sensibilidad de pesos mediante simulación Monte Carlo.
 */
public class Sensibilidad {
	private final Config config;
	private final Log log;
	private final ListaSoluciones soluciones;
	private final Consultas consultas;
	private final List<String> líneas;
	private final RegistroDatoID<Aeropuerto> registroAeropuertos;
	private final RegistroDatoID<Línea> registroLíneas;
	/*
	 * Contiene los diferentes gestores usados para evaluar cada una de las posibles soluciones. Cada gestor tendrá
	 * las líneas abiertas y cerradas correspondiente a su solución.
	 */
	private final List<SoluciónEvaluable> solucionesEv;
	private final ConversorLíneas conversorLíneas;

	/**
	 * Crea una instancia de la clase que permite ejecutar el análisis
	 * @param config Fichero de configuración de Vns-Rs que contiene la lista de criterios y sus pesos
	 * @param soluciones Lista con las diferentes soluciones que se usarán como referencia durante la ejecución
	 * @param db Acceso a la base de datos
	 * @param log Log de Neo4J
	 */
	public Sensibilidad(Config config, ListaSoluciones soluciones, GraphDatabaseService db, Log log) {
		this.soluciones = soluciones;
		this.log = log;
		this.config = config;

		consultas = new Consultas(db);
		líneas = consultas.getLíneas(config.díaInicio, config.díaFin, config.país);
		conversorLíneas = new ConversorLíneas(líneas);
		solucionesEv = new ArrayList<>();

		AeropuertoFactory fAeropuertos = new AeropuertoFactory(config.díaInicio, config.díaFin, db);
		registroAeropuertos = new RegistroDatoID<>(fAeropuertos);
		LíneaFactory fLíneas = new LíneaFactory(config.díaInicio, config.díaFin, db);
		registroLíneas = new RegistroDatoID<>(fLíneas);
	}

	/**
	 * Ejecuta el análisis de sensibilidad
	 */
	public void ejecutar() {
		CriterioFactory fCriterios = new CriterioFactory(consultas, config, registroAeropuertos);
		for (List<String> cerradasSolución : soluciones) {
			SoluciónEvaluable sol = new SoluciónEvaluable(líneas, cerradasSolución, config, fCriterios, registroLíneas,
				conversorLíneas, log);
			solucionesEv.add(sol);
		}

		// TODO: Bucle principal de la simulación
	}
}
