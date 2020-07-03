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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.Hough_Circle;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segments using a Hough circle transform
 */
@ACAQDocumentation(name = "Hough segmentation 2D", description = "Finds circular 2D objects via a Hough transform. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Threshold", algorithmCategory = ACAQAlgorithmCategory.Processor)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")

// Trait matching

// Trait configuration
public class HoughSegmentation2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

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
     * @param declaration algorithm declaration
     */
    public HoughSegmentation2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());

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
        });
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result));
    }

    @ACAQParameter("min-radius")
    @ACAQDocumentation(name = "Min radius", description = "Minimum radius to test")
    public int getMinRadius() {
        return minRadius;
    }

    @ACAQParameter("min-radius")
    public void setMinRadius(int minRadius) {
        this.minRadius = minRadius;

    }

    @ACAQParameter("max-radius")
    @ACAQDocumentation(name = "Max radius", description = "Maximum radius to test")
    public int getMaxRadius() {
        return maxRadius;
    }

    @ACAQParameter("max-radius")
    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;

    }

    @ACAQParameter("radius-increment")
    @ACAQDocumentation(name = "Radius increment", description = "By which value the radius should be increased")
    public int getRadiusIncrement() {
        return radiusIncrement;
    }

    @ACAQParameter("radius-increment")
    public void setRadiusIncrement(int radiusIncrement) {
        this.radiusIncrement = radiusIncrement;

    }

    @ACAQParameter("min-num-circles")
    @ACAQDocumentation(name = "Min number of circles", description = "How many circles there should be at least")
    public int getMinNumCircles() {
        return minNumCircles;
    }

    @ACAQParameter("min-num-circles")
    public void setMinNumCircles(int minNumCircles) {
        this.minNumCircles = minNumCircles;

    }

    @ACAQParameter("max-num-circles")
    @ACAQDocumentation(name = "Max number of circles", description = "How many circles there should be at most")
    public int getMaxNumCircles() {
        return maxNumCircles;
    }

    @ACAQParameter("max-num-circles")
    public void setMaxNumCircles(int maxNumCircles) {
        this.maxNumCircles = maxNumCircles;

    }

    @ACAQParameter("threshold")
    @ACAQDocumentation(name = "Threshold")
    public double getThreshold() {
        return threshold;
    }

    @ACAQParameter("threshold")
    public void setThreshold(double threshold) {
        this.threshold = threshold;

    }

    @ACAQParameter("resolution")
    @ACAQDocumentation(name = "Resolution")
    public int getResolution() {
        return resolution;
    }

    @ACAQParameter("resolution")
    public void setResolution(int resolution) {
        this.resolution = resolution;

    }

    @ACAQParameter("ratio")
    @ACAQDocumentation(name = "Ratio")
    public double getRatio() {
        return ratio;
    }

    @ACAQParameter("ratio")
    public void setRatio(double ratio) {
        this.ratio = ratio;

    }

    @ACAQParameter("bandwidth")
    @ACAQDocumentation(name = "Bandwidth")
    public int getBandwidth() {
        return bandwidth;
    }

    @ACAQParameter("bandwidth")
    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;

    }

    @ACAQParameter("local-radius")
    @ACAQDocumentation(name = "Local radius")
    public int getLocalRadius() {
        return localRadius;
    }

    @ACAQParameter("local-radius")
    public void setLocalRadius(int localRadius) {
        this.localRadius = localRadius;

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQDocumentation(name = "Local mode")
    @ACAQParameter("local")
    public boolean isLocal() {
        return local;
    }

    @ACAQParameter("local")
    public void setLocal(boolean local) {
        this.local = local;

    }
}