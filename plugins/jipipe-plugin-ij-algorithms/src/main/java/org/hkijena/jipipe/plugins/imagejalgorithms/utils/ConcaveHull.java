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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

public class ConcaveHull {
    private Coordinate[] dataSet;
    private boolean[] indices;
    private static final int[] PRIME_K = {3, 5, 7, 11, 13, 17, 21, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};
    private int primeIndex;

    public ConcaveHull(Coordinate[] points, int primeIndex) {
        if (points == null || points.length < 3) {
            throw new IllegalArgumentException("Provide at least 3 points.");
        }

        this.dataSet = Arrays.stream(points).distinct().toArray(Coordinate[]::new);
        this.indices = new boolean[this.dataSet.length];
        Arrays.fill(this.indices, true);
        this.primeIndex = primeIndex;
    }

    public Coordinate[] calculate(int k) {
        if (dataSet.length < 3) {
            return dataSet;
        }

        k = Math.min(k, dataSet.length);

        int firstPoint = getLowestLatitudeIndex(dataSet);
        int currentPoint = firstPoint;
        List<Coordinate> hull = new ArrayList<>();
        hull.add(dataSet[firstPoint]);
        indices[firstPoint] = false;

        double prevAngle = 270.0; // Start west
        int step = 2;

        while ((currentPoint != firstPoint || step == 2) && Arrays.stream(indices).anyMatch(b -> b)) {
            List<Integer> knn = getKNearest(currentPoint, k);

            List<Double> angles = calculateHeadings(currentPoint, knn, prevAngle);
            List<Integer> candidates = sortByAngles(angles, knn);

            boolean valid = false;
            for (int candidate : candidates) {
                GeometryFactory gf = new GeometryFactory();
                Coordinate[] testHull = hull.toArray(new Coordinate[0]);
                testHull = Arrays.copyOf(testHull, testHull.length + 1);
                testHull[testHull.length - 1] = dataSet[candidate];
                LineString line = gf.createLineString(testHull);

                if (line.isSimple()) {
                    valid = true;
                    prevAngle = calculateHeading(dataSet[candidate], dataSet[currentPoint]);
                    currentPoint = candidate;
                    hull.add(dataSet[currentPoint]);
                    indices[currentPoint] = false;
                    step++;
                    break;
                }
            }

            if (!valid) {
                return recurseCalculate();
            }
        }

        return hull.toArray(new Coordinate[0]);
    }

    private Coordinate[] recurseCalculate() {
        if (primeIndex + 1 >= PRIME_K.length) {
            return null;
        }
        return new ConcaveHull(dataSet, primeIndex + 1).calculate(PRIME_K[primeIndex + 1]);
    }

    private List<Integer> getKNearest(int index, int k) {
        Map<Integer, Double> distances = new HashMap<>();
        for (int i = 0; i < dataSet.length; i++) {
            if (indices[i]) {
                distances.put(i, haversineDistance(dataSet[index], dataSet[i]));
            }
        }
        return distances.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double haversineDistance(Coordinate p1, Coordinate p2) {
        final double R = 6371000; // Earth radius in meters
        double lat1 = Math.toRadians(p1.y), lat2 = Math.toRadians(p2.y);
        double lon1 = Math.toRadians(p1.x), lon2 = Math.toRadians(p2.x);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private List<Double> calculateHeadings(int sourceIndex, List<Integer> targetIndices, double refHeading) {
        Coordinate source = dataSet[sourceIndex];
        List<Double> bearings = new ArrayList<>();
        for (int target : targetIndices) {
            bearings.add(calculateHeading(source, dataSet[target]) - refHeading);
        }
        return bearings.stream().map(b -> b < 0 ? b + 360 : b).collect(Collectors.toList());
    }

    private double calculateHeading(Coordinate from, Coordinate to) {
        double lonDiff = Math.toRadians(to.x - from.x);
        double y = Math.sin(lonDiff) * Math.cos(Math.toRadians(to.y));
        double x = Math.cos(Math.toRadians(from.y)) * Math.sin(Math.toRadians(to.y)) -
                Math.sin(Math.toRadians(from.y)) * Math.cos(Math.toRadians(to.y)) * Math.cos(lonDiff);
        return Math.toDegrees(Math.atan2(y, x));
    }

    private List<Integer> sortByAngles(List<Double> angles, List<Integer> knn) {
        Map<Integer, Double> map = new HashMap<>();
        for (int i = 0; i < angles.size(); i++) {
            map.put(knn.get(i), angles.get(i));
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int getLowestLatitudeIndex(Coordinate[] points) {
        int index = 0;
        double minY = points[0].y;
        for (int i = 1; i < points.length; i++) {
            if (points[i].y < minY) {
                minY = points[i].y;
                index = i;
            }
        }
        return index;
    }
}
