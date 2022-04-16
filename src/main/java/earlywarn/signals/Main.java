package earlywarn.signals;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.time.LocalDate;
import java.util.List;

public class Main {
    @Context
    public GraphDatabaseService db;

    @UserFunction
    @Description("Test query")
    public String test(@Name("startDay") LocalDate startDay, @Name("endDay")LocalDate endDay) throws Exception {
        EWarningGeneral eWarn = new EWarningGeneral(this.db, startDay, endDay);
        eWarn.checkWindows();
        return eWarn.toString();
    }

    @UserFunction
    @Description("Test query1")
    public List<Double> test1(@Name("startDay") LocalDate startDay, @Name("endDay")LocalDate endDay,
                              @Name("cumulativeData") boolean cumulativeData, @Name("squareRootData") boolean squareRootData,
                              @Name("threshold") double threshold) throws Exception {
        EWarningSpecific eWarn = new EWarningSpecific(this.db, startDay, endDay, cumulativeData, squareRootData, threshold);
        eWarn.checkWindows();
        return eWarn.density();
    }
}
