package earlywarn.ejemplos;

import java.util.List;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.lang3.ArrayUtils;

public class Prueba {
	@UserFunction
	@Description("Ejemplo que suma 2 valores")
	public Long suma(@Name("a") Long a, @Name("b") Long b) {
		return a + b;
	}

	@UserFunction
	@Description("Ejemplo de Perason Coefficient Correlation entre dos listas de numeros")
	public Double pcc(@Name("var1") List<Double> var1, @Name("var2") List<Double> var2) {
		PearsonsCorrelation pcc = new PearsonsCorrelation();
		Double[] var1Array = var1.toArray(new Double[0]);
		Double[] var2Array = var2.toArray(new Double[0]);
		return (Double) pcc.correlation(ArrayUtils.toPrimitive(var1Array), ArrayUtils.toPrimitive(var2Array));
	}
}
