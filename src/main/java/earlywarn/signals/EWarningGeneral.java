package earlywarn.signals;

import org.apache.commons.math3.stat.correlation.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * General Class for generating early warning signals and markers to early detect outbreaks.
 * Notes: It assumes that every country has the same number of reports and that there is no gap between the first date
 * with covid reports and the last one. Also, it assumes tha all countries have the same date for the first report,
 * and hence all countries have the same date for its last report. (All things has been proved)
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
    protected List<String> countries;
    protected int windowSize;
    protected String correlation;
    protected double[][][] networks;
    protected int[][][] adjacencies;
    protected long[][] dataOriginal;
    protected double[][] data;

    /**
     * Secondary constructor for the Class that receive fewer parameters than the main one.
     * @param db Neo4j database instance annotated with the @Context from the main procedure function.
     * @throws Exception If startDate is greater than endDate or the database doesn't contain it.
     * If there are less than two selected countries. If any country inside the countries list isn't contain
     * in the database.
     * @author Angel Fragua
     */
    public EWarningGeneral(GraphDatabaseService db) throws Exception {
        this(db, startDateDefault, endDateDefault, countriesDefault, windowSizeDefault, correlationDefault);
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
    public EWarningGeneral(GraphDatabaseService db, LocalDate startDate, LocalDate endDate) throws Exception {
        this(db, startDate, endDate, countriesDefault, windowSizeDefault, correlationDefault);
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
     *      - "spearman": Spearman Correlation
     *      - "kendall":Kendall Correlation
     *      - any other value: Pearson Correlation
     * @throws Exception If startDate is greater than endDate or the database doesn't contain it.
     * If there are less than two selected countries. If any country inside the countries list isn't contain
     * in the database.
     * @author Angel Fragua
     */
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
        checkCountries();
    }

    /**
     * Assures that the user establish a start date of study previous to the end date. Also, it assures that the
     * database contains reports of covid confirmed cases for both dates, which means that it also will contain reports
     * for all the dates in the interval between the selected dates of study.
     * @throws Exception If startDate is greater than endDate or the database doesn't contain it.
     * @author Angel Fragua
     */
    protected void checkDates() throws Exception {
        if (this.startDate.isAfter(this.endDate)) {
            /* TODO: Generate a specific Exception */
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

    /**
     * Assures that there are at least two selected countries. And also assures, that all the selected countries
     * are contained in the database.
     * @throws Exception If there are less than two selected countries. If any country inside the countries list
     * isn't contain in the database.
     * @author Angel Fragua
     */
    private void checkCountries() throws Exception {
        Set<String> countriesSelected = new HashSet<>(this.countries);
        if (countriesSelected.size() < 2) {
            throw new Exception("There must be at least two ISO-3166-Alpha2 country references in <countries> and" +
                                "must be contained in the database.");
        }

        Queries queries = new Queries(this.db);
        Set<String> countriesDb = queries.getConfirmedCountries(this.countries);

        if (!countriesDb.containsAll(countriesSelected)) {
            countriesSelected.removeAll(countriesDb);
            throw new Exception("All ISO-3166-Alpha2 country references in <countries> must exist and be contained " +
                                "in the database. Errors: " + countriesSelected);
        }
    }

    /**
     * Main method, that makes sure that all the data is correctly imported and transformed to subsequently generate
     * the corresponding networks matrices with its adjacencies for each instance of time between the start and
     * end date. In case that there isn't enough reports previous to the start date to fill the window size, it shifts
     * the start date enough dates to fulfill it.
     * @author Angel Fragua
     */
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

    /**
     * Import for every country, the original cumulative data of confirmed covid cases between the established dates
     * of study taking into account the size of the window.
     * @param startDateWindow Start date corresponding to the first window's date, which will be as many days prior to
     * the real start date of study as the size of the windows minus one.
     * @return long[][] Matrix with the original cumulative data of confirmed covid cases. Each Row represents
     * a country, and each Column contains the cases from the first date of study until the end date.
     * @author Angel Fragua
     */
    protected long[][] importData(LocalDate startDateWindow) {
        int numCountries = this.countries.size();
        long[][] data = new long[numCountries][(int)ChronoUnit.DAYS.between(startDateWindow, endDate)];
        Queries queries = new Queries(this.db);
        for (int i = 0; i < numCountries; i++) {
            data[i] = queries.getReportConfirmed(this.countries.get(i), startDateWindow, this.endDate);
        }
        return data;
    }

    /**
     * Transform the original data of cumulative confirmed covid cases to its desired form. In this general case, it
     * returns a copy of the same data matrix.
     * @param startDateWindow Start date corresponding to the first window's date, which will be as many days prior to
     * the real start date of study as the size of the windows minus one.
     * @return long[][] Matrix with the transformed data. Each Row represents a country, and each Column contains
     * the cases from the first date of study until the end date.
     * @author Angel Fragua
     */
    protected double[][] transformData(LocalDate startDateWindow) {
        Object[] arrayOfUntyped = Arrays.stream(this.dataOriginal).map(longArray -> Arrays.stream(longArray).asDoubleStream().toArray()).toArray();
        return Arrays.copyOf(arrayOfUntyped, arrayOfUntyped.length, double[][].class);
    }

    /**
     * Generates an adjacency matrix for each instant of study between the start date and the end date. By default,
     * the matrix generated represents a complete graph, which means that each node can be connected to every other node
     * except itself. This means that all adjacency matrices will be filled with 1's except the main diagonal
     * (top-left to bottom-right) that will be filled with 0's.
     * @param startDateWindow Start date corresponding to the first window's date, which will be as many days prior to
     * the real start date of study as the size of the windows minus one.
     * @return int[][][] List of the adjacency matrices for each temporal instant from the start date to the end date.
     * @author Angel Fragua
     */
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

    /**
     * Generates a correlation matrix for each instant of study between the start date and the end date. This means
     * that for every pair of windows containing the confirmed covid cases for each possible pair of countries,
     * are used to calculate its correlation coefficient which will determinate the wight of the edge that
     * connects them both in the graph.
     * @param startDateWindow Start date corresponding to the first window's date, which will be as many days prior to
     * the real start date of study as the size of the windows minus one.
     * @return double[][][] List of the correlation matrices for each temporal instant from the start date to
     * the end date.
     * @author Angel Fragua
     */
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

    /**
     * Transform the data of the confirmed covid cases in a fixed window time to de matrix adjacency of the network,
     * where the edges represent the coefficient correlation between its pair of nodes, and the nodes represent each
     * country.
     * @param window Data of the confirmed covid cases in a fixed period of time, where the Rows represent each country
     * and the columns represent each date from the latest to the new ones.
     * @return double[][] The network's matrix adjacency created with the data of a fixed time window.
     * @author Angel Fragua
     */
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

    /**
     * Computes the correlation coefficient between two arrays. Depending on the value established on the class property
     * correlation different types of correlation will be used. List of possible correlation values:
     *      - "spearman": Spearman Correlation
     *      - "kendall":Kendall Correlation
     *      - any other value: Pearson Correlation
     * @param x First data array.
     * @param y Second data array.
     * @return double The computed correlation coefficient, or zero in case it returns NaN.
     * @author Angel Fragua
     * TODO: Granger Causality https://bmcecol.biomedcentral.com/articles/10.1186/s12898-016-0087-7
     */
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

    /**
     * Overrides the default method to transform the class to a string.
     * @return The generated string.
     * @author Angel Fragua
     */
    @Override
    public String toString() {
        return "EWarningGeneral{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", countries=" + countries +
                ", windowSize=" + windowSize +
                ", correlation=" + correlation +
                ", adjacencies=(" + adjacencies.length + "," + adjacencies[0].length + "," + adjacencies[0][0].length + ")" +
                ", networks=(" + networks.length + "," + networks[0].length + "," + networks[0][0].length + ")" +
                '}';
    }
}