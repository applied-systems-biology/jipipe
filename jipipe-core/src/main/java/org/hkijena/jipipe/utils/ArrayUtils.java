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
