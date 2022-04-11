package earlywarn.ejemplos;

import earlywarn.main.Consultas;
import earlywarn.main.Propiedad;
import earlywarn.main.Propiedades;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.*;

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
