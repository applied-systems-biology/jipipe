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

import com.google.common.primitives.Doubles;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of Octave's Hough Line segments
 * Based on code by Hartmut Gimpel <hg_code@gmx.de>
 */
public class HoughLineSegments {

    public static class Line {
        private final Point point1;
        private final Point point2;
        private final double theta;
        private final double rho;

        public Line(Point point1, Point point2, double theta, double rho) {
            this.point1 = point1;
            this.point2 = point2;
            this.theta = theta;
            this.rho = rho;
        }

        public Point getPoint1() {
            return point1;
        }

        public Point getPoint2() {
            return point2;
        }

        public double getTheta() {
            return theta;
        }

        public double getRho() {
            return rho;
        }
    }

    public static List<Line> findLineSegments(ImageProcessor bw, int numPeaks, double peakThreshold, int[] neighborhoodSize, double fillGap, double minLength, List<Double> thetas) {
        HoughResult houghResult = hough(bw, thetas);
        List<Point> peaks = houghPeaks(houghResult.getH(), numPeaks, peakThreshold, neighborhoodSize);
        return houghLines(bw, houghResult.getThetas(), houghResult.getRhos(), peaks, fillGap, minLength);
    }

    public static List<Line> houghLines(ImageProcessor BW, List<Double> thetas, List<Double> rhos, List<Point> peaks, double fillGap, double minLength) {
        if (fillGap <= 0) {
            throw new IllegalArgumentException("fillGap must be a positive scalar number");
        }
        if (minLength <= 0) {
            throw new IllegalArgumentException("minlength must be a positive scalar number");
        }

        List<Line> lines = new ArrayList<>();

        // Process each given Hough peak individually
        final int width = BW.getWidth();
        final int height = BW.getHeight();
        for (Point peak : peaks) {
            int rho_p_idx = peak.y;
            int theta_p_idx = peak.x;
            double rho_p = rhos.get(rho_p_idx);
            double theta_p = thetas.get(theta_p_idx);

            List<Point> peakPixels = new ArrayList<>();
            double cosTheta = Math.cos(Math.toRadians(theta_p));
            double sinTheta = Math.sin(Math.toRadians(theta_p));

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (BW.getPixel(x, y) != 0) {
                        double rho_val = (x * cosTheta + y * sinTheta);
//                        if (Math.round(rho_val) == rho_p_idx) {
//                            peakPixels.add(new Point(x, y));
//                        }
                        if (Math.round(rho_val) == Math.round(rho_p)) {
                            peakPixels.add(new Point(x, y));
                        }
                    }
                }
            }

            if (peakPixels.isEmpty()) {
                continue;
            }

            // Order pixels
            if (Math.abs(cosTheta) > Math.abs(sinTheta)) {
                peakPixels.sort(Comparator.comparingInt(p -> p.x));
            } else {
                peakPixels.sort(Comparator.comparingInt(p -> p.y));
            }

            // Split line into segments based on fillGap
            List<Point> segment = new ArrayList<>();
            segment.add(peakPixels.get(0));

