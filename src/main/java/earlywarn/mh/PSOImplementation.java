package earlywarn.mh;

/**
 * Class implementing the PSO algorithm.
 */
public class PSOImplementation {

    //TODO:Arreglar como se carga el valor
    int numCompanyias = 0;
    final int numDimensions = 30; //Number of dimensions for problem
    final int numParticles = 30; //Number of particles in swarm
    final int maxIterations = 10000; //Max number of iterations
    final double Is = 4/numDimensions; //Stickiness factor
    final double Ig = (1-Is)/3;//Social factor
    final double Ip = 2*Ig;//Cognitive factor
    public double linearDecayStickiness = 1/(8*maxIterations/100); //Decay of the stickiness in every iteration
    boolean[] best;
    Particle[] particles; //Array to hold all particles

    public PSOImplementation() {
        //PSO algorithm

        particles = new Particle[numParticles];
        PSOEngine PSO = new PSOEngine(numDimensions, numParticles, maxIterations, Is, Ig, Ip, linearDecayStickiness);
        Criterios.initCriterios(numDimensions,numCompanyias);

        //Initialize particles
        PSO.initParticles(particles);

        //PSO loop
        int numIter = 0;
        while (numIter<maxIterations) {
            // Evaluate fitness of each particle
            for (int i=0; i<numParticles; i++) {
                particles[i].fitness = Criterios.evaluateFitness(particles[i].position);

                //update personal best position
                if (particles[i].fitness <= Criterios.evaluateFitness(particles[i].personalBest)) {
                    particles[i].personalBest = particles[i].position.clone();
                }
            }
            //Find best particle in set
            best = PSO.findBest(particles);

            //Update the velocity and position vectors
            for (int i=0; i<numParticles;i++) {
                PSO.updateFlippingProbability(particles[i], best);
                PSO.updatePosition(particles[i]);
            }
            numIter++;
        }
        //Print the best solution
        print((best));
        System.out.println(Criterios.evaluateFitness(best));
    }


    /**
     * Helped method to print an array as a vector
     * @param a The given 1-D array
     */
    public void print (boolean[] a) {
        System.out.print("< ");
        for (int i=0; i<a.length; i++) {
            System.out.print(a[i]  + " ");
        }
        System.out.println(" > ");

    }

    /*
    public static void main(String[] args) {
        PSOImplementation p = new PSOImplementation();
    }
    */
}
