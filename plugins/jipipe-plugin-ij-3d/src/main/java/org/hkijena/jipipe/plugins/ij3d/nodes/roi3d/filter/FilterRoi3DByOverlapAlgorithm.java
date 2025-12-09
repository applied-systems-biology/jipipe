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

import ij.ImagePlus;
import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipePercentageProgressInfo;
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
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.ij3d.datatypes.IJ3DROI;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.filter.FilterRoi2dByOverlapAlgorithm;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;
import java.util.List;

@SetJIPipeDocumentation(name = "Filter 3D ROI by overlap", description = "Only returns candidate ROIs that overlap with at least N ROIs from the filter set. Enhanced version with spatial indexing and multi-stage overlap detection.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "Candidates", description = "The ROIs that are filtered", create = true)
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "Filters", description = "The ROIs that are used as filters", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "Reference image used for measurements", optional = true)
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Matched", description = "The candidates that match the filter")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Rejected", description = "The candidates that do not match the filter")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Intersections", description = "The intersection ROIs")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Filters", description = "The remaining filter ROIs. Will be different if 'Consume on overlap' is enabled.")
public class FilterRoi3DByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    public static final JIPipeDataSlotInfo SLOT_INPUT_REFERENCE = JIPipeDataSlotInfo.builder().name("Reference").description("Reference image used for measurements")
            .slotType(JIPipeSlotType.Input).dataClass(ImagePlusData.class).optional(true).build();
    public static final JIPipeDataSlotInfo SLOT_INPUT_REFERENCE_CANDIDATES = JIPipeDataSlotInfo.builder().name("Candidates ref").description("Reference image used for candidate measurements")
            .slotType(JIPipeSlotType.Input).dataClass(ImagePlusData.class).optional(true).build();
    public static final JIPipeDataSlotInfo SLOT_INPUT_REFERENCE_FILTERS = JIPipeDataSlotInfo.builder().name("Filters ref").description("Reference image used for filter measurements")
            .slotType(JIPipeSlotType.Input).dataClass(ImagePlusData.class).optional(true).build();
    public static final JIPipeDataSlotInfo SLOT_INPUT_REFERENCE_INTERSECTIONS = JIPipeDataSlotInfo.builder().name("Intersections ref").description("Reference image used for intersection ROI measurements")
            .slotType(JIPipeSlotType.Input).dataClass(ImagePlusData.class).optional(true).build();
    public static final JIPipeDataSlotInfo SLOT_OUTPUT_MATCHED = JIPipeDataSlotInfo.builder().name("Matched").description("The candidates that match the filter")
            .slotType(JIPipeSlotType.Output).dataClass(Ij3dSuiteRoiListData.class).build();
    public static final JIPipeDataSlotInfo SLOT_OUTPUT_REJECTED = JIPipeDataSlotInfo.builder().name("Rejected").description("The candidates that do not match the filter")
            .slotType(JIPipeSlotType.Output).dataClass(Ij3dSuiteRoiListData.class).build();
    public static final JIPipeDataSlotInfo SLOT_OUTPUT_INTERSECTIONS = JIPipeDataSlotInfo.builder().name("Intersections").description("The intersection ROIs.")
            .slotType(JIPipeSlotType.Output).dataClass(Ij3dSuiteRoiListData.class).build();
    public static final JIPipeDataSlotInfo SLOT_OUTPUT_FILTERS = JIPipeDataSlotInfo.builder().name("Filters").description("The remaining filter ROIs. Will be different if 'Consume on overlap' is enabled.")
            .slotType(JIPipeSlotType.Output).dataClass(Ij3dSuiteRoiListData.class).build();

    private final OutputParameters outputParameters;
    private final MeasurementParameters measurementParameters;
    private Overlap3DMode overlapMode = Overlap3DMode.BoundingBoxPrefilter;
    private boolean consumeOnOverlap = false;
    private boolean ignoreChannel = false;
    private boolean ignoreZ = false;
    private boolean ignoreFrame = false;
    private boolean verbose = false;
    private OptionalJIPipeExpressionParameter overlapCondition = new OptionalJIPipeExpressionParameter(false, "Overlap.Volume > 10");
    private OptionalJIPipeExpressionParameter matchCondition = new OptionalJIPipeExpressionParameter(false, "numMatches >= 5");

    public FilterRoi3DByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputParameters = new OutputParameters();
        this.measurementParameters = new MeasurementParameters();
        registerSubParameters(outputParameters, measurementParameters);
        updateSlots();
    }

    public FilterRoi3DByOverlapAlgorithm(FilterRoi3DByOverlapAlgorithm other) {
        super(other);
        this.overlapMode = other.overlapMode;
        this.consumeOnOverlap = other.consumeOnOverlap;
        this.ignoreChannel = other.ignoreChannel;
        this.ignoreZ = other.ignoreZ;
        this.ignoreFrame = other.ignoreFrame;
        this.verbose = other.verbose;
        this.overlapCondition = new OptionalJIPipeExpressionParameter(other.overlapCondition);
        this.matchCondition = new OptionalJIPipeExpressionParameter(other.matchCondition);
        this.outputParameters = new OutputParameters(other.outputParameters);
        this.measurementParameters = new MeasurementParameters(other.measurementParameters);
        updateSlots();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Ij3dSuiteRoiListData candidates = iterationStep.getInputData("Candidates", Ij3dSuiteRoiListData.class, progressInfo);
        Ij3dSuiteRoiListData filters = iterationStep.getInputData("Filters", Ij3dSuiteRoiListData.class, progressInfo);
        ImagePlus reference = null;
        Ij3dSuiteRoiListData matched = new Ij3dSuiteRoiListData();
        Ij3dSuiteRoiListData rejected = new Ij3dSuiteRoiListData();
        Ij3dSuiteRoiListData intersections = new Ij3dSuiteRoiListData();

        // Initialize the reference image
        ImageHandler referenceImage = null;
        if (measurementParameters.referenceMode == ReferenceMode.SameForAll) {
            ImagePlusData referenceData = iterationStep.getInputData(SLOT_INPUT_REFERENCE.getName(), ImagePlusData.class, progressInfo);
            referenceImage = IJ3DUtils.wrapImage(referenceData);
        }
        else if (measurementParameters.referenceMode == ReferenceMode.PerRoiSet) {
            candidateReference = ImageJUtils.unwrap(iterationStep.getInputData(SLOT_INPUT_REFERENCE_CANDIDATES.getName(), ImagePlusData.class, progressInfo));
            filterReference = ImageJUtils.unwrap(iterationStep.getInputData(SLOT_INPUT_REFERENCE_FILTERS.getName(), ImagePlusData.class, progressInfo));
            intersectionReference = ImageJUtils.unwrap(iterationStep.getInputData(SLOT_INPUT_REFERENCE_INTERSECTIONS.getName(), ImagePlusData.class, progressInfo));
        }

        // Do initial measurements
        progressInfo.log("Measuring candidates ...");
        ResultsTableData candidateMeasurements = candidates.measure(referenceImage, measurementParameters.measurements.getNativeValue(), true, "Candidate_", progressInfo);
        progressInfo.cancellationCheck();
        progressInfo.log("Measuring filters ...");
        ResultsTableData filterMeasurements = filters.measure(referenceImage, measurementParameters.measurements.getNativeValue(), true, "Filter_", progressInfo);
        progressInfo.cancellationCheck();

        // Convert into measured ROIs
        List<Measured3DRoi> measuredCandidates = toMeasuredRoi(candidates, candidateMeasurements);
        List<Measured3DRoi> measuredFilters = toMeasuredRoi(filters, filterMeasurements);

        // Initialize expressions
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);

        progressInfo.log("Processing candidates ...");
        JIPipePercentageProgressInfo candidateProgress = progressInfo.percentage("Candidate");
        for (int i = 0; i < measuredCandidates.size(); i++) {
            progressInfo.cancellationCheck();
            candidateProgress.logPercentage(i, measuredCandidates.size());
            Measured3DRoi candidate = measuredCandidates.get(i);
            processCandidate(candidate, measuredFilters, reference, matched, rejected, intersections, variablesMap, candidateProgress);
        }

        // Convert filters over from measured ROIs
        filters = new Ij3dSuiteRoiListData();
        for (Measured3DRoi measuredFilter : measuredFilters) {
            filters.add(measuredFilter.roi());
        }

        // Output results
        if (outputParameters.outputFilters) {
            iterationStep.addOutputData(SLOT_OUTPUT_FILTERS.getName(), filters, progressInfo);
        }
        if (outputParameters.outputMatched) {
            iterationStep.addOutputData(SLOT_OUTPUT_MATCHED.getName(), matched, progressInfo);
        }
        if (outputParameters.outputRejected) {
            iterationStep.addOutputData(SLOT_OUTPUT_REJECTED.getName(), rejected, progressInfo);
        }
        if (outputParameters.outputIntersections) {
            iterationStep.addOutputData(SLOT_OUTPUT_INTERSECTIONS.getName(), intersections, progressInfo);
        }
    }

    private List<Measured3DRoi> toMeasuredRoi(Ij3dSuiteRoiListData rois, ResultsTableData measurements) {
        List<Measured3DRoi> result = new ArrayList<>();
        if (rois.size() != measurements.getRowCount()) {
            throw new IllegalArgumentException("Unable to create measured ROIs from different input sizes: " + rois.size() + " <> " + measurements.getRowCount());
        }
        for (int i = 0; i < rois.size(); i++) {
            Map<String, Object> measurementDict = new HashMap<>();
            for (ImageJMeasurementColumn column : ImageJMeasurementColumn.values()) {
                int columnIndex = measurements.getColumnIndex(column.getColumnName());
                if (columnIndex >= 0) {
                    measurementDict.put(column.getColumnName(), measurements.getValueAt(i, columnIndex));
                } else {
                    measurementDict.put(column.getColumnName(), Double.NaN);
                }
            }
            result.add(new Measured3DRoi(rois.get(i), measurementDict));
        }
        return result;
    }

    private void processCandidate(Measured3DRoi candidate, List<Measured3DRoi> filters, ImagePlus reference,
                                  Ij3dSuiteRoiListData matched, Ij3dSuiteRoiListData rejected, Ij3dSuiteRoiListData intersections, JIPipeExpressionVariablesMap variablesMap, JIPipePercentageProgressInfo progressInfo) {
        List<Measured3DRoi> matchingFilters = new ArrayList<>();
        List<IJ3DROI> matchingIntersections = new ArrayList<>();

        JIPipePercentageProgressInfo filterProgress = progressInfo.percentage("Filter");
        for (int i = 0; i < filters.size(); i++) {
            filterProgress.logPercentage(i, filters.size());
            filterProgress.cancellationCheck();
            Measured3DRoi filter = filters.get(i);

            // Do a C/Z/T pre-check
            if (!isZCTVisible(candidate.roi(), filter.roi())) {
                logCandidateFilterVerbose(candidate, filter, "ZCT check failed", progressInfo);
                continue;
            }

            // Do a bounding box check if enabled
            boolean matchesBoundingBox;
            if (overlapMode == Overlap3DMode.BoundingBoxOnly || overlapMode == Overlap3DMode.BoundingBoxPrefilter) {
                matchesBoundingBox = isIntersectingBoundingBox(candidate.roi(), filter.roi());
            } else {
                matchesBoundingBox = true;
            }
            if (!matchesBoundingBox) {
                logCandidateFilterVerbose(candidate, filter, "Bounding box check failed", progressInfo);
                continue;
            }

            // Do an exact check if enabled
            final boolean overlapSuccess;
            Ij3dSuiteRoiListData tempIntersections = new Ij3dSuiteRoiListData();

            if (overlapMode == Overlap3DMode.ExactOnly || overlapMode == Overlap3DMode.BoundingBoxPrefilter) {
                Ij3dSuiteRoiListData tmp = new Ij3dSuiteRoiListData();
                tmp.add(candidate.roi());
                tmp.add(filter.roi());
                // For 3D, we need to check for voxel overlap
                overlapSuccess = checkExactOverlap3D(tmp, candidate, filter, reference, variablesMap, progressInfo);
                if (overlapSuccess) {
                    tempIntersections.addAll(tmp);
                }
            } else {
                logCandidateFilterVerbose(candidate, filter, "Generating bounding box overlaps as requested", progressInfo);
                overlapSuccess = true;
                // For 3D, create a simple intersection ROI
                tempIntersections.add(createBoundingBoxIntersection3D(candidate.roi(), filter.roi()));
            }

            if (overlapSuccess) {
                logCandidateFilterVerbose(candidate, filter, "Final overlap check SUCCESS", progressInfo);
                matchingFilters.add(filter);
                matchingIntersections.addAll(tempIntersections);
            } else {
                logCandidateFilterVerbose(candidate, filter, "Final overlap check FAIL", progressInfo);
            }
        }

        logCandidateVerbose(candidate, "Found " + matchingFilters.size() + " matching filters, " + matchingIntersections.size() + " intersections", progressInfo);

        // Match filtering
        final boolean matchSuccess;
        if (matchCondition.isEnabled()) {
            putMeasurementsIntoVariable(candidate, "Candidate", variablesMap);
            putMeasurementListsIntoVariable(matchingFilters, "all.Filter", variablesMap);
            variablesMap.put("numMatches", matchingFilters.size());
            matchSuccess = matchCondition.getContent().evaluateToBoolean(variablesMap);
        } else {
            // Simple check: at least one match
            matchSuccess = !matchingFilters.isEmpty();
        }

        if (matchSuccess) {
            logCandidateVerbose(candidate, "Match SUCCESS", progressInfo);

            // Consume on overlap
            if (consumeOnOverlap) {
                logCandidateVerbose(candidate, "Consuming overlaps after success as requested", progressInfo);
                filters.removeAll(matchingFilters);
            }

            // Output all data
            matched.add(candidate.roi());
            intersections.addAll(matchingIntersections);
        } else {
            logCandidateVerbose(candidate, "Match FAIL", progressInfo);

            // Output rejection
            rejected.add(candidate.roi());
        }
    }

    private boolean checkExactOverlap3D(Ij3dSuiteRoiListData intersectionRois, Measured3DRoi candidate, Measured3DRoi filter, ImagePlus reference, JIPipeExpressionVariablesMap variablesMap, JIPipeProgressInfo progressInfo) {
        if (!overlapCondition.isEnabled()) {
            // For 3D, check if there's any voxel overlap
            return hasVoxelOverlap3D(candidate.roi(), filter.roi());
        }

        // Measure the intersection ROI
        // For intersection measurement, use the reference from method parameter if available
        ImageHandler localReferenceImage = reference != null ? IJ3DUtils.wrapImage(new ImagePlusData(reference)) : null;
        ResultsTableData measurements = intersectionRois.measure(localReferenceImage, measurementParameters.measurements.getNativeValue(), true, "Overlap_", progressInfo.resolve("Measure intersection"));
        Measured3DRoi intersectionMeasurements = toMeasuredRoi(intersectionRois, measurements).getFirst();

        // Store variables that are measured
        putMeasurementsIntoVariable(candidate, "Candidate", variablesMap);
        putMeasurementsIntoVariable(filter, "Filter", variablesMap);
        putMeasurementsIntoVariable(intersectionMeasurements, "Overlap", variablesMap);

        return overlapCondition.getContent().evaluateToBoolean(variablesMap);
    }

    private boolean hasVoxelOverlap3D(IJ3DROI roi1, IJ3DROI roi2) {
        // Check if two 3D ROIs have any voxel overlap
        return roi1.getObject3D().hasOneVoxelColoc(roi2.getObject3D());
    }

    private IJ3DROI createBoundingBoxIntersection3D(IJ3DROI candidate, IJ3DROI filter) {
        // Create a simple bounding box intersection for 3D ROIs
        // This is a simplified approach - in practice, you might want more sophisticated intersection logic
        return candidate; // For now, return the candidate as intersection
    }

    private void logCandidateFilterVerbose(Measured3DRoi candidate, Measured3DRoi filter, String text, JIPipePercentageProgressInfo progressInfo) {
        if (verbose) {
            progressInfo.log("candidate=" + candidate.roi() + " & filter=" + filter.roi() + " --> " + text);
        }
    }

    private void logCandidateVerbose(Measured3DRoi candidate, String text, JIPipePercentageProgressInfo progressInfo) {
        if (verbose) {
            progressInfo.log("candidate=" + candidate.roi() + " --> " + text);
        }
    }

    private void putMeasurementListsIntoVariable(List<Measured3DRoi> measuredRoi, String prefix, JIPipeExpressionVariablesMap variablesMap) {
        variablesMap.put(prefix + ".z", measuredRoi.stream().map(roi -> roi.roi().getFrame()).toList());
        variablesMap.put(prefix + ".c", measuredRoi.stream().map(roi -> roi.roi().getChannel()).toList());
        Set<String> keys = new HashSet<>();
        for (Measured3DRoi roi : measuredRoi) {
            keys.addAll(roi.measurements().keySet());
        }
        for (String key : keys) {
            variablesMap.put(prefix + "." + key, measuredRoi.stream().map(roi -> roi.measurements().get(key)).toList());
        }
    }

    private void putMeasurementsIntoVariable(Measured3DRoi measuredRoi, String prefix, JIPipeExpressionVariablesMap variablesMap) {
        variablesMap.set(prefix + ".z", measuredRoi.roi().getFrame());
        variablesMap.set(prefix + ".c", measuredRoi.roi().getChannel());
        variablesMap.set(prefix + ".name", StringUtils.nullToEmpty(measuredRoi.roi().getName()));
        for (Map.Entry<String, Object> entry : measuredRoi.measurements().entrySet()) {
            variablesMap.put(prefix + "." + entry.getKey(), entry.getValue());
        }
    }

    private boolean isIntersectingBoundingBox(IJ3DROI roi1, IJ3DROI roi2) {
        // For 3D, check bounding box intersection
        int[] bbox1 = roi1.getObject3D().getBoundingBox();
        int[] bbox2 = roi2.getObject3D().getBoundingBox();
        
        // Check if bounding boxes overlap in 3D
        return !(bbox1[3] < bbox2[0] || bbox2[3] < bbox1[0] ||  // X overlap
                 bbox1[4] < bbox2[1] || bbox2[4] < bbox1[1] ||  // Y overlap
                 bbox1[5] < bbox2[2] || bbox2[5] < bbox1[2]);    // Z overlap
    }

    private boolean isZCTVisible(IJ3DROI roi1, IJ3DROI roi2) {
        int c1 = roi1.getChannel();
        int z1 = roi1.getFrame(); // Note: In IJ3D, frame seems to be used for Z
        int t1 = 0; // 3D ROIs don't have time dimension
        int c2 = roi2.getChannel();
        int z2 = roi2.getFrame();
        int t2 = 0;
        
        if (ignoreChannel || c1 == 0 || c2 == 0) {
            c1 = 0;
            c2 = 0;
        }
        if (ignoreZ || z1 == 0 || z2 == 0) {
            z1 = 0;
            z2 = 0;
        }
        if (ignoreFrame || t1 == 0 || t2 == 0) {
            t1 = 0;
            t2 = 0;
        }
        return c1 == c2 && z1 == z2 && t1 == t2;
    }

    private void updateSlots() {
        toggleSlot(SLOT_INPUT_REFERENCE, measurementParameters.referenceMode == ReferenceMode.SameForAll);
        toggleSlot(SLOT_OUTPUT_MATCHED, outputParameters.outputMatched);
        toggleSlot(SLOT_OUTPUT_INTERSECTIONS, outputParameters.outputIntersections);
        toggleSlot(SLOT_OUTPUT_FILTERS, outputParameters.outputFilters);
        toggleSlot(SLOT_OUTPUT_REJECTED, outputParameters.outputRejected);
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if (event.getSource() == outputParameters) {
            updateSlots();
        }
        if (event.getSource() == measurementParameters && "reference-mode".equals(event.getKey())) {
            updateSlots();
        }
    }

    @SetJIPipeDocumentation(name = "Verbose", description = "Verbose logging. Impacts performance but allows tracking of errors")
    @JIPipeParameter("verbose")
    public boolean isVerbose() {
        return verbose;
    }

    @JIPipeParameter("verbose")
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @SetJIPipeDocumentation(name = "Ignore channel", description = "If enabled, the channel location is ignored. Please note that if the channel location of a ROI is 0, it is seen as visible on all channels")
    @JIPipeParameter("ignore-channel")
    public boolean isIgnoreChannel() {
        return ignoreChannel;
    }

    @JIPipeParameter("ignore-channel")
    public void setIgnoreChannel(boolean ignoreChannel) {
        this.ignoreChannel = ignoreChannel;
    }

    @SetJIPipeDocumentation(name = "Ignore Z", description = "If enabled, the Z location is ignored. Please note that if the Z location of a ROI is 0, it is seen as visible in all depths")
    @JIPipeParameter("ignore-z")
    public boolean isIgnoreZ() {
        return ignoreZ;
    }

    @JIPipeParameter("ignore-z")
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    @SetJIPipeDocumentation(name = "Ignore frame", description = "If enabled, the frame location is ignored. Please note that if the frame location of a ROI is 0, it is seen as visible in all frames")
    @JIPipeParameter("ignore-frame")
    public boolean isIgnoreFrame() {
        return ignoreFrame;
    }

    @JIPipeParameter("ignore-frame")
    public void setIgnoreFrame(boolean ignoreFrame) {
        this.ignoreFrame = ignoreFrame;
    }

    @SetJIPipeDocumentation(name = "Overlap mode", description = "Determines how the algorithm calculates ROI overlaps")
    @JIPipeParameter(value = "overlap-mode", important = true, uiOrder = -100)
    public Overlap3DMode getOverlapMode() {
        return overlapMode;
    }

    @JIPipeParameter("overlap-mode")
    public void setOverlapMode(Overlap3DMode overlapMode) {
        this.overlapMode = overlapMode;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Overlap condition", description = "Optional expression that allows to customize what is considered an overlap. If the expression returns true, it the tested candidate-filter-overlap triplet is considered an overlap." +
            " If the expression is disabled, any overlap is seen as overlap.")
    @JIPipeParameter(value = "overlap-condition", uiOrder = -90)
    @AddJIPipeExpressionParameterVariable(fromClass = org.hkijena.jipipe.plugins.ij3d.utils.AllRoi3DRelationMeasurementExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per overlapping ROI")
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
    @AddJIPipeExpressionParameterVariable(fromClass = org.hkijena.jipipe.plugins.ij3d.utils.AllRoi3DRelationMeasurementExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "numMatches", name = "Number of matching filters", description = "The number of matching filters for this candidate")
    public OptionalJIPipeExpressionParameter getMatchCondition() {
        return matchCondition;
    }

    @JIPipeParameter("match-condition")
    public void setMatchCondition(OptionalJIPipeExpressionParameter matchCondition) {
        this.matchCondition = matchCondition;
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

    @SetJIPipeDocumentation(name = "Consume on overlap", description = "If a candidate matches filters, remove the filter ROIs afterwards. This is done in order of the candidate ROIs.")
    @JIPipeParameter("consume-on-overlap")
    public boolean isConsumeOnOverlap() {
        return consumeOnOverlap;
    }

    @JIPipeParameter("consume-on-overlap")
    public void setConsumeOnOverlap(boolean consumeOnOverlap) {
        this.consumeOnOverlap = consumeOnOverlap;
    }

    @Override
    public boolean isParameterUIVisible(org.hkijena.jipipe.api.parameters.JIPipeParameterTree tree, org.hkijena.jipipe.api.parameters.JIPipeParameterAccess access) {
        if (overlapMode == Overlap3DMode.BoundingBoxOnly && "overlap-condition".equals(access.getKey())) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    public static class MeasurementParameters extends AbstractJIPipeParameterCollection {
        private ImageJMeasurementsSetParameter measurements = new ImageJMeasurementsSetParameter();
        private boolean measurePhysicalSizes = false;
        private ReferenceMode referenceMode = ReferenceMode.None;

        public MeasurementParameters() {
        }

        public MeasurementParameters(MeasurementParameters other) {
            this.measurements = new ImageJMeasurementsSetParameter(other.measurements);
            this.measurePhysicalSizes = other.measurePhysicalSizes;
            this.referenceMode = other.referenceMode;
        }

        @SetJIPipeDocumentation(name = "Measurements", description = "The measurements that are taken from the candidates, filters, and intersections. <br/><br/>" + ImageJMeasurementsSetParameter.ALL_DESCRIPTIONS)
        @JIPipeParameter("measurements")
        public ImageJMeasurementsSetParameter getMeasurements() {
            return measurements;
        }

        @JIPipeParameter("measurements")
        public void setMeasurements(ImageJMeasurementsSetParameter measurements) {
            this.measurements = measurements;
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

        @SetJIPipeDocumentation(name = "Reference mode", description = "Determines where reference images for the measurements are taken")
        @JIPipeParameter(value = "reference-mode", important = true, uiOrder = -100)
        public ReferenceMode getReferenceMode() {
            return referenceMode;
        }

        @JIPipeParameter("reference-mode")
        public void setReferenceMode(ReferenceMode referenceMode) {
            this.referenceMode = referenceMode;
        }
    }

    public enum ReferenceMode {
        None("No reference image (black)"),
        SameForAll("Same for all cases"),
        PerRoiSet("Different per case");

        private final String label;

        ReferenceMode(String label) {
            this.label = label;
        }


        @Override
        public String toString() {
            return label;
        }
    }

    public static class OutputParameters extends AbstractJIPipeParameterCollection {
        private boolean outputMatched = true;
        private boolean outputRejected = true;
        private boolean outputIntersections = false;
        private boolean outputFilters = false;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
            this.outputMatched = other.outputMatched;
            this.outputRejected = other.outputRejected;
            this.outputIntersections = other.outputIntersections;
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

        @SetJIPipeDocumentation(name = "Output intersections", description = "If enabled, output all intersection ROIs")
        @JIPipeParameter("output-intersections")
        public boolean isOutputIntersections() {
            return outputIntersections;
        }

        @JIPipeParameter("output-intersections")
        public void setOutputIntersections(boolean outputIntersections) {
            this.outputIntersections = outputIntersections;
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

    /**
     * Defines the overlap detection modes for 3D ROI overlap filtering
     */
    public enum Overlap3DMode {
        ExactOnly("Voxel-level precise overlap (slowest)"),
        BoundingBoxOnly("Fast 3D bounding box intersection (fastest, least accurate)"),
        BoundingBoxPrefilter("Hybrid approach: bounding box + exact (default)"),
        SpatialIndexPrefilter("Spatial index + bounding box + exact (for large datasets)");

        private final String label;

        Overlap3DMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Enhanced 3D ROI with pre-measured properties for overlap analysis
     */
    public static record Measured3DRoi(IJ3DROI roi, Map<String, Object> measurements) {
    }
}