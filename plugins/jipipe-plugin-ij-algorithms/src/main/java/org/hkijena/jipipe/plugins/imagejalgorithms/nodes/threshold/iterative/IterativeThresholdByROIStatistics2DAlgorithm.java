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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.iterative;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.analyze.FindParticles2D;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.ThresholdsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.MeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SetJIPipeDocumentation(name = "Object-based iterative thresholding 2D", description = "Applies a ROI detection and measurement for each threshold value. Based on the user-provided ROI criteria the threshold is either accepted or rejected. " +
        "Returns the mask with the first applicable threshold or an image with zero values. "
        + "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nIterative")
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", description = "The image to be thresholded", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Reference", description = "An optional reference image for the ROI statistics. If none is provided, the input image is used as reference.", optional = true, create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", description = "The generated mask", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", description = "Pre-filtered ROI (according to the criteria)", create = true)
public class IterativeThresholdByROIStatistics2DAlgorithm extends JIPipeIteratingAlgorithm {

    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);
    private final FindParticles2D findParticles2DAlgorithm = JIPipe.createNode(FindParticles2D.class);
    private final FilteringParameters filteringParameters;
    private final ScoreParameters scoreParameters;
    private IntegerRange thresholds = new IntegerRange("0-255");
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;
    private OptionalTextAnnotationNameParameter thresholdAnnotation = new OptionalTextAnnotationNameParameter("Threshold", true);
    private JIPipeExpressionParameter thresholdCombinationExpression = new JIPipeExpressionParameter("MIN(thresholds)");
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;
    private boolean excludeEdgeROIs = false;

    public IterativeThresholdByROIStatistics2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.filteringParameters = new FilteringParameters();
        this.scoreParameters = new ScoreParameters();
        registerSubParameter(filteringParameters);
        registerSubParameter(scoreParameters);
    }

    public IterativeThresholdByROIStatistics2DAlgorithm(IterativeThresholdByROIStatistics2DAlgorithm other) {
        super(other);
        this.measurements = other.measurements;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.thresholdAnnotation = new OptionalTextAnnotationNameParameter(other.thresholdAnnotation);
        this.thresholdCombinationExpression = new JIPipeExpressionParameter(other.thresholdCombinationExpression);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
        this.excludeEdgeROIs = other.excludeEdgeROIs;
        this.thresholds = new IntegerRange(other.thresholds);
        this.filteringParameters = new FilteringParameters(other.filteringParameters);
        this.scoreParameters = new ScoreParameters(other.scoreParameters);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Set parameters of find particles
        findParticles2DAlgorithm.clearSlotData(false, progressInfo);
        findParticles2DAlgorithm.setExcludeEdges(excludeEdgeROIs);

        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.clearSlotData(false, progressInfo);
        roiStatisticsAlgorithm.setMeasurements(measurements);
        roiStatisticsAlgorithm.setMeasureInPhysicalUnits(measureInPhysicalUnits);

        // Continue with run
        super.run(runContext, progressInfo);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData("Input", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        ImagePlus referenceImage;
        {
            ImagePlusGreyscaleData referenceData = iterationStep.getInputData("Reference", ImagePlusGreyscaleData.class, progressInfo);
            if (referenceData != null) {
                referenceImage = referenceData.getImage();
            } else {
                referenceImage = inputImage;
            }
        }

        // Generate thresholds
        List<Integer> thresholds;
        {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            getDefaultCustomExpressionVariables().writeToVariables(variables);
            thresholds = this.thresholds.getIntegers(0, 255, variables);
        }

        // Generate variables
        JIPipeExpressionVariablesMap roiFilterVariables = new JIPipeExpressionVariablesMap();
        JIPipeExpressionVariablesMap thresholdCriteriaVariables = new JIPipeExpressionVariablesMap();
        JIPipeExpressionVariablesMap accumulationVariables = new JIPipeExpressionVariablesMap();

        roiFilterVariables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(roiFilterVariables);

        thresholdCriteriaVariables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(thresholdCriteriaVariables);

        accumulationVariables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(accumulationVariables);

        // Do we optimize?
        boolean optimize = scoreParameters.scoreExpression.isEnabled();

        // Apply thresholding
        List<Integer> detectedThresholds = new ArrayList<>();
        ROIListData outputROI = new ROIListData();
        ImageStack outputStack = new ImageStack(inputImage.getWidth(), inputImage.getHeight(), inputImage.getStackSize());
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (inputIp, index) -> {
            ImageProcessor referenceIp = ImageJUtils.getClosestSliceZero(referenceImage, index);
            List<ThresholdingResult> results = new ArrayList<>();
            for (int i = 0; i < thresholds.size(); i++) {
                int threshold = thresholds.get(i);
                JIPipeProgressInfo thresholdProgress = progressInfo.resolve(index.toString()).resolveAndLog("Threshold " + threshold, i, thresholds.size());
                ThresholdingResult result = applyThreshold(inputIp, referenceIp, threshold, roiFilterVariables, thresholdCriteriaVariables, accumulationVariables, runContext, thresholdProgress);
                if (result != null) {
                    results.add(result);
                    if (!optimize)
                        break;
                }
            }
            progressInfo.resolve(index.toString()).log("Detected " + results.size() + " candidates:");
            for (ThresholdingResult result : results) {
                progressInfo.resolve(index.toString()).log(" - threshold=" + result.threshold + " --> score=" + result.score);
            }

            if (results.isEmpty()) {
                outputStack.setProcessor(new ByteProcessor(inputIp.getWidth(), inputIp.getHeight()), index.zeroSliceIndexToOneStackIndex(inputImage));
            } else {
                ThresholdingResult bestResult = results.stream().max(Comparator.comparing(ThresholdingResult::getScore)).get();
                detectedThresholds.add(bestResult.getThreshold());
                outputStack.setProcessor(bestResult.mask, index.zeroSliceIndexToOneStackIndex(inputImage));
                for (Roi rois : bestResult.rois) {
                    rois.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                }
                outputROI.addAll(bestResult.rois);
            }

        }, progressInfo);

        // Combine thresholds
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        if (!detectedThresholds.isEmpty()) {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            getDefaultCustomExpressionVariables().writeToVariables(variables);
            variables.set("thresholds", detectedThresholds);
            Number combined = (Number) thresholdCombinationExpression.evaluate(variables);
            int threshold = Math.min(255, Math.max(0, combined.intValue()));
            if (thresholdAnnotation.isEnabled()) {
                annotations.add(thresholdAnnotation.createAnnotation("" + threshold));
            }
        }

        // Generate output
        ImagePlus outputMask = new ImagePlus(inputImage.getTitle() + " [Mask]", outputStack);
        ImageJUtils.copyHyperstackDimensions(inputImage, outputMask);

        // Write to output
        iterationStep.addOutputData("Mask", new ImagePlusGreyscaleMaskData(outputMask), annotations, thresholdAnnotationStrategy, progressInfo);
        iterationStep.addOutputData("ROI", outputROI, annotations, thresholdAnnotationStrategy, progressInfo);
    }

    private ThresholdingResult applyThreshold(ImageProcessor inputIp, ImageProcessor referenceIp, int threshold, JIPipeExpressionVariablesMap roiFilterVariables, JIPipeExpressionVariablesMap thresholdCriteriaVariables, JIPipeExpressionVariablesMap accumulationVariables, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputReference = new ImagePlus("reference", referenceIp);

        // Calculate mask
        ImageProcessor maskIp = inputIp.duplicate();
        maskIp.threshold(threshold);
        ImagePlus maskImage = new ImagePlus("mask", maskIp);

        // Detect ROI
        ROIListData rois;
        {
            findParticles2DAlgorithm.clearSlotData(false, progressInfo);
            findParticles2DAlgorithm.getInputSlot("Mask").addData(new ImagePlusGreyscaleMaskData(maskImage), progressInfo);
            findParticles2DAlgorithm.run(runContext, progressInfo);
            rois = findParticles2DAlgorithm.getFirstOutputSlot().getData(0, ROIListData.class, progressInfo);
            findParticles2DAlgorithm.clearSlotData(false, progressInfo);
        }

        // Filter ROI
        ROIListData filteredRois = new ROIListData();
        List<Double> scores = new ArrayList<>();
        if (!rois.isEmpty()) {
            roiStatisticsAlgorithm.clearSlotData(false, progressInfo);
            roiStatisticsAlgorithm.getInputSlot("ROI").addData(rois, progressInfo);
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(new ImagePlusGreyscaleData(inputReference), progressInfo);
            roiStatisticsAlgorithm.run(runContext, progressInfo);
            ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);

            // Apply filter
            for (int row = 0; row < statistics.getRowCount(); row++) {
                for (int col = 0; col < statistics.getColumnCount(); col++) {
                    roiFilterVariables.set(statistics.getColumnName(col), statistics.getValueAt(row, col));
                }
                if (filteringParameters.roiFilterExpression.test(roiFilterVariables)) {
                    filteredRois.add(rois.get(row));

                    // Apply scoring
                    if (scoreParameters.scoreExpression.isEnabled()) {
                        double score = scoreParameters.scoreExpression.getContent().evaluateToDouble(roiFilterVariables);
                        scores.add(score);
                    }
                }
            }

            progressInfo.log("Detected " + rois.size() + " ROI --> filtered to " + filteredRois.size() + " ROI");
        }

        // Apply criteria
        {
            thresholdCriteriaVariables.set("all_roi", rois);
            thresholdCriteriaVariables.set("filtered_roi", filteredRois);

            if (!filteringParameters.thresholdCriteriaExpression.test(thresholdCriteriaVariables)) {
                progressInfo.log("Criteria not matched.");
                return null;
            }
        }

        // Accumulate scores
        double score = 0;
        if (scoreParameters.scoreExpression.isEnabled()) {
            accumulationVariables.set("scores", scores);
            score = scoreParameters.scoreAccumulationExpression.evaluateToDouble(accumulationVariables);
            progressInfo.log("Accumulated score for threshold " + threshold + " is " + score);
        }

        return new ThresholdingResult(maskIp, filteredRois, threshold, score);
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @SetJIPipeDocumentation(name = "Thresholds", description = "Determines which thresholds should be tested")
    @JIPipeParameter("thresholds")
    public IntegerRange getThresholds() {
        return thresholds;
    }

    @JIPipeParameter("thresholds")
    public void setThresholds(IntegerRange thresholds) {
        this.thresholds = thresholds;
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @SetJIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }

    @SetJIPipeDocumentation(name = "Threshold combination function", description = "This expression combines multiple thresholds into one numeric threshold.")
    @JIPipeExpressionParameterSettings(variableSource = ThresholdsExpressionParameterVariablesInfo.class)
    @JIPipeParameter("threshold-combine-expression")
    public JIPipeExpressionParameter getThresholdCombinationExpression() {
        return thresholdCombinationExpression;
    }

    @JIPipeParameter("threshold-combine-expression")
    public void setThresholdCombinationExpression(JIPipeExpressionParameter thresholdCombinationExpression) {
        this.thresholdCombinationExpression = thresholdCombinationExpression;
    }

    @SetJIPipeDocumentation(name = "Threshold annotation", description = "Puts the generated threshold(s) into an annotation.")
    @JIPipeParameter("threshold-annotation")
    public OptionalTextAnnotationNameParameter getThresholdAnnotation() {
        return thresholdAnnotation;
    }

    @JIPipeParameter("threshold-annotation")
    public void setThresholdAnnotation(OptionalTextAnnotationNameParameter thresholdAnnotation) {
        this.thresholdAnnotation = thresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Exclude edge ROI", description = "If enabled, edge ROI are filtered out")
    @JIPipeParameter("exclude-edge-roi")
    public boolean isExcludeEdgeROIs() {
        return excludeEdgeROIs;
    }

    @JIPipeParameter("exclude-edge-roi")
    public void setExcludeEdgeROIs(boolean excludeEdgeROIs) {
        this.excludeEdgeROIs = excludeEdgeROIs;
    }

    @SetJIPipeDocumentation(name = "Threshold filtering", description = "The following parameters determine how ROI and thresholds are selected.")
    @JIPipeParameter("filtering")
    public FilteringParameters getFilteringParameters() {
        return filteringParameters;
    }

    @SetJIPipeDocumentation(name = "Scoring", description = "The following parameters allow to find optimal thresholds based on scoring. Please note that the algorithm will select the result with the maximum score.")
    @JIPipeParameter("scoring")
    public ScoreParameters getScoreParameters() {
        return scoreParameters;
    }

    public static class FilteringParameters extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter roiFilterExpression = new JIPipeExpressionParameter("Area > 200");
        private JIPipeExpressionParameter thresholdCriteriaExpression = new JIPipeExpressionParameter("LENGTH(filtered_roi) >= 1");

        public FilteringParameters() {
        }

        public FilteringParameters(FilteringParameters other) {
            this.roiFilterExpression = other.roiFilterExpression;
            this.thresholdCriteriaExpression = other.thresholdCriteriaExpression;
        }

        @SetJIPipeDocumentation(name = "ROI filter", description = "This expression is applied for each ROI and determines whether the ROI fulfills the required criteria.")
        @JIPipeExpressionParameterSettings(variableSource = MeasurementExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @JIPipeParameter(value = "roi-filter", important = true)
        public JIPipeExpressionParameter getRoiFilterExpression() {
            return roiFilterExpression;
        }

        @JIPipeParameter("roi-filter")
        public void setRoiFilterExpression(JIPipeExpressionParameter roiFilterExpression) {
            this.roiFilterExpression = roiFilterExpression;
        }

        @SetJIPipeDocumentation(name = "Threshold criteria", description = "This expression is applied for each threshold after the ROI were filtered.")
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(key = "all_roi", name = "All ROI", description = "A list of all ROI that were detected in this iteration")
        @JIPipeExpressionParameterVariable(key = "filtered_roi", name = "Filtered ROI", description = "A list of all filtered ROI that were detected in this iteration")
        @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @JIPipeParameter(value = "threshold-criteria", important = true)
        public JIPipeExpressionParameter getThresholdCriteriaExpression() {
            return thresholdCriteriaExpression;
        }

        @JIPipeParameter("threshold-criteria")
        public void setThresholdCriteriaExpression(JIPipeExpressionParameter thresholdCriteriaExpression) {
            this.thresholdCriteriaExpression = thresholdCriteriaExpression;
        }
    }

    public static class ScoreParameters extends AbstractJIPipeParameterCollection {

        private OptionalJIPipeExpressionParameter scoreExpression = new OptionalJIPipeExpressionParameter();

        private JIPipeExpressionParameter scoreAccumulationExpression = new JIPipeExpressionParameter("SUM(scores)");

        public ScoreParameters() {
        }

        public ScoreParameters(ScoreParameters other) {
            this.scoreExpression = new OptionalJIPipeExpressionParameter(other.scoreExpression);
            this.scoreAccumulationExpression = new JIPipeExpressionParameter(other.scoreAccumulationExpression);
        }

        @SetJIPipeDocumentation(name = "Score function", description = "If enabled, assigns a score to each filtered ROI that is accumulated. This score is maximized to find the best threshold. If disabled, the first threshold is applied where the criteria match.")
        @JIPipeParameter(value = "score", important = true)
        @JIPipeExpressionParameterVariable(fromClass = MeasurementExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        public OptionalJIPipeExpressionParameter getScoreExpression() {
            return scoreExpression;
        }

        @JIPipeParameter("score")
        public void setScoreExpression(OptionalJIPipeExpressionParameter scoreExpression) {
            this.scoreExpression = scoreExpression;
        }

        @SetJIPipeDocumentation(name = "Score accumulation function", description = "Expression that determines how the scores are accumulated")
        @JIPipeParameter("score-accumulation")
        @JIPipeExpressionParameterVariable(key = "scores", name = "List of scores", description = "The list of scores")
        public JIPipeExpressionParameter getScoreAccumulationExpression() {
            return scoreAccumulationExpression;
        }

        @JIPipeParameter("score-accumulation")
        public void setScoreAccumulationExpression(JIPipeExpressionParameter scoreAccumulationExpression) {
            this.scoreAccumulationExpression = scoreAccumulationExpression;
        }
    }

    public static class ThresholdingResult {
        private final ImageProcessor mask;
        private final ROIListData rois;

        private final int threshold;

        private final double score;

        private ThresholdingResult(ImageProcessor mask, ROIListData rois, int threshold, double score) {
            this.mask = mask;
            this.rois = rois;
            this.threshold = threshold;
            this.score = score;
        }

        public int getThreshold() {
            return threshold;
        }

        public ImageProcessor getMask() {
            return mask;
        }

        public ROIListData getRois() {
            return rois;
        }

        public double getScore() {
            return score;
        }
    }
}
