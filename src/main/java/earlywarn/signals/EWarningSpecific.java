package earlywarn.signals;

import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.neo4j.cypher.internal.expressions.In;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.*;
import java.lang.Math;
import java.util.stream.Collectors;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.nio.csv.*;
import org.jgrapht.alg.scoring.ClusteringCoefficient;

/**
 * Specialization fo the EWarningGeneral general class for the generation of early warning signals and markers
 * to early detect outbreaks.
 * Notes: It assumes that every country has the same number of reports and that there is no gap between the first date
 * with covid reports and the last one. Also, it assumes tha all countries have the same date for the first report,
 * and hence all countries have the same date for its last report. (All things has been proved)
 */
public class EWarningSpecific extends EWarningGeneral{
    /* Default values */
    protected final static boolean cumulativeDataDefault = false;
    protected final static boolean squareRootDataDefault = true;
    protected final static double thresholdDefault = 0.5;

    /* Class properties */
    private boolean cumulativeData;
    private boolean squareRootData;
    private double threshold;
    private int[][][] unweighted;

    /**
     * Secondary constructor for the Class that receive fewer parameters than the main one.
     * @param db Neo4j database instance annotated with the @Context from the main procedure function.
     * @throws Exception If startDate is greater than endDate or the database doesn't contain it.
     * If there are less than two selected countries. If any country inside the countries list isn't contain
     * in the database.
     * @author Angel Fragua
     */
    public EWarningSpecific(GraphDatabaseService db) throws RuntimeException {
        this(db, EWarningGeneral.startDateDefault, EWarningGeneral.endDateDefault, EWarningGeneral.countriesDefault,
             EWarningGeneral.windowSizeDefault, EWarningGeneral.correlationDefault, cumulativeDataDefault,
             squareRootDataDefault, thresholdDefault);
    }

    /**
     * Secondary constructor for the Class that receive fewer parameters than the main one.
     * @param db Neo4j database instance annotated with the @Context from the main procedure function.
     * @param startDate First date of the range of days of interest.
     * @param endDate Last date of the range of days of interest.
     * @throws Exception If startDate is greater than endDate or the database doesn't contain it.
     * If there are less than two selected countries. If any country inside the countries list isn't contain
     * in the database.
     * @author Angel Fragua
     */
    public EWarningSpecific(GraphDatabaseService db, LocalDate startDate, LocalDate endDate) throws RuntimeException {
        this(db, startDate, endDate, EWarningGeneral.countriesDefault, EWarningGeneral.windowSizeDefault,
             EWarningGeneral.correlationDefault, cumulativeDataDefault, squareRootDataDefault, thresholdDefault);
    }

    /**
     * Secondary constructor for the Class that receive fewer parameters than the main one.
     * @param db Neo4j database instance annotated with the @Context from the main procedure function.
     * @param startDate First date of the range of days of interest.
     * @param endDate Last date of the range of days of interest.
     * @param cumulativeData Boolean that determines whether to use cumulative confirmed covid cases (True) over
     * the time or new daily cases of confirmed covid cases (True).
     * @param squareRootData Boolean that determines whether to apply the square root to each confirmed covid case value
     * to smooth the results.
     * @param threshold Value from which it is determined that the correlation between two countries is high enough
     * establishing a connection.
     * @throws Exception If startDate is greater than endDate or the database doesn't contain it.
     * If there are less than two selected countries. If any country inside the countries list isn't contain
     * in the database.
     * @author Angel Fragua
     */
    public EWarningSpecific(GraphDatabaseService db, LocalDate startDate, LocalDate endDate, boolean cumulativeData,
                            boolean squareRootData, double threshold) throws RuntimeException {
        this(db, startDate, endDate, EWarningGeneral.countriesDefault, EWarningGeneral.windowSizeDefault,
                EWarningGeneral.correlationDefault, cumulativeData, squareRootData, threshold);
    }

