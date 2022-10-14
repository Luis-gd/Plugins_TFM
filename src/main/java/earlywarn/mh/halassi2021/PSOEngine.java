package earlywarn.mh.halassi2021;

import earlywarn.mh.Criterios;

import java.util.*;
import org.apache.commons.math3.distribution.BetaDistribution;

/**
 * Class representing the PSO Engine. This class implements all the necessary methods for initializing the swarm,
 * updating the velocity and position vectors, determining the fitness of particles and finding the best particle.
 */
public class PSOEngine {

    int numParticles; //Number of particles in swarm
    int numConexiones; //Numero de conexiones entre aeropuertos, equivale al tamaño de las soluciones
    int numCriterios; //Numero de criterios (multiobjetivo)
    public final int tamanoArchive; //Número de soluciones dentro del archive
    double c1; //Coeficiente
    double c2; //Coeficiente
    double w; //Coeficiente
    //Al introducir estos valores en la función sigmoide el resultado redondeado es -1 y 1
    double maxVelocity = 6.0;
    double minVelocity = -6.0;
    double refPointHypervolume = 1; //Número que utilizamos como referencia para calcular el hipervolumen que se
    //corresponde con el peor caso posible, al estar minimizando porcentajes le damos de valor 1
    Criterios evaluador;

    /**
     * Constructor del motor Binary particle swarm optimization
     * @param numParticles Número de partículas que se utilizaran
     * @param numConexiones Número de conexiones entre aeropuertos
     * @param c1 Coeficiente
     * @param c2 Coeficiente
     * @param w Coeficiente
     */
    public PSOEngine (int numParticles, int numConexiones, double c1, double c2, double w,
                      int numCriterios, int tamanoArchive, Criterios evaluador) {
        this.numParticles = numParticles;
        this.numConexiones = numConexiones;
        this.c1 = c1;
        this.c2 = c2;
        this.w = w;
        this.numCriterios = numCriterios;
        this.tamanoArchive = tamanoArchive;
        this.evaluador = evaluador;
    }

    /**
     * Método que inicializa las partículas
     * @param particles El array de partículas a inicializar
     * @param rnd El generador aleatorio
     */
    public void initParticles(List<Particle> particles, Random rnd) {
        double probAbierto = 0.0;
        double incrementoProb = (double) 1 / (numParticles-1);
        //For each particle
        for (int i=0; i<numParticles; i++) {
            List<Boolean> positions = new ArrayList<>();
            List<Double> velocities = new ArrayList<>();
            for (int j=0; j<numConexiones; j++) {
                if(rnd.nextDouble()<probAbierto){
                    positions.add(true);
                }else{
                    positions.add(false);
                }
                //Inicializamos la velocidad aleatoriamente en un rango
                velocities.add(rnd.nextDouble() * (maxVelocity - minVelocity) + minVelocity);
            }
            probAbierto += incrementoProb;
            //Create the particle
            particles.add(new Particle(positions, velocities));
            //Set particles personal best to initialized values
            for(int j=0;j<numConexiones;j++){
                particles.get(i).personalBest.add(particles.get(i).position.get(j));
            }
            for(int j=0;j<numCriterios;j++){
                particles.get(i).fitness.add(Double.MAX_VALUE);
            }
        }
    }

    /**
     * Método que actualiza la velocidad de una partícula
     * @param particle La partícula a la que se va a actualizar su velocidad
     * @param best La mejor partícula hasta ahora
     * @param r1 Conjunto de números aleatorios
     * @param r2 Conjunto de números aleatorios
     */
    public void updateVelocity(Particle particle, List<Boolean> best,List<Double> r1,List<Double> r2) {

        double difference1 = 0.0;
        double difference2 = 0.0;
        double valor;

        //Update particles velocity at all dimensions
        for (int i=0; i<numConexiones; i++) {

            //Calculate personal best - current position
            if(particle.personalBest.get(i).equals(particle.position.get(i))){
                difference1 = 0;
            }else if(particle.personalBest.get(i).equals(true)){
                difference1 = 1;
            }else if(particle.personalBest.get(i).equals(false)){
                difference1 = -1;
            }else{
                System.out.println("Error al calcular la diferencia de posición actual y la mejor posición anterior");
            }

            //Calculate neighbourhood best - current position
            if(best.get(i).equals(particle.position.get(i))){
                difference2 = 0;
            }else if(best.get(i).equals(true)){
                difference2 = 1;
            }else if(best.get(i).equals(false)){
                difference2 = -1;
            }else{
                System.out.println("Error al calcular la diferencia de posición actual y la mejor posición anterior");
            }

            valor = w*particle.velocity.get(i) + c1*r1.get(i)*difference1 + c2*r2.get(i)*difference2;

            if(valor < minVelocity){
                valor = minVelocity;
            }else if(valor > maxVelocity){
                valor = maxVelocity;
            }

            particle.velocity.set(i,valor);
        }
    }

