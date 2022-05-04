package earlywarn.main;

import earlywarn.definiciones.IDCriterio;
import earlywarn.main.modelo.criterio.Criterio;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;

/**
 * Builder usado para inicializar un gestor de líneas con una serie de criterios
 */
public class GestorLíneasBuilder extends GestorLíneas {
	public GestorLíneasBuilder(String idPaís, LocalDate díaInicio, LocalDate díaFin, GraphDatabaseService db) {
		super(idPaís, díaInicio, díaFin, db);
	}

	public GestorLíneasBuilder añadirCriterio(Criterio criterio) {
		_añadirCriterio(criterio);
		return this;
	}

	public GestorLíneas build() {
		return this;
	}
}
