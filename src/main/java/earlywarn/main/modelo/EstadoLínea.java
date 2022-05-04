package earlywarn.main.modelo;

/**
 * Representa una línea junto con su estado (abierta o cerrada) durante la ejecución del programa
 */
public class EstadoLínea {
	public Línea línea;
	public boolean abierta;

	public EstadoLínea(Línea línea, boolean abierta) {
		this.línea = línea;
		this.abierta = abierta;
	}
}
