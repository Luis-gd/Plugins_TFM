package earlywarn.definiciones;

public class Globales {

    public static double DEFAULT_ALPHA = (1.0/(9*96));
    public static double DEFAULT_BETA = (0.253/96);

    public static void updateAlpha(double alpha) {
        DEFAULT_ALPHA = alpha;
    }

    public static void updateBeta(double beta) {
        DEFAULT_BETA = beta;
    }
}
