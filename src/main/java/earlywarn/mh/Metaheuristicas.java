package earlywarn.mh;

import com.debacharya.nsgaii.datastructure.Population;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import com.debacharya.nsgaii.*;

public class Metaheuristicas {
	@UserFunction
	@Description("Suma dos numeros")
	public Long patata(@Name("k1") Long k1,@Name("k2") Long k2){
		return k1+k2;
	}
	@UserFunction
	@Description("Hola mundo")
	public String hola_mundo(){
		 return "Hola Mundo";
	}

	/*TODO: Empezar a programar o buscar la metaheurística multiobjetivo que nos sirva para hasta tres objetivos
	   y que también permita cambiar esos objetivos a restricciones. */
	@UserFunction
	@Description("Prueba nsga2")
	public double nsga2(){
		return new NSGA2().run().getLast().getAvgObjectiveValue();
	}

}

