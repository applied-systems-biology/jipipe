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

import com.google.common.primitives.Floats;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Strel;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.HoughLineSegments;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Detect line segments 2D (Hough, classic NMS)", description = "Finds lines within the image via a Hough lines transformation. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Detect")
@AddJIPipeCitation("Based on code by Hartmut Gimpel, https://sourceforge.net/p/octave/image/ci/default/tree/inst/")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Mask", description = "Mask that contains the segmented edges. ", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Lines", create = true, description = "The detected lines represented as ROI")
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Peaks", create = true, description = "The detected peaks in the accumulator")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", create = true, description = "Mask that contains the detected lines")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Results", create = true, description = "The detected lines as table")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Accumulator", create = true, description = "The Hough array")
public class LineSegmentsHoughDetection2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int numPeaks = 100;
    private JIPipeExpressionParameter peakThreshold = new JIPipeExpressionParameter("0.5 * MAX(H)");
    private OptionalJIPipeExpressionParameter houghArrayNMSNeighborhoodSize = new OptionalJIPipeExpressionParameter(false, "ARRAY(MAX(3, ROUND_ODD(H.width / 50)), MAX(3, ROUND_EVEN(H.height / 50)))");
    private int inputNMSNeighborhoodSize = 3;
    private HoughLinesNMSAlgorithm nmsAlgorithm = HoughLinesNMSAlgorithm.HoughArray;
    private double fillGap = 20;
    private double minLength = 40;
    private JIPipeExpressionParameter thetas = new JIPipeExpressionParameter("MAKE_SEQUENCE(-90, 90, 1)");
    private JIPipeExpressionParameter roiNameExpression = new JIPipeExpressionParameter("line_length");
    private boolean fastSegmentDetection = false;

    public LineSegmentsHoughDetection2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LineSegmentsHoughDetection2DAlgorithm(LineSegmentsHoughDetection2DAlgorithm other) {
        super(other);
        this.roiNameExpression = new JIPipeExpressionParameter(other.roiNameExpression);
        this.houghArrayNMSNeighborhoodSize = new OptionalJIPipeExpressionParameter(other.houghArrayNMSNeighborhoodSize);
        this.numPeaks = other.numPeaks;
        this.peakThreshold = new JIPipeExpressionParameter(other.peakThreshold);
        this.fillGap = other.fillGap;
        this.minLength = other.minLength;
        this.thetas = new JIPipeExpressionParameter(other.thetas);
        this.fastSegmentDetection = other.fastSegmentDetection;
        this.nmsAlgorithm = other.nmsAlgorithm;
        this.inputNMSNeighborhoodSize = other.inputNMSNeighborhoodSize;
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
        outputTable.addNumericColumn("Line Length");

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        List<Double> thetaAngles = thetas.evaluateToDoubleList(variables);

        // Fix thetas, so they are within the range [-90, 90)
        for (int i = 0; i < thetaAngles.size(); i++) {
            final double originalTheta = thetaAngles.get(i);
            double theta = thetaAngles.get(i);
            while(theta < -90) {
                theta += 180;
            }
            while(theta >= 90) {
                theta -= 180;
            }
            if(theta != originalTheta) {
                progressInfo.log("Fixed input theta angle " + originalTheta + " is set to " + theta);
            }
            thetaAngles.set(i, theta);
        }

        progressInfo.log("Thetas are: " + JsonUtils.toJsonString(thetaAngles));

        Map<ImageSliceIndex, ImageProcessor> outputMaskSlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> outputAccumulatorSlices = new HashMap<>();
        ROIListData outputPeaks = new ROIListData();

        ImageJUtils.forEachIndexedZCTSliceWithProgress(inputMask, (ip, index, sliceProgress) -> {

            ImageProcessor bw = ip.duplicate();
            final int numPeakIterations = nmsAlgorithm == HoughLinesNMSAlgorithm.HoughArray ? 1 : this.numPeaks;
            final int numPeaksPerIteration = nmsAlgorithm == HoughLinesNMSAlgorithm.HoughArray ? this.numPeaks : 1;
            ROIListData localROI = new ROIListData();
            ImageProcessor houghArray = null;

            for (int peakIterationIndex = 0; peakIterationIndex < numPeakIterations; peakIterationIndex++) {
                JIPipeProgressInfo peakIterationProgress = sliceProgress.resolve("Peak iteration", peakIterationIndex, numPeakIterations);
                HoughLineSegments.HoughResult houghResult = HoughLineSegments.hough(bw, thetaAngles);

                if(houghArray == null) {
                    houghArray = houghResult.getH().duplicate();
                }

                JIPipeExpressionVariablesMap peakThresholdVariables = new JIPipeExpressionVariablesMap();
                peakThresholdVariables.putAnnotations(iterationStep.getMergedTextAnnotations());
                peakThresholdVariables.put("H", Floats.asList((float[]) houghResult.getH().getPixels()));
                peakThresholdVariables.put("H.width", houghResult.getH().getWidth());
                peakThresholdVariables.put("H.height", houghResult.getH().getHeight());
                double peakThreshold = this.peakThreshold.evaluateToDouble(peakThresholdVariables);

                int[] neighborHood;
                if (houghArrayNMSNeighborhoodSize.isEnabled() && nmsAlgorithm == HoughLinesNMSAlgorithm.HoughArray) {
                    List<Double> doubles = houghArrayNMSNeighborhoodSize.getContent().evaluateToDoubleList(peakThresholdVariables);
                    neighborHood = new int[]{doubles.get(0).intValue(), doubles.get(1).intValue()};
                } else {
                    neighborHood = null;
                }

                List<Point> peaks = HoughLineSegments.houghPeaks(houghResult.getH(), numPeaksPerIteration, peakThreshold, neighborHood, peakIterationProgress);

                // Output peak ROIs
                for (Point peak : peaks) {
                    PointRoi roi = new PointRoi(peak.x, peak.y);
                    roi.setName(peak.x + ", " + peak.y);
                    roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                    outputPeaks.add(roi);
                }

                // Generate line ROIs and table rows
                List<HoughLineSegments.Line> lines = HoughLineSegments.houghLineSegments(bw, houghResult.getThetas(), houghResult.getRhos(), peaks, fillGap, minLength, fastSegmentDetection, peakIterationProgress);
                ROIListData currentIterationROI = new ROIListData();

                for (HoughLineSegments.Line line : lines) {

                    // Add to table
                    outputTable.addAndModifyRow().set("Image Z", index.getZ())
                            .set("Image C", index.getC())
                            .set("Image T", index.getT())
                            .set("Line Theta", line.getTheta())
                            .set("Line Rho", line.getRho())
                            .set("Line Length", line.getPoint1().distance(line.getPoint2()))
                            .set("Line X0", line.getPoint1().x)
                            .set("Line Y0", line.getPoint1().y)
                            .set("Line X1", line.getPoint2().x)
                            .set("Line Y1", line.getPoint2().y);

                    // Generate variables
                    variables.set("img_z", index.getZ());
                    variables.set("img_c", index.getC());
                    variables.set("img_t", index.getT());
                    variables.set("line_theta", line.getTheta());
                    variables.set("line_rho", line.getRho());
                    variables.set("line_length", line.getPoint1().distance(line.getPoint2()));
                    variables.set("line_x0", line.getPoint1().x);
                    variables.set("line_y0", line.getPoint1().y);
                    variables.set("line_x1", line.getPoint2().x);
                    variables.set("line_y1", line.getPoint2().y);

                    // Generate ROI
                    Line roi;
                    {
                        roi = new Line(line.getPoint1().x, line.getPoint1().y, line.getPoint2().x, line.getPoint2().y);
                        roi.setName(roiNameExpression.evaluateToString(variables));
                    }
                    localROI.add(roi);
                    currentIterationROI.add(roi);
                }

                // Erase on input
                if(nmsAlgorithm == HoughLinesNMSAlgorithm.Input) {
                    ImagePlus mask = currentIterationROI.toMask(ip.getWidth(), ip.getHeight(), true, false, 1);
                    if(inputNMSNeighborhoodSize > 0) {
                        Strel strel = Strel.Shape.DISK.fromRadius(inputNMSNeighborhoodSize);
                        ImageProcessor dilation = strel.dilation(mask.getProcessor());
                        mask.setProcessor(dilation);
                    }
                    byte[] bwPixels = (byte[]) bw.getPixels();
                    byte[] maskPixels = (byte[]) mask.getProcessor().getPixels();
                    assert bwPixels.length == maskPixels.length;
                    for (int i = 0; i < bwPixels.length; i++) {
                        if(Byte.toUnsignedInt(maskPixels[i]) > 0) {
                            bwPixels[i] = 0;
                        }
                    }
                }
            }

            // Generate mask
            ImagePlus sliceMask = localROI.toMask(ip.getWidth(), ip.getHeight(), true, false, 1);
            outputMaskSlices.put(index, sliceMask.getProcessor());

            // Output the ROI
            for (Roi roi : localROI) {
                roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
            }
            outputROI.addAll(localROI);

            // Create fallback hough array
            if(houghArray == null) {
                HoughLineSegments.HoughResult houghResult = HoughLineSegments.hough(bw, thetaAngles);
                houghArray = houghResult.getH();
            }

            // Generate accumulator
            outputAccumulatorSlices.put(index, houghArray);

        }, progressInfo);

        ImagePlus outputMask = ImageJUtils.mergeMappedSlices(outputMaskSlices);
        ImageJUtils.copyHyperstackDimensions(inputMask, outputMask);
        outputMask.copyScale(inputMask);

        ImagePlus outputAccumulator = ImageJUtils.mergeMappedSlices(outputAccumulatorSlices);
        ImageJUtils.copyHyperstackDimensions(inputMask, outputAccumulator);
        outputAccumulator.copyScale(inputMask);

        iterationStep.addOutputData("Lines", outputROI, progressInfo);
        iterationStep.addOutputData("Peaks", outputPeaks, progressInfo);
        iterationStep.addOutputData("Mask", new ImagePlusGreyscaleMaskData(outputMask), progressInfo);
        iterationStep.addOutputData("Accumulator", new ImagePlusGreyscaleData(outputAccumulator), progressInfo);
        iterationStep.addOutputData("Results", outputTable, progressInfo);
    }


    @SetJIPipeDocumentation(name = "ROI name", description = "The name of the generated line ROI is calculated from this expression")
    @JIPipeParameter("roi-name-expression")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "img_c", name = "Image C", description = "Current channel slice (zero-based)")
    @AddJIPipeExpressionParameterVariable(key = "img_z", name = "Image Z", description = "Current Z slice (zero-based)")
    @AddJIPipeExpressionParameterVariable(key = "img_t", name = "Image T", description = "Current frame slice (zero-based)")
    @AddJIPipeExpressionParameterVariable(key = "line_theta", name = "Line Theta", description = "Theta parameter of the line (polar coordinates)")
    @AddJIPipeExpressionParameterVariable(key = "line_rho", name = "Line Rho", description = "Rho parameter of the line (polar coordinates)")
