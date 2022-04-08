package earlywarn.ejemplos;

import earlywarn.main.Consultas;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.time.LocalDate;

public class Prueba {
	@Context
	public GraphDatabaseService db;

	@UserFunction
	@Description("Ejemplo que suma 2 valores")
	public Long suma(@Name("a") Long a, @Name("b") Long b) {
		return a + b;
	}

	@UserFunction
	@Description("Devuelve el nº de vuelos que salen del aeropuerto indicado en el día indicado")
	public Long vuelosSalida(@Name("idAeropuerto") String idAeropuerto, @Name("fechaInicio") LocalDate fecha) {
		Consultas consultas = new Consultas(db);
		return consultas.getVuelosSalidaAeropuerto(idAeropuerto, fecha);
	}
}
