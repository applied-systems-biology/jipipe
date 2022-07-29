package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Line;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.AnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.HoughLines;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Detect lines 2D (Hough)", description = "Finds lines within the image via a Hough lines transformation. " + "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@JIPipeCitation("Based on code by David Chatting, https://github.com/davidchatting/hough_lines/blob/master/HoughTransform.java")
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", description = "Mask that contains the segmented edges. ", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Lines", autoCreate = true, description = "The detected lines represented as ROI")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", autoCreate = true, description = "Mask that contains the detected lines")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Results", autoCreate = true, description = "The detected lines as table")
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Accumulator", autoCreate = true, description = "The Hough array")
public class LinesHoughDetection2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalIntegerParameter selectTopN = new OptionalIntegerParameter(true, 10);
    private int threshold = 0;

    private DefaultExpressionParameter roiNameExpression = new DefaultExpressionParameter("line_score");

    public LinesHoughDetection2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LinesHoughDetection2DAlgorithm(LinesHoughDetection2DAlgorithm other) {
        super(other);
        this.selectTopN = new OptionalIntegerParameter(other.selectTopN);
        this.threshold = other.threshold;
        this.roiNameExpression = new DefaultExpressionParameter(other.roiNameExpression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputMask = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
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

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        ImageStack outputMaskStack = new ImageStack(inputMask.getWidth(), inputMask.getHeight(), inputMask.getStackSize());
        final ImageStack[] outputAccumulatorStack = {null};

        ImageJUtils.forEachIndexedZCTSlice(inputMask, (ip, index) -> {
            HoughLines houghLines = new HoughLines(ip.getBufferedImage());
            ROIListData localROI = new ROIListData();
            for (HoughLines.HoughLine line : houghLines.getLines(selectTopN.isEnabled() ? selectTopN.getContent() : -1, threshold)) {

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
            outputMaskStack.setProcessor(sliceMask.getProcessor(), index.zeroSliceIndexToOneStackIndex(inputMask));

            // Output the ROI
            for (Roi roi : localROI) {
                roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
            }
            outputROI.addAll(localROI);

            // Generate accumulator
            ImageProcessor accumulator;
            {
                int[][] houghArray = houghLines.getHoughArray();
                int accHeight = houghArray.length;
                int accWidth = houghArray[0].length;
                accumulator = new FloatProcessor(accWidth, accHeight);
                float[] pixels = (float[]) accumulator.getPixels();
                for (int y = 0; y < accHeight; y++) {
                    int[] row = houghArray[y];
                    for (int x = 0; x < accWidth; x++) {
                        pixels[x + y * accWidth] = row[x];
                    }
                }
                if(outputAccumulatorStack[0] == null) {
                    outputAccumulatorStack[0] = new ImageStack(accWidth, accHeight, inputMask.getStackSize());
                }
            }
            outputAccumulatorStack[0].setProcessor(accumulator, index.zeroSliceIndexToOneStackIndex(inputMask));

        }, progressInfo);

        ImagePlus outputMask = new ImagePlus("Lines", outputMaskStack);
        ImageJUtils.copyHyperstackDimensions(inputMask, outputMask);
        outputMask.copyScale(inputMask);

        dataBatch.addOutputData("Lines", outputROI, progressInfo);
        dataBatch.addOutputData("Mask", new ImagePlusGreyscaleMaskData(outputMask), progressInfo);
        dataBatch.addOutputData("Accumulator", new ImagePlusGreyscaleData(new ImagePlus("Accumulator", outputAccumulatorStack[0])), progressInfo);
        dataBatch.addOutputData("Results", outputTable, progressInfo);
    }

    @JIPipeDocumentation(name = "Select top N lines", description = "If enabled, only the top N lines according to their scores are selected.")
    @JIPipeParameter("select-top-n")
    public OptionalIntegerParameter getSelectTopN() {
        return selectTopN;
    }

    @JIPipeParameter("select-top-n")
    public void setSelectTopN(OptionalIntegerParameter selectTopN) {
        this.selectTopN = selectTopN;
    }

    @JIPipeDocumentation(name = "Threshold", description = "The percentage threshold above which lines are determined from the hough array")
    @JIPipeParameter("threshold")
    public int getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @JIPipeDocumentation(name = "ROI name", description = "The name of the generated line ROI is calculated from this expression")
    @JIPipeParameter("roi-name-expression")
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "img_c", name = "Image C", description = "Current channel slice (zero-based)")
    @ExpressionParameterSettingsVariable(key = "img_z", name = "Image Z", description = "Current Z slice (zero-based)")
    @ExpressionParameterSettingsVariable(key = "img_t", name = "Image T", description = "Current frame slice (zero-based)")
    @ExpressionParameterSettingsVariable(key = "line_theta", name = "Line Theta", description = "Theta parameter of the line (polar coordinates)")
    @ExpressionParameterSettingsVariable(key = "line_rho", name = "Line Rho", description = "Rho parameter of the line (polar coordinates)")
    @ExpressionParameterSettingsVariable(key = "line_score", name = "Line score", description = "The score of the line")
    @ExpressionParameterSettingsVariable(key = "line_x0", name = "Line X0", description = "The first X position of the line (cartesian coordinates)")
    @ExpressionParameterSettingsVariable(key = "line_y0", name = "Line Y0", description = "The first Y position of the line (cartesian coordinates)")
    @ExpressionParameterSettingsVariable(key = "line_x1", name = "Line X1", description = "The second X position of the line (cartesian coordinates)")
    @ExpressionParameterSettingsVariable(key = "line_y1", name = "Line Y1", description = "The second Y position of the line (cartesian coordinates)")
    public DefaultExpressionParameter getRoiNameExpression() {
        return roiNameExpression;
    }

    @JIPipeParameter("roi-name-expression")
    public void setRoiNameExpression(DefaultExpressionParameter roiNameExpression) {
        this.roiNameExpression = roiNameExpression;
    }
}
