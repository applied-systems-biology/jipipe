package org.hkijena.jipipe.utils;

public class AIUtils {
    private AIUtils() {

    }

    public static float[] meanPoolAndNormalize(float[][] tokenEmbeddings, long[] attentionMask) {
        int dim = tokenEmbeddings[0].length;
        float[] pooled = new float[dim];
        int count = 0;

        for (int token = 0; token < tokenEmbeddings.length; token++) {
            if (attentionMask[token] == 0) continue;
            count++;
            for (int d = 0; d < dim; d++) {
                pooled[d] += tokenEmbeddings[token][d];
            }
        }

        if (count > 0) {
            for (int d = 0; d < dim; d++) {
                pooled[d] /= count;
            }
        }

        return MathUtils.l2Normalize(pooled);
    }

}
