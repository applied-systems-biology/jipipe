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

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.process.FloatProcessor;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.LegacyHoughLines;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalFloatParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Detect global lines 2D (Hough, legacy)", description = "Finds lines within the image via a Hough lines transformation. "
        + "If higher-dimensional data is provided, the filter is applied to each 2D slice. " +
        "Please note that this node will not find line segments, but finds global linear structures. " +
        "Legacy implementation based on code by David Chatting. We recommend to use the non-legacy algorithm for better results.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@AddJIPipeCitation("Based on code by David Chatting, https://github.com/davidchatting/hough_lines/blob/master/HoughTransform.java")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Mask", description = "Mask that contains the segmented edges. ", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Lines", create = true, description = "The detected lines represented as ROI")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", create = true, description = "Mask that contains the detected lines")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Results", create = true, description = "The detected lines as table")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Accumulator", create = true, description = "The Hough array")
public class LinesHoughDetection2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalIntegerParameter selectTopN = new OptionalIntegerParameter(true, 10);
    private int accumulatorThreshold = 0;
    private int neighborhoodSize = 4;
    private int maxTheta = 180;

    private OptionalFloatParameter pixelThreshold = new OptionalFloatParameter(true, 0f);

    private JIPipeExpressionParameter roiNameExpression = new JIPipeExpressionParameter("line_score");

    public LinesHoughDetection2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LinesHoughDetection2DAlgorithm(LinesHoughDetection2DAlgorithm other) {
        super(other);
        this.selectTopN = new OptionalIntegerParameter(other.selectTopN);
        this.accumulatorThreshold = other.accumulatorThreshold;
        this.roiNameExpression = new JIPipeExpressionParameter(other.roiNameExpression);
        this.pixelThreshold = new OptionalFloatParameter(other.pixelThreshold);
        this.neighborhoodSize = other.neighborhoodSize;
        this.maxTheta = other.maxTheta;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputMask = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ROIListData outputROI = new ROIListData();
        ResultsTableData outputTable = new ResultsTableData();
        outputTable.addNumericColumn("Image Z");
        outputTable.addNumericColumn("Image C");
        outputTable.addNumericColumn("Image T");
        outputTable.addNumericColumn("Line Theta");
        outputTable.addNumericColumn("Line Rho");
        outputTable.addNumericColumn("Line X0");
        outputTable.addNumericColumn("Line Y0");
        outputTable.addNumericColumn("Line X1");
        outputTable.addNumericColumn("Line Y1");
        outputTable.addNumericColumn("Line Score");

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        Map<ImageSliceIndex, ImageProcessor> outputMaskSlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> outputAccumulatorSlices = new HashMap<>();

        ImageJUtils.forEachIndexedZCTSlice(inputMask, (ip, index) -> {
            LegacyHoughLines houghLines = new LegacyHoughLines(neighborhoodSize, maxTheta);
            houghLines.initialise(ip.getWidth(), ip.getHeight());
            houghLines.addPoints(ip, pixelThreshold.isEnabled(), pixelThreshold.getContent());
            ROIListData localROI = new ROIListData();
            for (LegacyHoughLines.HoughLine line : houghLines.getLines(selectTopN.isEnabled() ? selectTopN.getContent() : -1, accumulatorThreshold)) {

                // Add to table
                outputTable.addAndModifyRow().set("Image Z", index.getZ())
                        .set("Image C", index.getC())
                        .set("Image T", index.getT())
                        .set("Line Theta", line.getTheta())
                        .set("Line Rho", line.getR())
                        .set("Line Score", line.getScore())
                        .set("Line X0", line.getX1())
                        .set("Line Y0", line.getY1())
                        .set("Line X1", line.getX2())
                        .set("Line Y1", line.getY2());

                // Generate variables
                variables.set("img_z", index.getZ());
                variables.set("img_c", index.getC());
                variables.set("img_t", index.getT());
                variables.set("line_theta", line.getTheta());
                variables.set("line_rho", line.getR());
                variables.set("line_score", line.getScore());
                variables.set("line_x0", line.getX1());
                variables.set("line_y0", line.getY1());
                variables.set("line_x1", line.getX2());
                variables.set("line_y1", line.getY2());

                // Generate ROI
                Line roi;
                {
                    roi = new Line(line.getX1(), line.getY1(), line.getX2(), line.getY2());
                    roi.setName(roiNameExpression.evaluateToString(variables));
                }
                localROI.add(roi);
            }

            // Generate mask
            ImagePlus sliceMask = localROI.toMask(ip.getWidth(), ip.getHeight(), true, false, 1);
            outputMaskSlices.put(index, sliceMask.getProcessor());

            // Output the ROI
            for (Roi roi : localROI) {
                roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
            }
            outputROI.addAll(localROI);

            // Generate accumulator
            ImageProcessor accumulator;
            {
                float[][] houghArray = houghLines.getHoughArray();
                int accHeight = houghArray.length;
                int accWidth = houghArray[0].length;
                accumulator = new FloatProcessor(accWidth, accHeight);
                float[] pixels = (float[]) accumulator.getPixels();
                for (int y = 0; y < accHeight; y++) {
                    float[] row = houghArray[y];
                    System.arraycopy(row, 0, pixels, y * accWidth, accWidth);
                }
            }
            outputAccumulatorSlices.put(index, accumulator);

        }, progressInfo);

        ImagePlus outputMask = ImageJUtils.mergeMappedSlices(outputMaskSlices);
        ImageJUtils.copyHyperstackDimensions(inputMask, outputMask);
        outputMask.copyScale(inputMask);

        ImagePlus outputAccumulator = ImageJUtils.mergeMappedSlices(outputAccumulatorSlices);
        ImageJUtils.copyHyperstackDimensions(inputMask, outputAccumulator);
        outputAccumulator.copyScale(inputMask);

        iterationStep.addOutputData("Lines", outputROI, progressInfo);
        iterationStep.addOutputData("Mask", new ImagePlusGreyscaleMaskData(outputMask), progressInfo);
        iterationStep.addOutputData("Accumulator", new ImagePlusGreyscaleData(outputAccumulator), progressInfo);
        iterationStep.addOutputData("Results", outputTable, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Neighborhood size", description = "The size of the neighbourhood in which to search for other local maxima")
    @JIPipeParameter("neighborhood-size")
    public int getNeighborhoodSize() {
        return neighborhoodSize;
    }

    @JIPipeParameter("neighborhood-size")
    public void setNeighborhoodSize(int neighborhoodSize) {
        this.neighborhoodSize = neighborhoodSize;
    }

    @SetJIPipeDocumentation(name = "Theta step count", description = "The number of thetas to check (the step size is automatically determined as PI/step_count)")
    @JIPipeParameter("max-theta")
    public int getMaxTheta() {
        return maxTheta;
    }

    @JIPipeParameter("max-theta")
    public void setMaxTheta(int maxTheta) {
        this.maxTheta = maxTheta;
    }

    @SetJIPipeDocumentation(name = "Select top N lines", description = "If enabled, only the top N lines according to their scores are selected.")
    @JIPipeParameter("select-top-n")
    public OptionalIntegerParameter getSelectTopN() {
        return selectTopN;
    }

    @JIPipeParameter("select-top-n")
    public void setSelectTopN(OptionalIntegerParameter selectTopN) {
        this.selectTopN = selectTopN;
    }

    @SetJIPipeDocumentation(name = "Accumulator threshold", description = "The percentage threshold above which lines are determined from the hough array")
    @JIPipeParameter("accumulator-threshold")
    public int getAccumulatorThreshold() {
        return accumulatorThreshold;
    }

    @JIPipeParameter("accumulator-threshold")
    public void setAccumulatorThreshold(int accumulatorThreshold) {
        this.accumulatorThreshold = accumulatorThreshold;
    }

    @SetJIPipeDocumentation(name = "Pixel threshold", description = "If enabled (default), points are only counted if they are above the specified threshold. This this mode is deactivated, each pixel is counted, but weighted by its intensity.")
    @JIPipeParameter("pixel-threshold")
    public OptionalFloatParameter getPixelThreshold() {
        return pixelThreshold;
    }

    @JIPipeParameter("pixel-threshold")
    public void setPixelThreshold(OptionalFloatParameter pixelThreshold) {
        this.pixelThreshold = pixelThreshold;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "The name of the generated line ROI is calculated from this expression")
    @JIPipeParameter("roi-name-expression")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "img_c", name = "Image C", description = "Current channel slice (zero-based)")
    @AddJIPipeExpressionParameterVariable(key = "img_z", name = "Image Z", description = "Current Z slice (zero-based)")
    @AddJIPipeExpressionParameterVariable(key = "img_t", name = "Image T", description = "Current frame slice (zero-based)")
    @AddJIPipeExpressionParameterVariable(key = "line_theta", name = "Line Theta", description = "Theta parameter of the line (polar coordinates)")
    @AddJIPipeExpressionParameterVariable(key = "line_rho", name = "Line Rho", description = "Rho parameter of the line (polar coordinates)")
    @AddJIPipeExpressionParameterVariable(key = "line_score", name = "Line score", description = "The score of the line")
    @AddJIPipeExpressionParameterVariable(key = "line_x0", name = "Line X0", description = "The first X position of the line (cartesian coordinates)")
    @AddJIPipeExpressionParameterVariable(key = "line_y0", name = "Line Y0", description = "The first Y position of the line (cartesian coordinates)")
    @AddJIPipeExpressionParameterVariable(key = "line_x1", name = "Line X1", description = "The second X position of the line (cartesian coordinates)")
    @AddJIPipeExpressionParameterVariable(key = "line_y1", name = "Line Y1", description = "The second Y position of the line (cartesian coordinates)")
    public JIPipeExpressionParameter getRoiNameExpression() {
        return roiNameExpression;
    }

    @JIPipeParameter("roi-name-expression")
    public void setRoiNameExpression(JIPipeExpressionParameter roiNameExpression) {
        this.roiNameExpression = roiNameExpression;
    }
}
