package earlywarn.mh.vnsrs;

import java.util.Random;

/**
 * Clase que implementa los método necesarios para ejecutar la metaheurística del recocido simulado
 */
public class RecocidoSimulado {
	// Iteración actual
	public int iteración;
	// Valor cacheado de la temperatura actual. Solo se recalcula cuando se cambia de fase.
	public double temperatura;
	// Valor cacheado del número de fase. Usado para saber cuándo hemos cambiado de fase.
	public int fase;

	private final ConfigRS config;
	private final Random rand;

	public RecocidoSimulado(ConfigRS config) {
		this.config = config;
		rand = new Random();

		iteración = 0;
		temperatura = config.tInicial;
		fase = 0;
	}

	/**
	 * Avanza a la siguiente iteración, actualizando la temperatura si es necesario.
	 */
	public void sigIter() {
		iteración++;
		int faseActual = iteración / config.itReducciónT;
		if (faseActual != fase) {
			fase = faseActual;
			temperatura = config.tInicial * Math.pow(config.alfa, fase);
		}
	}

	/**
	 * Dado el fitness de dos soluciones, determina si se debe aceptar la nueva solución o si se debe mantener
	 * la actual.
	 * @param fitnessActual Fitness de la solución con la que se trabaja actualmente
	 * @param fitnessNueva Fitness de la nueva solución a considerar
	 * @return True si debería aceptarse la nueva solución, false si debería mantenerse la anterior
	 */
	public boolean considerarSolución(double fitnessActual, double fitnessNueva) {
		if (fitnessNueva > fitnessActual) {
			return true;
		} else {
			double probabilidad = Math.exp((fitnessNueva - fitnessActual) / temperatura);
			return rand.nextDouble() < probabilidad;
		}
	}
}
