package earlywarn.mh;

import java.util.TreeMap;

public class CalculoSIR {

    public static TreeMap<String, Double> calcularRiesgoVuelo(double sInitial, double iInitial, double rInitial, double durationInSeconds, double flightOccupancy, double alpha, double beta){
        TreeMap<String, Double> ret = new TreeMap<>();
        double sFinal = sInitial;
        double iFinal = iInitial;
        double rFinal = rInitial;

        for (int i = 0; i < ((durationInSeconds / 60) / 15); i++) {
            double sAux = sFinal;
            double iAux = iFinal;
            double rAux = rFinal;
            sFinal = sAux - beta * sAux * iAux / flightOccupancy;
            iFinal = iAux + beta * sAux * iAux / flightOccupancy - alpha * iAux;
            rFinal = rAux + alpha * iAux;
        }
        // Añadir datos de cálculo
        ret.put("S_inicial", sInitial);
        ret.put("I_inicial", iInitial);
        ret.put("R_inicial", rInitial);
        ret.put("S_final", sFinal);
        ret.put("I_final", iFinal);
        ret.put("R_final", rFinal);
        ret.put("Alpha_recuperacion", beta);
        ret.put("Beta_transmision", alpha);

        return ret;
    }
}
