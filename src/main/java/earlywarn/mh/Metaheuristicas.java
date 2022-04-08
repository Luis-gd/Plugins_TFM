package earlywarn.mh;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;


public class Metaheuristicas {
    @UserFunction
    @Description("Hola mundo")
    public void hola_mundo(){
        System.out.println("hola_mundo");
    }
    @UserFunction
    @Description("Suma dos valores")
    public Integer suma(@Name("n1") int n1,@Name("n2") int n2){
        return n1+n2;
    }
}
