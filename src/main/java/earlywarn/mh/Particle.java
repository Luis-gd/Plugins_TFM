package earlywarn.mh;

/**
 * Class representing a particle.
 */
public class Particle {

    boolean[] position; //The position vector of this particle
    double fitness; //The fitness of this particle
    double[] velocity; //The velocity vector of this particle
    boolean[] personalBest; //Personal best of the particle

    public Particle(boolean[] position, double[] velocity) {
        this.position = position;
        this.velocity = velocity;
    }

}
