package earlywarn.definiciones;

public class Globales {

    public static double default_alpha = (1.0/(9*96));
    public static double default_beta = (0.253/96);

    public static void updateAlpha(double alpha) {
        default_alpha = alpha;
    }

    public static void updateBeta(double beta) {
        default_beta = beta;
    }
}
