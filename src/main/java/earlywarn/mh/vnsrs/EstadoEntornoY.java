package earlywarn.mh.vnsrs;

/**
 * Representa una entrada en la memoria de últimas posiciones usada en los cambios de entorno verticales
 */
public class EstadoEntornoY {
	// Indica si cada línea está abierta o cerrada
	public boolean[] líneas;
	// Distancia de Hamming entre esta entrada y el estado actual
	public int distancia;

	public EstadoEntornoY(int numLíneas) {
		líneas = new boolean[numLíneas];
		for (int i = 0; i < numLíneas; i++) {
			líneas[i] = true;
		}
		distancia = -1;
	}

	public EstadoEntornoY(EstadoEntornoY otra) {
		líneas = otra.líneas.clone();
		distancia = otra.distancia;
	}
}
