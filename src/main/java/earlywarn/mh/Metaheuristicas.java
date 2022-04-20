package earlywarn.mh;

import com.debacharya.nsgaii.datastructure.Chromosome;
import com.debacharya.nsgaii.datastructure.Population;
import com.debacharya.nsgaii.objectivefunction.AbstractObjectiveFunction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.UserFunction;
import com.debacharya.nsgaii.*;
import java.util.*;


public class Metaheuristicas {
	@UserFunction
	@Description("Hola mundo Metaheuristicas")
	public String hola_mundo(){
		 return "Hola Mundo Metaheuristicas";
	}

	/*TODO: Empezar a programar o buscar la metaheurística multiobjetivo que nos sirva para hasta tres objetivos
	   y que también permita cambiar esos objetivos a restricciones. */

	@UserFunction
	@Description("Prueba nsga2")
	public String nsga2(int tamanyoPoblacion, int numeroGeneraciones, int tamanyoCromosoma){
		// Creación de la lista de objetivos de nuestro problema
		List<AbstractObjectiveFunction> objectivos = new ArrayList<>();
		// Añadimos nuestras funciones objetivo usando las clases auxiliares que hemos definido
		// Pueden ser tantas como queramos
		objectivos.add(new FuncionObjetivo2());
		objectivos.add(new FuncionObjetivo2());

		// Creamos la configuración con los nuevos parámetros
		Configuration configuracion = new Configuration(
				tamanyoPoblacion,
				numeroGeneraciones,
				tamanyoPoblacion
		);

		// Si queremos cambiar la función de cruce predeterminada podemos hacerlo siguiendo lo que viene en la
		// web https://github.com/onclave/NSGA-II/wiki/Getting-Started

		// Para la creación del cromosoma usaremos BooleanAllele

		// Añadimos las funciones objetivo que hemos creado
		configuracion.objectives = objectivos;

		// Creamos una instancia con la configuración que hemos desarrollado antes
		NSGA2 nsga2 = new NSGA2(configuracion);

		//run() returns the final child population or the pareto front
		Population paretoFront = nsga2.run();
		return "Ha salido bien!";
	}

	/*@UserFunction
	@Description("Prueba nsga2")
	public String nsga2(int populationSize,
						int generations,
						int chromosomeLength,
						PopulationProducer populationProducer,
						ChildPopulationProducer childPopulationProducer,
						GeneticCodeProducer geneticCodeProducer,
						List<AbstractObjectiveFunction> objectives,
						AbstractCrossover crossover,
						AbstractMutation mutation,
						TerminatingCriterion terminatingCriterion,
						boolean silent,
						boolean plotGraph,
						boolean writeToDisk,
						FitnessCalculator fitnessCalculator){
		return "patata";
				//new Configuration(populationSize, generations, chromosomeLength, populationProducer, childPopulationProducer,
				//geneticCodeProducer, objectives, crossover, mutation, terminatingCriterion, silent, plotGraph, writeToDisk,
				//fitnessCalculator).toString();
		//return new NSGA2().run().getLast().getAvgObjectiveValue();
	}/*

	/*@UserFunction
	@Description("Prueba nsga2Sencilla")
	public String nsga2Sencilla() {
		Configuration tomate = new Configuration();
		String s = tomate.toString();
		return "pepe";
	}*/


}

//TODO: Definir las funciones objetivo

// La información para hacer esto se puede encontrar en https://github.com/onclave/NSGA-II/wiki/Getting-Started
class FuncionObjetivo1 extends AbstractObjectiveFunction {
	@Override
	public double getValue(Chromosome chromosome) {
		return 0;
	}
}
class FuncionObjetivo2 extends AbstractObjectiveFunction {
	@Override
	public double getValue(Chromosome chromosome) {
		return 0;
	}
}

class FuncionObjetivo3 extends AbstractObjectiveFunction {
	@Override
	public double getValue(Chromosome chromosome) {
		return 0;
	}
}

