package earlywarn.main;

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
}
