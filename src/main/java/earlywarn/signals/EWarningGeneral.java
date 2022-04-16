package earlywarn.signals;

import org.apache.commons.math3.stat.correlation.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

/**
 * General Class for generating early warning signals and markers
 */
public class EWarningGeneral {
    /* Default values */
    protected final static LocalDate startDateDefault = LocalDate.of(2020, 2, 15);
    protected final static LocalDate endDateDefault = LocalDate.of(2020, 9, 15);
    protected final static int windowSizeDefault = 14;
    protected final static List<String> countriesDefault = new ArrayList<>(Arrays.asList(
            "AL", "AD", "AM", "AT", "AZ", "BE", "BA", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "GE", "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "MC", "ME", "NL",
            "MK", "NO", "PL", "PT", "MD", "RO", "SM", "RS", "SK", "SI", "ES", "SE", "CH", "GB", "TR",
            "UA"));
    protected final static String correlationDefault = "pearson";

    /* Class properties */
    protected GraphDatabaseService db;
    protected LocalDate startDate;
    protected LocalDate endDate;
    protected int windowSize;
    protected List<String> countries;
    protected String correlation;
    protected double[][][] networks;
    protected int[][][] adjacencies;
    protected long[][] dataOriginal;
    protected double[][] data;

    public EWarningGeneral(GraphDatabaseService db) throws Exception {
        this(db, startDateDefault, endDateDefault, countriesDefault, windowSizeDefault, correlationDefault);
    }

    public EWarningGeneral(GraphDatabaseService db, LocalDate startDate, LocalDate endDate) throws Exception {
        this(db, startDate, endDate, countriesDefault, windowSizeDefault, correlationDefault);
    }

    public EWarningGeneral(GraphDatabaseService db, LocalDate startDate, LocalDate endDate, List<String> countries,
                           int windowSize, String correlation) throws Exception {
        this.db = db;
        this.startDate = startDate;
        this.endDate = endDate;
        this.countries = countries;
        Collections.sort(countries); /* The list is sorted to avoid using dictionaries */
        this.windowSize = windowSize;
        this.correlation = correlation;

        checkDates();
    }

    protected void checkDates() throws Exception {
        if (this.startDate.isAfter(this.endDate)) {
            throw new Exception("<startDate> must be older than <endDate>");
        }
        Queries queries = new Queries(this.db);
        LocalDate maxDate = queries.maxReportDate();
        LocalDate minDate = queries.minReportDate();
        if (this.startDate.isBefore(minDate) || this.endDate.isAfter(maxDate)) {
            throw new Exception("Dates out of range. [" + minDate +
                                " , " + maxDate + "] (year-month-day)");
        }
    }

    public void checkWindows() {
        Queries queries = new Queries(this.db);
        LocalDate minDate = queries.minReportDate();
        long restDays = ChronoUnit.DAYS.between(minDate, this.startDate);
        if (restDays >= this.windowSize) {
            LocalDate startDateWindow = this.startDate.minusDays(this.windowSize - 1);
            this.dataOriginal = importData(startDateWindow);
            this.data = transformData(startDateWindow);
            this.adjacencies = generateAdjacencies(startDateWindow);
            this.networks = generateNetworks(startDateWindow);
        }
        else {
            LocalDate startDateWindow = this.startDate.minusDays(restDays);
            this.dataOriginal = importData(startDateWindow);
            this.data = transformData(startDateWindow);
            this.adjacencies = generateAdjacencies(startDateWindow);
            this.networks = generateNetworks(startDateWindow);
            this.startDate = this.startDate.plusDays(this.windowSize - 1 - restDays);
        }
    }

    protected long[][] importData(LocalDate startDateWindow) {
        int numCountries = this.countries.size();
        long[][] data = new long[numCountries][(int)ChronoUnit.DAYS.between(startDateWindow, endDate)];
        Queries queries = new Queries(this.db);
        for (int i = 0; i < numCountries; i++) {
            data[i] = queries.getReportConfirmed(this.countries.get(i), startDateWindow, this.endDate);
        }
        return data;
    }

    protected double[][] transformData(LocalDate startDateWindow) {
        Object[] arrayOfUntyped = Arrays.stream(this.dataOriginal).map(longArray -> Arrays.stream(longArray).asDoubleStream().toArray()).toArray();
        return Arrays.copyOf(arrayOfUntyped, arrayOfUntyped.length, double[][].class);
    }

    protected double[][][] generateNetworks(LocalDate startDateWindow) {
        int numCountries = this.countries.size();
        List<double[][]> networks = new ArrayList<>();
        double[][] window = new double[numCountries][this.windowSize];

        int i = 0;
        while (startDateWindow.plusDays(this.windowSize).compareTo(this.endDate.plusDays(1)) <= 0) {
            for (int j = 0; j < numCountries; j ++) {
                for (int k = 0; k < this.windowSize; k++) {
                    window[j][k] = this.data[j][k+i];
                }
            }
            networks.add(windowToNetwork(window));
            startDateWindow = startDateWindow.plusDays(1);
            i = i + 1;
        }
        return networks.toArray(new double[networks.size()][][]);
    }

    protected double[][] windowToNetwork(double[][] window) {
        int numCountries = this.countries.size();
        double[][] network = new double[numCountries][numCountries];

        double cc;
        for (int i = 0; i < numCountries; i++) {
            for (int j = 0; j < numCountries; j++) {
                if (j > i) {
                    cc = calculateCorrelation(window[i], window[j]);
                    network[i][j] = cc;
                    network[j][i] = cc;
                }
            }
        }
        return network;
    }

    protected int[][][] generateAdjacencies(LocalDate startDateWindow) {
        int numCountries = this.countries.size();
        int[][] adjacency = new int[numCountries][numCountries];
        for (int[] row: adjacency) {
            Arrays.fill(row, 1);
        }
        /* Fill adjacency diagonal with zeros */
        for (int i = 0; i < numCountries; i++) {
            adjacency[i][i] = 0;
        }
        List<int[][]> adjacencies = new ArrayList<>();
        while (startDateWindow.plusDays(this.windowSize).compareTo(this.endDate.plusDays(1)) <= 0) {
            adjacencies.add(adjacency);
            startDateWindow = startDateWindow.plusDays(1);
        }
        return adjacencies.toArray(new int[adjacencies.size()][][]);
    }

    protected double calculateCorrelation(double[] x, double[] y) {
        double cc;
        if (this.correlation.equals("spearman")) {
            SpearmansCorrelation c = new SpearmansCorrelation();
            cc = c.correlation(x, y);
        }
        else if (this.correlation.equals("kendall")) {
            KendallsCorrelation c = new KendallsCorrelation();
            cc = c.correlation(x, y);
        }
        else {
            PearsonsCorrelation c = new PearsonsCorrelation();
            cc = c.correlation(x, y);
        }
        return Double.isNaN(cc) ? 0 : cc;
    }

    @Override
    public String toString() {
        return "EWarningGeneral{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", windowSize=" + windowSize +
                ", adjacencies=(" + adjacencies.length + "," + adjacencies[0].length + "," + adjacencies[0][0].length + ")" +
                ", networks=(" + networks.length + "," + networks[0].length + "," + networks[0][0].length + ")" +
                '}';
    }
}