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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.ZProjector;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;


/**
 * Segments using a Hough circle transform
 * This code is based on <a href="https://github.com/closms/detectcircles">...</a>
 */
@SetJIPipeDocumentation(name = "Circular Hough segmentation 2D (fast)", description = "Finds circular 2D objects via a Hough transform. This implementation is based on code by Michael Closson and is generally faster than the other Hough-based segmentation. " +
        "It outputs the segmented mask, the maximum Hough accumulator image, and a table of all detected circles (x, y, Diameter, and Score)." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeCitation("https://github.com/closms/detectcircles")
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Accumulator", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Measurements", create = true)
public class FastCircularHoughSegmentation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final int minEdgeVal = 1;
    private int minRadius = 50;
    private int maxRadius = 150;
    private double minScore = 200;

    /**
     * @param info algorithm info
     */
    public FastCircularHoughSegmentation2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FastCircularHoughSegmentation2DAlgorithm(FastCircularHoughSegmentation2DAlgorithm other) {
        super(other);
        this.minRadius = other.minRadius;
        this.maxRadius = other.maxRadius;
        this.minScore = other.minScore;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    private void addHoughOutput(ImageStack imageStack, double[][] z, double maxval) {
        int W = z.length;
        int H = z[0].length;

        byte[] Y = new byte[W * H];
        for (int h = 0; h < H; h++) {
            for (int w = 0; w < W; w++) {
                Y[h * W + w] = (byte) ((z[w][h] / maxval) * 255.0);
            }
        }
        ImageProcessor IP = new ByteProcessor(W, H, Y);
        imageStack.addSlice(IP);
    }

    private void drawCircle(double[][] img, int cx, int cy, int R, double val) {
        /* For a typical image, this method will be called 13 million times. */

        int W = img.length;
        int H = img[0].length;

        int f = 1 - R;
        int ddF_x = 1;
        int ddF_y = -2 * R;
        int x = 0;
        int y = R;

        if (cy + R < H) {
            img[cx][cy + R] += val;
        }
        if (cy - R >= 0) {
            img[cx][cy - R] += val;
        }
        if (cx + R < W) {
            img[cx + R][cy] += val;
        }
        if (cx - R >= 0) {
            img[cx - R][cy] += val;
        }

        while (x < y) {
            if (f >= 0) {
                y--;
                ddF_y += 2;
                f += ddF_y;
            }
            x++;
            ddF_x += 2;
            f += ddF_x;

            if (cx + x < W && cy + y < H) {
                img[cx + x][cy + y] += val;
            }
            if (cx - x >= 0 && cy + y < H) {
                img[cx - x][cy + y] += val;
            }
            if (cx + x < W && cy - y >= 0) {
                img[cx + x][cy - y] += val;
            }
            if (cx - x >= 0 && cy - y >= 0) {
                img[cx - x][cy - y] += val;
            }
            if (cx + y < W && cy + x < H) {
                img[cx + y][cy + x] += val;
            }
            if (cx - y >= 0 && cy + x < H) {
                img[cx - y][cy + x] += val;
            }
            if (cx + y < W && cy - x >= 0) {
                img[cx + y][cy - x] += val;
            }
            if (cx - y >= 0 && cy - x >= 0) {
                img[cx - y][cy - x] += val;
            }

        }
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack maskStack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        ImageStack houghStack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        ResultsTableData measurements = new ResultsTableData();

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            progressInfo.log("Slice " + index + "/" + img.getStackSize());
            applyHough(imp, maskStack, houghStack, measurements, progressInfo);
        }, progressInfo);

        ImagePlus mask = new ImagePlus("Segmented Image", maskStack);
        mask.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        ImagePlus hough = new ImagePlus("Maximum accumulator", houghStack);
        hough.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

