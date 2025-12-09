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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoi;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dRelationMeasurement;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dRelationMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dRelationMeasurementExpressionParameterVariablesInfo2;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dRelationMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Filter IJ3D ROI by overlap", description = "Filters the 3D ROI lists by testing for mutual overlap. The ROI1 output contains all ROI1 input ROI that overlap with any of ROI2. " +
        "The ROI2 output contains all ROI2 input ROI that overlap with a ROI1 ROI.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "Candidates", description = "The ROIs that are filtered", create = true)
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "Filters", description = "The ROIs that are used as filters", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "The reference image for measurements", create = true, optional = true)
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Matched", description = "The candidates that match the filter")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Rejected", description = "The candidates that do not match the filter")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Filters", description = "The remaining filter ROIs. Will be different if 'Consume on overlap' is enabled.")
public class FilterRoi3dByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    public static final JIPipeDataSlotInfo SLOT_OUTPUT_MATCHED = JIPipeDataSlotInfo.builder().name("Matched").description("The candidates that match the filter")
            .slotType(JIPipeSlotType.Output).dataClass(Ij3dSuiteRoiListData.class).build();
    public static final JIPipeDataSlotInfo SLOT_OUTPUT_REJECTED = JIPipeDataSlotInfo.builder().name("Rejected").description("The candidates that do not match the filter")
            .slotType(JIPipeSlotType.Output).dataClass(Ij3dSuiteRoiListData.class).build();
    public static final JIPipeDataSlotInfo SLOT_OUTPUT_FILTERS = JIPipeDataSlotInfo.builder().name("Filters").description("The remaining filter ROIs. Will be different if 'Consume on overlap' is enabled.")
            .slotType(JIPipeSlotType.Output).dataClass(Ij3dSuiteRoiListData.class).build();

    private ColocalizationMode colocalizationMode = ColocalizationMode.Precise;
    private final MeasurementParameters measurementParameters;
    private final OutputParameters outputParameters;

    private boolean ignoreC = true;
    private boolean ignoreT = true;
    private boolean consumeOnOverlap = false;

    private OptionalJIPipeExpressionParameter overlapCondition = new OptionalJIPipeExpressionParameter(false, "Colocalization > 10");
    private OptionalJIPipeExpressionParameter matchCondition = new OptionalJIPipeExpressionParameter(false, "numMatches >= 5");

    public FilterRoi3dByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.measurementParameters = new MeasurementParameters();
        this.outputParameters = new OutputParameters();

        registerSubParameters(measurementParameters, outputParameters);
        updateSlots();
    }

    public FilterRoi3dByOverlapAlgorithm(FilterRoi3dByOverlapAlgorithm other) {
        super(other);
        this.measurementParameters = new MeasurementParameters(other.measurementParameters);
        this.outputParameters = new OutputParameters(other.outputParameters);
        this.ignoreC = other.ignoreC;
        this.ignoreT = other.ignoreT;
        this.overlapCondition = new OptionalJIPipeExpressionParameter(other.overlapCondition);
        this.matchCondition = new OptionalJIPipeExpressionParameter(other.matchCondition);
        this.colocalizationMode = other.colocalizationMode;
        this.consumeOnOverlap = other.consumeOnOverlap;

        registerSubParameters(measurementParameters, outputParameters);
        updateSlots();
    }

    private void updateSlots() {
        toggleSlot(SLOT_OUTPUT_MATCHED, outputParameters.outputMatched);
        toggleSlot(SLOT_OUTPUT_FILTERS, outputParameters.outputFilters);
        toggleSlot(SLOT_OUTPUT_REJECTED, outputParameters.outputRejected);
    }

    @SetJIPipeDocumentation(name = "Require colocalization", description = "Determines if ROIs are pre-filtered by their colocalization, which can be fast (using bounding boxes/spheres) or precise (voxel).")
    @JIPipeParameter("colocalization-mode")
    public ColocalizationMode getColocalizationMode() {
        return colocalizationMode;
    }

    @JIPipeParameter("colocalization-mode")
    public void setColocalizationMode(ColocalizationMode colocalizationMode) {
        this.colocalizationMode = colocalizationMode;
    }

    @SetJIPipeDocumentation(name = "Overlap condition", description = "Optional expression that allows to customize what is considered an overlap. If the expression returns true, it the tested candidate-filter-overlap triplet is considered an overlap." +
            " If the expression is disabled, any overlap is seen as overlap.")
    @JIPipeParameter(value = "overlap-condition", uiOrder = -90)
    @JIPipeExpressionParameterSettings(hint = "per overlapping ROI")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = Roi3dRelationMeasurementExpressionParameterVariablesInfo2.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getOverlapCondition() {
        return overlapCondition;
    }

    @JIPipeParameter("overlap-condition")
    public void setOverlapCondition(OptionalJIPipeExpressionParameter overlapCondition) {
        this.overlapCondition = overlapCondition;
    }

    @SetJIPipeDocumentation(name = "Match condition", description = "Optional expression that allows to customize what is considered a candidate match, for example by requiring a minimum number of overlaps. " +
            "If the expression is disabled, at least one overlapping filter is required for a match")
    @JIPipeParameter(value = "match-condition", uiOrder = -80)
    @JIPipeExpressionParameterSettings(hint = "per candidate")
