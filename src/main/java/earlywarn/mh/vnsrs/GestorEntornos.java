package earlywarn.mh.vnsrs;

import earlywarn.definiciones.OperaciónLínea;

/**
 * Clase que implementa los métodos correspondientes con la gestión de entornos de VNS
 */
public class GestorEntornos {
	private final ConfigVNS config;
	// Número de iteraciones restantes hasta que se tenga que considerar otro posible cambio de entorno
	private int sigCambioEntorno;
	private EntornoVNS entornoActual;

	public GestorEntornos(ConfigVNS config) {
		this.config = config;
		sigCambioEntorno = config.itCambioEntorno;
		entornoActual = new EntornoVNS(OperaciónLínea.CERRAR, 0);
	}

	/*
	 * Devuelve el entorno de VNS en el que nos encontramos ahora mismo
	 */
	public EntornoVNS getEntorno() {
		return entornoActual;
	}

	/**
	 * Usado para indicar que se ha completado una iteración. Comprueba si es necesario realizar un cambio de
	 * entorno y lo realiza si es así.
	 * @param temperaturaActual Temperatura actual del recocido simulado tras finalizar la iteración actual
	 */
	public void sigIter(double temperaturaActual) {
		sigCambioEntorno--;
		if (sigCambioEntorno <= 0) {
			cambioEntorno(temperaturaActual);
			sigCambioEntorno += config.itCambioEntorno;
		}
	}

	/**
	 * Ejecuta el procedimiento de cambio de entorno, que podrá o no pasar a un entorno diferente del actual.
	 * @param temperaturaActual Temperatura actual del recocido simulado tras finalizar la iteración actual
	 */
	private void cambioEntorno(double temperaturaActual) {
		// TODO
	}
}