    public double simgoid(double x){
        return 1 / (1 + Math.exp(-x));
    }

    /**
     * Method to update the positions vector of a particle
     * @param particle The particle to update the position for
     */
    public void updatePosition(Particle particle, Random rnd) {
        //TODO: Mejorar eficiencia del cálculo de la función sigmoide
        double numAleatorio = rnd.nextDouble();
        for (int i=0; i<numConexiones; i++) {
            if(particle.velocity.get(i) == minVelocity){
                particle.position.set(i,false);
            }else if(particle.velocity.get(i) == maxVelocity){
                particle.position.set(i,true);
            }else if(numAleatorio <= simgoid(particle.velocity.get(i))){
                particle.position.set(i,true);
            }else{
                particle.position.set(i,false);
            }
        }
    }

    public double calculateHypervolumeWFG(List<Particle> archiveSet){
        double sum=0.0;
        for(int i=0;i<archiveSet.size();i++){
            sum=sum+exclusiveHypervolume(archiveSet,i);
        }
        return sum;
    }

    public double exclusiveHypervolume(List<Particle> archiveSet, int indexParticula){
        return inclusiveHypervolume(archiveSet.get(indexParticula)) - calculateHypervolumeWFG(
                limitSet(archiveSet,indexParticula));
    }

    public double inclusiveHypervolume(Particle particula){
        double resultado = 1;
        for(double coordenada:particula.fitness){
            resultado = resultado * (coordenada - refPointHypervolume) * -1;
        }
        return resultado;
    }

    public List<Particle> limitSet(List<Particle> archiveSet, int indexParticula){
        List<Particle> archiveAux = new ArrayList<>();
        for(int i=1;i<archiveSet.size()-indexParticula;i++){
            Particle auxParticula = new Particle();
            for(int j=0;j<archiveSet.get(0).fitness.size();j++){
                if(archiveSet.get(indexParticula).fitness.get(j)>archiveSet.get(indexParticula+i).fitness.get(j)){
                    auxParticula.fitness.add(archiveSet.get(indexParticula).fitness.get(j));
                }else{
                    auxParticula.fitness.add(archiveSet.get(indexParticula+i).fitness.get(j));
                }
            }
            archiveAux.add(auxParticula);
        }
        Set<Integer> indexAQuitar = new HashSet<>();
        for(int i=0;i<archiveAux.size();i++){
            for(int j=0;j<archiveAux.size();j++){
                if(evaluador.isDominated(archiveAux.get(i).fitness,archiveAux.get(j).fitness)){
                    indexAQuitar.add(i);
                }
            }
        }
        Iterator<Integer> bucle = indexAQuitar.iterator();
        while(bucle.hasNext()){
            archiveAux.remove(bucle.next());
        }
        return archiveAux;
    }

