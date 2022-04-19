package earlywarn.signals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueriesTest {

    private Neo4j embeddedDatabaseServer;
    private Queries queries;

    @BeforeAll
    void initializeNeo4j() throws IOException {

        var sw = new StringWriter();
        try (var in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/countries.cypher")))) {
            in.transferTo(sw);
            sw.flush();
        }

        this.embeddedDatabaseServer = Neo4jBuilders
                .newInProcessBuilder()
                .withFixture(sw.toString())
                .build();

        this.queries = new Queries(this.embeddedDatabaseServer.defaultDatabaseService());
    }

    @AfterAll
    void closeNeo4j() {
        this.embeddedDatabaseServer.close();
    }

    @Test
    void countryTest1() {
        List<String> countries = new ArrayList<>(Arrays.asList("ES","FR","NL","AD","GB","PL"));
        Set<String> countriesSet = new HashSet<>(countries);

        Set<String> countriesResponse = queries.getConfirmedCountries(countries);
        assertThat(countriesResponse).isEqualTo(countriesSet);
    }

    @Test
    void countryTest2() {
        List<String> countries = new ArrayList<>(Arrays.asList("ES","FR","NL","AD","GB","PL","XX"));
        Set<String> countriesSet = new HashSet<>(countries);

        Set<String> countriesResponse = queries.getConfirmedCountries(countries);

        assertThat(countriesResponse).isNotEqualTo(countriesSet);
        assertThat(countriesResponse).doesNotContain("XX");
        assertThat(countriesResponse.size() + 1).isEqualTo(countriesSet.size());
        countriesResponse.add("XX");
        assertThat(countriesResponse).isEqualTo(countriesSet);
    }

}
