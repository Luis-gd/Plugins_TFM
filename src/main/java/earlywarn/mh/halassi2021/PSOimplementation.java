package earlywarn.mh.halassi2021;

import earlywarn.mh.Criterios;
//import org.neo4j.cypher.internal.compiler.planner.logical.steps.index.AbstractNodeIndexSeekPlanProvider;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;

/**
 * Class implementing the PSO algorithm. 
 */
public class PSOimplementation {

	public final int numParticles = 250; //Number of particles in swarm
	public final int maxIterations = 100000; //Max number of iterations
	public final int numCriterios = 3; //Numero de criterios (multiobjetivo)
	public final int tamanoArchive = 25; //Número de soluciones dentro del archive
	// c1>c2 more beneficial to multimodal problems
	public final double c1 = 1.496180; //Cognitive coefficient
	public final double c2 = 1.496180; //Social coefficient
	public final double w = 0.729844; //Inertia coefficient
	public List<Double> r1; //Random vector 1
	public List<Double> r2;  //Random vector 2
	public List<Particle> archive = new ArrayList<>();
	List<Particle> particles; //Array to hold all particles
	long startReferencia, endReferencia;
	int seed = 1;
	Random rnd = new Random(seed);
	
	public PSOimplementation() {
		//PSO algorithm
		int i;
		particles = new ArrayList<>();
		Criterios evaluador = new Criterios();
		int numConexiones = evaluador.getNumConexiones();
		PSOEngine PSO = new PSOEngine(numParticles, numConexiones, c1, c2, w, numCriterios,tamanoArchive,evaluador);

		//Initialize particles
		PSO.initParticles(particles, rnd);

		//PSO loop
		int numIter = 0;
		try {
			FileWriter myWriter = new FileWriter("resultadosPrueba.txt");
			while (numIter<maxIterations) {
				numIter++;
				System.out.println("Iteracion: "+(numIter));

				startReferencia = System.nanoTime();

				// Evaluate fitness of each particle
				for (i=0; i<numParticles; i++) {
					Collections.copy(particles.get(i).fitness,evaluador.evaluateFitness(particles.get(i).position));

					//update personal best position
					//Comprobamos si la nueva solución domina a la anterior, si es así mejor posición pasa a ser la nueva
					if(evaluador.isDominated(evaluador.evaluateFitness(particles.get(i).personalBest),particles.get(i).fitness)){
						Collections.copy(particles.get(i).personalBest,particles.get(i).position);
					}
				}

				//Añade en el archive las soluciones no dominadas
				PSO.addSolutionsToArchive(particles, archive);

				System.out.println("Hipervolumen archive: "+PSO.calculateHypervolumeWFG(archive));

				//TODO: Poner una condición de parada mejor
				/*if(evaluador.evaluateFitness(best)<=0.229){
					numIter = 100000;
				}*/

				//Aplicamos mutación
				PSO.mutation(particles, rnd);

				//Initialize the random vectors for updates
				r1 = new ArrayList<>();
				r2 = new ArrayList<>();
				for (i=0; i<particles.get(0).velocity.size(); i++) {
					r1.add(rnd.nextDouble());
					r2.add(rnd.nextDouble());
				}

				//Update the velocity and position vectors
				for (i=0; i<numParticles;i++) {
					//Elegimos aleatoriamente una partícula dentro del archive set como global guide
					int elegido = rnd.nextInt(archive.size());
					PSO.updateVelocity(particles.get(i), archive.get(elegido).position, r1, r2);
					PSO.updatePosition(particles.get(i), rnd);
				}

				//Aplicamos crossover
				PSO.crossover(particles, rnd);

				endReferencia = System.nanoTime();
				//System.out.println("Tiempo de ejecución: "+(double)(endReferencia - startReferencia) /
				//		1_000_000_000.0);

				if(numIter%20000==1){
					for(Particle particula:archive){
						myWriter.write("Valores: [");
						for(int j=0;j<particula.fitness.size();j++){
							myWriter.write(String.valueOf(particula.fitness.get(j)));
							if(j!=particula.fitness.size()-1){
								myWriter.write(", ");
							}
						}
						myWriter.write("].\n");
					}
					myWriter.write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
				}
			}
			//Print the solutions in the archive
			for(i=0;i<archive.size();i++) {
				System.out.println("Solucion: " + i);
				//print(archive.get(i).position);
				System.out.println(archive.get(i).fitness);
				System.out.println(evaluador.evaluateFitness(archive.get(i).position));
				System.out.println(evaluador.evaluateFitnessUniobjetivo(archive.get(i).position));
				myWriter.write("Valores: [");
				for(int j=0;j<archive.get(i).fitness.size();j++){
					myWriter.write(String.valueOf(archive.get(i).fitness.get(j)));
					if(j!=archive.get(i).fitness.size()-1){
						myWriter.write(", ");
					}
				}
				myWriter.write("].\n");
			}
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}


	/**
	 * Helped method to print an array as a vector
	 * @param a The given 1-D array
	 */
	public void print (List<Boolean> a) {
		System.out.print("< ");
		for(boolean i:a){
			System.out.print(i  + " ");
		}
		System.out.println(" > ");
	}
	
	public static void main(String[] args) {

		new PSOimplementation();

		/*Criterios evaluador = new Criterios();
		PSOEngine PSO = new PSOEngine(3, 5, 1.0, 1.0, 1.0, 2,5,evaluador);
		List<Particle> particles = new ArrayList<>();
		//For each particle
		for (int i=0; i<3; i++) {
			List<Boolean> positions = new ArrayList<>();
			List<Double> velocities = new ArrayList<>();
			//Create the particle
			particles.add(new Particle(positions, velocities));
		}
		particles.get(0).fitness.add(0.3);
		particles.get(0).fitness.add(0.1);
		particles.get(1).fitness.add(0.2);
		particles.get(1).fitness.add(0.2);
		particles.get(2).fitness.add(0.1);
		particles.get(2).fitness.add(0.3);
		for (int i=0; i<3; i++) {
			for(int j=0;j<particles.get(0).fitness.size();j++){
				System.out.println(particles.get(i).fitness.get(j));
			}
		}
		Double resultado = PSO.calculateHypervolumeWFG(particles);
		System.out.println(resultado);*/
	}
}
