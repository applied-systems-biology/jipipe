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
import ij.ImageStack;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.HoughLines;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalFloatParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.OptionalVector2dParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.OptionalVector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Detect line segments 2D (Hough)", description = "Finds lines within the image via a Hough lines transformation. " + "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@AddJIPipeCitation("Based on code by Hartmut Gimpel, https://sourceforge.net/p/octave/image/ci/default/tree/inst/")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Mask", description = "Mask that contains the segmented edges. ", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Lines", create = true, description = "The detected lines represented as ROI")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", create = true, description = "Mask that contains the detected lines")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Results", create = true, description = "The detected lines as table")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Accumulator", create = true, description = "The Hough array")
public class LineSegmentsHoughDetection2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int numPeaks = 100;
    private OptionalDoubleParameter peakThreshold = new OptionalDoubleParameter(0, false);
    private OptionalVector2iParameter neighborhoodSize = new OptionalVector2iParameter(new Vector2iParameter(5, 5), false);

//    int[] neighborhoodSize, double fillGap, double minLength, double thetaRes

    private JIPipeExpressionParameter roiNameExpression = new JIPipeExpressionParameter("line_score");

    public LineSegmentsHoughDetection2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LineSegmentsHoughDetection2DAlgorithm(LineSegmentsHoughDetection2DAlgorithm other) {
        super(other);
        this.roiNameExpression = new JIPipeExpressionParameter(other.roiNameExpression);
        this.neighborhoodSize = new OptionalVector2iParameter(other.neighborhoodSize);
        this.numPeaks = other.numPeaks;
        this.peakThreshold = new OptionalDoubleParameter(other.peakThreshold);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
//        ImagePlus inputMask = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
//        ROIListData outputROI = new ROIListData();
//        ResultsTableData outputTable = new ResultsTableData();
//        outputTable.addNumericColumn("Image Z");
//        outputTable.addNumericColumn("Image C");
//        outputTable.addNumericColumn("Image T");
//        outputTable.addNumericColumn("Line Theta");
//        outputTable.addNumericColumn("Line Rho");
//        outputTable.addNumericColumn("Line X0");
//        outputTable.addNumericColumn("Line Y0");
//        outputTable.addNumericColumn("Line X1");
//        outputTable.addNumericColumn("Line Y1");
//        outputTable.addNumericColumn("Line Score");
//
//        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
//        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
//
//        ImageStack outputMaskStack = new ImageStack(inputMask.getWidth(), inputMask.getHeight(), inputMask.getStackSize());
//        final ImageStack[] outputAccumulatorStack = {null};
//
//        ImageJUtils.forEachIndexedZCTSlice(inputMask, (ip, index) -> {
//            HoughLines houghLines = new HoughLines();
//            houghLines.initialise(ip.getWidth(), ip.getHeight());
//            houghLines.addPoints(ip, pixelThreshold.isEnabled(), pixelThreshold.getContent());
//            ROIListData localROI = new ROIListData();
//            for (HoughLines.HoughLine line : houghLines.getLines(selectTopN.isEnabled() ? selectTopN.getContent() : -1, accumulatorThreshold)) {
//
//                // Add to table
//                outputTable.addAndModifyRow().set("Image Z", index.getZ())
//                        .set("Image C", index.getC())
//                        .set("Image T", index.getT())
//                        .set("Line Theta", line.getTheta())
//                        .set("Line Rho", line.getR())
//                        .set("Line Score", line.getScore())
//                        .set("Line X0", line.getX1())
//                        .set("Line Y0", line.getY1())
//                        .set("Line X1", line.getX2())
//                        .set("Line Y1", line.getY2());
//
//                // Generate variables
//                variables.set("img_z", index.getZ());
//                variables.set("img_c", index.getC());
//                variables.set("img_t", index.getT());
//                variables.set("line_theta", line.getTheta());
//                variables.set("line_rho", line.getR());
//                variables.set("line_score", line.getScore());
//                variables.set("line_x0", line.getX1());
//                variables.set("line_y0", line.getY1());
//                variables.set("line_x1", line.getX2());
//                variables.set("line_y1", line.getY2());
//
//                // Generate ROI
//                Line roi;
//                {
//                    roi = new Line(line.getX1(), line.getY1(), line.getX2(), line.getY2());
//                    roi.setName(roiNameExpression.evaluateToString(variables));
//                }
//                localROI.add(roi);
//            }
//
//            // Generate mask
//            ImagePlus sliceMask = localROI.toMask(ip.getWidth(), ip.getHeight(), true, false, 1);
//            outputMaskStack.setProcessor(sliceMask.getProcessor(), index.zeroSliceIndexToOneStackIndex(inputMask));
//
//            // Output the ROI
//            for (Roi roi : localROI) {
//                roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
//            }
//            outputROI.addAll(localROI);
//
//            // Generate accumulator
//            ImageProcessor accumulator;
//            {
//                float[][] houghArray = houghLines.getHoughArray();
//                int accHeight = houghArray.length;
//                int accWidth = houghArray[0].length;
//                accumulator = new FloatProcessor(accWidth, accHeight);
//                float[] pixels = (float[]) accumulator.getPixels();
//                for (int y = 0; y < accHeight; y++) {
//                    float[] row = houghArray[y];
//                    System.arraycopy(row, 0, pixels, y * accWidth, accWidth);
//                }
//                if (outputAccumulatorStack[0] == null) {
//                    outputAccumulatorStack[0] = new ImageStack(accWidth, accHeight, inputMask.getStackSize());
//                }
//            }
//            outputAccumulatorStack[0].setProcessor(accumulator, index.zeroSliceIndexToOneStackIndex(inputMask));
//
//        }, progressInfo);
//
//        ImagePlus outputMask = new ImagePlus("Lines", outputMaskStack);
//        ImageJUtils.copyHyperstackDimensions(inputMask, outputMask);
//        outputMask.copyScale(inputMask);
//
//        iterationStep.addOutputData("Lines", outputROI, progressInfo);
//        iterationStep.addOutputData("Mask", new ImagePlusGreyscaleMaskData(outputMask), progressInfo);
//        iterationStep.addOutputData("Accumulator", new ImagePlusGreyscaleData(new ImagePlus("Accumulator", outputAccumulatorStack[0])), progressInfo);
//        iterationStep.addOutputData("Results", outputTable, progressInfo);
    }


    @SetJIPipeDocumentation(name = "ROI name", description = "The name of the generated line ROI is calculated from this expression")
    @JIPipeParameter("roi-name-expression")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "img_c", name = "Image C", description = "Current channel slice (zero-based)")
    @JIPipeExpressionParameterVariable(key = "img_z", name = "Image Z", description = "Current Z slice (zero-based)")
    @JIPipeExpressionParameterVariable(key = "img_t", name = "Image T", description = "Current frame slice (zero-based)")
    @JIPipeExpressionParameterVariable(key = "line_theta", name = "Line Theta", description = "Theta parameter of the line (polar coordinates)")
    @JIPipeExpressionParameterVariable(key = "line_rho", name = "Line Rho", description = "Rho parameter of the line (polar coordinates)")
    @JIPipeExpressionParameterVariable(key = "line_score", name = "Line score", description = "The score of the line")
    @JIPipeExpressionParameterVariable(key = "line_x0", name = "Line X0", description = "The first X position of the line (cartesian coordinates)")
    @JIPipeExpressionParameterVariable(key = "line_y0", name = "Line Y0", description = "The first Y position of the line (cartesian coordinates)")
    @JIPipeExpressionParameterVariable(key = "line_x1", name = "Line X1", description = "The second X position of the line (cartesian coordinates)")
    @JIPipeExpressionParameterVariable(key = "line_y1", name = "Line Y1", description = "The second Y position of the line (cartesian coordinates)")
    public JIPipeExpressionParameter getRoiNameExpression() {
        return roiNameExpression;
    }

    @JIPipeParameter("roi-name-expression")
    public void setRoiNameExpression(JIPipeExpressionParameter roiNameExpression) {
        this.roiNameExpression = roiNameExpression;
    }

    @SetJIPipeDocumentation(name = "Neighborhood size", description = "Neighborhood size for the non-maximum suppression. Defaults to [height / 50, width / 50] if not enabled. Must be positive odd integers.")
    @JIPipeParameter("neighborhood-size")
    @VectorParameterSettings(xLabel = "Width", yLabel = "Height")
    public OptionalVector2iParameter getNeighborhoodSize() {
        return neighborhoodSize;
    }

    @JIPipeParameter("neighborhood-size")
    public void setNeighborhoodSize(OptionalVector2iParameter neighborhoodSize) {
        this.neighborhoodSize = neighborhoodSize;
    }

    @SetJIPipeDocumentation(name = "Peak threshold", description = "Threshold for the Hough array H peak detection. Defaults to 0.5 * MAX(H).")
    @JIPipeParameter("peak-threshold")
    public OptionalDoubleParameter getPeakThreshold() {
        return peakThreshold;
    }

    @JIPipeParameter("peak-threshold")
    public void setPeakThreshold(OptionalDoubleParameter peakThreshold) {
        this.peakThreshold = peakThreshold;
    }

    @SetJIPipeDocumentation(name = "Number of peaks", description = "The maximum number of peaks/line segments that will be detected.")
    @JIPipeParameter("num-peaks")
    public int getNumPeaks() {
        return numPeaks;
    }

    @JIPipeParameter("num-peaks")
    public void setNumPeaks(int numPeaks) {
        this.numPeaks = numPeaks;
    }
}
