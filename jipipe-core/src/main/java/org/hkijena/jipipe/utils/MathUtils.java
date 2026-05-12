package org.hkijena.jipipe.utils;

public class MathUtils {
    private MathUtils() {

    }

    public static float[] l2Normalize(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += v * v;
        }

        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return vector;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= (float) norm;
        }

        return vector;
    }
}
