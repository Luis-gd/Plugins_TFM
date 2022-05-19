package earlywarn.mh.vnsrs;

/**
 * Clase que almacena los valores necesarios para configurar VNS
 */
public class ConfigVNS {
	// Cada cuántas iteraciones se debería re-evaluar si es necesario un cambio de entorno
	public int itCambioEntorno;
	/*
	 * Factor de tamaño de la memoria usada para el cambio de entorno horizontal (abrir o cerrar líneas). El valor
	 * final se obtiene multiplicando por el número de líneas.
	 */
	public float tamañoMemoriaX;
	/*
	 * Al seleccionar entradas de la memoria de casos X, se elegirán los que tengan un número de líneas abiertas
	 * del +-<este parámetro>% con respecto a las que haya abiertas actualmente.
	 */
	public float distanciaMemoriaX;

	/*
	 * Hasta qué porcentaje de líneas totales se van a realizar comprobaciones para asegurar la diversidad
	 * de las soluciones
	 */
	public float maxPorcentLíneas;
	/*
	 * Número de comprobaciones totales a realizar (cada una con un porcentaje diferente, desde el máximo indicado
	 * hasta 0 (no inclusive))
	 */
	public int numComprobaciones;
	/*
	 * Dos valores usados para marcar el ritmo de diversificación. Especifican cada cuántas iteraciones se debería
	 * producir una variación en el número de líneas abiertas de al menos el porcentaje indicado.
	 */
	public float porcentLíneas;
	public int iteraciones;
	/*
	 * Determina la velocidad de operación del algoritmo al abrir y cerrar líneas cuando justo se logra una
	 * diversificación igual a la especificada con los dos parámetros anteriores.
	 */
	public float líneasPorIt;
}
