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

import java.awt.*;

/**
 * Stores three points, representing the center point, and an area around it
 * All values are absolute.
 */
public class PointRange {
    public Point center;
    public Point min;
    public Point max;

    /**
     * Creates a new point range at 0, 0 without areas around them
     */
    public PointRange() {
        this.center = new Point();
        this.min = new Point();
        this.max = new Point();
    }

    public PointRange(PointRange other) {
        this.center = new Point(other.center);
        this.min = new Point(other.min);
        this.max = new Point(other.max);
    }

    /**
     * Creates a new point range without margins around it
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public PointRange(int x, int y) {
        this.center = new Point(x, y);
        this.min = new Point(x, y);
        this.max = new Point(x, y);
    }

    /**
     * Creates a point range and initializes all values
     *
     * @param center center coordinate
     * @param min    absolute min coordinate
     * @param max    absolute max coordinate
     */
    public PointRange(Point center, Point min, Point max) {
        this.center = center;
        this.min = min;
        this.max = max;
    }

    /**
     * Gets a point within the range that is closest to the target point
     *
     * @param pointRange the point range
     * @param target     the target point
     * @return point within the range that is closest to the target point
     */
    public static Point getTightenedTo(PointRange pointRange, Point target) {
        return new Point(Math.max(pointRange.min.x, Math.min(pointRange.max.x, target.x)),
                Math.max(pointRange.min.y, Math.min(pointRange.max.y, target.y)));
    }

    /**
     * Gets a point within the range that is closest to the target point range
     *
     * @param pointRange the point range
     * @param target     the target point range
     * @return point within the range that is closest to the target point range
     */
    public static Point getTightenedTo(PointRange pointRange, PointRange target) {
        Point toMin = getTightenedTo(pointRange, target.min);
        Point toMax = getTightenedTo(pointRange, target.max);
        return new Point((toMin.x + toMax.x) / 2, (toMin.y + toMax.y) / 2);
    }

    /**
     * Modifies the center points, so they are closer together.
     * The min and max ranges are conserved
     *
     * @param p0 the first point
     * @param p1 the second point
     */
    public static void tighten(PointRange p0, PointRange p1) {
        p0.center = getTightenedTo(p0, p1);
        p1.center = getTightenedTo(p1, p0);
    }

    /**
     * Apply scaling to all components and return the result.
     * Returns a copy
     *
     * @param zoom the zoom
     * @return scaled copy
     */
    public PointRange zoom(double zoom) {
        PointRange result = new PointRange(this);
        result.center.x = (int) (result.center.x * zoom);
        result.center.y = (int) (result.center.y * zoom);
        result.min.x = (int) (result.min.x * zoom);
        result.min.y = (int) (result.min.y * zoom);
        result.max.x = (int) (result.max.x * zoom);
        result.max.y = (int) (result.max.y * zoom);
        return result;
    }

    /**
     * Adds the point's x and y coordinates to all points
     *
     * @param point shift coordinate
     */
    public void add(Point point) {
        center.x += point.x;
        center.y += point.y;
        min.x += point.x;
        min.y += point.y;
        max.x += point.x;
        max.y += point.y;
    }
}
