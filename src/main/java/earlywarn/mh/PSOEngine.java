package earlywarn.mh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Class representing the PSO Engine. This class implements all the necessary methods for initializing the swarm,
 * updating the velocity and position vectors, determining the fitness of particles and finding the best particle.
 */
public class PSOEngine {

    int numDimensions = 30; //Number of dimensions for problem
    int numParticles = 30; //Number of particles in swarm
    int maxIterations = 10000; //Max number of iterations
    //TODO: Modificar para convertir el algoritmo en dinámico
    double Is = (double)4/numDimensions; //Stickiness factor
    double Ig = (1-Is)/3;//Social factor
    double Ip = 2*Ig;//Cognitive factor
    double linearDecayStickiness = (double)1/((double)8*maxIterations/100); //Decay of the stickiness in every iteration
    Random generadorAleatorio = new Random();

    public PSOEngine (int numDimensions, int numParticles, int maxIterations, double Is, double Ig, double Ip,
                      double linearDecayStickiness) {
        this.numDimensions = numDimensions;
        this.numParticles = numParticles;
        this.maxIterations = maxIterations;
        this.Is = Is;
        this.Ig = Ig;
        this.Ip = Ip;
        this.linearDecayStickiness = linearDecayStickiness;
    }


    /**
     * Método que inicializa las partículas, la posición y la pegasojidad son aleatorias,
     * con valores [0,1] y 1 respectivamente
     * @param particles The set of particles to initialize
     */
    public void initParticles(List<Particle> particles) {
        //For each particle
        for (int i=0; i<particles.size();i++) {
            List<Boolean> positions = new ArrayList<>();
            List<Double> stickiness = new ArrayList<>();
            for (int j=0; j<numDimensions; j++) {
                positions.add(generadorAleatorio.nextBoolean());
                stickiness.add(1.0);
            }
            //Create the particle
            particles.set(i,new Particle(positions, stickiness));
            //Set particles personal best to initialized values
            for(int j=0;j<particles.get(i).position.size();j++){
                particles.get(i).personalBest.set(j,particles.get(i).position.get(j));
            }
        }
    }

    /**
     * Method to update the velocities vector of a particle
     * @param particle The particle to update the velocity for
     */
    public void updateFlippingProbability(Particle particle, List<Boolean> best) {

        double[] StickinessProbability = new double[numDimensions];
        double[] differenceCognitiveTerm = new double[numDimensions];
        double[] differenceSocialTerm = new double[numDimensions];

        //Calculate stickiness probability and updates stickiness
        for (int i=0; i<numDimensions; i++) {
            StickinessProbability[i]=Is*(1-particle.stickiness.get(i));
            particle.stickiness.set(i,particle.stickiness.get(i) - linearDecayStickiness);
            if(particle.stickiness.get(i)<0){
                particle.stickiness.set(i,0.0);
            }
        }

        //Calculate the cognitive component
        //Calculate personal best - current position using hamming distance
        for (int i=0; i<numDimensions; i++) {
            if(particle.personalBest.set(i,particle.position.get(i))){
                differenceCognitiveTerm[i] = 1;
            }else{
                differenceCognitiveTerm[i] = 0;
            }
        }

        //Calculate the social term
        //Calculate neighbourhood best - current position
        for (int i=0; i<numDimensions; i++) {
            if(best.get(i)!=particle.position.get(i)){
                differenceSocialTerm[i] = 1;
            }else{
                differenceSocialTerm[i] = 0;
            }
        }

        //Update particles flipping probability at all dimensions
        for (int i=0; i<numDimensions; i++) {
            particle.flippingProbability.set(i,StickinessProbability[i]+Ip*differenceCognitiveTerm[i]+Ig*
                    differenceSocialTerm[i]);
        }
    }

    /**
     * Method to update the positions vector of a particle
     * @param particle The particle to update the position for
     */
    public void updatePosition(Particle particle) {
        for (int i=0; i<numDimensions; i++) {
            if(generadorAleatorio.nextDouble()<particle.flippingProbability.get(i)){
                particle.position.set(i,!particle.position.get(i));
                particle.stickiness.set(i,1.0);
            }
        }
    }

    /**
     * Method to find the best (fittest) particle from a given set of particles
     * @param particles The collection of particles to determine the best from
     * @return The best (fittest) particle from the collection of particles
     */
    public List<Boolean> findBest(List<Particle> particles) {
        List<Boolean> best = new ArrayList<>();
        double bestFitness = Double.MAX_VALUE;
        for(int i=0; i<numParticles; i++) {
            Particle actual = particles.get(i);
            if (Criterios.evaluateFitness(actual.personalBest)<= bestFitness) {
                bestFitness = Criterios.evaluateFitness(actual.personalBest);
                for(int j=0;j<actual.personalBest.size();j++){
                    best.set(j,actual.personalBest.get(j));
                }
            }
        }
        return best;
    }
}
