package earlywarn.signals;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used to declare the queries needed for generating early warning signals and markers.
 */
public class Queries {
	/*
	 * Neo4j instance database
	 * Must be obtained using the annotation @Context in a procedure or function
	 */
	private final GraphDatabaseService db;

	/**
	 * Basic class constructor that receive the context of the Neo4j instance.
	 * @param db Neo4j database instance annotated with the @Context from the main procedure function.
	 * @author Angel Fragua
	 */
	public Queries(GraphDatabaseService db) {
		this.db = db;
	}

	/**
	 * Gets the minimum date of reports in the database.
	 * Assumes that all countries have the same number and daily distribution of reports.
	 * @return LocalDate First date of reports in the database.
	 * @author Angel Fragua
	 */
	public LocalDate minReportDate() {
		try (Transaction tx = db.beginTx()) {
			try (Result res = tx.execute(
				"MATCH (n:Country) - [:REPORTS] - (r:Report)\n" +
					"RETURN min(date(r.releaseDate)) AS minDate")) {
				Map<String, Object> row = res.next();
				// Doesn't need formatter because is already ISO_LOCAL_DATE
				return (LocalDate) row.get("minDate");
			}
		}
	}

	/**
	 * Gets the maximum date of reports in the database.
	 * Assumes that all countries have the same number and daily distribution of reports.
	 * @return LocalDate Last date of reports in the database.
	 * @author Angel Fragua
	 */
	public LocalDate maxReportDate() {
		try (Transaction tx = db.beginTx()) {
			try (Result res = tx.execute(
				"MATCH (n:Country) - [:REPORTS] - (r:Report)\n" +
					"RETURN max(date(r.releaseDate)) AS maxDate")) {
				Map<String, Object> row = res.next();
				return (LocalDate) row.get("maxDate");
			}
		}
	}

	/**
	 * Gets the list of confirmed reported cases of a specific country between two dates from the database.
	 * @param countryIso2 ISO-3166 Alpha2 of the country to search for.
	 * @param startDate First date of the range of days of interest.
	 * @param endDate Last date of the range of days of interest.
	 * @return int[] List of the confirmed cases between the specified dates in a specific country.
	 */
	public long[] getReportConfirmed(String countryIso2, LocalDate startDate, LocalDate endDate) {
		try (Transaction tx = db.beginTx()) {
			try (Result res = tx.execute(
				"MATCH (c:Country{iso2: '" + countryIso2 + "'}) - [:REPORTS] - (r:Report) \n" +
					"WHERE \n" +
						"date(r.lastUpdate) >= date({year:" + startDate.getYear() + ", month:" +
							startDate.getMonthValue() + ", day:" + startDate.getDayOfMonth() + "}) AND\n" +
						"date(r.lastUpdate) <= date({year:" + endDate.getYear() + ", month:" +
							endDate.getMonthValue() + ", day:" + endDate.getDayOfMonth() + "})\n" +
					"RETURN r.confirmed AS confirmed ORDER BY date(r.lastUpdate)")) {
				List<Long> confirmed = new ArrayList<>();
				while (res.hasNext()) {
					Map<String, Object> row = res.next();
					confirmed.add((Long) row.get("confirmed"));
				}
				return ArrayUtils.toPrimitive(confirmed.toArray(new Long[0]));
			}
		}
	}

	/**
	 * Gets a set of the ISO-3166-Alpha2 referencing the countries that are both present in the list passed as argument
	 * and the database.
	 * @param countries List of countries references in the ISO-3166-Alpha2 format to be checked.
	 * @return Set<String> Set of the ISO-3166-Alpha2 referencing the countries matched.
	 * @author Angel Fragua
	 */
	public Set<String> getConfirmedCountries(List<String> countries) {
		try (Transaction tx = db.beginTx()) {
			try (Result res = tx.execute(
					"MATCH (c:Country)\n" +
						"WHERE c.iso2 IN " + countries.stream().collect(Collectors.joining("','", "['", "']")) + "\n" +
						"RETURN c.iso2 AS country")) {
				Set<String> confirmedCountries = new HashSet<>();
				while (res.hasNext()) {
					Map<String, Object> row = res.next();
					confirmedCountries.add((String)row.get("country"));
				}
				return confirmedCountries;
			}
		}
	}
}
