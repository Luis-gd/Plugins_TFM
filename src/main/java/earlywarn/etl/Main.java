package earlywarn.etl;

import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

/**
 * Clase que contiene el método principal con el que se inician las operaciones ETL sobre la BD.
 */
public class Main {
	/**
	 * Ejecuta todas las operaciones ETL.
	 */
	@Procedure(mode = Mode.WRITE)
	public void mainETL() {
		Modificar modificar = new Modificar();
		modificar.convertirRelacionesAOD();
		modificar.calcularNúmeroPasajeros();
	}
}
