package earlywarn.main;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que almacena utilidades varias
 */
public class Utils {
	private static final double LN_2 = 0.6931471805599453;

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

	/**
	 * Convierte una lista de nodos XML en una lista de instancias de Element, manteniendo solo los nodos de ese
	 * tipo.
	 * @param lista Lista de nodos a procesar
	 * @return Lista con los elementos contenidos en la lista de entrada
	 */
	public static List<Element> toLista(NodeList lista) {
		List<Element> res = new ArrayList<>();
		Node actual;
		for (int i = 0; i < lista.getLength(); i++) {
			actual = lista.item(i);
			if (actual.getNodeType() == Node.ELEMENT_NODE) {
				res.add((Element) actual);
			}
		}
		return res;
	}

	public static double log2(double valor) {
		return Math.log(valor) / LN_2;
	}
}
