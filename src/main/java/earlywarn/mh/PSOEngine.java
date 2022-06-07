package earlywarn.mh;

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
    double Is = 4/numDimensions; //Stickiness factor
    double Ig = (1-Is)/3;//Social factor
    double Ip = 2*Ig;//Cognitive factor
    double linearDecayStickiness = 1/(8*maxIterations/100); //Decay of the stickiness in every iteration
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
    public void initParticles(Particle[] particles) {
        //For each particle
        for (int i=0; i<particles.length;i++) {
            boolean[] positions = new boolean[numDimensions];
            double[] stickiness = new double[numDimensions];
            for (int j=0; j<numDimensions; j++) {
                positions[j] = generadorAleatorio.nextBoolean();
                stickiness[j] = 1;
            }
            //Create the particle
            particles[i] = new Particle(positions, stickiness);
            //Set particles personal best to initialized values
            particles[i].personalBest = particles[i].position.clone();
        }
    }

    /**
     * Method to update the velocities vector of a particle
     * @param particle The particle to update the velocity for
     */
    public void updateFlippingProbability(Particle particle, boolean[] best) {

        double[] StickinessProbability = new double[numDimensions];
        double[] differenceCognitiveTerm = new double[numDimensions];
        double[] differenceSocialTerm = new double[numDimensions];

        //Calculate stickiness probability and updates stickiness
        for (int i=0; i<numDimensions; i++) {
            StickinessProbability[i]=Is*(1-particle.stickiness[i]);
            particle.stickiness[i] = particle.stickiness[i] - linearDecayStickiness;
            if(particle.stickiness[i]<0){
                particle.stickiness[i] = 0;
            }
        }

        //Calculate the cognitive component
        //Calculate personal best - current position using hamming distance
        for (int i=0; i<numDimensions; i++) {
            if(particle.personalBest[i]!=particle.position[i]){
                differenceCognitiveTerm[i] = 1;
            }else{
                differenceCognitiveTerm[i] = 0;
            }
        }

        //Calculate the social term
        //Calculate neighbourhood best - current position
        for (int i=0; i<numDimensions; i++) {
            if(best[i]!=particle.position[i]){
                differenceSocialTerm[i] = 1;
            }else{
                differenceSocialTerm[i] = 0;
            }
        }

        //Update particles flipping probability at all dimensions
        for (int i=0; i<numDimensions; i++) {
            particle.flippingProbability[i] = StickinessProbability[i]+Ip*differenceCognitiveTerm[i]+Ig*differenceSocialTerm[i];
        }
    }

    /**
     * Method to update the positions vector of a particle
     * @param particle The particle to update the position for
     */
    public void updatePosition(Particle particle) {
        for (int i=0; i<numDimensions; i++) {
            if(generadorAleatorio.nextDouble()<particle.flippingProbability[i]){
                if(particle.position[i]){
                    particle.position[i]=false;
                }else{
                    particle.position[i]=true;
                }
                particle.stickiness[i]=1;
            }
        }
    }

    /**
     * Method to find the best (fittest) particle from a given set of particles
     * @param particles The collection of particles to determine the best from
     * @return The best (fittest) particle from the collection of particles
     */
    public boolean[] findBest(Particle[] particles) {
        boolean[] best = null;
        double bestFitness = Double.MAX_VALUE;
        for(int i=0; i<numParticles; i++) {
            //CAMBIAR COMO SE CALCULA EL FITNESS
            /*if (evaluateFitness(particles[i].personalBest)<= bestFitness) {
                bestFitness = evaluateFitness(particles[i].personalBest);
                best = particles[i].personalBest;
            }*/
        }
        return best;
    }

    /**
     * Method to calculate the fitness of a particle using the Rastrigin function
     * @param positions The position vector to evaluate the fitness for
     * @return The fitness of the particle
     */
    /*
    public double evaluateFitness(double[] positions) {
        double fitness = 0;
        for (int i=0; i<numDimensions; i++) {
            fitness = fitness + (Math.pow(positions[i],2)-(10*Math.cos(2*Math.PI*positions[i])));
        }

        fitness = fitness + (10*numDimensions);
        return fitness;
    }
    */
}