//    @JIPipeExpressionParameterVariable(key = "line_score", name = "Line score", description = "The score of the line")
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

    @SetJIPipeDocumentation(name = "Neighborhood size (H)", description = "Neighborhood size for the non-maximum suppression in the space of the hough array (the X axis containing the theta values, Y containing the rho values). " +
            "Defaults to [width / 50, height / 50] with a minimum of 3 if not enabled. Must be positive odd integers.")
    @JIPipeParameter("hough-array-nms-neighborhood-size")
    @AddJIPipeExpressionParameterVariable(name = "Hough array", description = "The hough array as list of values", key = "H")
    @AddJIPipeExpressionParameterVariable(name = "Hough array width", description = "The hough array as list of values", key = "H.width")
    @AddJIPipeExpressionParameterVariable(name = "Hough array height", description = "The hough array as list of values", key = "H.height")
    public OptionalJIPipeExpressionParameter getHoughArrayNMSNeighborhoodSize() {
        return houghArrayNMSNeighborhoodSize;
    }

    @JIPipeParameter("hough-array-nms-neighborhood-size")
    public void setHoughArrayNMSNeighborhoodSize(OptionalJIPipeExpressionParameter houghArrayNMSNeighborhoodSize) {
        this.houghArrayNMSNeighborhoodSize = houghArrayNMSNeighborhoodSize;
    }

    @SetJIPipeDocumentation(name = "Neighborhood size (I)", description = "Neighborhood size for the non-maximum suppression on the input mask. Set to zero or lower to turn off.")
    @JIPipeParameter("input-nms-neighborhood-size")
    public int getInputNMSNeighborhoodSize() {
        return inputNMSNeighborhoodSize;
    }

    @JIPipeParameter("input-nms-neighborhood-size")
    public void setInputNMSNeighborhoodSize(int inputNMSNeighborhoodSize) {
        this.inputNMSNeighborhoodSize = inputNMSNeighborhoodSize;
    }

    @SetJIPipeDocumentation(name = "Non-maximum suppression", description = "Determines the algorithm used for the non-maximum suppression. " +
            "The classic method erases peaks within the Hough array, which is fast, but may not always yield the expected results. " +
            "Alternatively, detected lines can be erased within the input mask (slower, as the Hough array needs to be regenerated)")
    @JIPipeParameter("nms-algorithm")
    public HoughLinesNMSAlgorithm getNmsAlgorithm() {
        return nmsAlgorithm;
    }

    @JIPipeParameter("nms-algorithm")
    public void setNmsAlgorithm(HoughLinesNMSAlgorithm nmsAlgorithm) {
        this.nmsAlgorithm = nmsAlgorithm;
        emitParameterUIChangedEvent();
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if(nmsAlgorithm == HoughLinesNMSAlgorithm.HoughArray) {
            if("input-nms-neighborhood-size".equals(access.getKey())) {
                return false;
            }
        }
        if(nmsAlgorithm == HoughLinesNMSAlgorithm.Input) {
            if("hough-array-nms-neighborhood-size".equals(access.getKey())) {
                return false;
            }
        }
        return super.isParameterUIVisible(tree, access);
    }


    @SetJIPipeDocumentation(name = "Peak threshold", description = "Threshold for the Hough array H peak detection. Defaults to 0.5 * MAX(H).")
    @JIPipeParameter("peak-threshold")
    @AddJIPipeExpressionParameterVariable(name = "Hough array", description = "The hough array as list of values", key = "H")
    @AddJIPipeExpressionParameterVariable(name = "Hough array width", description = "The hough array as list of values", key = "H.width")
    @AddJIPipeExpressionParameterVariable(name = "Hough array height", description = "The hough array as list of values", key = "H.height")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getPeakThreshold() {
        return peakThreshold;
    }

    @JIPipeParameter("peak-threshold")
    public void setPeakThreshold(JIPipeExpressionParameter peakThreshold) {
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

    @SetJIPipeDocumentation(name = "Fill gap", description = "Distance between two line segments associated with the same Hough transform bin")
    @JIPipeParameter("fill-gap")
    public double getFillGap() {
        return fillGap;
    }

    @JIPipeParameter("fill-gap")
    public void setFillGap(double fillGap) {
        this.fillGap = fillGap;
    }

    @SetJIPipeDocumentation(name = "Minimum length", description = "Minimum line length")
    @JIPipeParameter("min-length")
    public double getMinLength() {
        return minLength;
    }

    @JIPipeParameter("min-length")
    public void setMinLength(double minLength) {
        this.minLength = minLength;
    }

    @SetJIPipeDocumentation(name = "Thetas", description = "Expression that generates the tested theta angles.")
    @JIPipeParameter("thetas")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getThetas() {
        return thetas;
    }

    @JIPipeParameter("thetas")
    public void setThetas(JIPipeExpressionParameter thetas) {
        this.thetas = thetas;
    }

    @SetJIPipeDocumentation(name = "Fast line segment detection", description = "If enabled, use the classical algorithm for line segment detection that first sorts all points and proceeds to iteratively build each segment. " +
            "This algorithm may not always have the expected results for all line configurations. If disabled, use a more advanced algorithm based on graph components.")
    @JIPipeParameter("fast-segment-detection")
    public boolean isFastSegmentDetection() {
        return fastSegmentDetection;
    }

    @JIPipeParameter("fast-segment-detection")
    public void setFastSegmentDetection(boolean fastSegmentDetection) {
        this.fastSegmentDetection = fastSegmentDetection;
    }
}