    public void crossover(List<Particle> particles, Random rnd){
        int i, indexParticula, indexAux, tamano = particles.size(), tamano2 = particles.size()-1, contador;
        double porcentajeAMezclar = 0.05, valor;
        List<Integer> listIndex = new ArrayList<>(), indexMezcla = new ArrayList<>();
        List<Boolean> posibleParticle = new ArrayList<>();

        for(i = 0;i<particles.size()*porcentajeAMezclar;i++) {
            //Elegimos la partícula a modificar
            valor = rnd.nextDouble();
            indexParticula = (int) (valor * (tamano-0.00001));
            tamano--;
            contador=0;
            for(int indiceGuardado:listIndex){
                if(indiceGuardado<=indexParticula){
                    contador++;
                }
            }
            indexParticula = indexParticula + contador;
            listIndex.add(indexParticula);

            //Elegimos las partículas a mezclar
            indexMezcla.add(indexParticula);
            //El máximo es 3 porque necesitamos 3 partículas para hacer el crossover + la elegida como cambio
            for(int j=0;j<3;j++){
                valor = rnd.nextDouble();
                indexAux = (int) (valor * (tamano2-0.00001));
                tamano2--;
                contador=0;
                for(int indiceGuardado:indexMezcla){
                    if(indiceGuardado<=indexAux){
                        contador++;
                    }
                }
                indexAux = indexAux + contador;
                indexMezcla.add(indexAux);
            }
            tamano2 = particles.size()-1;

            for(int j=0;j<particles.get(indexMezcla.get(0)).position.size();j++){

                if(!particles.get(indexMezcla.get(2)).position.get(j).equals(particles.get(indexMezcla.get(3)).
                        position.get(j))){
                    valor = rnd.nextDouble();
                }else{
                    valor = 0.0;
                }

                if(rnd.nextDouble()<=simgoid(valor)){
                    posibleParticle.add(particles.get(indexMezcla.get(1)).position.get(j));
                }else{
                    posibleParticle.add(!particles.get(indexMezcla.get(1)).position.get(j));
                }
            }

            if (evaluador.isDominated(evaluador.evaluateFitness(particles.get(indexMezcla.get(0)).position),
                    evaluador.evaluateFitness(posibleParticle))) {
                System.out.println("Crossover mejor");
                System.out.println("Fitness anterior:");
                for (double fitness : evaluador.evaluateFitness(particles.get(indexMezcla.get(0)).position)) {
                    System.out.println(fitness);
                }
                System.out.println("Fitness nuevo:");
                for (double fitness : evaluador.evaluateFitness(posibleParticle)) {
                    System.out.println(fitness);
                }
                Collections.copy(particles.get(indexMezcla.get(0)).position, posibleParticle);
            }

            indexMezcla.clear();
            posibleParticle.clear();
        }
    }

    /**
     * Función que aplica mutación en la solución mejor encontrada hasta ahora, si esta nueva solución es mejor que la
     * anterior la sustituimos
     */
    public void mutation(List<Particle> particles, Random rnd){
        int indexParticula, contador, i, tamano = particles.size();
        double valor, rand, parte1, parte2, strategyParameter, valorObtenido, porcentajeAMezclar = 0.05;
        Random rnd2 = new Random();
        List<Boolean> posiblePersonalBest = new ArrayList<>();
        List<Integer> listIndex = new ArrayList<>();
        //TODO: Hay que probar distinto valores de alpha y beta
        BetaDistribution beta = new BetaDistribution(0.8, 0.8);

        for(i = 0;i<particles.size()*porcentajeAMezclar;i++) {

            //Elegimos la partícula a modificar
            valor = rnd.nextDouble();
            indexParticula = (int) (valor * (tamano-0.00001));
            tamano--;
            contador=0;
            for(int indiceGuardado:listIndex){
                if(indiceGuardado<=indexParticula){
                    contador++;
                }
            }
            indexParticula = indexParticula + contador;
            listIndex.add(indexParticula);

            parte1 = particles.get(indexParticula).mutationStrategyParameter*Math.exp(1/Math.sqrt(2*particles.size())*
                    rnd2.nextGaussian());

            for (boolean dimension : particles.get(indexParticula).personalBest) {

                parte2 = 1 / Math.sqrt(2 * Math.sqrt(particles.size())) * rnd2.nextGaussian();
                strategyParameter = parte1 + parte2;

                //calculo del Rand
                rand = beta.sample();

                valorObtenido = simgoid(strategyParameter * rand);

                if (rnd.nextDouble() <= valorObtenido) {
                    if (dimension) {
                        posiblePersonalBest.add(true);
                    } else {
                        posiblePersonalBest.add(false);
                    }
                } else {
                    if (dimension) {
                        posiblePersonalBest.add(false);
                    } else {
                        posiblePersonalBest.add(true);
                    }
                }
            }

            if (evaluador.isDominated(evaluador.evaluateFitness(particles.get(indexParticula).personalBest),
                    evaluador.evaluateFitness(posiblePersonalBest))) {
                System.out.println("Mutacion mejor");
                System.out.println("Fitness anterior:");
                for (double fitness : evaluador.evaluateFitness(particles.get(indexParticula).personalBest)) {
                    System.out.println(fitness);
                }
                System.out.println("Fitness nuevo:");
                for (double fitness : evaluador.evaluateFitness(posiblePersonalBest)) {
                    System.out.println(fitness);
                }
                Collections.copy(particles.get(indexParticula).personalBest, posiblePersonalBest);
            }
            posiblePersonalBest.clear();
        }
    }

