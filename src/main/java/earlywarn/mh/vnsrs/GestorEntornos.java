package earlywarn.mh.vnsrs;

import earlywarn.definiciones.OperaciónLínea;

import java.util.*;

/**
 * Clase que implementa los métodos correspondientes con la gestión de entornos de VNS
 */
public class GestorEntornos {
	private final Random random;
	private final ConfigVNS config;
	// Número total de líneas
	private final int numLíneas;
	// Número de iteraciones restantes hasta que se tenga que considerar otro posible cambio de entorno
	private int sigCambioEntorno;
	private final EntornoVNS entornoActual;

	private final MemoriaCasosX casosX;

	public GestorEntornos(ConfigVNS config, int numLíneas) {
		random = new Random();
		this.config = config;
		this.numLíneas = numLíneas;
		sigCambioEntorno = config.itCambioEntorno;
		entornoActual = new EntornoVNS(OperaciónLínea.CERRAR, 0);
		casosX = new MemoriaCasosX(Math.round(config.tamañoMemoriaX * numLíneas), numLíneas);
	}

	/*
	 * Devuelve el entorno de VNS en el que nos encontramos ahora mismo
	 */
	public EntornoVNS getEntorno() {
		return entornoActual;
	}

	/**
	 * Añade un nuevo caso a la memoria de casos usada para los cambios de entorno horizontales
	 * @param caso Caso que contiene información acerca de la acción realizada en esta iteración y si se
	 *             logró mejorar el fitness con respecto al valor anterior o no
	 */
	public void registrarCasoX(CasoEntornoX caso) {
		casosX.añadir(caso, caso.numLíneasAbiertas);
	}

	/**
	 * Usado para indicar que se ha completado una iteración. Comprueba si es necesario realizar un cambio de
	 * entorno y lo realiza si es así.
	 * @param numLíneasAbiertas Número de líneas actualmente abiertas
	 * @param temperaturaActual Temperatura actual del recocido simulado tras finalizar la iteración actual
	 */
	public void sigIter(int numLíneasAbiertas, double temperaturaActual) {
		sigCambioEntorno--;
		if (sigCambioEntorno <= 0) {
			cambioEntorno(numLíneasAbiertas, temperaturaActual);
			sigCambioEntorno += config.itCambioEntorno;
		}
	}

	/**
	 * Ejecuta el procedimiento de cambio de entorno, que podrá o no pasar a un entorno diferente del actual.
	 * @param numLíneasAbiertas Número de líneas actualmente abiertas
	 * @param temperaturaActual Temperatura actual del recocido simulado tras finalizar la iteración actual
	 */
	private void cambioEntorno(int numLíneasAbiertas, double temperaturaActual) {
		// No fijamos los valores directamente para asegurarnos de que la operación es atómica
		OperaciónLínea entornoX = entornoX(numLíneasAbiertas);
		int numEntornoY = numEntornoY(temperaturaActual);
		entornoActual.operación = entornoX;
		entornoActual.numEntornoY = numEntornoY;
	}

	/**
	 * @param numLíneasAbiertas Número de líneas actualmente abiertas
	 * @return Operación horizontal (cierre o apertura de líneas) que se debería realizar en el próximo entorno
	 */
	private OperaciónLínea entornoX(int numLíneasAbiertas) {
		// Seleccionar los casos que votarán cuál debe ser la siguiente operación
		int numMinLíneas = Math.round(numLíneasAbiertas - numLíneas * config.distanciaMemoriaX);
		int numMaxLíneas = Math.round(numLíneasAbiertas + numLíneas * config.distanciaMemoriaX);
		if (numMaxLíneas < 0) {
			numMinLíneas = 0;
		}
		if (numMaxLíneas > numLíneas) {
			numMaxLíneas = numLíneas;
		}
		List<CasoEntornoX> casos = casosX.getCasosEnRango(numMinLíneas, numMaxLíneas);

		/*
		 * Recorremos los casos. Cada caso tiene un peso que depende de la diferencia entre su número de líneas
		 * abiertas y el número de líneas actualmente abiertas. Los casos que tengan la mayor diferencia tendrán un
		 * peso de 1 solo voto. Por cada unidad más cerca del número de líneas actual que esté el caso, éste gana 1
		 * voto más.
		 */
		int diferenciaMax = Math.max(numLíneasAbiertas - numMinLíneas, numMaxLíneas - numLíneasAbiertas);
		int votosAbrir = 0;
		int votosCerrar = 0;
		for (CasoEntornoX caso : casos) {
			int diferencia = Math.abs(caso.numLíneasAbiertas - numLíneasAbiertas);
			int peso = diferenciaMax - diferencia + 1;
			if (caso.getOperación() == OperaciónLínea.ABRIR) {
				votosAbrir += peso;
			} else {
				votosCerrar += peso;
			}
		}
		// Elegimos la operación a realizar usando los pesos como probabilidades
		if (random.nextDouble() < (double) votosAbrir / (votosAbrir + votosCerrar)) {
			return OperaciónLínea.ABRIR;
		} else {
			return OperaciónLínea.CERRAR;
		}
	}

	/**
	 * @return Número de entorno vertical (afecta al número de líneas a abrir o cerrar) del próximo entorno
	 */
	private int numEntornoY(double temperaturaActual) {
		// TODO
		throw new IllegalStateException("No implementado");
	}
}
