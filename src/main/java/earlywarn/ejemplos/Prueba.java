package earlywarn.ejemplos;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class Prueba {
	@UserFunction
	@Description("Ejemplo que suma 2 valores")
	public Long suma(@Name("a") Long a, @Name("b") Long b) {
		return a + b;
	}
}
