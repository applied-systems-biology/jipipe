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
}