//    @AddJIPipeExpressionParameterVariable(fromClass = RoiOverlapMatchStatisticsVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "numMatches", name = "Number of matching filters", description = "The number of matching filters for this candidate")
    public OptionalJIPipeExpressionParameter getMatchCondition() {
        return matchCondition;
    }

    @JIPipeParameter("match-condition")
    public void setMatchCondition(OptionalJIPipeExpressionParameter matchCondition) {
        this.matchCondition = matchCondition;
    }

    @SetJIPipeDocumentation(name = "Consume on overlap", description = "If enabled, ROI are consumed if an overlap is detected, meaning " +
            "that no other tested ROI can overlap with it. This is useful if you want to prevent duplicate overlaps (e.g., if you " +
            "compare automated vs manually segmented blobs).")
    @JIPipeParameter("consume-overlap")
    public boolean isConsumeOnOverlap() {
        return consumeOnOverlap;
    }

    @JIPipeParameter("consume-overlap")
    public void setConsumeOnOverlap(boolean consumeOnOverlap) {
        this.consumeOnOverlap = consumeOnOverlap;
    }


    @SetJIPipeDocumentation(name = "Outputs", description = "Allows to control the outputs of the node")
    @JIPipeParameter("output-parameters")
    public OutputParameters getOutputParameters() {
        return outputParameters;
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "Allows to control measurement-related settings")
    @JIPipeParameter("measurement-parameters")
    public MeasurementParameters getMeasurementParameters() {
        return measurementParameters;
    }

    @SetJIPipeDocumentation(name = "Ignore channel", description = "If enabled, ROI located at different channels are compared")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @SetJIPipeDocumentation(name = "Ignore frame", description = "If enabled, ROI located at different frames are compared")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }


    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if (event.getSource() == outputParameters) {
            updateSlots();
        }
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Ij3dSuiteRoiListData candidatesOriginal = iterationStep.getInputData("Candidates", Ij3dSuiteRoiListData.class, progressInfo);
        Ij3dSuiteRoiListData filtersOriginal = iterationStep.getInputData("Filters", Ij3dSuiteRoiListData.class, progressInfo);
        ImageHandler imageHandler = IJ3DUtils.wrapImage(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        Ij3dSuiteRoiListData candidatesCopy = new Ij3dSuiteRoiListData();
        Ij3dSuiteRoiListData filtersCopy = new Ij3dSuiteRoiListData();
        candidatesCopy.addAll(candidatesOriginal);
        filtersCopy.addAll(filtersOriginal);

        // Optimize if we are only interested in colocalization
        int relationMeasurements = measurementParameters.overlapFilterMeasurements.getNativeValue();
        if(!overlapCondition.isEnabled()) {
            relationMeasurements = Roi3dRelationMeasurement.Colocalization.getNativeValue();
        }

        // ROI relation measurements
        ResultsTableData measurements = new ResultsTableData();
        IJ3DUtils.measureRoi3dRelation(imageHandler,
                candidatesCopy,
                filtersCopy,
                relationMeasurements,
                measurementParameters.measurePhysicalSizes,
                colocalizationMode == ColocalizationMode.Fast || colocalizationMode == ColocalizationMode.Precise,
                colocalizationMode == ColocalizationMode.Precise,
                ignoreC,
                ignoreT,
                "",
                measurements,
                progressInfo.resolve("Measure Overlaps"));

        // Find overlaps
        progressInfo.log("Processing measurements ...");
        Multimap<Integer, Integer> candidatesToOverlappingFiltersMap = HashMultimap.create();

        for (int row = 0; row < measurements.getRowCount(); row++) {

            progressInfo.cancellationCheck();

            int roi1Index = (int) measurements.getValueAsDouble(row, "Current.Index");
            int roi2Index = (int) measurements.getValueAsDouble(row, "Other.Index");

            if (overlapCondition.isEnabled()) {
                // Do a full test
                for (int col = 0; col < measurements.getColumnCount(); col++) {
                    variables.set(Roi3dRelationMeasurementExpressionParameterVariablesInfo2.translateColumnName(measurements.getColumnName(col)), measurements.getValueAt(row, col));
                }
                if (overlapCondition.getContent().test(variables)) {
                    candidatesToOverlappingFiltersMap.put(roi1Index, roi2Index);
                }
            } else {
                // Check for overlap (= colocalization)
                if (colocalizationMode == ColocalizationMode.Precise || colocalizationMode == ColocalizationMode.Fast) {
                    // Already fulfilled (coloc. test)
                    candidatesToOverlappingFiltersMap.put(roi1Index, roi2Index);
                } else if (measurements.containsColumn("Colocalization")) {
                    if (measurements.getValueAsDouble(row, "Colocalization") > 0) {
                        candidatesToOverlappingFiltersMap.put(roi1Index, roi2Index);
                    }
                } else {
                    Ij3dSuiteRoi roi1 = candidatesCopy.get(roi1Index);
                    Ij3dSuiteRoi roi2 = filtersCopy.get((int) measurements.getValueAsDouble(row, "Roi2.Index"));
                    if (roi1.getObject3D().hasOneVoxelColoc(roi2.getObject3D())) {
                        candidatesToOverlappingFiltersMap.put(roi1Index, roi2Index);
                    }
                }
            }
        }

        // Find matches and classify
        Ij3dSuiteRoiListData matched = new Ij3dSuiteRoiListData();
        Ij3dSuiteRoiListData rejected = new Ij3dSuiteRoiListData();

        TIntSet consumedFilters = new TIntHashSet();
        for (int i = 0; i < candidatesCopy.size(); i++) {

            progressInfo.cancellationCheck();

            TIntSet overlappingFilters = new TIntHashSet(candidatesToOverlappingFiltersMap.get(i));
            if (consumeOnOverlap) {
                overlappingFilters.removeAll(consumedFilters);
            }

            boolean matches;
            if(matchCondition.isEnabled()) {
                variables.put("numMatches", overlappingFilters.size());
                matches = matchCondition.getContent().test(variables);
            }
            else {
                matches = !overlappingFilters.isEmpty();
            }

            if (consumeOnOverlap && matches && !overlappingFilters.isEmpty()) {
                consumedFilters.add(overlappingFilters.iterator().next());
            }
            if(matches) {
                matched.add(candidatesCopy.get(i));
            }
            else {
                rejected.add(candidatesCopy.get(i));
            }
        }

        if(outputParameters.outputMatched) {
            iterationStep.addOutputData("Matched", matched, progressInfo);
        }
        if(outputParameters.outputRejected) {
            iterationStep.addOutputData("Rejected", rejected, progressInfo);
        }

        // Output filters
        if(outputParameters.outputFilters) {
            Ij3dSuiteRoiListData finalFilters = new Ij3dSuiteRoiListData();
            for (int i = 0; i < filtersCopy.size(); i++) {
                if(!consumedFilters.contains(i)) {
                    finalFilters.add(filtersCopy.get(i));
                }
            }
            iterationStep.addOutputData("Filters", finalFilters, progressInfo);
        }
    }


    public enum ColocalizationMode {
        None("No colocalization"),
        Fast("Fast colocalization (inaccurate)"),
        Precise("Precise colocalization (slow)");

        private final String label;

        ColocalizationMode(String label) {
            this.label = label;
        }


        @Override
        public String toString() {
            return label;
        }
    }

    public static class MeasurementParameters extends AbstractJIPipeParameterCollection {
        private Roi3dRelationMeasurementSetParameter overlapFilterMeasurements = new Roi3dRelationMeasurementSetParameter();
        private boolean measurePhysicalSizes = false;

        public MeasurementParameters() {
        }

        public MeasurementParameters(MeasurementParameters other) {
            this.overlapFilterMeasurements = new Roi3dRelationMeasurementSetParameter(other.overlapFilterMeasurements);
            this.measurePhysicalSizes = other.measurePhysicalSizes;
        }

        @SetJIPipeDocumentation(name = "Overlap filter measurements", description = "Measurements extracted for the overlap filter.")
        @JIPipeParameter("overlap-filter-measurements")
        public Roi3dRelationMeasurementSetParameter getOverlapFilterMeasurements() {
            return overlapFilterMeasurements;
        }

        @JIPipeParameter("overlap-filter-measurements")
        public void setOverlapFilterMeasurements(Roi3dRelationMeasurementSetParameter overlapFilterMeasurements) {
            this.overlapFilterMeasurements = overlapFilterMeasurements;
        }

        @SetJIPipeDocumentation(name = "Measure physical sizes", description = "Measure physical sizes if available")
        @JIPipeParameter("measure-physical-sizes")
        public boolean isMeasurePhysicalSizes() {
            return measurePhysicalSizes;
        }

        @JIPipeParameter("measure-physical-sizes")
        public void setMeasurePhysicalSizes(boolean measurePhysicalSizes) {
            this.measurePhysicalSizes = measurePhysicalSizes;
        }
    }

    public static class OutputParameters extends AbstractJIPipeParameterCollection {
        private boolean outputMatched = true;
        private boolean outputRejected = true;
        private boolean outputFilters = false;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
            this.outputMatched = other.outputMatched;
            this.outputRejected = other.outputRejected;
            this.outputFilters = other.outputFilters;
        }

        @SetJIPipeDocumentation(name = "Output matches", description = "If enabled, output all matched candidates")
        @JIPipeParameter("output-matched")
        public boolean isOutputMatched() {
            return outputMatched;
        }

        @JIPipeParameter("output-matched")
        public void setOutputMatched(boolean outputMatched) {
            this.outputMatched = outputMatched;
        }

        @SetJIPipeDocumentation(name = "Output rejections", description = "If enabled, output all rejected candidates")
        @JIPipeParameter("output-rejected")
        public boolean isOutputRejected() {
            return outputRejected;
        }

        @JIPipeParameter("output-rejected")
        public void setOutputRejected(boolean outputRejected) {
            this.outputRejected = outputRejected;
        }

        @SetJIPipeDocumentation(name = "Output modified filters", description = "If enabled, output the filter ROIs. Will be different from the input filters if 'Consume on overlap' is enabled")
        @JIPipeParameter("output-filters")
        public boolean isOutputFilters() {
            return outputFilters;
        }

        @JIPipeParameter("output-filters")
        public void setOutputFilters(boolean outputFilters) {
            this.outputFilters = outputFilters;
        }
    }
}