    /**
     * Función que añade una solución al archive, si el archive ya estuviese lleno compara el crowding distance de la
     * peor partícula en el archive set y la nueva partícula a introducir, se introduce la nueva si el CD de la nueva
     * particula es mayor
     * @param particles El listado de particulas
     * @param archive El archive set
     */
    public void addSolutionsToArchive(List<Particle> particles, List<Particle> archive){
        int j;
        boolean anyadir = true;
        boolean domina = false;
        boolean dentro = true;
        boolean parada = false;
        List<Particle> auxAnayadirNuevaParticula = new ArrayList<>();
        for(Particle particula:particles){
            //Comprobamos si la partícula ya está dentro del archive
            for(int i=0;i<archive.size()&&!parada;i++){
                for(j=0;j<archive.get(i).fitness.size();j++){
                    if(!Objects.equals(archive.get(i).fitness.get(j), particula.fitness.get(j))){
                        dentro = false;
                    }
                }
                if(!dentro){
                    dentro = true;
                }else{
                    parada = true;
                }
            }
            if(!parada){
                j=0;
                while(j<archive.size()&&anyadir){
                    //Comprobamos que la partícula no sea dominada por ninguna de las partículas en el archive
                    if(!domina){
                        if(evaluador.isDominated(particula.fitness,archive.get(j).fitness)){
                            anyadir = false;
                        }
                    }
                    //Comprobamos que la partícula no domine a ninguna de las partículas en el archive
                    if(evaluador.isDominated(archive.get(j).fitness,particula.fitness)){
                        archive.remove(j);
                        domina = true;
                    }else{
                        j++;
                    }
                }
                if(anyadir){
                    if(archive.size()<tamanoArchive){
                        archive.add(new Particle(particula.crowdingDistance,particula.position,particula.fitness,
                                particula.velocity,particula.personalBest));
                    }else{
                        AuxCD peorParticula = calculateCD(archive);
                        for (j=0;j<archive.size();j++){
                            if(j==peorParticula.position){
                                auxAnayadirNuevaParticula.add(new Particle(particula.crowdingDistance,
                                        particula.position,particula.fitness,particula.velocity,
                                        particula.personalBest));
                            }else{
                                auxAnayadirNuevaParticula.add(archive.get(j));
                            }
                        }
                        if(peorParticula.functionValue<calculateCD(auxAnayadirNuevaParticula).functionValue){
                            Collections.copy(archive,auxAnayadirNuevaParticula);
                        }
                        auxAnayadirNuevaParticula.clear();
                    }
                }
                anyadir = true;
                domina = false;
            }else{
                parada = false;
            }
        }
    }

    /**
     * Función que reordena de mayor a menor parte de una estructura
     * @param array Los datos a ordenar
     * @param low La posición menor
     * @param high La posición mayor
     * @return Devuelve la posición donde se va a quedar el pivote utilizado
     */
    static int partition(List<AuxCD> array, int low, int high) {

        // choose the rightmost element as pivot
        double pivot = array.get(high).functionValue;

        // pointer for greater element
        int i = (low - 1);

        // traverse through all elements
        // compare each element with pivot
        for (int j = low; j < high; j++) {
            if (array.get(j).functionValue <= pivot) {

                // if element smaller than pivot is found
                // swap it with the greater element pointed by i
                i++;

                // swapping element at i with element at j
                int auxPosition = array.get(i).position;
                double auxFunctionValue = array.get(i).functionValue;
                array.get(i).position = array.get(j).position;
                array.get(i).functionValue = array.get(j).functionValue;
                array.get(j).position = auxPosition;
                array.get(j).functionValue = auxFunctionValue;
            }
        }

        // swap the pivot element with the greater element specified by i
        int auxPosition = array.get(i+1).position;
        double auxFunctionValue = array.get(i+1).functionValue;
        array.get(i+1).position = array.get(high).position;
        array.get(i+1).functionValue = array.get(high).functionValue;
        array.get(high).position = auxPosition;
        array.get(high).functionValue = auxFunctionValue;

        // return the position from where partition is done
        return (i + 1);
    }

