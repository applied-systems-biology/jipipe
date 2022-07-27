package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.iterative;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.AnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.analyze.FindParticles2D;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.measure.RoiStatisticsAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.ThresholdsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@JIPipeDocumentation(name = "Object-based iterative thresholding 2D", description = "Applies a ROI detection and measurement for each threshold value. Based on the user-provided ROI criteria the threshold is either accepted or rejected. " +
        "Returns the mask with the first applicable threshold or an image with zero values. "
+"If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nIterative")
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", description = "The image to be thresholded", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Reference", description = "An optional reference image for the ROI statistics. If none is provided, the input image is used as reference.", optional = true, autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", description = "The generated mask", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", description = "Pre-filtered ROI (according to the criteria)", autoCreate = true)
public class IterativeThresholdByROIStatistics2DAlgorithm extends JIPipeIteratingAlgorithm {

    private IntegerRange thresholds = new IntegerRange("0-255");
    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private final CustomExpressionVariablesParameter customFilterVariables;
    private boolean measureInPhysicalUnits = true;
    private final RoiStatisticsAlgorithm roiStatisticsAlgorithm =
            JIPipe.createNode(RoiStatisticsAlgorithm.class);

    private final FindParticles2D findParticles2DAlgorithm = JIPipe.createNode(FindParticles2D.class);

    private OptionalAnnotationNameParameter thresholdAnnotation = new OptionalAnnotationNameParameter("Threshold", true);

    private DefaultExpressionParameter thresholdCombinationExpression = new DefaultExpressionParameter("MIN(thresholds)");

    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    private boolean excludeEdgeROIs = false;

    private final FilteringParameters filteringParameters;

    private final ScoreParameters scoreParameters;

