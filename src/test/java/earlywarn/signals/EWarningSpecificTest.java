package earlywarn.signals;

import org.junit.jupiter.api.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.math3.util.Precision.round;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit Class used to test the class EWarningSpecific. It builds a temporal Neo4j database witch is loaded with
 * the nodes declared in the resources files.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS) /* This annotation is needed for creating a JUnit Class*/
public class EWarningSpecificTest {

    private Neo4j embeddedDatabaseServer;
    private GraphDatabaseService db;

    /**
     * Initialize a temporal Neo4j instance Database for the current Class tests.
     * It reads a file containing the queries for the creation of some Country Nodes. It also reads a file with the
     * queries needed to create some Report Nodes of the previous Country Nodes between the date 22-1-2020 and 1-3-2020.
     * Last it creates execute a query that creates a Relationship between each Country Node and its corresponding
     * Report Nodes. Furthermore, it saves a reference to the Database Service used to run queries in the Database.
     * Thanks to the @BeforeAll annotation is the first method of the test to be executed, so it works as a constructor.
     * @throws IOException If there is a problem reading any resource file.
     */
    @BeforeAll
    void initializeNeo4j() throws IOException {

        var countries = new StringWriter();
        try (var in = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/countries.cypher")))) {
            in.transferTo(countries);
            countries.flush();
        }

        /* 40 Reports for each country starting the 22/01/2020 until the 01/03/2020  */
        var reports = new StringWriter();
        try (var in = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/reports.cypher")))) {
            in.transferTo(reports);
            reports.flush();
        }

        this.embeddedDatabaseServer = Neo4jBuilders
                .newInProcessBuilder()
                /* Loads the Country Nodes */
                .withFixture(countries.toString())
                /* Loads the Report Nodes */
                .withFixture(reports.toString())
                /* Creates a :REPORTS Relationship between previous Nodes */
                .withFixture("MATCH (c:Country), (r:Report) " +
                             "WHERE c.countryName = r.country " +
                             "MERGE (c) - [:REPORTS] -> (r)")
                .build();

        this.db = this.embeddedDatabaseServer.defaultDatabaseService();
    }

    /**
     * This method is the last executed by the class thanks to the @AfterAll annotation, which can be used to close all
     * connections to the Database.
     */
    @AfterAll
    void closeNeo4j() {
        this.embeddedDatabaseServer.close();
    }

