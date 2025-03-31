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

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.spanning.PrimMinimumSpanningTree;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Implementation of Octave's Hough Line segments
 * Based on code by Hartmut Gimpel (hg_code@gmx.de)
 */
public class HoughLineSegments {

    public static List<Line> houghLineSegments(ImageProcessor BW, List<Double> thetas, List<Double> rhos, List<Point> peaks, double fillGap, double minLength, boolean fastMode, JIPipeProgressInfo progressInfo) {
        if (fillGap <= 0) {
            throw new IllegalArgumentException("fillGap must be a positive scalar number");
        }
        if (minLength <= 0) {
            throw new IllegalArgumentException("minlength must be a positive scalar number");
        }

        progressInfo.log("Finding segments, given " + peaks.size() + " peaks");

        List<Line> lines = new ArrayList<>();

        // Process each given Hough peak individually
        final int width = BW.getWidth();
        final int height = BW.getHeight();
        for (int j = 0; j < peaks.size(); j++) {
            JIPipeProgressInfo peakProgress = progressInfo.resolve("Peak", j, peaks.size());
            Point peak = peaks.get(j);
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
                peakProgress.log("Peak has no associated pixels!");
                continue;
            }

            if (fastMode) {
                extractLineSegmentsFast(fillGap, minLength, cosTheta, sinTheta, peakPixels, lines, theta_p, rho_p, peakProgress);
            } else {
                extractLineSegmentsPrecise(fillGap, minLength, peakPixels, lines, theta_p, rho_p, peakProgress);
            }
        }

