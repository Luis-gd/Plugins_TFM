package earlywarn.mh;

/**
 * Class representing a particle.
 */
public class Particle {

    boolean[] position; //The position vector of this particle
    double fitness; //The fitness of this particle
    double[] stickiness; //The velocity vector of this particle
    boolean[] personalBest; //Personal best of the particle
    double[] flippingProbability; //Probability of the particle changing position

    public Particle(boolean[] position, double[] stickiness) {
        this.position = position;
        this.stickiness = stickiness;
    }

}
