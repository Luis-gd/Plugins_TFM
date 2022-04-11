package earlywarn.ejemplos;

import java.util.List;
import earlywarn.main.Consultas;
import earlywarn.main.Propiedad;
import earlywarn.main.Propiedades;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.*;
import java.time.LocalDate;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.lang3.ArrayUtils;

public class Prueba {
	@Context
	public GraphDatabaseService db;

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

	@UserFunction
	@Description("Devuelve el nº de vuelos que salen del aeropuerto indicado en el rango de días indicado")
	public Long vuelosSalida(@Name("idAeropuerto") String idAeropuerto, @Name("fechaInicio") LocalDate fechaInicio,
							 @Name("fechaFin") LocalDate fechaFin) {
		Consultas consultas = new Consultas(db);
		return consultas.getVuelosSalidaAeropuerto(idAeropuerto, fechaInicio, fechaFin);
	}

	@UserFunction
	@Description("Prueba para Propiedades.inicializadas()")
	public Boolean propInit() {
		return new Propiedades(db).inicializadas();
	}

	@UserFunction
	@Description("Prueba para Propiedades.getBool()")
	public Boolean propGetBool(@Name("nombreProp") String nombreProp) {
		return new Propiedades(db).getBool(Propiedad.valueOf(nombreProp));
	}

	@Procedure(mode = Mode.WRITE)
	@Description("Prueba para Propiedades.setBool()")
	public void propSetBool(@Name("nombreProp") String nombreProp, @Name("valor") boolean valor) {
		new Propiedades(db).setBool(Propiedad.valueOf(nombreProp), valor);
	}
}
