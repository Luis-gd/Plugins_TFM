package earlywarn.main.modelo;

import earlywarn.definiciones.ICálculoFitness;
import earlywarn.definiciones.IDCriterio;
import earlywarn.definiciones.IllegalOperationException;
import earlywarn.main.modelo.criterio.Criterio;

import java.util.Collection;
import java.util.List;

/**
 * Calcula el fitness de una solución aplicando unos pesos predeterminados a cada criterio
 */
public class FitnessPorPesos implements ICálculoFitness {

	@Override
	public double calcularFitness(Collection<Criterio> criterios) {
		double total = 0;
		for (Criterio criterio : criterios) {
			total += getPesoCriterio(criterio.id) * criterio.getPorcentaje();
		}
		return total;
	}

	private float getPesoCriterio(IDCriterio criterio) {
		// TODO Pesos finales
		switch (criterio) {
			case RIESGO_IMPORTADO:
				return 0.333333f;
			case NÚMERO_PASAJEROS:
				return 0.083333f;
			case INGRESOS_TURÍSTICOS:
				return 0.083333f;
			case HOMOGENEIDAD_AEROLÍNEAS:
				return 0.083333f;
			case HOMOGENEIDAD_AEROPUERTOS:
				return 0.083333f;
			case CONECTIVIDAD:
				return 0.333333f;
			default:
				throw new IllegalOperationException();
		}
	}
}