package earlywarn.main;

import java.util.List;

/**
 * Clase que almacena utilidades varias
 */
public class Utils {
	/**
	 * Convierte un resultado de una consulta de Neo4J que puede ser un Long o un Double a un Double.
	 * Esto puede pasar al usar funciones de agregación sobre un campo con decimales, ya que un resultado de 0 se
	 * devuelve como un Long (por alguna razón).
	 * @param resultado Resultado obtenido de la consulta de Neo4J. Debe ser un Long o un Double.
	 * @return El resultado como tipo Double
	 */
	public static Double resultadoADouble(Object resultado) {
		//noinspection ChainOfInstanceofChecks
		if (resultado instanceof Double) {
			return (Double) resultado;
		} else if (resultado instanceof Long) {
			return ((Long) resultado).doubleValue();
		} else {
			throw new IllegalArgumentException("El resultado especificado no es un Long ni un Double");
		}
	}

	/**
	 * Calcula la media de una lista de valores decimales
	 * @param valores Lista de valores
	 * @return Media de la lista de valores
	 */
	public static double getMedia(List<Double> valores) {
		double total = 0;
		for (Double valor : valores) {
			total += valor;
		}
		return total / valores.size();
	}

	/**
	 * Calcula la desviación típica de una lista de valores decimales
	 * @param valores Lista de valores
	 * @return Desvuación típica de la lista de valores
	 */
	public static double getStd(List<Double> valores) {
		double totalCuadrados = 0;
		double media = getMedia(valores);
		for (Double valor : valores) {
			totalCuadrados += Math.pow(valor - media, 2);
		}
		return Math.sqrt(totalCuadrados / valores.size());
	}

	/**
	 * Calcula la desviación típica máxima posible en un conjunto de datos del tamaño indicado, asumiendo que todos los
	 * elementos toman valores entre 0 y 1.
	 * @param númeroDatos Número de elementos en el conjunto de datos
	 * @return Desviación típica máxima que puede obtenerse variando los valores del conjunto de datos entre 0 y 1
	 */
	public static double getStdMáxima(int númeroDatos) {
		int numElemAltos = (int) Math.floor(númeroDatos);
		int numElemBajos = (int) Math.ceil(númeroDatos);
		float media = (float) numElemAltos / númeroDatos;

		return Math.sqrt(((1 - media) * numElemAltos + (0 - media) * numElemBajos) / númeroDatos);
	}
}