    public IterativeThresholdByROIStatistics2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customFilterVariables = new CustomExpressionVariablesParameter(this);
        this.filteringParameters = new FilteringParameters();
        this.scoreParameters = new ScoreParameters();
        registerSubParameter(filteringParameters);
        registerSubParameter(scoreParameters);
    }

    public IterativeThresholdByROIStatistics2DAlgorithm(IterativeThresholdByROIStatistics2DAlgorithm other) {
        super(other);
        this.measurements = other.measurements;
        this.customFilterVariables = new CustomExpressionVariablesParameter(other.customFilterVariables, this);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        this.thresholdAnnotation = new OptionalAnnotationNameParameter(other.thresholdAnnotation);
        this.thresholdCombinationExpression = new DefaultExpressionParameter(other.thresholdCombinationExpression);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
        this.excludeEdgeROIs = other.excludeEdgeROIs;
        this.thresholds = new IntegerRange(other.thresholds);
        this.filteringParameters = new FilteringParameters(other.filteringParameters);
        this.scoreParameters = new ScoreParameters(other.scoreParameters);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {

        // Set parameters of find particles
        findParticles2DAlgorithm.clearSlotData();
        findParticles2DAlgorithm.setAllSlotsVirtual(false, false, progressInfo);
        findParticles2DAlgorithm.setExcludeEdges(excludeEdgeROIs);

        // Set parameters of ROI statistics algorithm
        roiStatisticsAlgorithm.clearSlotData();
        roiStatisticsAlgorithm.setAllSlotsVirtual(false, false, progressInfo);
        roiStatisticsAlgorithm.setMeasurements(measurements);
        roiStatisticsAlgorithm.setMeasureInPhysicalUnits(measureInPhysicalUnits);

        // Continue with run
        super.run(progressInfo);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData("Input", ImagePlusGreyscale8UData.class, progressInfo).getImage();
        ImagePlus referenceImage;
        {
            ImagePlusGreyscaleData referenceData = dataBatch.getInputData("Reference", ImagePlusGreyscaleData.class, progressInfo);
            if(referenceData != null) {
                referenceImage = referenceData.getImage();
            }
            else {
                referenceImage = inputImage;
            }
        }

        // Generate thresholds
        List<Integer> thresholds;
        {
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAnnotations(dataBatch.getMergedTextAnnotations());
            customFilterVariables.writeToVariables(variables, true, "custom.", true, "custom");
            thresholds = this.thresholds.getIntegers(0, 255, variables);
        }

        // Generate variables
        ExpressionVariables roiFilterVariables = new ExpressionVariables();
        ExpressionVariables thresholdCriteriaVariables = new ExpressionVariables();
        ExpressionVariables accumulationVariables = new ExpressionVariables();

        roiFilterVariables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(roiFilterVariables, true, "custom.", true, "custom");

        thresholdCriteriaVariables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(thresholdCriteriaVariables, true, "custom.", true, "custom");

        accumulationVariables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(accumulationVariables, true, "custom.", true, "custom");

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
                ThresholdingResult result = applyThreshold(inputIp, referenceIp, threshold, roiFilterVariables, thresholdCriteriaVariables, accumulationVariables, thresholdProgress);
                if(result != null) {
                    results.add(result);
                    if(!optimize)
                        break;
                }
            }
            progressInfo.resolve(index.toString()).log("Detected " + results.size() + " candidates:");
            for (ThresholdingResult result : results) {
                progressInfo.resolve(index.toString()).log(" - threshold=" + result.threshold + " --> score=" + result.score);
            }

            if(results.isEmpty()) {
                outputStack.setProcessor(new ByteProcessor(inputIp.getWidth(), inputIp.getHeight()), index.oneSliceIndexToOneStackIndex(inputImage));
            }
            else {
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
        if(!detectedThresholds.isEmpty()) {
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAnnotations(dataBatch.getMergedTextAnnotations());
            customFilterVariables.writeToVariables(variables, true, "custom.", true, "custom");
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
        dataBatch.addOutputData("Mask", new ImagePlusGreyscaleMaskData(outputMask), annotations, thresholdAnnotationStrategy, progressInfo);
        dataBatch.addOutputData("ROI", outputROI, annotations, thresholdAnnotationStrategy, progressInfo);
    }

    private ThresholdingResult applyThreshold(ImageProcessor inputIp, ImageProcessor referenceIp, int threshold, ExpressionVariables roiFilterVariables, ExpressionVariables thresholdCriteriaVariables, ExpressionVariables accumulationVariables, JIPipeProgressInfo progressInfo) {
        ImagePlus inputReference = new ImagePlus("reference", referenceIp);

        // Calculate mask
        ImageProcessor maskIp = inputIp.duplicate();
        maskIp.threshold(threshold);
        ImagePlus maskImage = new ImagePlus("mask", maskIp);

        // Detect ROI
        ROIListData rois;
        {
            findParticles2DAlgorithm.clearSlotData();
            findParticles2DAlgorithm.getInputSlot("Mask").addData(new ImagePlusGreyscaleMaskData(maskImage), progressInfo);
            findParticles2DAlgorithm.run(progressInfo);
            rois = findParticles2DAlgorithm.getFirstOutputSlot().getData(0, ROIListData.class, progressInfo);
            findParticles2DAlgorithm.clearSlotData();
        }

        // Filter ROI
        ROIListData filteredRois = new ROIListData();
        List<Double> scores = new ArrayList<>();
        if(!rois.isEmpty()) {
            roiStatisticsAlgorithm.clearSlotData();
            roiStatisticsAlgorithm.getInputSlot("ROI").addData(rois, progressInfo);
            roiStatisticsAlgorithm.getInputSlot("Reference").addData(new ImagePlusGreyscaleData(inputReference), progressInfo);
            roiStatisticsAlgorithm.run(progressInfo);
            ResultsTableData statistics = roiStatisticsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);

            // Apply filter
            for (int row = 0; row < statistics.getRowCount(); row++) {
                for (int col = 0; col < statistics.getColumnCount(); col++) {
                    roiFilterVariables.set(statistics.getColumnName(col), statistics.getValueAt(row, col));
                }
                if (filteringParameters.roiFilterExpression.test(roiFilterVariables)) {
                    filteredRois.add(rois.get(row));

                    // Apply scoring
                    if(scoreParameters.scoreExpression.isEnabled()) {
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

            if(!filteringParameters.thresholdCriteriaExpression.test(thresholdCriteriaVariables)) {
                progressInfo.log("Criteria not matched.");
                return null;
            }
        }

        // Accumulate scores
        double score = 0;
        if(scoreParameters.scoreExpression.isEnabled()) {
            accumulationVariables.set("scores", scores);
            score = scoreParameters.scoreAccumulationExpression.evaluateToDouble(accumulationVariables);
            progressInfo.log("Accumulated score for threshold " + threshold + " is " + score);
        }

        return new ThresholdingResult(maskIp, filteredRois, threshold, score);
    }

    @JIPipeDocumentation(name = "Custom filter variables", description = "Here you can add parameters that will be included into the filter as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomFilterVariables() {
        return customFilterVariables;
    }

    @JIPipeDocumentation(name = "Measurements", description = "The measurements to calculate.")
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Thresholds", description = "Determines which thresholds should be tested")
    @JIPipeParameter("thresholds")
    public IntegerRange getThresholds() {
        return thresholds;
    }

    @JIPipeParameter("thresholds")
    public void setThresholds(IntegerRange thresholds) {
        this.thresholds = thresholds;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @JIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }

    @JIPipeDocumentation(name = "Threshold combination function", description = "This expression combines multiple thresholds into one numeric threshold.")
    @ExpressionParameterSettings(variableSource = ThresholdsExpressionParameterVariableSource.class)
    @JIPipeParameter("threshold-combine-expression")
    public DefaultExpressionParameter getThresholdCombinationExpression() {
        return thresholdCombinationExpression;
    }

    @JIPipeParameter("threshold-combine-expression")
    public void setThresholdCombinationExpression(DefaultExpressionParameter thresholdCombinationExpression) {
        this.thresholdCombinationExpression = thresholdCombinationExpression;
    }

    @JIPipeDocumentation(name = "Threshold annotation", description = "Puts the generated threshold(s) into an annotation.")
    @JIPipeParameter("threshold-annotation")
    public OptionalAnnotationNameParameter getThresholdAnnotation() {
        return thresholdAnnotation;
    }

    @JIPipeParameter("threshold-annotation")
    public void setThresholdAnnotation(OptionalAnnotationNameParameter thresholdAnnotation) {
        this.thresholdAnnotation = thresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Exclude edge ROI", description = "If enabled, edge ROI are filtered out")
    @JIPipeParameter("exclude-edge-roi")
    public boolean isExcludeEdgeROIs() {
        return excludeEdgeROIs;
    }

    @JIPipeParameter("exclude-edge-roi")
    public void setExcludeEdgeROIs(boolean excludeEdgeROIs) {
        this.excludeEdgeROIs = excludeEdgeROIs;
    }

    @JIPipeDocumentation(name = "Threshold filtering", description = "The following parameters determine how ROI and thresholds are selected.")
    @JIPipeParameter("filtering")
    public FilteringParameters getFilteringParameters() {
        return filteringParameters;
    }

    @JIPipeDocumentation(name = "Scoring", description = "The following parameters allow to find optimal thresholds based on scoring. Please note that the algorithm will select the result with the maximum score.")
    @JIPipeParameter("scoring")
    public ScoreParameters getScoreParameters() {
        return scoreParameters;
    }

    public static class FilteringParameters extends AbstractJIPipeParameterCollection {
        private DefaultExpressionParameter roiFilterExpression = new DefaultExpressionParameter("Area > 200");
        private DefaultExpressionParameter thresholdCriteriaExpression = new DefaultExpressionParameter("LENGTH(filtered_roi) >= 1");

        public FilteringParameters() {
        }

        public FilteringParameters(FilteringParameters other) {
            this.roiFilterExpression = other.roiFilterExpression;
            this.thresholdCriteriaExpression = other.thresholdCriteriaExpression;
        }

        @JIPipeDocumentation(name = "ROI filter", description = "This expression is applied for each ROI and determines whether the ROI fulfills the required criteria.")
        @ExpressionParameterSettings(variableSource = MeasurementExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom filter variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @JIPipeParameter(value = "roi-filter", important = true)
        public DefaultExpressionParameter getRoiFilterExpression() {
            return roiFilterExpression;
        }

        @JIPipeParameter("roi-filter")
        public void setRoiFilterExpression(DefaultExpressionParameter roiFilterExpression) {
            this.roiFilterExpression = roiFilterExpression;
        }

        @JIPipeDocumentation(name = "Threshold criteria", description = "This expression is applied for each threshold after the ROI were filtered.")
        @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(key = "all_roi", name = "All ROI", description = "A list of all ROI that were detected in this iteration")
        @ExpressionParameterSettingsVariable(key = "filtered_roi", name = "Filtered ROI", description = "A list of all filtered ROI that were detected in this iteration")
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom filter variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @JIPipeParameter(value = "threshold-criteria", important = true)
        public DefaultExpressionParameter getThresholdCriteriaExpression() {
            return thresholdCriteriaExpression;
        }

        @JIPipeParameter("threshold-criteria")
        public void setThresholdCriteriaExpression(DefaultExpressionParameter thresholdCriteriaExpression) {
            this.thresholdCriteriaExpression = thresholdCriteriaExpression;
        }
    }

    public static class ScoreParameters extends AbstractJIPipeParameterCollection {

        private OptionalDefaultExpressionParameter scoreExpression = new OptionalDefaultExpressionParameter();

        private DefaultExpressionParameter scoreAccumulationExpression = new DefaultExpressionParameter("SUM(scores)");

        public ScoreParameters() {
        }

        public ScoreParameters(ScoreParameters other) {
            this.scoreExpression = new OptionalDefaultExpressionParameter(other.scoreExpression);
            this.scoreAccumulationExpression = new DefaultExpressionParameter(other.scoreAccumulationExpression);
        }

        @JIPipeDocumentation(name = "Score function", description = "If enabled, assigns a score to each filtered ROI that is accumulated. This score is maximized to find the best threshold. If disabled, the first threshold is applied where the criteria match.")
        @JIPipeParameter(value = "score", important = true)
        public OptionalDefaultExpressionParameter getScoreExpression() {
            return scoreExpression;
        }

        @JIPipeParameter("score")
        public void setScoreExpression(OptionalDefaultExpressionParameter scoreExpression) {
            this.scoreExpression = scoreExpression;
        }

        @JIPipeDocumentation(name = "Score accumulation function", description = "Expression that determines how the scores are accumulated")
        @JIPipeParameter("score-accumulation")
        @ExpressionParameterSettingsVariable(key = "scores", name = "List of scores", description = "The list of scores")
        public DefaultExpressionParameter getScoreAccumulationExpression() {
            return scoreAccumulationExpression;
        }

        @JIPipeParameter("score-accumulation")
        public void setScoreAccumulationExpression(DefaultExpressionParameter scoreAccumulationExpression) {
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