    /**
     * Main constructor for the Class that receive all possible parameters.
     * @param db Neo4j database instance annotated with the @Context from the main procedure function.
     * @param startDate First date of the range of days of interest.
     * @param endDate Last date of the range of days of interest.
     * @param countries List of countries to take into account in the ISO-3166-Alpha2 format (2 letters by country).
     * @param windowSize Size of the window to shift between startDate and endDate.
     * @param correlation Type of correlation to use for each window between each pair of countries.
     * List of possible correlation values:
     *      - "pearson": Pearson Correlation
     *      - "spearman": Spearman Correlation
     *      - "kendall":Kendall Correlation
     *      - any other value: Pearson Correlation
     * @param cumulativeData Boolean that determines whether to use cumulative confirmed covid cases (True) over
     * the time or new daily cases of confirmed covid cases (True).
     * @param squareRootData Boolean that determines whether to apply the square root to each confirmed covid case value
     * to smooth the results.
     * @param threshold Value from which it is determined that the correlation between two countries is high enough
     * establishing a connection.
     * @throws Exception If startDate is greater than endDate or the database doesn't contain it.
     * If there are less than two selected countries. If any country inside the countries list isn't contain
     * in the database.
     * @author Angel Fragua
     */
    public EWarningSpecific(GraphDatabaseService db, LocalDate startDate, LocalDate endDate, List<String> countries,
                            int windowSize, String correlation, boolean cumulativeData, boolean squareRootData,
                            double threshold) throws RuntimeException {
        super(db, startDate, endDate, countries, windowSize, correlation);
        this.cumulativeData = cumulativeData;
        this.squareRootData = squareRootData;
        this.threshold = threshold;
    }

    /**
     * Transform the original data of cumulative confirmed covid cases to its desired form. In this specialized case,
     * depending on the class properties cumulativeData and squareRootData, it will leave the covid confirmed cases as
     * cumulative data (False) or will make the discrete difference to contain the daily new confirmed cases of covid
     * (True); and will apply the square root to each value of covid cases to smooth it, respectively.
     * @param startDateWindow Start date corresponding to the first window's date, which will be as many days prior to
     * the real start date of study as the size of the windows minus one.
     * @return long[][] Matrix with the transformed data. Each Row represents a country, and each Column contains
     * the cases from the first date of study until the end date.
     * @author Angel Fragua
     */
    @Override
    protected double[][] transformData(LocalDate startDateWindow) {
        int numCountries = this.countries.size();
        Queries queries = new Queries(this.db);

        if (!this.cumulativeData) {
            long[] extraDate = new long[numCountries];
            if (startDateWindow.isAfter(queries.minReportDate())) {
                for (int i = 0; i < numCountries; i++) {
                    extraDate[i] = queries.getReportConfirmed(this.countries.get(i), startDateWindow.minusDays(1),
                                                              startDateWindow.minusDays(1))[0];
                }
            }

            double[][] tmpData = new double[numCountries][this.dataOriginal[0].length + 1];

            double x;
            for (int i = 0; i < numCountries; i++) {
                x =  extraDate[i];
                if (this.squareRootData) {
                    x = Math.sqrt(x);
                }
                tmpData[i][0] = x;
            }
            for (int i = 0; i < numCountries; i++) {
                for (int j = 0; j < this.dataOriginal[0].length; j++) {
                    x =  this.dataOriginal[i][j];
                    if (this.squareRootData) {
                        x = Math.sqrt(x);
                    }
                    tmpData[i][j+1] = x;
                }
            }
            return diffData(tmpData);
        }
        else if (this.squareRootData) {
            double[][] data = new double[numCountries][this.dataOriginal[0].length];
            for (int i = 0; i < numCountries; i++) {
                for (int j = 0; j < this.dataOriginal[0].length; j++) {
                    data[i][j] = Math.sqrt(this.dataOriginal[i][j]);
                }
            }
            return data;
        }
        else {
            return super.transformData(startDateWindow);
        }
    }

