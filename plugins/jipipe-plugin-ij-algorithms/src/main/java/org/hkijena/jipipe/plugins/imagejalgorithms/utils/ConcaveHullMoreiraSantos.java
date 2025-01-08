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

import java.awt.geom.Point2D;
import java.util.*;

/**
 * ConcaveHull.java - 14/10/16
 *
 * @author Udo Schlegel - Udo.3.Schlegel(at)uni-konstanz.de
 * @version 1.0
 * <p>
 * This is an implementation of the algorithm described by Adriano Moreira and Maribel Yasmina Santos:
 * CONCAVE HULL: A K-NEAREST NEIGHBOURS APPROACH FOR THE COMPUTATION OF THE REGION OCCUPIED BY A SET OF POINTS.
 * GRAPP 2007 - International Conference on Computer Graphics Theory and Applications; pp 61-68.
 * <p>
 * https://repositorium.sdum.uminho.pt/bitstream/1822/6429/1/ConcaveHull_ACM_MYS.pdf
 * <p>
 * With help from https://github.com/detlevn/QGIS-ConcaveHull-Plugin/blob/master/concavehull.py
 * <p>
 * Adapted to modern Java standards by Ruman Gerst
 */
public class ConcaveHullMoreiraSantos {

    private ConcaveHullMoreiraSantos() {

    }

