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

import ij.process.ImageProcessor;

import java.awt.geom.Line2D;
import java.util.Collections;
import java.util.Vector;

/**
 * Adapted from <a href="https://github.com/davidchatting/hough_lines/blob/master/HoughTransform.java">HoughTransform</a>
 *
 * @author David Chatting - 4th March 2013
 */
public class LegacyHoughLines {
    // The size of the neighbourhood in which to search for other local maxima
    final int neighbourhoodSize;

    // How many discrete values of theta shall we check?
    final int maxTheta;

    // Using maxTheta, work out the step
    final double thetaStep;

    // the width and height of the image
    protected int width, height;

    // the hough array
    protected float[][] houghArray;

    // the coordinates of the centre of the image
    protected float centerX, centerY;

    // the height of the hough array
    protected int houghHeight;

    // double the hough height (allows for negative numbers)
    protected int doubleHeight;

    // the number of points that have been added
    protected int numPoints;

    // cache of values of sin and cos for different theta values. Has a significant performance improvement.
    private double[] sinCache;
    private double[] cosCache;

    public LegacyHoughLines(int neighbourhoodSize, int maxTheta) {
        this.neighbourhoodSize = neighbourhoodSize;
        this.maxTheta = maxTheta;
        this.thetaStep = Math.PI / maxTheta;
    }

    /**
     * Initialises the hough array. Called by the constructor so you don't need to call it
     * yourself, however you can use it to reset the transform if you want to plug in another
     * image (although that image must have the same width and height)
     */
    public void initialise(int width, int height) {
        this.width = width;
        this.height = height;

        // Calculate the maximum height the hough array needs to have
        houghHeight = (int) (Math.sqrt(2) * Math.max(height, width)) / 2;

        // Double the height of the hough array to cope with negative r values
        doubleHeight = 2 * houghHeight;

        // Create the hough array
        houghArray = new float[maxTheta][doubleHeight];

        // Find edge points and vote in array
        centerX = width / 2;
        centerY = height / 2;

        // Count how many points there are
        numPoints = 0;

        // cache the values of sin and cos for faster processing
        sinCache = new double[maxTheta];
        cosCache = sinCache.clone();
        for (int t = 0; t < maxTheta; t++) {
            double realTheta = t * thetaStep;
            sinCache[t] = Math.sin(realTheta);
            cosCache[t] = Math.cos(realTheta);
        }
    }

    /**
     * Adds points from an image. The image is assumed to be greyscale black and white, so all pixels that are
     * not black are counted as edges. The image should have the same dimensions as the one passed to the constructor.
     */
    public void addPoints(ImageProcessor image, boolean threshold, float minThreshold) {

        // Now find edge points and update the hough array
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                float value = image.getf(x, y);
                if (threshold) {
                    if (value > minThreshold) {
                        addPoint(x, y, 1);
                    }
                } else {
                    addPoint(x, y, value);
                }
            }
        }
    }

    /**
     * Adds a single point to the hough transform. You can use this method directly
     * if your data isn't represented as a buffered image.
     */
    public void addPoint(int x, int y, float value) {

        // Go through each value of theta
        for (int t = 0; t < maxTheta; t++) {

            //Work out the r values for each theta step
            int r = (int) (((x - centerX) * cosCache[t]) + ((y - centerY) * sinCache[t]));

            // this copes with negative values of r
            r += houghHeight;

            if (r < 0 || r >= doubleHeight) continue;

            // Increment the hough array
            houghArray[t][r] += value;
        }

        numPoints++;
    }

    public Vector<HoughLine> getLines(int n) {
        return (getLines(n, 0));
    }

    public float[][] getHoughArray() {
        return houghArray;
    }

    /**
     * Once points have been added in some way this method extracts the lines and returns them as a Vector
     * of HoughLine objects, which can be used to draw on the
     *
     * @param threshold The percentage threshold above which lines are determined from the hough array
     */
    public Vector<HoughLine> getLines(int n, int threshold) {

        // Initialise the vector of lines that we'll return
        Vector<HoughLine> lines = new Vector<>(20);

        // Only proceed if the hough array is not empty
        if (numPoints == 0) return lines;

        // Search for local peaks above threshold to draw
        for (int t = 0; t < maxTheta; t++) {
            loop:
            for (int r = neighbourhoodSize; r < doubleHeight - neighbourhoodSize; r++) {

                // Only consider points above threshold
                if (houghArray[t][r] > threshold) {

                    float peak = houghArray[t][r];

                    // Check that this peak is indeed the local maxima
                    for (int dx = -neighbourhoodSize; dx <= neighbourhoodSize; dx++) {
                        for (int dy = -neighbourhoodSize; dy <= neighbourhoodSize; dy++) {
                            int dt = t + dx;
                            int dr = r + dy;
                            if (dt < 0) dt = dt + maxTheta;
                            else if (dt >= maxTheta) dt = dt - maxTheta;
                            if (houghArray[dt][dr] > peak) {
                                // found a bigger point nearby, skip
                                continue loop;
                            }
                        }
                    }

                    // calculate the true value of theta
                    double theta = t * thetaStep;

                    // add the line to the vector
                    lines.add(new HoughLine(theta, r, width, height, houghArray[t][r]));
                }
            }
        }
        lines.sort(Collections.reverseOrder());
        if (n >= 0 && lines.size() > n) {
            lines.setSize(n);
        }

        return lines;
    }

    /**
     * Represents a linear line as detected by the hough transform.
     * This line is represented by an angle theta and a radius from the centre.
     *
     * @author Olly Oechsle, University of Essex, Date: 13-Mar-2008
     * @version 1.0
     */
    public static class HoughLine extends Line2D.Float implements Comparable<HoughLine> {
        protected double theta;
        protected double r;
        protected float score;

        /**
         * Initialises the hough line
         */
        public HoughLine(double theta, double r, int width, int height, float score) {
            this.theta = theta;
            this.r = r;
            this.score = score;

            // During processing h_h is doubled so that -ve r values
            int houghHeight = (int) (Math.sqrt(2) * Math.max(height, width)) / 2;

            // Find edge points and vote in array
            float centerX = width / 2;
            float centerY = height / 2;

            // Draw edges in output array
            double tsin = Math.sin(theta);
            double tcos = Math.cos(theta);

            if (theta < Math.PI * 0.25 || theta > Math.PI * 0.75) {
                int x1 = 0, y1 = 0;
                int x2 = 0, y2 = height - 1;

                x1 = (int) ((((r - houghHeight) - ((y1 - centerY) * tsin)) / tcos) + centerX);
                x2 = (int) ((((r - houghHeight) - ((y2 - centerY) * tsin)) / tcos) + centerX);

                setLine(x1, y1, x2, y2);
            } else {
                int x1 = 0, y1 = 0;
                int x2 = width - 1, y2 = 0;

                y1 = (int) ((((r - houghHeight) - ((x1 - centerX) * tcos)) / tsin) + centerY);
                y2 = (int) ((((r - houghHeight) - ((x2 - centerX) * tcos)) / tsin) + centerY);

                setLine(x1, y1, x2, y2);
            }
        }

        public double getTheta() {
            return theta;
        }

        public double getR() {
            return r;
        }

        public float getScore() {
            return score;
        }


        public int compareTo(HoughLine o) {
            return java.lang.Double.compare(this.score, o.score);
        }
    }
}
