package earlywarn.signals;

import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

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

    public EWarningSpecific(GraphDatabaseService db) throws Exception {
        this(db, EWarningGeneral.startDateDefault, EWarningGeneral.endDateDefault, EWarningGeneral.countriesDefault,
             EWarningGeneral.windowSizeDefault, EWarningGeneral.correlationDefault, cumulativeDataDefault,
             squareRootDataDefault, thresholdDefault);
    }

    public EWarningSpecific(GraphDatabaseService db, LocalDate startDate, LocalDate endDate) throws Exception {
        this(db, startDate, endDate, EWarningGeneral.countriesDefault, EWarningGeneral.windowSizeDefault,
             EWarningGeneral.correlationDefault, cumulativeDataDefault, squareRootDataDefault, thresholdDefault);
    }

    public EWarningSpecific(GraphDatabaseService db, LocalDate startDate, LocalDate endDate, boolean cumulativeData,
                            boolean squareRootData, double threshold) throws Exception {
        this(db, startDate, endDate, EWarningGeneral.countriesDefault, EWarningGeneral.windowSizeDefault,
                EWarningGeneral.correlationDefault, cumulativeData, squareRootData, threshold);
    }

    public EWarningSpecific(GraphDatabaseService db, LocalDate startDate, LocalDate endDate, List<String> countries,
                            int windowSize, String correlation, boolean cumulativeData, boolean squareRootData,
                            double threshold) throws Exception {
        super(db, startDate, endDate, countries, windowSize, correlation);
        this.cumulativeData = cumulativeData;
        this.squareRootData = squareRootData;
        this.threshold = threshold;
    }

    @Override
    protected double[][] transformData(LocalDate startDateWindow) {
        int numCountries = this.countries.size();
        Queries queries = new Queries(this.db);

        if (this.cumulativeData == false) {
            long[] extraDate = new long[numCountries];
            if (startDateWindow.isBefore(queries.minReportDate())) {
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
            double[][] data = new double[numCountries][this.dataOriginal.length + 1];
            double x;
            for (int i = 0; i < numCountries; i++) {
                for (int j = 0; j < this.dataOriginal[0].length; j++) {
                    x =  this.dataOriginal[i][j];
                    if (this.squareRootData) {
                        x = Math.sqrt(x);
                    }
                    data[i][j] = x;
                }
            }
            return data;
        }
        else {
            return super.transformData(startDateWindow);
        }
    }

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

    @Override
    public void checkWindows() {
        super.checkWindows();
        this.unweighted = generateUnweighted();
    }

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

    private long calculateConnections(int[][] network, String type) {
        long conections = 0;
        if (type.equals("unweighted")) {
            for (int i = 0; i < network.length; i++) {
                for (int j = 0; j < network.length; j++) {
                    conections += network[i][j] == 1 ? 1 : 0;
                }
            }
            conections /= 2;
        }
        return conections;
    }

    public List<Double> density() {
        List<Double> densities = new ArrayList<>();
        for (int i = 0; i < this.networks.length; i++) {
            long actualConnections = calculateConnections(this.unweighted[i], "unweighted");
            long possibleConnections = calculateConnections(this.adjacencies[i], "unweighted");
            double density = (double) actualConnections / (double) possibleConnections;
            densities.add(density);
        }
        return densities;
    }
}