    private static double euclideanDistance(Point2D a, Point2D b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    private static List<Point2D> kNearestNeighbors(List<Point2D> l, Point2D q, int k) {
        List<Pair<Double, Point2D>> nearestList = new ArrayList<>();
        for (Point2D o : l) {
            nearestList.add(new Pair<>(euclideanDistance(q, o), o));
        }

        nearestList.sort(Comparator.comparing(Pair::getKey));

        ArrayList<Point2D> result = new ArrayList<>();

        for (int i = 0; i < Math.min(k, nearestList.size()); i++) {
            result.add(nearestList.get(i).getValue());
        }

        return result;
    }

    private static Point2D findMinYPoint2D(List<Point2D> l) {
        l.sort(Comparator.comparing(Point2D::getY));
        return l.get(0);
    }

    private static double calculateAngle(Point2D o1, Point2D o2) {
        return Math.atan2(o2.getY() - o1.getY(), o2.getX() - o1.getX());
    }

    private static double angleDifference(double a1, double a2) {
        // calculate angle difference in clockwise directions as radians
        if ((a1 > 0 && a2 >= 0) && a1 > a2) {
            return Math.abs(a1 - a2);
        } else if ((a1 >= 0 && a2 > 0) && a1 < a2) {
            return 2 * Math.PI + a1 - a2;
        } else if ((a1 < 0 && a2 <= 0) && a1 < a2) {
            return 2 * Math.PI + a1 + Math.abs(a2);
        } else if ((a1 <= 0 && a2 < 0) && a1 > a2) {
            return Math.abs(a1 - a2);
        } else if (a1 <= 0 && 0 < a2) {
            return 2 * Math.PI + a1 - a2;
        } else if (a1 >= 0 && 0 >= a2) {
            return a1 + Math.abs(a2);
        } else {
            return 0.0;
        }
    }

    private static List<Point2D> sortByAngle(List<Point2D> l, Point2D q, double a) {
        // Sort by angle descending
        l.sort((o1, o2) -> {
            double a1 = angleDifference(a, calculateAngle(q, o1));
            double a2 = angleDifference(a, calculateAngle(q, o2));
            return Double.compare(a2, a1);
        });
        return l;
    }

    private static boolean intersect(Point2D l1p1, Point2D l1p2, Point2D l2p1, Point2D l2p2) {
        // calculate part equations for line-line intersection
        double a1 = l1p2.getY() - l1p1.getY();
        double b1 = l1p1.getX() - l1p2.getX();
        double c1 = a1 * l1p1.getX() + b1 * l1p1.getY();
        double a2 = l2p2.getY() - l2p1.getY();
        double b2 = l2p1.getX() - l2p2.getX();
        double c2 = a2 * l2p1.getX() + b2 * l2p1.getY();
        // calculate the divisor
        double tmp = (a1 * b2 - a2 * b1);

        // calculate intersection point x coordinate
        double pX = (c1 * b2 - c2 * b1) / tmp;

        // check if intersection x coordinate lies in line line segment
        if ((pX > l1p1.getX() && pX > l1p2.getX()) || (pX > l2p1.getX() && pX > l2p2.getX())
                || (pX < l1p1.getX() && pX < l1p2.getX()) || (pX < l2p1.getX() && pX < l2p2.getX())) {
            return false;
        }

        // calculate intersection point y coordinate
        double pY = (a1 * c2 - a2 * c1) / tmp;

        // check if intersection y coordinate lies in line line segment
        if ((pY > l1p1.getY() && pY > l1p2.getY()) || (pY > l2p1.getY() && pY > l2p2.getY())
                || (pY < l1p1.getY() && pY < l1p2.getY()) || (pY < l2p1.getY() && pY < l2p2.getY())) {
            return false;
        }

        return true;
    }

    private static boolean pointInPolygon(Point2D p, List<Point2D> pp) {
        boolean result = false;
        for (int i = 0, j = pp.size() - 1; i < pp.size(); j = i++) {
            if ((pp.get(i).getY() > p.getY()) != (pp.get(j).getY() > p.getY()) &&
                    (p.getX() < (pp.get(j).getX() - pp.get(i).getX()) * (p.getY() - pp.get(i).getY()) / (pp.get(j).getY() - pp.get(i).getY()) + pp.get(i).getX())) {
                result = !result;
            }
        }
        return result;
    }

    public static List<Point2D> calculateConcaveHull(List<Point2D> pointArrayList, int k) {

        // the resulting concave hull
        List<Point2D> concaveHull = new ArrayList<>();

        // optional remove duplicates
        Set<Point2D> set = new HashSet<>(pointArrayList);
        List<Point2D> pointArraySet = new ArrayList<>(set);

        // k has to be greater than 3 to execute the algorithm
        int kk = Math.max(k, 3);

        // return Point2Ds if already Concave Hull
        if (pointArraySet.size() < 3) {
            return pointArraySet;
        }

        // make sure that k neighbors can be found
        kk = Math.min(kk, pointArraySet.size() - 1);

        // find first point and remove from point list
        Point2D firstPoint2D = findMinYPoint2D(pointArraySet);
        concaveHull.add(firstPoint2D);
        Point2D currentPoint2D = firstPoint2D;
        pointArraySet.remove(firstPoint2D);

        double previousAngle = 0.0;
        int step = 2;

        while ((currentPoint2D != firstPoint2D || step == 2) && !pointArraySet.isEmpty()) {

            // after 3 steps add first point to dataset, otherwise hull cannot be closed
            if (step == 5) {
                pointArraySet.add(firstPoint2D);
            }

            // get k nearest neighbors of current point
            List<Point2D> kNearestPoint2Ds = kNearestNeighbors(pointArraySet, currentPoint2D, kk);

            // sort points by angle clockwise
            List<Point2D> clockwisePoint2Ds = sortByAngle(kNearestPoint2Ds, currentPoint2D, previousAngle);

            // check if clockwise angle nearest neighbors are candidates for concave hull
            boolean its = true;
            int i = -1;
            while (its && i < clockwisePoint2Ds.size() - 1) {
                i++;

                int lastPoint2D = 0;
                if (clockwisePoint2Ds.get(i) == firstPoint2D) {
                    lastPoint2D = 1;
                }

                // check if possible new concave hull point intersects with others
                int j = 2;
                its = false;
                while (!its && j < concaveHull.size() - lastPoint2D) {
                    its = intersect(concaveHull.get(step - 2), clockwisePoint2Ds.get(i), concaveHull.get(step - 2 - j), concaveHull.get(step - 1 - j));
                    j++;
                }
            }

            // if there is no candidate increase k - try again
            if (its) {
                return calculateConcaveHull(pointArrayList, k + 1);
            }

            // add candidate to concave hull and remove from dataset
            currentPoint2D = clockwisePoint2Ds.get(i);
            concaveHull.add(currentPoint2D);
            pointArraySet.remove(currentPoint2D);

            // calculate last angle of the concave hull line
            previousAngle = calculateAngle(concaveHull.get(step - 1), concaveHull.get(step - 2));

            step++;

        }

        // Check if all points are contained in the concave hull
        boolean insideCheck = true;
        int i = pointArraySet.size() - 1;

        while (insideCheck && i > 0) {
            insideCheck = pointInPolygon(pointArraySet.get(i), concaveHull);
            i--;
        }

        // if not all points inside -  try again
        if (!insideCheck) {
            return calculateConcaveHull(pointArrayList, k + 1);
        } else {
            return concaveHull;
        }

    }

    public static class Pair<K, V> {
        public final K key;
        public final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

}
