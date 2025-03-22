package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class ImageJMathUtils {

    public static final float[] DEFAULT_CURVATURE_KERNEL = {1f, 1f, 1f, 1f, 1f};

    public static double rodbard(double x) {
        // y = c*((a-x/(x-d))^(1/b)
        // a=3.9, b=.88, c=712, d=44
        double ex;
        if (x == 0.0)
            ex = 5.0;
        else
            ex = Math.exp(Math.log(x / 700.0) * 0.88);
        double y = 3.9 - 44.0;
        y = y / (1.0 + ex);
        return y + 44.0;
    }

    /**
     * Smooth the specified line, preserving the end points.<br>
     * Author: Eugene Katrukha
     */
    public static int[] lineSmooth(int[] a, int n) {
        float[] out = new float[n];
        int i, j;
        //no point in averaging 2 points
        if (n < 3)
            return a;
        //preserve end points
        out[0] = a[0];
        out[n - 1] = a[n - 1];
        //average middle points
        for (i = 1; i < (n - 1); i++) {
            out[i] = 0.0f;
            for (j = (i - 1); j < (i + 2); j++) {
                out[i] += a[j];
            }
            out[i] /= 3.0f;
        }
        for (i = 0; i < n; i++)
            a[i] = (int) Math.round(out[i]);
        return a;
    }

    /**
     * Smooth the specified line, preserving the end points.<br>
     * Author: Eugene Katrukha
     */
    public static float[] lineSmooth(float[] a, int n) {
        float[] out = new float[n];
        int i, j;
        //no point in averaging 2 points
        if (n < 3)
            return a;
        //preserve end points
        out[0] = a[0];
        out[n - 1] = a[n - 1];
        for (i = 1; i < (n - 1); i++) {
            out[i] = 0.0f;
            for (j = (i - 1); j < (i + 2); j++)
                out[i] += a[j];
            out[i] /= 3.0f;
        }
        return out;
    }

    public static float[] getCurvature(int[] x, int[] y, int n, float[] kernel) {
        float[] x2 = new float[n];
        float[] y2 = new float[n];
        for (int i = 0; i < n; i++) {
            x2[i] = x[i];
            y2[i] = y[i];
        }
        ImageProcessor ipx = new FloatProcessor(n, 1, x2, null);
        ImageProcessor ipy = new FloatProcessor(n, 1, y2, null);
        ipx.convolve(kernel, kernel.length, 1);
        ipy.convolve(kernel, kernel.length, 1);
        float[] indexes = new float[n];
        float[] curvature = new float[n];
        for (int i = 0; i < n; i++) {
            indexes[i] = i;
            curvature[i] = (float) Math.sqrt((x2[i] - x[i]) * (x2[i] - x[i]) + (y2[i] - y[i]) * (y2[i] - y[i]));
        }
        return curvature;
    }

    public static float[] getCurvature(float[] x, float[] y, int n, float[] kernel) {
        float[] x2 = new float[n];
        float[] y2 = new float[n];
        for (int i = 0; i < n; i++) {
            x2[i] = x[i];
            y2[i] = y[i];
        }
        ImageProcessor ipx = new FloatProcessor(n, 1, x, null);
        ImageProcessor ipy = new FloatProcessor(n, 1, y, null);
        ipx.convolve(kernel, kernel.length, 1);
        ipy.convolve(kernel, kernel.length, 1);
        float[] indexes = new float[n];
        float[] curvature = new float[n];
        for (int i = 0; i < n; i++) {
            indexes[i] = i;
            curvature[i] = (float) Math.sqrt((x2[i] - x[i]) * (x2[i] - x[i]) + (y2[i] - y[i]) * (y2[i] - y[i]));
        }
        return curvature;
    }
}
