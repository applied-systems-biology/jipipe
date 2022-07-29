package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Vector;

/**
 * Adapted from https://github.com/davidchatting/hough_lines/blob/master/HoughTransform.java
 * @author David Chatting - 4th March 2013
 */
public class HoughLines {
    // The size of the neighbourhood in which to search for other local maxima
    final int neighbourhoodSize = 4;

    // How many discrete values of theta shall we check?
    final int maxTheta = 180;

    // Using maxTheta, work out the step
    final double thetaStep = Math.PI / maxTheta;

    // the width and height of the image
    protected int width, height;

    // the hough array
    protected int[][] houghArray;

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

    public HoughLines(BufferedImage image) {
        initialise(image.getWidth(), image.getHeight());
        addPoints(image);
    }

    /**
     * Initialises the hough transform. The dimensions of the input image are needed
     * in order to initialise the hough array.
     *
     * @param width  The width of the input image
     * @param height The height of the input image
     */
    public HoughLines(int width, int height) {
        initialise(width, height);
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
        houghArray = new int[maxTheta][doubleHeight];

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
    public void addPoints(BufferedImage image) {

        // Now find edge points and update the hough array
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                // Find non-black pixels
                if ((image.getRGB(x, y) & 0x000000ff) != 0) {
                    addPoint(x, y);
                }
            }
        }
    }

    /**
     * Adds a single point to the hough transform. You can use this method directly
     * if your data isn't represented as a buffered image.
     */
    public void addPoint(int x, int y) {

        // Go through each value of theta
        for (int t = 0; t < maxTheta; t++) {

            //Work out the r values for each theta step
            int r = (int) (((x - centerX) * cosCache[t]) + ((y - centerY) * sinCache[t]));

            // this copes with negative values of r
            r += houghHeight;

            if (r < 0 || r >= doubleHeight) continue;

            // Increment the hough array
            houghArray[t][r]++;
        }

        numPoints++;
    }

    public Vector<HoughLine> getLines(int n) {
        return(getLines(n, 0));
    }

    public int[][] getHoughArray() {
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

                    int peak = houghArray[t][r];

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
        if(n >= 0 && lines.size() > n) {
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
        protected int score;

        /**
         * Initialises the hough line
         */
        public HoughLine(double theta, double r, int width, int height, int score) {
            this.theta = theta;
            this.r = r;
            this.score=score;

            // During processing h_h is doubled so that -ve r values
            int houghHeight = (int) (Math.sqrt(2) * Math.max(height, width)) / 2;

            // Find edge points and vote in array
            float centerX = width / 2;
            float centerY = height / 2;

            // Draw edges in output array
            double tsin = Math.sin(theta);
            double tcos = Math.cos(theta);

            if (theta < Math.PI * 0.25 || theta > Math.PI * 0.75) {
                int x1=0, y1=0;
                int x2=0, y2=height-1;

                x1=(int) ((((r - houghHeight) - ((y1 - centerY) * tsin)) / tcos) + centerX);
                x2=(int) ((((r - houghHeight) - ((y2 - centerY) * tsin)) / tcos) + centerX);

                setLine(x1, y1, x2, y2);
            }
            else {
                int x1=0, y1=0;
                int x2=width-1, y2=0;

                y1=(int) ((((r - houghHeight) - ((x1 - centerX) * tcos)) / tsin) + centerY);
                y2=(int) ((((r - houghHeight) - ((x2 - centerX) * tcos)) / tsin) + centerY);

                setLine(x1, y1, x2, y2);
            }
        }

        public double getTheta() {
            return theta;
        }

        public double getR() {
            return r;
        }

        public int getScore() {
            return score;
        }


        public int compareTo(HoughLine o) {
            return(this.score-o.score);
        }
    }
}
