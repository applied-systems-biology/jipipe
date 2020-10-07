/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.Hough_Circle;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segments using a Hough circle transform
 */
@JIPipeDocumentation(name = "Hough segmentation 2D", description = "Finds circular 2D objects via a Hough transform. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)


@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements")




public class HoughSegmentation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

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
    public HoughSegmentation2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Mask", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .addOutputSlot("Measurements", ResultsTableData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public HoughSegmentation2DAlgorithm(HoughSegmentation2DAlgorithm other) {
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        ResultsTableData measurements = new ResultsTableData();

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            algorithmProgress.accept(subProgress.resolve("Slice " + index + "/" + img.getStackSize()));
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
            measurements.mergeWith(new ResultsTableData(resultsTable));
        });
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataBatch.addOutputData("Mask", new ImagePlusGreyscaleMaskData(result));
        dataBatch.addOutputData("Measurements", measurements);
    }

    @JIPipeParameter("min-radius")
    @JIPipeDocumentation(name = "Min radius", description = "Minimum radius to test")
    public int getMinRadius() {
        return minRadius;
    }

    @JIPipeParameter("min-radius")
    public void setMinRadius(int minRadius) {
        this.minRadius = minRadius;

    }

    @JIPipeParameter("max-radius")
    @JIPipeDocumentation(name = "Max radius", description = "Maximum radius to test")
    public int getMaxRadius() {
        return maxRadius;
    }

    @JIPipeParameter("max-radius")
    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;

    }

    @JIPipeParameter("radius-increment")
    @JIPipeDocumentation(name = "Radius increment", description = "By which value the radius should be increased")
    public int getRadiusIncrement() {
        return radiusIncrement;
    }

    @JIPipeParameter("radius-increment")
    public void setRadiusIncrement(int radiusIncrement) {
        this.radiusIncrement = radiusIncrement;

    }

    @JIPipeParameter("min-num-circles")
    @JIPipeDocumentation(name = "Min number of circles", description = "How many circles there should be at least")
    public int getMinNumCircles() {
        return minNumCircles;
    }

    @JIPipeParameter("min-num-circles")
    public void setMinNumCircles(int minNumCircles) {
        this.minNumCircles = minNumCircles;

    }

    @JIPipeParameter("max-num-circles")
    @JIPipeDocumentation(name = "Max number of circles", description = "How many circles there should be at most")
    public int getMaxNumCircles() {
        return maxNumCircles;
    }

    @JIPipeParameter("max-num-circles")
    public void setMaxNumCircles(int maxNumCircles) {
        this.maxNumCircles = maxNumCircles;

    }

    @JIPipeParameter("threshold")
    @JIPipeDocumentation(name = "Threshold")
    public double getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(double threshold) {
        this.threshold = threshold;

    }

    @JIPipeParameter("resolution")
    @JIPipeDocumentation(name = "Resolution")
    public int getResolution() {
        return resolution;
    }

    @JIPipeParameter("resolution")
    public void setResolution(int resolution) {
        this.resolution = resolution;

    }

    @JIPipeParameter("ratio")
    @JIPipeDocumentation(name = "Ratio")
    public double getRatio() {
        return ratio;
    }

    @JIPipeParameter("ratio")
    public void setRatio(double ratio) {
        this.ratio = ratio;

    }

    @JIPipeParameter("bandwidth")
    @JIPipeDocumentation(name = "Bandwidth")
    public int getBandwidth() {
        return bandwidth;
    }

    @JIPipeParameter("bandwidth")
    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;

    }

    @JIPipeParameter("local-radius")
    @JIPipeDocumentation(name = "Local radius")
    public int getLocalRadius() {
        return localRadius;
    }

    @JIPipeParameter("local-radius")
    public void setLocalRadius(int localRadius) {
        this.localRadius = localRadius;

    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {

    }

    @JIPipeDocumentation(name = "Local mode")
    @JIPipeParameter("local")
    public boolean isLocal() {
        return local;
    }

    @JIPipeParameter("local")
    public void setLocal(boolean local) {
        this.local = local;

    }
}