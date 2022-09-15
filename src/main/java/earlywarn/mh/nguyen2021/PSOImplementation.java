package earlywarn.mh.nguyen2021;

import earlywarn.mh.Criterios;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing the PSO algorithm.
 */
public class PSOImplementation {

    //TODO:Arreglar como se carga el valor
    final int numDimensions = 30; //Number of dimensions for problem
    final int numParticles = 30; //Number of particles in swarm
    final int maxIterations = 10000; //Max number of iterations
    final double Is = 4/numDimensions; //Stickiness factor
    final double Ig = (1-Is)/3;//Social factor
    final double Ip = 2*Ig;//Cognitive factor
    public double linearDecayStickiness = 1/(8*maxIterations/100); //Decay of the stickiness in every iteration
    List<Boolean> best;
    List<Particle> particles; //Array to hold all particles

    public PSOImplementation() {
        //PSO algorithm

        particles = new ArrayList<>();
        PSOEngine PSO = new PSOEngine(numDimensions, numParticles, maxIterations, Is, Ig, Ip, linearDecayStickiness);
        //Criterios.initCriterios();

        //Initialize particles
        PSO.initParticles(particles);

        //PSO loop
        int numIter = 0;
        int i;
        /*while (numIter<maxIterations) {
            // Evaluate fitness of each particle
            for (i=0; i<numParticles; i++) {
                particles.get(i).fitness = Criterios.evaluateFitness(particles.get(i).position);

                //update personal best position
                if (particles.get(i).fitness <= Criterios.evaluateFitness(particles.get(i).personalBest)) {
                    for(int j=0;j<particles.get(i).position.size();j++){
                        particles.get(i).personalBest.set(j,particles.get(i).position.get(j));
                    }
                }
            }
            //Find best particle in set
            for(i=0;i<PSO.findBest(particles).size();i++){
                best.set(i,PSO.findBest(particles).get(i));
            }

            //Update the velocity and position vectors
            for (i=0; i<numParticles;i++) {
                PSO.updateFlippingProbability(particles.get(i), best);
                PSO.updatePosition(particles.get(i));
            }
            numIter++;
        }
        //Print the best solution
        print((best));
        System.out.println(Criterios.evaluateFitness(best));*/
    }


    /**
     * Helped method to print an array as a vector
     * @param a The given 1-D array
     */
    public void print ( List<Boolean> a) {
        System.out.print("< ");
        for (int i=0; i<a.size(); i++) {
            System.out.print(a.get(i)  + " ");
        }
        System.out.println(" > ");

    }

    /*public static void main(String[] args) {
        //PSOImplementation p = new PSOImplementation();
        Criterios.initCriterios();
    }*/
}
