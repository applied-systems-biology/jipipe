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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.detect;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
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
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.Hough_Circle;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;


/**
 * Segments using a Hough circle transform
 */
@SetJIPipeDocumentation(name = "Detect circles 2D (Hough)", description = "Finds circular 2D objects via a Hough transform. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Detect", nodeTypeCategory = ImagesNodeTypeCategory.class)


@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Measurements", create = true)


public class CircularHoughSegmentation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int minRadius = 7;
    private int maxRadius = 25;
    private int radiusIncrement = 1;
    private int minNumCircles = 0;
    private int maxNumCircles = 700;
    private double threshold = 0.6;
    private int resolution = 113;
    private double ratio = 1.0;
    private int bandwidth = 10;
    private int localRadius = 10;
    private boolean local = false;

    /**
     * @param info algorithm info
     */
    public CircularHoughSegmentation2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public CircularHoughSegmentation2DAlgorithm(CircularHoughSegmentation2DAlgorithm other) {
        super(other);
        this.minRadius = other.minRadius;
        this.maxRadius = other.maxRadius;
        this.radiusIncrement = other.radiusIncrement;
        this.minNumCircles = other.minNumCircles;
        this.maxNumCircles = other.maxNumCircles;
        this.threshold = other.threshold;
        this.resolution = other.resolution;
        this.ratio = other.ratio;
        this.bandwidth = other.bandwidth;
        this.localRadius = other.localRadius;
        this.local = other.local;
    }

    private boolean outOfBounds(int width, int height, int y, int x) {
        if (x >= width) {
            return true;
        } else if (x <= 0) {
            return true;
        } else if (y >= height) {
            return true;
        } else {
            return y <= 0;
        }
    }

    private ImagePlus drawCircleMask(ImagePlus img, ResultsTable table) {

        int nCircles = (int) table.getValue("nCircles", 0);
        int width = img.getWidth();
        int height = img.getHeight();

        byte[] circleMaskPixels = new byte[width * height];
        for (int l = 0; l < nCircles; ++l) {
            int i = (int) table.getValue("X (" + img.getCalibration().getUnits() + ")", l);
            int j = (int) table.getValue("Y (" + img.getCalibration().getUnits() + ")", l);
            int radius = (int) table.getValue("Radius (" + img.getCalibration().getUnits() + ")", l);
            short ID = (short) (int) table.getValue("ID", l);
            float score = (float) (int) table.getValue("Score", l) / (float) this.resolution;
            int rSquared = radius * radius;

            for (int y = -1 * radius; y <= radius; ++y) {
                for (int x = -1 * radius; x <= radius; ++x) {
                    if (x * x + y * y <= rSquared) {
                        if (!this.outOfBounds(width, height, j + y, i + x)) {
                            circleMaskPixels[(j + y) * width + i + x] = (byte) 255;
                        }
                    }
                }
            }
        }

        ImagePlus result = IJ.createImage("HoughCircles", width, height, 1, 8);
        result.getProcessor().setPixels(circleMaskPixels);

        return result;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public int getParallelizationBatchSize() {
        // Hough does its own parallelization
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        ResultsTableData measurements = new ResultsTableData();

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            progressInfo.log("Slice " + index + "/" + img.getStackSize());
            ImagePlus slice = new ImagePlus("slice", imp);
            // Apply Hough circle transform
            Hough_Circle hough_circle = new Hough_Circle();
            hough_circle.setParameters(minRadius,
                    maxRadius,
                    radiusIncrement,
                    minNumCircles,
                    maxNumCircles,
                    threshold,
                    resolution,
                    ratio,
                    bandwidth,
                    localRadius,
                    true,
                    local,
                    false,
                    false,
                    true,
                    false,
                    true,
                    false);
            WindowManager.setTempCurrentImage(slice);
            hough_circle.startTransform();
            WindowManager.setTempCurrentImage(null);

            // Draw the circles
            ResultsTable resultsTable = Analyzer.getResultsTable();
            ImagePlus processedSlice = drawCircleMask(slice, resultsTable);
            stack.addSlice("slice" + index, processedSlice.getProcessor());
            measurements.addRows(new ResultsTableData(resultsTable));
        }, progressInfo);
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);

        iterationStep.addOutputData("Mask", new ImagePlusGreyscaleMaskData(result), progressInfo);
        iterationStep.addOutputData("Measurements", measurements, progressInfo);
    }

    @JIPipeParameter("min-radius")
    @SetJIPipeDocumentation(name = "Min radius", description = "Minimum radius to test")
    public int getMinRadius() {
        return minRadius;
    }

    @JIPipeParameter("min-radius")
    public void setMinRadius(int minRadius) {
        this.minRadius = minRadius;

    }

    @JIPipeParameter("max-radius")
    @SetJIPipeDocumentation(name = "Max radius", description = "Maximum radius to test")
    public int getMaxRadius() {
        return maxRadius;
    }

    @JIPipeParameter("max-radius")
    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;

    }

    @JIPipeParameter("radius-increment")
    @SetJIPipeDocumentation(name = "Radius increment", description = "By which value the radius should be increased")
    public int getRadiusIncrement() {
        return radiusIncrement;
    }

    @JIPipeParameter("radius-increment")
    public void setRadiusIncrement(int radiusIncrement) {
        this.radiusIncrement = radiusIncrement;

    }

    @JIPipeParameter("min-num-circles")
    @SetJIPipeDocumentation(name = "Min number of circles", description = "How many circles there should be at least")
    public int getMinNumCircles() {
        return minNumCircles;
    }

    @JIPipeParameter("min-num-circles")
    public void setMinNumCircles(int minNumCircles) {
        this.minNumCircles = minNumCircles;

    }

    @JIPipeParameter("max-num-circles")
    @SetJIPipeDocumentation(name = "Max number of circles", description = "How many circles there should be at most")
    public int getMaxNumCircles() {
        return maxNumCircles;
    }

    @JIPipeParameter("max-num-circles")
    public void setMaxNumCircles(int maxNumCircles) {
        this.maxNumCircles = maxNumCircles;

    }

    @JIPipeParameter("threshold")
    @SetJIPipeDocumentation(name = "Threshold")
    public double getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(double threshold) {
        this.threshold = threshold;

    }

    @JIPipeParameter("resolution")
    @SetJIPipeDocumentation(name = "Resolution")
    public int getResolution() {
        return resolution;
    }

    @JIPipeParameter("resolution")
    public void setResolution(int resolution) {
        this.resolution = resolution;

    }

    @JIPipeParameter("ratio")
    @SetJIPipeDocumentation(name = "Ratio")
    public double getRatio() {
        return ratio;
    }

    @JIPipeParameter("ratio")
    public void setRatio(double ratio) {
        this.ratio = ratio;

    }

    @JIPipeParameter("bandwidth")
    @SetJIPipeDocumentation(name = "Bandwidth")
    public int getBandwidth() {
        return bandwidth;
    }

    @JIPipeParameter("bandwidth")
    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;

    }

    @JIPipeParameter("local-radius")
    @SetJIPipeDocumentation(name = "Local radius")
    public int getLocalRadius() {
        return localRadius;
    }

    @JIPipeParameter("local-radius")
    public void setLocalRadius(int localRadius) {
        this.localRadius = localRadius;

    }

    @SetJIPipeDocumentation(name = "Local mode")
    @JIPipeParameter("local")
    public boolean isLocal() {
        return local;
    }

    @JIPipeParameter("local")
    public void setLocal(boolean local) {
        this.local = local;

    }
}