package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.global.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.global.GoodForTrait;
import org.hkijena.acaq5.api.traits.global.RemovesTrait;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.RoundBioObjects;
import org.hkijena.acaq5.extension.api.traits.quality.ImageQuality;
import org.hkijena.acaq5.utils.Hough_Circle;

@ACAQDocumentation(name = "Hough segmentation")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQGreyscaleImageData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskData.class, slotName = "Mask", autoCreate = true)

// Trait matching
@GoodForTrait(RoundBioObjects.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(ImageQuality.class)
@RemovesTrait(ClusterBioObjects.class)
public class HoughSegmenter extends ACAQIteratingAlgorithm {

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

    public HoughSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public HoughSegmenter(HoughSegmenter other) {
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
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQGreyscaleImageData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus img = inputData.getImage();

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
                false,
                false,
                false,
                true,
                false,
                true,
                false);
        WindowManager.setTempCurrentImage(img);
        hough_circle.startTransform();
        WindowManager.setTempCurrentImage(null);

        // Draw the circles
        ResultsTable resultsTable = Analyzer.getResultsTable();
        ImagePlus result = drawCircleMask(img, resultsTable);

        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQMaskData(result));
    }

    @ACAQParameter("min-radius")
    @ACAQDocumentation(name = "Min radius")
    public int getMinRadius() {
        return minRadius;
    }

    @ACAQParameter("min-radius")
    public void setMinRadius(int minRadius) {
        this.minRadius = minRadius;
    }

    @ACAQParameter("max-radius")
    @ACAQDocumentation(name = "Max radius")
    public int getMaxRadius() {
        return maxRadius;
    }

    @ACAQParameter("max-radius")
    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
    }

    @ACAQParameter("radius-increment")
    @ACAQDocumentation(name = "Radius increment")
    public int getRadiusIncrement() {
        return radiusIncrement;
    }

    @ACAQParameter("radius-increment")
    public void setRadiusIncrement(int radiusIncrement) {
        this.radiusIncrement = radiusIncrement;
    }

    @ACAQParameter("min-num-circles")
    @ACAQDocumentation(name = "Min number of circles")
    public int getMinNumCircles() {
        return minNumCircles;
    }

    @ACAQParameter("min-num-circles")
    public void setMinNumCircles(int minNumCircles) {
        this.minNumCircles = minNumCircles;
    }

    @ACAQParameter("max-num-circles")
    @ACAQDocumentation(name = "Max number of circles")
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
}