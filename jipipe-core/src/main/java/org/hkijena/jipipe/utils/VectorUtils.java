package org.hkijena.jipipe.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VectorUtils {
    public static List<Double> objectListToNumericVector(Collection<?> items) {
        List<Double> vector = new ArrayList<>(items.size());
        for (Object item : items) {
            if(item instanceof Number) {
                vector.add(((Number) item).doubleValue());
            }
            else {
                vector.add(StringUtils.parseDouble(StringUtils.nullToEmpty(item)));
            }
        }
        return vector;
    }

    public static double l2Norm(List<Double> vector) {
        double sum = 0;
        for (double v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    public  static double l1Norm(List<Double> vector) {
        double sum = 0;
        for (double v : vector) {
            sum += Math.abs(v);
        }
        return sum;
    }

    public static double lInfinityNorm(List<Double> vector) {
        double max = 0;
        for (double v : vector) {
            max = Math.max(max, Math.abs(v));
        }
        return max;
    }

    public static List<Double> multiplyScalar(List<Double> vector, double scalar) {
        List<Double> result = new ArrayList<>(vector.size());
        for (Double x : vector) {
            result.add(x * scalar);
        }
        return result;
    }

    public static List<Double> normalize(List<Double> vector) {
        double l2 = l2Norm(vector);
        return multiplyScalar(vector, 1.0 / l2);
    }

    public static List<Double> add(List<Double> v1, List<Double> v2) {
        if(v1.size() != v2.size()) {
            throw new NumberFormatException("The vectors have a different size!");
        }
        List<Double> result =  new ArrayList<>(v1.size());
        for (int i = 0; i < v1.size(); i++) {
            result.add(v1.get(i) + v2.get(i));
        }
        return result;
    }

    public static List<Double> subtract(List<Double> v1, List<Double> v2) {
        if(v1.size() != v2.size()) {
            throw new NumberFormatException("The vectors have a different size!");
        }
        List<Double> result =  new ArrayList<>(v1.size());
        for (int i = 0; i < v1.size(); i++) {
            result.add(v1.get(i) - v2.get(i));
        }
        return result;
    }


    public static double scalarProduct(List<Double> v1, List<Double> v2) {
        if(v1.size() != v2.size()) {
            throw new NumberFormatException("The vectors have a different size!");
        }
        double result = 0;
        for (int i = 0; i < v1.size(); i++) {
            result += v1.get(i) * v2.get(i);
        }
        return result;
    }
}
