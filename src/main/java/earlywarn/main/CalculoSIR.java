package earlywarn.main;

import earlywarn.main.modelo.SIRVuelo;

public class CalculoSIR {

    public static SIRVuelo calcularRiesgoVuelo(double sInitial, double iInitial, double rInitial,
                                               double durationInSeconds, double seatsCapacity,
                                               double occupancyPercentage, double alpha, double beta) {
        SIRVuelo ret;
        double sFinal = sInitial;
        double iFinal = iInitial;
        double rFinal = rInitial;
        double flightOccupancy = seatsCapacity * (occupancyPercentage / 100);

        for (int i = 0; i < ((durationInSeconds / 60) / 15); i++) {
            double sAux = sFinal;
            double iAux = iFinal;
            double rAux = rFinal;
            sFinal = sAux - beta * sAux * iAux / flightOccupancy;
            iFinal = iAux + beta * sAux * iAux / flightOccupancy - alpha * iAux;
            rFinal = rAux + alpha * iAux;
        }
        // Añadir datos de cálculo
        ret = new SIRVuelo(sInitial, iInitial, rInitial, sFinal, iFinal, rFinal, alpha, beta);

        return ret;
    }
}
