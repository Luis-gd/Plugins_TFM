package earlywarn.main.modelo.criterio;

import earlywarn.definiciones.IDCriterio;
import earlywarn.main.modelo.Línea;

/**
 * Representa el riesgo importado total que llega a través de la red de tráfico aéreo
 */
public class RiesgoImportado extends Criterio {
	private final int valorInicial;
	private int valorActual;

	public RiesgoImportado(int valorInicial) {
		this.valorInicial = valorInicial;
		valorActual = valorInicial;
		id = IDCriterio.RIESGO_IMPORTADO;
	}

	public int getValorInicial() {
		return valorInicial;
	}

	public int getValorActual() {
		return valorActual;
	}

	@Override
	public double getPorcentaje() {
		return 1 - (double) valorActual / valorInicial;
	}

	@Override
	public void recalcular(Línea línea, boolean abrir) {
		if (abrir) {
			valorActual += línea.getRiesgoImportado();
		} else {
			valorActual -= línea.getRiesgoImportado();
		}
	}
}
