package earlywarn.mh.halassi2021;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Class representing a particle.
 */
public class Particle {
    //TODO: De momento vamos a inicializar en un rango de 0 y 1 debido a que eso entiendo en el artículo, esperar a
    // respuesta del autor o probar distintos valores
    double mutationStrategyParameter; // Valor utilizado para realizar la mutación, en la primera iteración tiene valor 0
    double crowdingDistance = 0.0; //Valor que habla de lo cercano que está la solución a otras, cuanto menor sea su valor
    // más cercano es, solo se utilizará si la variable está dentro del archive set
    List<Boolean> position; //The position vector of this particle
    List<Double> fitness = new ArrayList<>(); //The fitness of this particle
    List<Double> velocity; //The velocity vector of this particle
    List<Boolean> personalBest = new ArrayList<>(); //Personal best of the particle

    /**
     * Contructor de la clase partícula
     * @param position Es la solución, el valor a true representa una conexión abierta y a false una conexión cerrada
     * @param velocity La velocidad que lleva la partícula en cada una de sus dimensiones
     */
    public Particle(List<Boolean> position, List<Double> velocity) {
        List<Boolean> positionNuevo = new ArrayList<>(position);
        List<Double> velocityNuevo = new ArrayList<>(velocity);
        Random rnd = new Random();
        this.position = positionNuevo;
        this.velocity = velocityNuevo;
        mutationStrategyParameter = rnd.nextDouble();
    }

    public Particle(double crowdingDistance, List<Boolean> position, List<Double> fitness, List<Double> velocity,
                    List<Boolean> personalBest){
        List<Boolean> positionNuevo = new ArrayList<>(position);
        List<Double> fitnessNuevo = new ArrayList<>(fitness);
        List<Double> velocityNuevo = new ArrayList<>(velocity);
        List<Boolean> personalBestNuevo = new ArrayList<>(personalBest);
        Random rnd = new Random();
        this.crowdingDistance = crowdingDistance;
        this.position = positionNuevo;
        this.fitness = fitnessNuevo;
        this.velocity = velocityNuevo;
        this.personalBest = personalBestNuevo;
        mutationStrategyParameter = rnd.nextDouble();
    }

    public List<Boolean> getPosition(){
        return position;
    }

    public void setFitness(List<Double> newFitness){
        for(int i=0;i<newFitness.size();i++){
            fitness.set(i,newFitness.get(i));
        }
    }

    public boolean equals(Particle aComparar){
        boolean resultado = true;
        for(int i = 0; i<fitness.size(); i++){
            if (!Objects.equals(fitness.get(i), aComparar.fitness.get(i))) {
                resultado = false;
                break;
            }
        }
        System.out.println(resultado);
        return resultado;
    }
}