    /**
     * Tests that the method density() from the EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = false which means that the data will be converted from the cumulative covid
     * cases to daily cases, and squareRootData = false which won't apply the square root to the original data to
     * smooth it. Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void density1() {
        List<Double> densities = new ArrayList<>(Arrays.asList(0.002898550724638,0.002898550724638,0.002898550724638,
                0.002898550724638,0.002898550724638,0.001932367149758,0.001932367149758,0.001932367149758,
                0.001932367149758,0.001932367149758,0.001932367149758,0.,0.,0.000966183574879,0.000966183574879,
                0.000966183574879,0.000966183574879,0.000966183574879,0.,0.000966183574879,0.000966183574879,
                0.02512077294686,0.065700483091787,0.085990338164251,0.085024154589372,0.114009661835749,
                0.150724637681159));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 3, 1),
                                                   EWarningSpecific.countriesDefault, 14, "pearson", false, false, 0.5);
        ew.checkWindows();

        assertThat(ew.density().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(densities.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method density() from the EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = false which means that the data will be converted from the cumulative covid
     * cases to daily cases, and squareRootData = true which apply the square root to the original data to smooth it.
     * Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void density2() {
        List<Double> densities = new ArrayList<>(Arrays.asList(0.001932367149758,0.001932367149758,0.001932367149758,
                0.001932367149758,0.001932367149758,0.001932367149758,0.003864734299517,0.003864734299517,
                0.003864734299517,0.002898550724638,0.,0.,0.000966183574879,0.000966183574879,0.000966183574879,
                0.000966183574879,0.000966183574879,0.,0.,0.,0.014492753623188,0.052173913043478,0.057971014492754,
                0.055072463768116,0.060869565217391,0.077294685990338));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 2, 5), LocalDate.of(2020, 3, 1),
                EWarningSpecific.countriesDefault, 14, "pearson", false, true, 0.6);
        ew.checkWindows();

        assertThat(ew.density().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(densities.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method density() from the EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = true which means that the data won't be converted from the cumulative covid
     * cases to daily cases, and squareRootData = false which won't apply the square root to the original data to
     * smooth it. Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void density3() {
        List<Double> densities = new ArrayList<>(Arrays.asList(0.002898550724638,0.009661835748792,0.018357487922705,
                0.018357487922705,0.018357487922705,0.016425120772947,0.016425120772947,0.011594202898551,
                0.004830917874396,0.007729468599034,0.010628019323671,0.009661835748792,0.009661835748792,
                0.009661835748792,0.005797101449275,0.002898550724638,0.001932367149758,0.001932367149758,
                0.000966183574879,0.000966183574879,0.000966183574879,0.000966183574879,0.000966183574879,
                0.000966183574879,0.000966183574879,0.000966183574879,0.027053140096618));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 30), LocalDate.of(2020, 2, 25),
                EWarningSpecific.countriesDefault, 7, "spearman", true, false, 0.4);
        ew.checkWindows();

        assertThat(ew.density().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(densities.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method density() from the EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = true which means that the data won't be converted from the cumulative covid
     * cases to daily cases, and squareRootData = true which apply the square root to the original data to smooth it.
     * Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void density4() {
        List<Double> densities = new ArrayList<>(Arrays.asList(0.166666666666667,0.166666666666667,0.166666666666667,
                0.194444444444444,0.194444444444444,0.194444444444444,0.166666666666667,0.166666666666667,
                0.111111111111111,0.083333333333333,0.083333333333333,0.083333333333333,0.083333333333333,
                0.083333333333333,0.027777777777778,0.027777777777778,0.027777777777778,0.027777777777778,
                0.027777777777778,0.,0.,0.027777777777778,0.111111111111111,0.166666666666667,0.25,0.277777777777778,
                0.277777777777778));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 22), LocalDate.of(2020, 3, 1),
                new ArrayList<>(Arrays.asList("AL", "BE", "FR", "ES", "SE", "CH", "GB", "TR", "UA")), 14, "kendall",
                true, true, 0.7);
        ew.checkWindows();

        assertThat(ew.density().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(densities.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method clusteringCoefficient() from EWarningSpecific returns the correct List with
     * a precision of 10 decimal.
     * This test checks cumulativeData = false which means that the data will be converted from the cumulative covid
     * cases to daily cases, and squareRootData = false which won't apply the square root to the original data to
     * smooth it. Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void clusteringCoefficient1() {
        List<Double> clusteringCoefficients = new ArrayList<>(Arrays.asList(0.032608695652174,0.032608695652174,
                0.032608695652174,0.032608695652174,0.032608695652174,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,
                0.081780538302277,0.128623188405797,0.188255153840192,0.192595598845599,0.208019999324347,
                0.223542912401608));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 22), LocalDate.of(2020, 3, 1),
                EWarningSpecific.countriesDefault, 14, "pearson", false, false, 0.5);
        ew.checkWindows();

        assertThat(ew.clusteringCoefficient().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(clusteringCoefficients.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method clusteringCoefficient() from EWarningSpecific returns the correct List with
     * a precision of 10 decimal.
     * This test checks cumulativeData = false which means that the data will be converted from the cumulative covid
     * cases to daily cases, and squareRootData = true which apply the square root to the original data to smooth it.
     * Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void clusteringCoefficient2() {
        List<Double> clusteringCoefficients = new ArrayList<>(Arrays.asList(0.,0.,0.,0.,0.,0.,0.,0.02536231884058,
                0.036231884057971,0.036231884057971,0.036231884057971,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.,0.065217391304348));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 30), LocalDate.of(2020, 2, 25),
                EWarningSpecific.countriesDefault, 14, "spearman", false, true, 0.4);
        ew.checkWindows();

        assertThat(ew.clusteringCoefficient().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(clusteringCoefficients.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method clusteringCoefficient() from EWarningSpecific returns the correct List with
     * a precision of 10 decimal.
     * This test checks cumulativeData = true which means that the data won't be converted from the cumulative covid
     * cases to daily cases, and squareRootData = false which won't apply the square root to the original data to
     * smooth it. Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void clusteringCoefficient3() {
        List<Double> clusteringCoefficients = new ArrayList<>(Arrays.asList(0.,0.,0.,0.,0.,0.,0.053260869565217,
                0.058695652173913,0.03804347826087,0.03804347826087,0.,0.,0.,0.,0.032608695652174,0.032608695652174,0.,
                0.,0.,0.,0.));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 22), LocalDate.of(2020, 2, 15),
                EWarningSpecific.countriesDefault, 5, "pearson", true, false, 0.7);
        ew.checkWindows();

        assertThat(ew.clusteringCoefficient().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(clusteringCoefficients.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method clusteringCoefficient() from EWarningSpecific returns the correct List with
     * a precision of 10 decimal.
     * This test checks cumulativeData = true which means that the data won't be converted from the cumulative covid
     * cases to daily cases, and squareRootData = true which apply the square root to the original data to smooth it.
     * Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void clusteringCoefficient4() {
        List<Double> clusteringCoefficients = new ArrayList<>(Arrays.asList(0.277777777777778,0.277777777777778,
                0.277777777777778,0.277777777777778,0.277777777777778,0.277777777777778,0.277777777777778,
                0.277777777777778,0.277777777777778,0.277777777777778,0.222222222222222,0.222222222222222,
                0.222222222222222,0.166666666666667,0.166666666666667,0.166666666666667,0.166666666666667,0.,0.,0.,
                0.222222222222222,0.277777777777778,0.277777777777778));

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 25), LocalDate.of(2020, 2, 27),
                new ArrayList<>(Arrays.asList("AL", "BE", "FR", "ES", "SE", "CH", "GB", "TR", "UA")), 15, "kendall",
                true, true, 0.3);
        ew.checkWindows();

        assertThat(ew.clusteringCoefficient().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(clusteringCoefficients.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method numberEdges() from EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = false which means that the data will be converted from the cumulative covid
     * cases to daily cases, and squareRootData = false which won't apply the square root to the original data to
     * smooth it. Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void numberEdges1() {
        List<Long> numberEdges = Arrays
                .stream(new long[]{0,0,1,1,1,1,1,0,1,1,26,68,89,88,118,156})
                .boxed().collect(Collectors.toList());

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 2, 15), LocalDate.of(2020, 3, 1),
                EWarningSpecific.countriesDefault, 14, "pearson", false, false, 0.5);
        ew.checkWindows();

        assertThat(ew.numberEdges().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(numberEdges.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method numberEdges() from EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = false which means that the data will be converted from the cumulative covid
     * cases to daily cases, and squareRootData = true which apply the square root to the original data to smooth it.
     * Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void numberEdges2() {
        List<Long> numberEdges = Arrays
                .stream(new long[]{2,2,2,2,2,2,2,4,4,4,3,0,1,1,1,1,1,2,1,1,1,21,68,98,95,117,148})
                .boxed().collect(Collectors.toList());

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 3, 1),
                EWarningSpecific.countriesDefault, 14, "pearson", false, true, 0.4);
        ew.checkWindows();

        assertThat(ew.numberEdges().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(numberEdges.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method numberEdges() from EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = true which means that the data won't be converted from the cumulative covid
     * cases to daily cases, and squareRootData = false which won't apply the square root to the original data to
     * smooth it. Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void numberEdges3() {
        List<Long> numberEdges = Arrays
                .stream(new long[]{4,13,19,19,18,18,17,17,13,12,12,10,10,10,9,6,4,3,2,1,1,1,1,1,1,16})
                .boxed().collect(Collectors.toList());

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 22), LocalDate.of(2020, 2, 25),
                EWarningSpecific.countriesDefault, 10, "kendall", true, false, 0.6);
        ew.checkWindows();

        assertThat(ew.numberEdges().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(numberEdges.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }

    /**
     * Tests that the method numberEdges() from EWarningSpecific returns the correct List with a 10 decimal precision.
     * This test checks cumulativeData = true which means that the data won't be converted from the cumulative covid
     * cases to daily cases, and squareRootData = true which apply the square root to the original data to smooth it.
     * Rest of parameters are being changed to assure its correctness.
     */
    @Test
    void numberEdges4() {
        List<Long> numberEdges = Arrays
                .stream(new long[]{7, 7, 7, 7, 6, 5, 4, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 3, 4})
                .boxed().collect(Collectors.toList());

        EWarningSpecific ew = new EWarningSpecific(this.db, LocalDate.of(2020, 1, 30), LocalDate.of(2020, 2, 28),
                new ArrayList<>(Arrays.asList("AL", "BE", "FR", "ES", "SE", "CH", "GB", "TR", "UA")), 20, "spearman",
                true, true, 0.8);
        ew.checkWindows();

        assertThat(ew.numberEdges().stream().map(x -> round(x, 10)).collect(Collectors.toList()))
                .isEqualTo(numberEdges.stream().map(x -> round(x, 10)).collect(Collectors.toList()));
    }
}
