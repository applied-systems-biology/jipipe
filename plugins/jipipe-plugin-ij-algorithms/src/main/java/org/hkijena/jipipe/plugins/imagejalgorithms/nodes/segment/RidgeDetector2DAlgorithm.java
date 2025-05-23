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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.segment;

import de.biomedical_imaging.ij.steger.*;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;

import java.awt.*;

@SetJIPipeDocumentation(name = "Ridge detector", description = "A ridge detector that detects lines and outputs a binary mask where lines are detected.")
@AddJIPipeCitation("https://github.com/thorstenwagner/ij-ridgedetection")
@AddJIPipeCitation("https://imagej.net/plugins/ridge-detection")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Segment")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Lines", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Junctions", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins", aliasName = "Ridge Detection")
public class RidgeDetector2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    // Mandatory parameters
    private double sigma = 1.51;
    private double lowerThreshold = 3.06;
    private double upperThreshold = 7.99;
    private boolean darkLine = false;

    // Estimator parameters
    private boolean estimateSigma = false;
    private boolean estimateLowThreshold = false;
    private boolean estimateHighThreshold = false;
    private double lineWidth = 3.5;
    private double contrastHigh = 230;
    private double contrastLow = 87;

    private double minLength = 0;
    private double maxLength = 0;

    private boolean doCorrectPosition = false;
    private boolean doEstimateWidth = false;
    private boolean doExtendLine = true;
    private OverlapOption overlapResolution = OverlapOption.NONE;


    public RidgeDetector2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RidgeDetector2DAlgorithm(RidgeDetector2DAlgorithm other) {
        super(other);
        this.sigma = other.sigma;
        this.lowerThreshold = other.lowerThreshold;
        this.upperThreshold = other.upperThreshold;
        this.darkLine = other.darkLine;
        this.lineWidth = other.lineWidth;
        this.contrastHigh = other.contrastHigh;
        this.contrastLow = other.contrastLow;
        this.estimateSigma = other.estimateSigma;
        this.estimateLowThreshold = other.estimateLowThreshold;
        this.estimateHighThreshold = other.estimateHighThreshold;
        this.minLength = other.minLength;
        this.maxLength = other.maxLength;
        this.doCorrectPosition = other.doCorrectPosition;
        this.doEstimateWidth = other.doEstimateWidth;
        this.doExtendLine = other.doExtendLine;
        this.overlapResolution = other.overlapResolution;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();

        ROI2DListData outputLines = new ROI2DListData();
        ROI2DListData outputJunctions = new ROI2DListData();
        ImagePlus outputMask = ImageJIterationUtils.generateForEachIndexedZCTSlice(inputImage, (ip, index) -> {
            double sigma_ = sigma;
            double lowerThreshold_ = lowerThreshold;
            double upperThreshold_ = upperThreshold;
            if (estimateSigma) {
                sigma_ = lineWidth / (2 * Math.sqrt(3)) + 0.5;
            }
            if (estimateLowThreshold) {
                double clow = contrastLow;
                if (darkLine) {
                    clow = 255 - contrastLow;
                }
                lowerThreshold_ = Math.floor(Math.abs(-2 * clow * (lineWidth / 2.0)
                        / (Math.sqrt(2 * Math.PI) * sigma_ * sigma_ * sigma_)
                        * Math.exp(-((lineWidth / 2.0) * (lineWidth / 2.0)) / (2 * sigma_ * sigma_))));
            }
            if (estimateHighThreshold) {
                double chigh = contrastHigh;
                if (darkLine) {
                    chigh = 255 - contrastHigh;
                }
                upperThreshold_ = Math.floor(Math.abs(-2 * chigh * (lineWidth / 2.0)
                        / (Math.sqrt(2 * Math.PI) * sigma_ * sigma_ * sigma_)
                        * Math.exp(-((lineWidth / 2.0) * (lineWidth / 2.0)) / (2 * sigma_ * sigma_))));
            }
            LineDetector lineDetector = new LineDetector();
            Lines lines = lineDetector.detectLines(ip, sigma_, upperThreshold_, lowerThreshold_, minLength, maxLength, darkLine, doCorrectPosition, doEstimateWidth, doExtendLine, overlapResolution);
            extractLines(outputLines, lines, index);
            extractJunctions(outputJunctions, lineDetector, index);
            return extractMask(ip.getWidth(), ip.getHeight(), lines);
        }, progressInfo);

        iterationStep.addOutputData("Mask", new ImagePlusGreyscaleMaskData(outputMask), progressInfo);
        iterationStep.addOutputData("Lines", outputLines, progressInfo);
        iterationStep.addOutputData("Junctions", outputJunctions, progressInfo);
    }

    private ImageProcessor extractMask(int outputWidth, int outputHeight, Lines lines) {
        ImageProcessor ip = new ByteProcessor(outputWidth, outputHeight);
        for (Line c : lines) {
            float[] x = c.getXCoordinates();
            float[] y = c.getYCoordinates();

            int[] x_poly_r = new int[x.length];
            int[] y_poly_r = new int[x.length];

            Polygon LineSurface = new Polygon();

            ip.setLineWidth(1);
            ip.setColor(255);

            for (int j = 0; j < x.length; j++) {
                // this draws the identified line
                if (j > 0) {
                    ip.drawLine((int) Math.round(x[j - 1]), (int) Math.round(y[j - 1]), (int) Math.round(x[j]),
                            (int) Math.round(y[j]));
                }

                // If Estimate Width is ticked, we also draw the line surface in the binary
                if (doEstimateWidth) {

                    double nx = Math.sin(c.getAngle()[j]);
                    double ny = Math.cos(c.getAngle()[j]);

                    // left point coordinates are directly added to the polygon. right coordinates
                    // are saved to be added at the end of the coordinates list
                    LineSurface.addPoint((int) Math.round(x[j] - c.getLineWidthL()[j] * nx),
                            (int) Math.round(y[j] - c.getLineWidthL()[j] * ny));

                    x_poly_r[j] = (int) Math.round(x[j] + c.getLineWidthR()[j] * nx);
                    y_poly_r[j] = (int) Math.round(y[j] + c.getLineWidthR()[j] * ny);
                }
            }

            if (doEstimateWidth) {
                // loop to add the right coordinates to the end of the polygon, reversed
                for (int j = 0; j < x.length; j++) {
                    if (j < x.length) {
                        LineSurface.addPoint(x_poly_r[x.length - 1 - j], y_poly_r[x.length - 1 - j]);
                    }
                }
                // draw surfaces.
                ip.fillPolygon(LineSurface);
            }
        }
        return ip;
    }

    private void extractJunctions(ROI2DListData outputJunctions, LineDetector lineDetector, ImageSliceIndex index) {
        for (Junction j : lineDetector.getJunctions()) {
            PointRoi pr = new PointRoi(j.getX() + 0.5, j.getY() + 0.5);
            pr.setName("JP-C" + j.getLine1().getID() + "-C" + j.getLine2().getID());
            pr.setPosition(j.getLine1().getFrame());
            pr.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
            outputJunctions.add(pr);
        }
    }

    private void extractLines(ROI2DListData outputLines, Lines lines, ImageSliceIndex index) {
        for (Line c : lines) {
            float[] x = c.getXCoordinates();
            for (int j = 0; j < x.length; j++) {
                x[j] = (float) (x[j] + 0.5);
            }
            float[] y = c.getYCoordinates();
            for (int j = 0; j < y.length; j++) {
                y[j] = (float) (y[j] + 0.5);
            }

            FloatPolygon p = new FloatPolygon(x, y, c.getNumber());
            Roi r = new PolygonRoi(p, Roi.FREELINE);
            r.setPosition(c.getFrame());
            r.setName("C" + c.getID());
            r.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
            outputLines.add(r);
        }
    }

    @SetJIPipeDocumentation(name = "Min line length", description = "Minimum length of a line")
    @JIPipeParameter("min-length")
    public double getMinLength() {
        return minLength;
    }

    @JIPipeParameter("min-length")
    public void setMinLength(double minLength) {
        this.minLength = minLength;
    }

    @SetJIPipeDocumentation(name = "Max line length", description = "Maximum length of a line")
    @JIPipeParameter("max-length")
    public double getMaxLength() {
        return maxLength;
    }

    @JIPipeParameter("max-length")
    public void setMaxLength(double maxLength) {
        this.maxLength = maxLength;
    }

    @SetJIPipeDocumentation(name = "Correct position", description = "Correct the line position if it has different contrast on each side of it.")
    @JIPipeParameter("correct-position")
    public boolean isDoCorrectPosition() {
        return doCorrectPosition;
    }

    @JIPipeParameter("correct-position")
    public void setDoCorrectPosition(boolean doCorrectPosition) {
        this.doCorrectPosition = doCorrectPosition;
    }

    @SetJIPipeDocumentation(name = "Estimate width", description = "If this option is selected the width of the line is estimated.")
    @JIPipeParameter("estimate-width")
    public boolean isDoEstimateWidth() {
        return doEstimateWidth;
    }

    @JIPipeParameter("estimate-width")
    public void setDoEstimateWidth(boolean doEstimateWidth) {
        this.doEstimateWidth = doEstimateWidth;
    }

    @SetJIPipeDocumentation(name = "Extend lines", description = "Extends the detect lines to find more junction points")
    @JIPipeParameter("extend-line")
    public boolean isDoExtendLine() {
        return doExtendLine;
    }

    @JIPipeParameter("extend-line")
    public void setDoExtendLine(boolean doExtendLine) {
        this.doExtendLine = doExtendLine;
    }

    @SetJIPipeDocumentation(name = "Resolve overlaps", description = "Method for resolving overlaps")
    @JIPipeParameter("overlap-resolution")
    public OverlapOption getOverlapResolution() {
        return overlapResolution;
    }

    @JIPipeParameter("overlap-resolution")
    public void setOverlapResolution(OverlapOption overlapResolution) {
        this.overlapResolution = overlapResolution;
    }

    @SetJIPipeDocumentation(name = "Sigma", description = "Determines the sigma for the derivatives. It depends on the line width.")
    @JIPipeParameter(value = "sigma", important = true, uiOrder = -18)
    public double getSigma() {
        return sigma;
    }

    @JIPipeParameter("sigma")
    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    @SetJIPipeDocumentation(name = "Lower threshold", description = "Line points with a response smaller as this threshold are rejected")
    @JIPipeParameter(value = "lower-threshold", important = true, uiOrder = -20)
    public double getLowerThreshold() {
        return lowerThreshold;
    }

    @JIPipeParameter("lower-threshold")
    public void setLowerThreshold(double lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }

    @SetJIPipeDocumentation(name = "Upper threshold", description = "Line points with a response larger as this threshold are accepted")
    @JIPipeParameter(value = "upper-threshold", important = true, uiOrder = -19)
    public double getUpperThreshold() {
        return upperThreshold;
    }

    @JIPipeParameter("upper-threshold")
    public void setUpperThreshold(double upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    @SetJIPipeDocumentation(name = "Dark lines", description = "This parameter determines whether dark or bright lines are extracted")
    @JIPipeParameter(value = "dark-line", important = true)
    public boolean isDarkLine() {
        return darkLine;
    }

    @JIPipeParameter("dark-line")
    public void setDarkLine(boolean darkLine) {
        this.darkLine = darkLine;
    }

    @SetJIPipeDocumentation(name = "Line width", description = "The line diameter in pixels. It estimates the 'Sigma' parameter.")
    @JIPipeParameter(value = "line-width", important = true)
    public double getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "High contrast", description = "Highest grayscale value of the line. It estimates the mandatory parameter 'Upper threshold'")
    @JIPipeParameter(value = "high-contrast", important = true)
    public double getContrastHigh() {
        return contrastHigh;
    }

    @JIPipeParameter("high-contrast")
    public void setContrastHigh(double contrastHigh) {
        this.contrastHigh = contrastHigh;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Low contrast", description = "Lowest grayscale value of the line. It estimates the mandatory parameter 'Lower threshold'")
    @JIPipeParameter(value = "low-contrast", important = true)
    public double getContrastLow() {
        return contrastLow;
    }

    @JIPipeParameter("low-contrast")
    public void setContrastLow(double contrastLow) {
        this.contrastLow = contrastLow;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Estimate sigma", description = "If enabled, 'Sigma' will be estimated by the 'Line width' parameter. The formula is SIGMA = LINE_WIDTH / (2 * SQRT(3)) + 0.5")
    @JIPipeParameter("estimate-sigma")
    public boolean isEstimateSigma() {
        return estimateSigma;
    }

    @JIPipeParameter("estimate-sigma")
    public void setEstimateSigma(boolean estimateSigma) {
        this.estimateSigma = estimateSigma;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Estimate low threshold", description = "If enabled, the lower threshold will be estimated by the contrast parameters. " +
            "The formula is FLOOR(ABS(-2 * CONTRAST_LOW * (LINE_WIDTH / 2.0) / (SQRT(2 * PI) * POW(SIGMA, 3)) * EXP(-((LINE_WIDTH / 2.0) * (LINE_WIDTH / 2.0)) / (2 * SIGMA * SIGMA))))")
    @JIPipeParameter("estimate-low-threshold")
    public boolean isEstimateLowThreshold() {
        return estimateLowThreshold;
    }

    @JIPipeParameter("estimate-low-threshold")
    public void setEstimateLowThreshold(boolean estimateLowThreshold) {
        this.estimateLowThreshold = estimateLowThreshold;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Estimate high threshold", description = "If enabled, the lower and higher threshold will be estimated by the contrast parameters. " +
            "The formula is FLOOR(ABS(-2 * CONTRAST_HIGH * (LINE_WIDTH / 2.0) / (SQRT(2 * PI) * POW(SIGMA, 3)) * EXP(-((LINE_WIDTH / 2.0) * (LINE_WIDTH / 2.0)) / (2 * SIGMA * SIGMA))))")
    @JIPipeParameter("estimate-high-threshold")
    public boolean isEstimateHighThreshold() {
        return estimateHighThreshold;
    }

    @JIPipeParameter("estimate-high-threshold")
    public void setEstimateHighThreshold(boolean estimateHighThreshold) {
        this.estimateHighThreshold = estimateHighThreshold;
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == this) {
            if ("sigma".equals(access.getKey()) && estimateSigma) {
                return false;
            }
            if ("lower-threshold".equals(access.getKey()) && estimateLowThreshold) {
                return false;
            }
            if ("upper-threshold".equals(access.getKey()) && estimateHighThreshold) {
                return false;
            }
            if ("low-contrast".equals(access.getKey()) && !estimateLowThreshold) {
                return false;
            }
            if ("high-contrast".equals(access.getKey()) && !estimateHighThreshold) {
                return false;
            }
            if ("line-width".equals(access.getKey()) && !estimateLowThreshold && !estimateHighThreshold && !estimateSigma) {
                return false;
            }
        }
        return super.isParameterUIVisible(tree, access);
    }
}
