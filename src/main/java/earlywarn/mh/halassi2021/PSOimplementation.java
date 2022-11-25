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
	public final int maxIterations = 1001; //Max number of iterations
	public final int numCriterios = 3; //Numero de criterios (multiobjetivo)
	public final int tamanoArchive = 30; //Número de soluciones dentro del archive
	// c1>c2 more beneficial to multimodal problems
	public final boolean timeVariantAcceleration = false;
	public final double c1 = 1.496180; //Cognitive coefficient
	public final double c2 = 1.496180; //Social coefficient
	public final double w = 0.729844; //Inertia coefficient
	public final double c1initial = 2.5; //Cognitive coefficient
	public final double c2initial = 0.5; //Social coefficient
	public final double wInitial = 0.9; //Inertia coefficient
	public final double c1final = 0.5; //Cognitive coefficient
	public final double c2final = 2.5; //Social coefficient
	public final double wFinal = 0.4; //Inertia coefficient
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

				System.out.println("Iteracion: "+(numIter+1));

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

					if(timeVariantAcceleration==false){
						PSO.updateVelocity(particles.get(i), archive.get(elegido).position, r1, r2, c1, c2, w);
					}else{
						double normalizedIteration = (double)(maxIterations-1-numIter)/(maxIterations-1);
						double c1Actual = (c1final+(c1initial-c1final)*normalizedIteration);
						double c2Actual = (c2final+(c2initial-c2final)*normalizedIteration);
						double wActual = (wFinal+(wInitial-wFinal)*normalizedIteration);
						/*if(i==0){
							System.out.println("Valor normalizedIteration:"+normalizedIteration);
							System.out.println("Valor w:"+(wFinal+(wInitial-wFinal)*normalizedIteration));
							System.out.println("Valor c1:"+(c1final+(c1initial-c1final)*normalizedIteration));
							System.out.println("Valor c2:"+(c2final+(c2initial-c2final)*normalizedIteration));
						}*/
						PSO.updateVelocity(particles.get(i), archive.get(elegido).position, r1, r2, c1Actual, c2Actual,
								wActual);
					}
					PSO.updatePosition(particles.get(i), rnd);
				}

				//Aplicamos crossover
				PSO.crossover(particles, rnd);

				endReferencia = System.nanoTime();
				//System.out.println("Tiempo de ejecución: "+(double)(endReferencia - startReferencia) /
				//		1_000_000_000.0);

				numIter++;

				if(numIter%20000==10000){

					startReferencia = System.nanoTime();

					System.out.println("Hipervolumen archive: "+PSO.calculateHypervolumeWFG(archive));

					endReferencia = System.nanoTime();
					System.out.println("Tiempo de ejecución hipervolumen: "+(double)(endReferencia - startReferencia)/
							1_000_000_000.0);

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
			System.out.println("Hipervolumen archive: "+PSO.calculateHypervolumeWFG(archive));
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
		List<Particle> particles = new ArrayList<>(List.of(
				new Particle(0.99204972, 0.007230161, 0.000101515),
				new Particle(0.569071141, 0.13694727, 0.012257414),
				new Particle(0.424910278, 0.207293855, 0.037434514),
				new Particle(0.317088694, 0.276039627, 0.085331428),
				new Particle(0.214591547, 0.424292147, 0.117112519),
				new Particle(0.125186102, 0.485331342, 0.284446278),
				new Particle(0.042425896, 0.513959989, 0.537540203),
				new Particle(0.020205594, 0.506205559, 0.686893424),
				new Particle(0.004733187, 0.50890019, 0.849808562),
				new Particle(0.215711912, 0.447226849, 0.103988028),
				new Particle(0.266249825, 0.352718184, 0.091356599),
				new Particle(0.370538902, 0.238346615, 0.05972014),
				new Particle(0.421667045, 0.199715484, 0.049631677),
				new Particle(0.520926138, 0.156046976, 0.021501452),
				new Particle(0.995000869, 0.006592851, 0.000097828),
				new Particle(1, 0.005617252, 8.685001183e-15),
				new Particle(0.291616462, 0.257212374, 0.236890899),
				new Particle(0.344179534, 0.247118745, 0.082483372),
				new Particle(0.386000632, 0.221676936, 0.060044898),
				new Particle(0.442866288, 0.208983598, 0.02158329),
				new Particle(0.454402598, 0.204586847, 0.017761541),
				new Particle(0.467539871, 0.202735294, 0.012915341),
				new Particle(0.473083935, 0.200683505, 0.011994032),
				new Particle(0.48913654, 0.189834269, 0.011172056),
				new Particle(0.655168623, 0.125699911, 0.003630266)));
		//For each particle
		/*for (int i=0; i<3; i++) {
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