    /**
     * Función recursiva que se encarga de ordenar de menor a mayor
     * @param array Los datos a ordenar
     * @param low La posición menor
     * @param high La posición mayor
     */
    static void quickSort(List<AuxCD> array, int low, int high) {
        if (low < high) {
            int pi = partition(array, low, high);
            quickSort(array, low, pi - 1);
            quickSort(array, pi + 1, high);
        }
    }

    /**
     * Calcula el crowding distance del listado de particulas
     * @param archive El listado de particulas, puede ser el archive actual o el potencial nuevo archive
     * @return Devuelve una clase con la menor CD y su posición en el archive
     */
    public AuxCD calculateCD(List<Particle> archive){
        List<AuxCD> valoresFuncionObjetivo1 = new ArrayList<>(), valoresFuncionObjetivo2 = new ArrayList<>(),
                valoresFuncionObjetivo3 = new ArrayList<>();
        double minCD = Double.MAX_VALUE;
        int positionMinCD = 0;

        for(int i=0;i<archive.size();i++){
            valoresFuncionObjetivo1.add(new AuxCD(i,archive.get(i).fitness.get(0)));
            valoresFuncionObjetivo2.add(new AuxCD(i,archive.get(i).fitness.get(1)));
            valoresFuncionObjetivo3.add(new AuxCD(i,archive.get(i).fitness.get(2)));
            archive.get(i).crowdingDistance = 0.0;
        }

        quickSort(valoresFuncionObjetivo1,0,valoresFuncionObjetivo1.size()-1);
        quickSort(valoresFuncionObjetivo2,0,valoresFuncionObjetivo2.size()-1);
        quickSort(valoresFuncionObjetivo3,0,valoresFuncionObjetivo3.size()-1);

        for(int i=1;i<valoresFuncionObjetivo1.size()-1;i++){
            archive.get(valoresFuncionObjetivo1.get(i).position).crowdingDistance =
                    archive.get(valoresFuncionObjetivo1.get(i).position).crowdingDistance +
                            valoresFuncionObjetivo1.get(i+1).functionValue - valoresFuncionObjetivo1.get(i-1).functionValue;
            archive.get(valoresFuncionObjetivo2.get(i).position).crowdingDistance =
                    archive.get(valoresFuncionObjetivo2.get(i).position).crowdingDistance +
                            valoresFuncionObjetivo2.get(i+1).functionValue - valoresFuncionObjetivo2.get(i-1).functionValue;
            archive.get(valoresFuncionObjetivo3.get(i).position).crowdingDistance =
                    archive.get(valoresFuncionObjetivo3.get(i).position).crowdingDistance +
                            valoresFuncionObjetivo3.get(i+1).functionValue - valoresFuncionObjetivo3.get(i-1).functionValue;
        }
        archive.get(valoresFuncionObjetivo1.get(0).position).crowdingDistance = Double.MAX_VALUE;
        archive.get(valoresFuncionObjetivo1.get(valoresFuncionObjetivo1.size()-1).position).crowdingDistance =
                Double.MAX_VALUE;
        archive.get(valoresFuncionObjetivo2.get(0).position).crowdingDistance = Double.MAX_VALUE;
        archive.get(valoresFuncionObjetivo2.get(valoresFuncionObjetivo2.size()-1).position).crowdingDistance =
                Double.MAX_VALUE;
        archive.get(valoresFuncionObjetivo3.get(0).position).crowdingDistance = Double.MAX_VALUE;
        archive.get(valoresFuncionObjetivo3.get(valoresFuncionObjetivo3.size()-1).position).crowdingDistance =
                Double.MAX_VALUE;

        for (int i=0;i<archive.size();i++){
            if(archive.get(i).crowdingDistance<minCD){
                minCD = archive.get(i).crowdingDistance;
                positionMinCD = i;
            }
        }
        
        return new AuxCD(positionMinCD,minCD);
    }
}