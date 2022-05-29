package earlywarn.mh.vnsrs;

import earlywarn.definiciones.OperaciónLínea;

/**
 * Representa un entorno en la metaheurística que emplea VNS
 */
public class EntornoVNS {
	public OperaciónLínea operación;
	/*
	 * Número de entorno vertical en el que nos encontramos. Determina el número de líneas a abrir o cerrar (más
	 * cuanto mayor sea el valor).
	 */
	public int numEntornoY;

	public EntornoVNS(OperaciónLínea operación, int numEntornoY) {
		this.operación = operación;
		this.numEntornoY = numEntornoY;
	}

	/**
	 * @return Número de líneas a abrir o cerrar en este entorno
	 */
	public int getNumLíneas() {
		return 1 << numEntornoY;
	}
}