        return lines;
    }

    public static List<Line> houghGlobalLines(ImageProcessor BW, List<Double> thetas, List<Double> rhos, List<Point> peaks, double minLength, Dimension imageSize, JIPipeProgressInfo progressInfo) {
        if (minLength <= 0) {
            throw new IllegalArgumentException("minlength must be a positive scalar number");
        }

        progressInfo.log("Finding segments, given " + peaks.size() + " peaks");

        List<Line> lines = new ArrayList<>();

        // Process each given Hough peak individually
        final int width = BW.getWidth();
        final int height = BW.getHeight();
        for (int j = 0; j < peaks.size(); j++) {
            JIPipeProgressInfo peakProgress = progressInfo.resolve("Peak", j, peaks.size());
            Point peak = peaks.get(j);
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
                        if (Math.round(rho_val) == Math.round(rho_p)) {
                            peakPixels.add(new Point(x, y));
                        }
                    }
                }
            }

            if (peakPixels.isEmpty()) {
                peakProgress.log("Peak has no associated pixels!");
                continue;
            }

            // Extract the line
            if (peakPixels.size() <= 1) {
                peakProgress.log("Not enough points");
            } else if (peakPixels.size() == 2) {
                // Trivial line
                peakProgress.log("Is trivial line");
                addLineSegment(lines, peakPixels, minLength, theta_p, rho_p, imageSize, true, peakProgress);
            } else {
                peakProgress.log("Has " + peakPixels.size() + " points");
                // Calculate variance in x values to detect vertical lines
                extractLineSegmentsWithRegression(minLength, lines, theta_p, rho_p, peakPixels, imageSize, true, peakProgress);
            }
        }

        return lines;
    }

    private static void extractLineSegmentsPrecise(double fillGap, double minLength, List<Point> peakPixels, List<Line> lines, double theta_p, double rho_p, JIPipeProgressInfo peakProgress) {
        peakProgress.log("Building graph (" + peakPixels.size() + " vertices, " + (peakPixels.size() * peakPixels.size() - peakPixels.size()) + " edges)");
        DefaultUndirectedWeightedGraph<Point, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (Point peakPixel : peakPixels) {
            graph.addVertex(peakPixel);
        }
        for (int i = 0; i < peakPixels.size(); i++) {
            Point p1 = peakPixels.get(i);
            for (int j = i + 1; j < peakPixels.size(); j++) {
                Point p2 = peakPixels.get(j);
                if (i != j) {
                    DefaultWeightedEdge edge = graph.addEdge(p1, p2);
                    graph.setEdgeWeight(edge, p1.distance(p2));
                } else {
                    assert false;
                }
            }
        }

        peakProgress.log("Calculating MST");
        PrimMinimumSpanningTree<Point, DefaultWeightedEdge> minimumSpanningTree = new PrimMinimumSpanningTree<>(graph);
        SpanningTreeAlgorithm.SpanningTree<DefaultWeightedEdge> spanningTree = minimumSpanningTree.getSpanningTree();

        peakProgress.log("Filter edges");
        for (DefaultWeightedEdge edge : ImmutableList.copyOf(graph.edgeSet())) {
            if (graph.getEdgeWeight(edge) > fillGap || !spanningTree.getEdges().contains(edge)) {
                graph.removeEdge(edge);
            }
        }

        peakProgress.log("Finding components");
        ConnectivityInspector<Point, DefaultWeightedEdge> connectivityInspector = new ConnectivityInspector<>(graph);
        List<Set<Point>> connectedSets = connectivityInspector.connectedSets();
        for (int i = 0; i < connectedSets.size(); i++) {
            List<Point> points = new ArrayList<>(connectedSets.get(i));
            if (points.size() <= 1) {
                peakProgress.log("Component " + i + " not enough vertices");
            } else if (points.size() == 2) {
                // Trivial line
                peakProgress.log("Component " + i + " is trivial line");
                addLineSegment(lines, points, minLength, theta_p, rho_p, null, false, peakProgress);
            } else {
                peakProgress.log("Component " + i + " has " + points.size() + " vertices");
                // Calculate variance in x values to detect vertical lines
                extractLineSegmentsWithRegression(minLength, lines, theta_p, rho_p, points, null, false, peakProgress);
            }
        }

    }

    private static void extractLineSegmentsWithRegression(double minLength, List<Line> lines, double theta_p, double rho_p, List<Point> points, Dimension imageSize, boolean expandToImageSize, JIPipeProgressInfo peakProgress) {
        Variance variance = new Variance();
        double[] xValues = points.stream().mapToDouble(Point::getX).toArray();
        double xVariance = variance.evaluate(xValues);

        if (xVariance < 1e-10) { // Adjust threshold as needed
            // Handle vertical line case
            int x = points.get(0).x;
            int minY = points.stream().mapToInt(p -> p.y).min().getAsInt();
            int maxY = points.stream().mapToInt(p -> p.y).max().getAsInt();

            Point startPoint = new Point(x, minY);
            Point endPoint = new Point(x, maxY);

            addLineSegment(lines, Arrays.asList(startPoint, endPoint), minLength, theta_p, rho_p, imageSize, expandToImageSize, peakProgress);

        } else {
            // Handle general case using linear regression
            SimpleRegression regression = new SimpleRegression();
            for (Point point : points) {
                regression.addData(point.getX(), point.getY());
            }

            double slope = regression.getSlope();
            double intercept = regression.getIntercept();

            int minX = points.stream().mapToInt(p -> p.x).min().getAsInt();
            int maxX = points.stream().mapToInt(p -> p.x).max().getAsInt();

            Point startPoint = new Point(minX, (int) Math.round(slope * minX + intercept));
            Point endPoint = new Point(maxX, (int) Math.round(slope * maxX + intercept));

            addLineSegment(lines, Arrays.asList(startPoint, endPoint), minLength, theta_p, rho_p, imageSize, expandToImageSize, peakProgress);
        }
    }

    private static void extractLineSegmentsFast(double fillGap, double minLength, double cosTheta, double sinTheta, List<Point> peakPixels, List<Line> lines, double theta_p, double rho_p, JIPipeProgressInfo peakProgress) {
        // Order pixels
        if (Math.abs(cosTheta) > Math.abs(sinTheta)) {
            // Line is more horizontal
            peakPixels.sort(Comparator.comparingInt((Point p) -> p.x).thenComparingInt((Point p) -> p.y));
        } else {
            // Line is more vertical
            peakPixels.sort(Comparator.comparingInt((Point p) -> p.y).thenComparingInt((Point p) -> p.x));
        }

        // Split line into segments based on fillGap
        List<Point> segment = new ArrayList<>();
        segment.add(peakPixels.get(0));

        for (int i = 1; i < peakPixels.size(); i++) {
            Point prev = peakPixels.get(i - 1);
            Point curr = peakPixels.get(i);
            double distance = prev.distance(curr);

            if (distance > fillGap) {
                if (segment.size() > 1) {
                    peakProgress.log("Distance threshold reached (" + distance + " > " + fillGap + "). Adding new line segment (" + segment.size() + " pixels)");
                    addLineSegment(lines, segment, minLength, theta_p, rho_p, null, false, peakProgress);
                } else {
                    peakProgress.log("Distance threshold reached (" + distance + " > " + fillGap + "). Not enough pixels!");
                }
                segment.clear();
            }
            segment.add(curr);
        }
        if (segment.size() > 1) {
            addLineSegment(lines, segment, minLength, theta_p, rho_p, null, false, peakProgress);
        }
    }

    private static void addLineSegment(List<Line> lines, List<Point> segment, double minLength, double theta, double rho, Dimension imageSize, boolean expandToImageSize, JIPipeProgressInfo peakProgress) {
        Point first = segment.get(0);
        Point last = segment.get(segment.size() - 1);
        double length = first.distance(last);

        if (length >= minLength) {
            peakProgress.log("Successfully detected line segment " + first + " -> " + last + " with length=" + length);

            // Expand if needed
            if (imageSize != null && expandToImageSize) {
                extendLine(first, last, imageSize);
            }

            lines.add(new Line(first, last, theta, rho));
        } else {
            peakProgress.log("Rejected line segment " + first + " -> " + last + " with length=" + length + " < " + minLength);
        }
    }

    public static void extendLine(Point first, Point last, Dimension imageSize) {
        int xmin = 0;
        int ymin = 0;
        int xmax = imageSize.width;
        int ymax = imageSize.height;

        int x1 = first.x;
        int y1 = first.y;
        int x2 = last.x;
        int y2 = last.y;


        Point newFirst = null;
        Point newLast = null;

        if (x1 == x2) { // Vertical line
            newFirst = new Point(x1, ymin);
            newLast = new Point(x1, ymax);
        } else if (y1 == y2) { // Horizontal line
            newFirst = new Point(xmin, y1);
            newLast = new Point(xmax, y1);
        } else {
            double slope = (double) (y2 - y1) / (x2 - x1);
            double intercept = y1 - slope * x1;

            // Intersection with left boundary (x = 0)
            int yAtLeft = (int) intercept;
            if (yAtLeft >= ymin && yAtLeft <= ymax) {
                if (newFirst == null) {
                    newFirst = new Point(xmin, yAtLeft);
                } else {
                    newLast = new Point(xmin, yAtLeft);
                }
            }

            // Intersection with right boundary (x = xmax)
            int yAtRight = (int) (slope * xmax + intercept);
            if (yAtRight >= ymin && yAtRight <= ymax) {
                if (newFirst == null) {
                    newFirst = new Point(xmax, yAtRight);
                } else {
                    newLast = new Point(xmax, yAtRight);
                }
            }

            // Intersection with top boundary (y = 0)
            int xAtTop = (int) (-intercept / slope);
            if (xAtTop >= xmin && xAtTop <= xmax) {
                if (newFirst == null) {
                    newFirst = new Point(xAtTop, ymin);
                } else {
                    newLast = new Point(xAtTop, ymin);
                }
            }

            // Intersection with bottom boundary (y = ymax)
            int xAtBottom = (int) ((ymax - intercept) / slope);
            if (xAtBottom >= xmin && xAtBottom <= xmax) {
                if (newFirst == null) {
                    newFirst = new Point(xAtBottom, ymax);
                } else {
                    newLast = new Point(xAtBottom, ymax);
                }
            }
        }

        // If only one intersection point was found, the other point should be at the original point.
        if (newFirst == null) {
            newFirst = new Point(x1, y1);
        }
        if (newLast == null) {
            newLast = new Point(x2, y2);
        }

        first.setLocation(newFirst);
        last.setLocation(newLast);
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
    public static List<Point> houghPeaks(FloatProcessor H, int numPeaks, double threshold, int[] neighborhoodSize, JIPipeProgressInfo progressInfo) {
        List<Point> peaks = new ArrayList<>();

        int width = H.getWidth();
        int height = H.getHeight();

        // Set default parameters if they are not provided
        if (numPeaks <= 0) {
            numPeaks = 1;
        }
        if (threshold < 0) {
            threshold = 0.5 * H.getStats().max;
        }
        if (neighborhoodSize == null || neighborhoodSize.length != 2) {
            neighborhoodSize = new int[]{Math.max((width / 50) | 1, 3), Math.max((height / 50) | 1, 3)};
        }

        progressInfo.log("Neighborhood size is " + Arrays.toString(neighborhoodSize));

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
