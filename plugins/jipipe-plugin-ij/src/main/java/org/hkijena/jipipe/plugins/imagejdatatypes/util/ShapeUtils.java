package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class ShapeUtils {
    /**
     * Creates a Rectangle from two points (x1, y1) and (x2, y2).
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @return a Rectangle object representing the area between the two points
     */
    public static Rectangle pointsToRectangle(int x1, int y1, int x2, int y2) {
        // Calculate the top-left corner and dimensions
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int width = Math.abs(x2 - x1);
        int height = Math.abs(y2 - y1);

        // Create and return the Rectangle
        return new Rectangle(x, y, width, height);
    }

    /**
     * Creates a Rectangle2D.Double from two points (x1, y1) and (x2, y2).
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @return a Rectangle2D.Double object representing the area between the two points
     */
    public static Rectangle2D.Double pointsToRectangle(double x1, double y1, double x2, double y2) {
        // Calculate the top-left corner and dimensions
        double x = Math.min(x1, x2);
        double y = Math.min(y1, y2);
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);

        // Create and return the Rectangle2D.Double
        return new Rectangle2D.Double(x, y, width, height);
    }
}