//        DisplayRangeCalibrationAlgorithm.calibrate(mask, CalibrationMode.AutomaticImageJ, 0,0);
//        DisplayRangeCalibrationAlgorithm.calibrate(hough, CalibrationMode.AutomaticImageJ, 0,0);
        mask.copyScale(img);
        hough.copyScale(img);

        iterationStep.addOutputData("Mask", new ImagePlusGreyscaleMaskData(mask), progressInfo);
        iterationStep.addOutputData("Accumulator", new ImagePlusGreyscaleData(hough), progressInfo);
        iterationStep.addOutputData("Measurements", measurements, progressInfo);
    }

    private void applyHough(ImageProcessor imp, ImageStack maskStack, ImageStack houghStack, ResultsTableData measurements, JIPipeProgressInfo progressInfo) {
        final int W = imp.getWidth();
        final int H = imp.getHeight();
        ResultsTable rt = new ResultsTable();
        imp.setOverlay(null);

        ImageProcessor nip = new ByteProcessor(imp, false);
        nip.findEdges();

        List<double[][]> allAccumulators = new ArrayList<>();
        double[][] scores = new double[imp.getWidth()][imp.getHeight()];
        int[][] radii = new int[imp.getWidth()][imp.getHeight()];
        double maxHoughScore = 0;

        double[][] Z;

        for (int R = minRadius; R <= maxRadius; R++) {
            progressInfo.log("R=" + R);
            Z = new double[imp.getWidth()][imp.getHeight()];

            /* traverse the image and update the accumulator. */
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    double I = nip.getPixel(x, y);

                    if (I > minEdgeVal) {
                        /* This pixel is possibly on a circle.  Update the accumulator */
                        /* Compute the score by dividing the value in the accumulator
                         * by the circumference.  This way, the scores for circles of
                         * different radiuses are comparable.
                         */
                        I /= (Math.PI * 2 * R);
                        this.drawCircle(Z, x, y, R, I);
                    }
                }
            }

            /* go through the best score list and update it */
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    double S = Z[x][y];
                    if (S > minScore && S > scores[x][y]) {
                        scores[x][y] = S;
                        radii[x][y] = R;
                    }
                }
            }
            allAccumulators.add(Z);
        }

        /* Go through the scores, for each hit, delete the other hits
         * that are within its radius, that scored less.
         */
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {

                if (scores[x][y] > 0) {
                    int R = radii[x][y];

                    for (int j = Math.max(0, y - R + 1); j < Math.min(H, y + R); j++) {
                        for (int i = Math.max(0, x - R + 1); i < Math.min(W, x + R); i++) {
                            if (i == x && j == y) {
                                /* don't compare scores with yourself. */
                                continue;
                            }
                            if (scores[i][j] <= scores[x][y]) {
                                /* this point's score is less, delete him. */
                                scores[i][j] = 0;
                                radii[i][j] = 0;
                            }
                        }
                    }
                }
            }
        }

        /* draw the circles we found.  Using the best score list. */
        ROIListData rois = new ROIListData();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (scores[x][y] > 0) {
                    int R = radii[x][y];
                    double S = scores[x][y];
                    if (IJ.debugMode)
                        IJ.log("HIT. Z[" + x + "][" + y + "] = " + scores[x][y] + " -> (" + x + ", " + y + ")@" + R);

                    //The Roi unit is always pixels.
                    Roi r = new OvalRoi(x - R, y - R, 2 * R, 2 * R);
                    // Set a magic string in the name, so the circle toggle
                    // tool can distinguish between ROIs that were detected
                    // by this code from all other ROIs.
                    r.setName("DetectCircles:" + x + ":" + y + ":" + R + ":" + S);
                    rois.add(r);

                    //The unit of the result table is the image scale unit.
                    rt.incrementCounter();
                    rt.addValue("x", x);
                    rt.addValue("y", y);
                    rt.addValue("Diameter", ((double) 2 * R));
                    rt.addValue("Score", scores[x][y]);
                }
            }
        }

        /*
        Generate Hough output
         */
        maxHoughScore = 0;
        for (double[][] a : allAccumulators) {
            for (int h = 0; h < H; h++) {
                for (int w = 0; w < W; w++) {
                    if (a[w][h] > maxHoughScore) {
                        maxHoughScore = a[w][h];
                    }
                }
            }
        }
        /* there is a different accumulator for each radius.
         * add them all to an ImageStack */
        ImageStack allAccumulatorsStack = new ImageStack(W, H, houghStack.getColorModel());
        for (double[][] a : allAccumulators) {
            addHoughOutput(allAccumulatorsStack, a, maxHoughScore);
        }

        // Create a maximum projection
        ImagePlus allAccumulatorsImage = new ImagePlus("Accumulator", allAccumulatorsStack);
        ImagePlus maxHough = ZProjector.run(allAccumulatorsImage, "MaxIntensity");

        /*
        Add to the results
         */
        Margin imageMargin = new Margin();
        imageMargin.setAnchor(Anchor.TopLeft);
        imageMargin.setLeft(new NumericFunctionExpression("0"));
        imageMargin.setTop(new NumericFunctionExpression("0"));
        imageMargin.setWidth(new NumericFunctionExpression(imp.getWidth() + ""));
        imageMargin.setHeight(new NumericFunctionExpression(imp.getHeight() + ""));
        ImagePlus mask = IJ.createImage("Mask", "8-bit black", W, H, 1);
        rois.drawMask(false, true, 1, mask);
//        mask.show();
//        maxHough.show();

        maskStack.addSlice(mask.getProcessor());
        houghStack.addSlice(maxHough.getProcessor());
        measurements.addRows(new ResultsTableData(rt));
    }

    @SetJIPipeDocumentation(name = "Min radius", description = "Minimum radius of circles in pixels")
    @JIPipeParameter("min-radius")
    public int getMinRadius() {
        return minRadius;
    }

    @JIPipeParameter("min-radius")
    public void setMinRadius(int minRadius) {
        this.minRadius = minRadius;
    }

    @SetJIPipeDocumentation(name = "Max radius", description = "Maximum radius of circles in pixels")
    @JIPipeParameter("max-radius")
    public int getMaxRadius() {
        return maxRadius;
    }

    @JIPipeParameter("max-radius")
    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
    }

    @SetJIPipeDocumentation(name = "Min score", description = "Minimum score for a circle to be detected")
    @JIPipeParameter("min-score")
    public double getMinScore() {
        return minScore;
    }

    @JIPipeParameter("min-score")
    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }
}