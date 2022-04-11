package earlywarn.mh;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

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
}

