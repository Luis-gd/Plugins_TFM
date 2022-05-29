package earlywarn.main;

import earlywarn.definiciones.ICálculoFitness;
import earlywarn.main.modelo.criterio.Criterio;
import earlywarn.mh.vnsrs.ConversorLíneas;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;
import java.util.List;

/**
 * Builder usado para inicializar un gestor de líneas con una serie de criterios
 */
public class GestorLíneasBuilder extends GestorLíneas {
	public GestorLíneasBuilder(List<String> líneas, ConversorLíneas conversorLíneas, LocalDate díaInicio,
							   LocalDate díaFin, GraphDatabaseService db) {
		super(líneas, conversorLíneas, díaInicio, díaFin, db);
	}

	public GestorLíneasBuilder añadirCriterio(Criterio criterio) {
		_añadirCriterio(criterio);
		return this;
	}

	public GestorLíneasBuilder añadirCálculoFitness(ICálculoFitness cálculoFitness) {
		_añadirCálculoFitness(cálculoFitness);
		return this;
	}

	public GestorLíneas build() {
		return this;
	}
}
