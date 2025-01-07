/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.utils;

import java.lang.reflect.Array;

public class ArrayUtils {
    public static byte[] reverse(byte[] arr) {
        byte[] target = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            target[arr.length - i - 1] = arr[i];
        }
        return target;
    }

    public static short[] reverse(short[] arr) {
        short[] target = new short[arr.length];
        for (int i = 0; i < arr.length; i++) {
            target[arr.length - i - 1] = arr[i];
        }
        return target;
    }

    public static int[] reverse(int[] arr) {
        int[] target = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            target[arr.length - i - 1] = arr[i];
        }
        return target;
    }

    public static long[] reverse(long[] arr) {
        long[] target = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            target[arr.length - i - 1] = arr[i];
        }
        return target;
    }

    public static float[] reverse(float[] arr) {
        float[] target = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            target[arr.length - i - 1] = arr[i];
        }
        return target;
    }

    public static double[] reverse(double[] arr) {
        double[] target = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            target[arr.length - i - 1] = arr[i];
        }
        return target;
    }

    public static <T> T[] reverse(T[] arr) {
        T[] target = (T[]) Array.newInstance(arr.getClass(), arr.length);
        for (int i = 0; i < arr.length; i++) {
            target[arr.length - i - 1] = arr[i];
        }
        return target;
    }

    public static byte[][] deepCopy(byte[][] original) {
        if (original == null) {
            return null;
        }
        byte[][] copy = new byte[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new byte[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            }
        }
        return copy;
    }

    public static short[][] deepCopy(short[][] original) {
        if (original == null) {
            return null;
        }
        short[][] copy = new short[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new short[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            }
        }
        return copy;
    }

    public static int[][] deepCopy(int[][] original) {
        if (original == null) {
            return null;
        }
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new int[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            }
        }
        return copy;
    }

    public static long[][] deepCopy(long[][] original) {
        if (original == null) {
            return null;
        }
        long[][] copy = new long[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new long[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            }
        }
        return copy;
    }

    public static float[][] deepCopy(float[][] original) {
        if (original == null) {
            return null;
        }
        float[][] copy = new float[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new float[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            }
        }
        return copy;
    }

    public static double[][] deepCopy(double[][] original) {
        if (original == null) {
            return null;
        }
        double[][] copy = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new double[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            }
        }
        return copy;
    }

}
