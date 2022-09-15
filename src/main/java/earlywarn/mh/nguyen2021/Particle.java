package earlywarn.mh.nguyen2021;

import java.util.List;

/**
 * Class representing a particle.
 */
public class Particle {

    public List<Boolean> position; //The position vector of this particle
    public double fitness; //The fitness of this particle
    public List<Double> stickiness; //The velocity vector of this particle
    public List<Boolean> personalBest; //Personal best of the particle
    public List<Double> flippingProbability; //Probability of the particle changing position

    public Particle(List<Boolean> position, List<Double> stickiness) {
        this.position = position;
        this.stickiness = stickiness;
    }

}
