package earlywarn.main.modelo;

import earlywarn.definiciones.ICálculoFitness;
import earlywarn.definiciones.IDCriterio;
import earlywarn.main.modelo.criterio.Criterio;

import java.util.Collection;
import java.util.Map;

/**
 * Calcula el fitness de una solución aplicando unos pesos predeterminados a cada criterio
 */
public class FitnessPorPesos implements ICálculoFitness {
	private final Map<IDCriterio, Float> pesos;

	public FitnessPorPesos(Map<IDCriterio, Float> pesos) {
		this.pesos = pesos;
	}

	@Override
	public double calcularFitness(Collection<Criterio> criterios) {
		double total = 0;
		for (Criterio criterio : criterios) {
			total += getPesoCriterio(criterio.id) * criterio.getPorcentaje();
		}
		return total;
	}

	/**
	 * Devuelve el peso asignado a un cierto criterio
	 * @param criterio Critero cuyo peso se quiere obtener
	 * @throws IllegalArgumentException Si el criterio indicado no tiene un peso asignado
	 * @return Peso del criterio indicado
	 */
	public float getPesoCriterio(IDCriterio criterio) {
		Float peso = pesos.get(criterio);
		if (peso != null) {
			return peso;
		} else {
			throw new IllegalArgumentException("El criterio especificado (" + criterio + ") no tiene un peso asignado.");
		}
	}

	/**
	 * Fija el peso de un criterio
	 * @param criterio Critero cuyo peso se quiere fijar
	 * @param valor Valor al que fijar el peso del criterio
	 */
	public void setPesoCriterio(IDCriterio criterio, float valor) {
		pesos.put(criterio, valor);
	}
}
