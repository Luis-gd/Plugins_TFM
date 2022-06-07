package earlywarn.mh;

/**
 * Class representing a particle.
 */
public class Particle {

    public boolean[] position; //The position vector of this particle
    public double fitness; //The fitness of this particle
    public double[] stickiness; //The velocity vector of this particle
    public boolean[] personalBest; //Personal best of the particle
    public double[] flippingProbability; //Probability of the particle changing position

    public Particle(boolean[] position, double[] stickiness) {
        this.position = position;
        this.stickiness = stickiness;
    }

}