    /**
     * Calculates the discrete difference of the received data, which means that each column with index i will be
     * column[i] = column[i+1] - column[i].
     * @param data Data to be transformed.
     * @return double[][] The discrete difference of the data, where its first column will be lost.
     * @author Angel Fragua
     */
    private double[][] diffData(double[][] data) {
        int numCols = data[0].length;
        int numRows = data.length;
        double[][] diffData = new double[numRows][numCols - 1];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols - 1; j++) {
                diffData[i][j] = data[i][j+1] - data[i][j];
            }
        }
        return diffData;
    }

    /**
     * Specialization of the main method, where it makes sure that all the data is correctly imported and transformed
     * to subsequently generate the corresponding networks matrices with its adjacencies for each instance of time
     * between the start and end date. In case that there isn't enough reports previous to the start date to fill
     * the window size, it shifts the start date enough dates to fulfill it. Thanks to the specialization it also
     * generates a list of unweighted networks based on the original networks list and the corresponding threshold.
     * @author Angel Fragua
     */
    @Override
    public void checkWindows() {
        super.checkWindows();
        this.unweighted = generateUnweighted();
    }

    /**
     * Generates an unweighted adjacency matrix for each instant of study between the start date and the end date.
     * Each matrix is obtained by checking in the same time corresponding correlation network if the correlation
     * coefficient between each pair of nodes is greater than the threshold property of the class.
     * @return int[][][] List of the unweighted adjacency matrices for each temporal instant from the start date to
     * the end date.
     * @author Angel Fragua
     */
    private int[][][] generateUnweighted() {
        int numCountries = this.countries.size();
        int[][][] unweighted = new int[this.networks.length][numCountries][numCountries];
        for (int net = 0; net < this.networks.length; net++) {
            for (int i = 0; i < numCountries; i++) {
                for (int j = 0; j < numCountries; j++) {
                    unweighted[net][i][j] = this.networks[net][i][j] > this.threshold ? 1 : 0;
                }
            }
        }
        return unweighted;
    }

    /**
     * Calculate the number of actual connections within a graph or network.
     * @param network The network or graph to calculate the number of connections.
     * @param type Type of graph. Possible values:
     *      - "unweighted": Only one possible connection without direction between each pair of nodes.
     * @return long Number of possible connections.
     * @author Angel Fragua
     */
    private long calculateConnections(int[][] network, String type) {
        long connections = 0;
        if (type.equals("unweighted")) {
            for (int i = 0; i < network.length; i++) {
                for (int j = 0; j < network.length; j++) {
                    connections += network[i][j] == 1 ? 1 : 0;
                }
            }
            connections /= 2;
        }
        return connections;
    }

    /**
     * Calculates the early warning signals based on the network density.
     * @return List<Double> List of all the values of the densities of each network between the established dates.
     * @author Angel Fragua
     */
    public List<Double> density() {
        List<Double> densities = new ArrayList<>();
        for (int i = 0; i < this.networks.length; i++) {
            /* density = actualConnections / possibleConnections */
            densities.add((double) calculateConnections(this.unweighted[i], "unweighted") /
                            (double) calculateConnections(this.adjacencies[i], "unweighted"));
        }
        return densities;
    }

    private String networkToString(int[][] network) {
        StringBuilder result = new StringBuilder();
        for (int i = 0, j = 0; i < network.length; i++, j = 0) {
            for (; j < network.length - 1; j++) {
                result.append(network[i][j] + ",");
            }
            result.append(network[i][j] + System.lineSeparator());
        }
        return result.toString();
    }

    /**
     * Calculates the early warning signals based on the clustering coefficient of the network.
     * @return List<Double> List of all the values of the clustering coefficients of each network between
     * the established dates.
     * @author Angel Fragua
     */
    public List<Double> clusteringCoefficient() {
        List<Double> clusteringCoefficients = new ArrayList<>();
        for (int i = 0; i < this.unweighted.length; i++) {
            /* Graph Type builder */
            Graph<String, DefaultEdge> g = GraphTypeBuilder
                    .undirected().allowingMultipleEdges(false).allowingSelfLoops(false).weighted(false)
                    .edgeClass(DefaultEdge.class).vertexSupplier(SupplierUtil.createStringSupplier(1))
                    .buildGraph();
            /* Specification of the importation of the graph */
            CSVImporter gImporter = new CSVImporter(CSVFormat.MATRIX, ',');
            gImporter.setParameter(CSVFormat.Parameter.MATRIX_FORMAT_ZERO_WHEN_NO_EDGE, true);
            /* Creation of graph by adjacency matrix and calculation of its Clustering Coefficient */
            gImporter.importGraph(g, new StringReader(networkToString(this.unweighted[i])));
            ClusteringCoefficient clusteringCoefficient = new ClusteringCoefficient(g);
            /* The average is divided by 2 because in undirected graphs the local clustering coefficient should be
            multiplied by 2 something that the ClusteringCoefficient Class doesn't apply by default
            (https://en.wikipedia.org/wiki/Clustering_coefficient#Local_clustering_coefficient)*/
            clusteringCoefficients.add(clusteringCoefficient.getAverageClusteringCoefficient() / 2);
        }
        return clusteringCoefficients;
    }

    /**
     * Calculates the early warning signals based on the degree assortativity coefficient of the network.
     * This method is the generic one. Is maintained for historical reasons, the used one is assortativityCoefficient().
     * https://www.youtube.com/watch?v=gzWlSPxpHZE
     * @return List<Double> List of all the values of the degree assortativity coefficients of each network between
     * the established dates.
     * @author Angel Fragua
     */
    public List<Double> assortativityCoefficientGeneric() {
        List<Double> assortativityCoefficients = new ArrayList<>();
        for (int i = 0; i < this.unweighted.length; i++) {
            /* Graph Type builder */
            Graph<String, DefaultEdge> g = GraphTypeBuilder
                    .undirected().allowingMultipleEdges(false).allowingSelfLoops(false).weighted(false)
                    .edgeClass(DefaultEdge.class).vertexSupplier(SupplierUtil.createStringSupplier(1))
                    .buildGraph();
            /* Specification of the importation of the graph */
            CSVImporter gImporter = new CSVImporter(CSVFormat.MATRIX, ',');
            gImporter.setParameter(CSVFormat.Parameter.MATRIX_FORMAT_ZERO_WHEN_NO_EDGE, true);
            /* Creation of graph by adjacency matrix and calculation of its Degree Assortativity Coefficient */
            gImporter.importGraph(g, new StringReader(networkToString(this.unweighted[i])));

            Set<DefaultEdge> edges = g.edgeSet();
            double XAverage = 0, YAverage = 0;

            for (DefaultEdge edge: edges) {
                XAverage += g.degreeOf(g.getEdgeSource(edge));
                YAverage += g.degreeOf(g.getEdgeTarget(edge));
            }
            XAverage /= edges.size();
            YAverage /= edges.size();

            double r = 0, XStd = 0, YStd = 0;
            for (DefaultEdge edge: edges) {
                r += (g.degreeOf(g.getEdgeSource(edge)) - XAverage) * (g.degreeOf(g.getEdgeTarget(edge)) - YAverage);
                XStd += Math.pow(g.degreeOf(g.getEdgeSource(edge)) - XAverage, 2);
                YStd += Math.pow(g.degreeOf(g.getEdgeTarget(edge)) - YAverage, 2);
            }

            assortativityCoefficients.add(r / (Math.sqrt(XStd) * Math.sqrt(YStd)));
        }
        return assortativityCoefficients;
    }

    /**
     * Calculates the early warning signals based on the degree assortativity coefficient of the network.
     * This method implements the equation of Newman 2002.
     * @return List<Double> List of all the values of the degree assortativity coefficients of each network between
     * the established dates.
     * @author Angel Fragua
     */
    public List<Double> assortativityCoefficient() {
        List<Double> assortativityCoefficients = new ArrayList<>();
        for (int i = 0; i < this.unweighted.length; i++) {
            /* Graph Type builder */
            Graph<String, DefaultEdge> g = GraphTypeBuilder
                    .undirected().allowingMultipleEdges(false).allowingSelfLoops(false).weighted(false)
                    .edgeClass(DefaultEdge.class).vertexSupplier(SupplierUtil.createStringSupplier(1))
                    .buildGraph();
            /* Specification of the importation of the graph */
            CSVImporter gImporter = new CSVImporter(CSVFormat.MATRIX, ',');
            gImporter.setParameter(CSVFormat.Parameter.MATRIX_FORMAT_ZERO_WHEN_NO_EDGE, true);
            /* Creation of graph by adjacency matrix and calculation of its Degree Assortativity Coefficient */
            gImporter.importGraph(g, new StringReader(networkToString(this.unweighted[i])));

            /* Get all possible values for the nodes Degree */
            Set<Integer> degreesSet = new TreeSet();
            for (String vertex: g.vertexSet()) {
                degreesSet.add(g.degreeOf(vertex));
            }

            /* Map for each degree and its respective index, and the other way around */
            Map<Integer, Integer> degreeIdx = degreesSet.stream().collect(
                    Collectors.toMap(x -> x, x -> new ArrayList<>(degreesSet).indexOf(x)));
            Map<Integer, Integer> idxDegree = degreesSet.stream().collect(
                    Collectors.toMap(x -> new ArrayList<>(degreesSet).indexOf(x), x -> x));
            /* Get the Joint Probability Distribution of degrees */
            double[][] degreeMatrix = new double[degreesSet.size()][degreesSet.size()];
            for (DefaultEdge edge: g.edgeSet()) {
                degreeMatrix[degreeIdx.get(g.degreeOf(g.getEdgeSource(edge)))]
                            [degreeIdx.get(g.degreeOf(g.getEdgeTarget(edge)))] += 1;
                degreeMatrix[degreeIdx.get(g.degreeOf(g.getEdgeTarget(edge)))]
                            [degreeIdx.get(g.degreeOf(g.getEdgeSource(edge)))] += 1;
            }
            /* Normalize the matrix */
            for (int j = 0; j < degreeMatrix.length; j++) {
                for (int k = 0; k < degreeMatrix.length; k++) {
                    degreeMatrix[j][k] = degreeMatrix[j][k] / (g.edgeSet().size() * 2);
                }
            }
            /* Calculate the probability of rows and columns */
            double[] a = new double[degreesSet.size()];
            double[] b = new double[degreesSet.size()];
            for (int j = 0; j < degreeMatrix.length; j++) {
                for (int k = 0; k < degreeMatrix.length; k++) {
                    a[k] += degreeMatrix[j][k];
                    b[j] += degreeMatrix[j][k];
                }
            }
            /* Calculate the variance to normalize the index between -1 and 1 */
            double aVar = 0;
            double aTmp = 0;
            double bVar = 0;
            double bTmp = 0;
            for (int j = 0; j < a.length; j++) {
                aVar += a[j] * Math.pow(idxDegree.get(j), 2);
                aTmp += a[j] * idxDegree.get(j);
                bVar += b[j] * Math.pow(idxDegree.get(j), 2);
                bTmp += b[j] * idxDegree.get(j);
            }
            aVar -= Math.pow(aTmp, 2);
            bVar -= Math.pow(bTmp, 2);
            /* Calculate the final index */
            double r = 0;
            for (int j = 0; j < degreeMatrix.length; j++) {
                for (int k = 0; k < degreeMatrix.length; k++) {
                    r += idxDegree.get(j) * idxDegree.get(k) * (degreeMatrix[j][k] - a[j] * b[k]);
                }
            }
            assortativityCoefficients.add(r / Math.sqrt(aVar * bVar));
        }

        return assortativityCoefficients;
    }

    /**
     * Calculates the early warning signals based on the number of edges inside the network.
     * @return List<Double> List of all the values of the number of edges inside each network between
     * the established dates.
     * @author Angel Fragua
     */
    public List<Long> numberEdges() {
        List<Long> numberEdges = new ArrayList<>();
        for (int i = 0; i < this.unweighted.length; i++) {
            /* Graph Type builder */
            Graph<String, DefaultEdge> g = GraphTypeBuilder
                    .undirected().allowingMultipleEdges(false).allowingSelfLoops(false).weighted(false)
                    .edgeClass(DefaultEdge.class).vertexSupplier(SupplierUtil.createStringSupplier(1))
                    .buildGraph();
            /* Specification of the importation of the graph */
            CSVImporter gImporter = new CSVImporter(CSVFormat.MATRIX, ',');
            gImporter.setParameter(CSVFormat.Parameter.MATRIX_FORMAT_ZERO_WHEN_NO_EDGE, true);
            /* Creation of graph by adjacency matrix and calculation of the number of edges */
            gImporter.importGraph(g, new StringReader(networkToString(this.unweighted[i])));
            numberEdges.add((long) g.edgeSet().size());
        }
        return numberEdges;
    }

    /**
     * Calculates the early warning signals based on the Preparedness Risk Score (PRS) inside the network.
     * @return List<Double> List of all the values of the Preparedness Risk Score (PRS) of each network between
     * the established dates.
     * @author Angel Fragua
     */
    public List<Long> PRS() {
        List<Long> PRSs = new ArrayList<>();

        Queries queries = new Queries(this.db);
        long[] population = queries.getPopulation(this.countries);
        long[] susceptibles = new long[population.length];
        long[] confirmed = new long[population.length];

        long s;
        for (int i = 0; i < this.unweighted.length; i++) {
            /* Calculate susceptible population in each time t */
            for (int j = 0; j < this.countries.size(); j++) {
                confirmed[j] = this.dataOriginal[j][this.windowSize - 1 + i];
                susceptibles[j] = population[j] - confirmed[j];
            }

            s = 0;
            for (int j = 0; j < this.unweighted[i].length; j++) {
                for (int k = 0; k < this.unweighted[i].length; k++) {
                    s += susceptibles[j] * this.unweighted[i][j][k] * susceptibles[k];
                }
            }

            PRSs.add(s);
        }

        return PRSs;
    }
}