            for (int i = 1; i < peakPixels.size(); i++) {
                Point prev = peakPixels.get(i - 1);
                Point curr = peakPixels.get(i);

                if (Math.hypot(curr.x - prev.x, curr.y - prev.y) > fillGap) {
                    if (segment.size() > 1) {
                        addLineSegment(lines, segment, minLength, theta_p, rho_p);
                    }
                    segment = new ArrayList<>();
                }
                segment.add(curr);
            }
            if (segment.size() > 1) {
                addLineSegment(lines, segment, minLength, theta_p, rho_p);
            }
        }

        return lines;
    }

    private static void addLineSegment(List<Line> lines, List<Point> segment, double minLength, double theta, double rho) {
        Point first = segment.get(0);
        Point last = segment.get(segment.size() - 1);
        double length = Math.hypot(last.x - first.x, last.y - first.y);

        if (length >= minLength) {
            lines.add(new Line(first, last, theta, rho));
        }
    }


    /**
     * Implementation of Hough Peaks
     *
     * @param H                Hough Array (generated by the hough function)
     * @param numPeaks         number of peaks
     * @param threshold        the threshold (if less than 0, it's 0.5 * MAX(H))
     * @param neighborhoodSize neighborhood size. array with length 2. defaults to [height / 50, width / 50] if null. must be positive odd integers.
     * @return the peaks
     */
    public static List<Point> houghPeaks(FloatProcessor H, int numPeaks, double threshold, int[] neighborhoodSize) {
        List<Point> peaks = new ArrayList<>();

        int width = H.getWidth();
        int height = H.getHeight();

        // Set default parameters if they are not provided
        if (numPeaks <= 0) numPeaks = 1;
        if (threshold < 0) threshold = 0.5 * H.getStatistics().max;
        if (neighborhoodSize == null || neighborhoodSize.length != 2) {
            neighborhoodSize = new int[]{Math.max((width / 50) | 1, 3), Math.max((height / 50) | 1, 3)};
        }

        // Check if neighborhoodSize elements are positive odd integers
        for (int size : neighborhoodSize) {
            if (size <= 0 || size % 2 == 0) {
                throw new IllegalArgumentException("neighborhoodSize must be a 2-element vector of positive odd integers");
            }
        }

        int neighborhoodX = (neighborhoodSize[0] - 1) / 2;
        int neighborhoodY = (neighborhoodSize[1] - 1) / 2;

        for (int n = 0; n < numPeaks; n++) {
            // Find the peak value and its coordinates
            float maxVal = -Float.MAX_VALUE;
            Point maxCoord = new Point();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float val = H.getPixelValue(x, y);
                    if (val > maxVal) {
                        maxVal = val;
                        maxCoord.x = x;
                        maxCoord.y = y;
                    }
                }
            }

            // If peak value is below threshold, stop the search
            if (maxVal < threshold) {
                break;
            }

            peaks.add(maxCoord);

            // Suppress the neighborhood
            for (int dy = -neighborhoodY; dy <= neighborhoodY; dy++) {
                for (int dx = -neighborhoodX; dx <= neighborhoodX; dx++) {
                    int nx = maxCoord.x + dx;
                    int ny = maxCoord.y + dy;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        H.putPixelValue(nx, ny, 0);
                    }
                }
            }

            // Use anti-symmetry in theta direction
            if (maxCoord.y + neighborhoodY >= height || maxCoord.y - neighborhoodY < 0) {
                for (int dy = -neighborhoodY; dy <= neighborhoodY; dy++) {
                    int ny = maxCoord.y + dy;
                    if (ny >= height) ny -= height;
                    if (ny < 0) ny += height;
                    for (int dx = -neighborhoodX; dx <= neighborhoodX; dx++) {
                        int nx = maxCoord.x + dx;
                        if (nx >= 0 && nx < width) {
                            H.putPixelValue(nx, ny, 0);
                        }
                    }
                }
            }
        }

        return peaks;
    }

    public static HoughResult hough(ImageProcessor bw, List<Double> thetas) {

        int width = bw.getWidth();
        int height = bw.getHeight();

        // Calculate the diagonal distance of the image
        final double diagonal = Math.hypot(width, height);
        final int rhoMax = (int) Math.ceil(diagonal);
        final int rhoRange = 2 * rhoMax + 1;

        // Create Hough accumulator array
        FloatProcessor H = new FloatProcessor(thetas.size(), rhoRange);

        // Perform Hough Transform
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bw.getPixel(x, y) != 0) {
                    for (int i = 0; i < thetas.size(); i++) {
                        double theta = thetas.get(i);
                        double thetaRad = Math.toRadians(theta);
                        double rho = x * Math.cos(thetaRad) + y * Math.sin(thetaRad);
                        int rhoIdx = (int) Math.round(rho + rhoMax);
                        H.setf(i, rhoIdx, H.getf(i, rhoIdx) + 1);
                    }
                }
            }
        }

        return new HoughResult(H, thetas, rhoMax);
    }

    private static List<Double> getThetas(double thetaRes) {
        List<Double> thetaList = new ArrayList<>();
        for (double t = -90; t < 90; t += thetaRes) {
            thetaList.add(t);
        }
        return thetaList;
    }

    public static class HoughResult {
        private final FloatProcessor H;
        private final List<Double> thetas;
        private final int rhoMax;

        public HoughResult(FloatProcessor h, List<Double> thetas, int rhoMax) {
            H = h;
            this.thetas = thetas;
            this.rhoMax = rhoMax;
        }

        public FloatProcessor getH() {
            return H;
        }

        public List<Double> getThetas() {
            return thetas;
        }

        public int getRhoMax() {
            return rhoMax;
        }

        public List<Double> getRhos() {
            double[] rho = new double[H.getHeight()];
            for (int i = 0; i < H.getHeight(); i++) {
                rho[i] = i - rhoMax;
            }
            return Doubles.asList(rho);
        }
    }


}
