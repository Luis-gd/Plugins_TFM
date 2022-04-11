package earlywarn.definiciones;

/**
 * Lanzada al intentar ejecutar una operación sobre la BD que requiere que se haya ejecutado una cierta operación
 * ETL con anterioridad que no se ha ejecutado.
 */
// Neo4J dice que la forma correcta de indicar errores en procedimientos definidos por el usuario es lanzar
// una RuntimeException, aunque en teoría esto no debería hacerse ya que es una excepción unchecked.
@SuppressWarnings("UncheckedExceptionClass")
public class ETLOperationRequiredException extends RuntimeException {
	public ETLOperationRequiredException(String message) {
		super(message);
	}
}
