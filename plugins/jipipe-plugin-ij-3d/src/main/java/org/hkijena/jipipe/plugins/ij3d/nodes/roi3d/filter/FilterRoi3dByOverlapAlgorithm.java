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
import ij.gui.Roi;
import ij.gui.ShapeRoi;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoi;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.filter.RoiOverlapMatchStatisticsVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurement;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

@SetJIPipeDocumentation(name = "Filter 3D ROI by overlap", description = "Only returns candidate ROIs that overlap with at least N ROIs from the filter set.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "Candidates", description = "The ROIs that are filtered", create = true)
@AddJIPipeInputSlot(value = Ij3dSuiteRoiListData.class, name = "Filters", description = "The ROIs that are used as filters", create = true)
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Matched", description = "The candidates that match the filter")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Rejected", description = "The candidates that do not match the filter")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Intersections", description = "The intersection ROIs")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Filters", description = "The remaining filter ROIs. Will be different if 'Consume on overlap' is enabled.")
public class FilterRoi3dByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

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
    private OverlapMode overlapMode = OverlapMode.BoundingBoxPrefilter;
    private boolean consumeOnOverlap = false;
    private boolean ignoreChannel = false;
    private boolean ignoreFrame = false;
    private boolean verbose = false;
    private OptionalJIPipeExpressionParameter overlapCondition = new OptionalJIPipeExpressionParameter(false, "Overlap.Area > 10");
    private OptionalJIPipeExpressionParameter matchCondition = new OptionalJIPipeExpressionParameter(false, "numMatches >= 5");

    public FilterRoi3dByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputParameters = new OutputParameters();
        this.measurementParameters = new MeasurementParameters();
        registerSubParameters(outputParameters, measurementParameters);
        updateSlots();
    }

    public FilterRoi3dByOverlapAlgorithm(FilterRoi3dByOverlapAlgorithm other) {
        super(other);
        this.overlapMode = other.overlapMode;
        this.consumeOnOverlap = other.consumeOnOverlap;
        this.ignoreChannel = other.ignoreChannel;
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
        ImagePlus candidateReference = null;
        ImagePlus filterReference = null;
        ImagePlus intersectionReference = null;
        Ij3dSuiteRoiListData matched = new Ij3dSuiteRoiListData();
        Ij3dSuiteRoiListData rejected = new Ij3dSuiteRoiListData();
        Ij3dSuiteRoiListData intersections = new Ij3dSuiteRoiListData();

        // Initialize the reference images
        if (measurementParameters.referenceMode == ReferenceMode.SameForAll) {
            ImagePlus imp = ImageJUtils.unwrap(iterationStep.getInputData(SLOT_INPUT_REFERENCE.getName(), ImagePlusData.class, progressInfo));
            candidateReference = imp;
            filterReference = imp;
            intersectionReference = imp;
        } else if (measurementParameters.referenceMode == ReferenceMode.PerRoiSet) {
            candidateReference = ImageJUtils.unwrap(iterationStep.getInputData(SLOT_INPUT_REFERENCE_CANDIDATES.getName(), ImagePlusData.class, progressInfo));
            filterReference = ImageJUtils.unwrap(iterationStep.getInputData(SLOT_INPUT_REFERENCE_FILTERS.getName(), ImagePlusData.class, progressInfo));
            intersectionReference = ImageJUtils.unwrap(iterationStep.getInputData(SLOT_INPUT_REFERENCE_INTERSECTIONS.getName(), ImagePlusData.class, progressInfo));
        }

        // Do initial measurements
        progressInfo.log("Measuring candidates ...");
        ResultsTableData candidateMeasurements = candidates.measure(candidateReference, measurementParameters.measurements, true, measurementParameters.measurePhysicalSizes);
        progressInfo.cancellationCheck();
        progressInfo.log("Measuring filters ...");
        ResultsTableData filterMeasurements = filters.measure(filterReference, measurementParameters.measurements, true, measurementParameters.measurePhysicalSizes);
        progressInfo.cancellationCheck();

        // Convert into measured ROIs
        List<MeasuredRoi> measuredCandidates = toMeasuredRoi(candidates, candidateMeasurements);
        List<MeasuredRoi> measuredFilters = toMeasuredRoi(filters, filterMeasurements);

        // Initialize expressions
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);

        progressInfo.log("Processing candidates ...");
        JIPipePercentageProgressInfo candidateProgress = progressInfo.percentage("Candidate");
        for (int i = 0; i < measuredCandidates.size(); i++) {
            progressInfo.cancellationCheck();
            candidateProgress.logPercentage(i, measuredCandidates.size());
            MeasuredRoi candidate = measuredCandidates.get(i);
            processCandidate(candidate, measuredFilters, intersectionReference, matched, rejected, intersections, variablesMap, candidateProgress);
        }

        // Convert filters over from measured ROIs
        filters = new Ij3dSuiteRoiListData();
        for (MeasuredRoi measuredFilter : measuredFilters) {
            filters.add(measuredFilter.roi);
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

    private List<MeasuredRoi> toMeasuredRoi(Ij3dSuiteRoiListData rois, ResultsTableData measurements) {
        List<MeasuredRoi> result = new ArrayList<>();
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
            result.add(new MeasuredRoi(rois.get(i), measurementDict));
        }
        return result;
    }

    private void processCandidate(MeasuredRoi candidate, List<MeasuredRoi> filters, ImagePlus intersectionReference,
                                  Ij3dSuiteRoiListData matched, Ij3dSuiteRoiListData rejected, Ij3dSuiteRoiListData intersections, JIPipeExpressionVariablesMap variablesMap, JIPipePercentageProgressInfo progressInfo) {
        List<MeasuredRoi> matchingFilters = new ArrayList<>();
        List<Roi> matchingIntersections = new ArrayList<>();

        JIPipePercentageProgressInfo filterProgress = progressInfo.percentage("Filter");
        for (int i = 0; i < filters.size(); i++) {
            filterProgress.logPercentage(i, filters.size());
            filterProgress.cancellationCheck();
            MeasuredRoi filter = filters.get(i);

            // Do a C/Z/T pre-check
            if (!isZCTVisible(candidate.roi, filter.roi)) {
                logCandidateFilterVerbose(candidate, filter, "ZCT check failed", progressInfo);
                continue;
            }

            // Do a bounding box check if enabled
            boolean matchesBoundingBox;
            if (overlapMode == OverlapMode.BoundingBoxOnly || overlapMode == OverlapMode.BoundingBoxPrefilter) {
                matchesBoundingBox = isIntersectingBoundingBox(candidate.roi, filter.roi);
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

            if (overlapMode == OverlapMode.ExactOnly || overlapMode == OverlapMode.BoundingBoxPrefilter) {
                Ij3dSuiteRoiListData tmp = new Ij3dSuiteRoiListData();
                tmp.add(candidate.roi);
                tmp.add(filter.roi);
                tmp.logicalAnd();
                tmp.removeIf(this::isEmptyIntersection);

                if (tmp.isEmpty()) {
                    logCandidateFilterVerbose(candidate, filter, "Exact overlap check failed", progressInfo);
                    overlapSuccess = false;
                } else {
                    // Merge into one intersection ROI
                    if (tmp.size() > 1) {
                        logCandidateFilterVerbose(candidate, filter, "Found split intersection with " + tmp.size() + " parts - merging with OR", progressInfo);
                        tmp.logicalOr();
                    }

                    overlapSuccess = isIntersectingExact(tmp, candidate, filter, intersectionReference, variablesMap);
                    tempIntersections.addAll(tmp);
                }
            } else {
                logCandidateFilterVerbose(candidate, filter, "Generating bounding box overlaps as requested", progressInfo);

                // Generate bounding box overlaps
                overlapSuccess = true;
                tempIntersections.add(createBoundingBoxIntersection(candidate.roi, filter.roi));
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
            matched.add(candidate.roi);
            intersections.addAll(matchingIntersections);
        } else {
            logCandidateVerbose(candidate, "Match FAIL", progressInfo);

            // Output rejection
            rejected.add(candidate.roi);
        }
    }

    private void logCandidateFilterVerbose(MeasuredRoi candidate, MeasuredRoi filter, String text, JIPipePercentageProgressInfo progressInfo) {
        if (verbose) {
            progressInfo.log("candidate=" + candidate.roi + " & filter=" + filter.roi + " --> " + text);
        }
    }

    private void logCandidateVerbose(MeasuredRoi candidate, String text, JIPipePercentageProgressInfo progressInfo) {
        if (verbose) {
            progressInfo.log("candidate=" + candidate.roi + " --> " + text);
        }
    }

    private void putMeasurementListsIntoVariable(List<MeasuredRoi> measuredRoi, String prefix, JIPipeExpressionVariablesMap variablesMap) {
        variablesMap.put(prefix + ".z", measuredRoi.stream().map(roi -> roi.roi.getZPosition()).toList());
        variablesMap.put(prefix + ".c", measuredRoi.stream().map(roi -> roi.roi.getCPosition()).toList());
        variablesMap.put(prefix + ".t", measuredRoi.stream().map(roi -> roi.roi.getTPosition()).toList());
        Set<String> keys = new HashSet<>();
        for (MeasuredRoi roi : measuredRoi) {
            keys.addAll(roi.measurements.keySet());
        }
        for (String key : keys) {
            variablesMap.put(prefix + "." + key, measuredRoi.stream().map(roi -> roi.measurements.get(key)).toList());
        }
    }

    private void putMeasurementsIntoVariable(MeasuredRoi measuredRoi, String prefix, JIPipeExpressionVariablesMap variablesMap) {
        variablesMap.set(prefix + ".z", measuredRoi.roi.getZPosition());
        variablesMap.set(prefix + ".c", measuredRoi.roi.getCPosition());
        variablesMap.set(prefix + ".t", measuredRoi.roi.getTPosition());
        variablesMap.set(prefix + ".name", StringUtils.nullToEmpty(measuredRoi.roi.getName()));
        for (Map.Entry<String, Object> entry : measuredRoi.measurements.entrySet()) {
            variablesMap.put(prefix + "." + entry.getKey(), entry.getValue());
        }
    }

    private boolean isEmptyIntersection(Roi roi) {
        // Do simple bounding box check
        Rectangle bounds = roi.getBounds();
        if (bounds == null) {
            return true;
        }
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return true;
        }

        // Measure only the area and use that
        Ij3dSuiteRoiListData tmp = new Ij3dSuiteRoiListData();
        tmp.add(roi);
        ResultsTableData measured = tmp.measure(null, new ImageJMeasurementsSetParameter(Set.of(ImageJMeasurement.Area)), false, false);

        if (measured == null || measured.getRowCount() <= 0) {
            return true;
        }
        TableColumnData areaColumn = measured.getColumnReference("Area");
        if (areaColumn == null) {
            return true;
        }
        double sumArea = 0;
        for (int i = 0; i < areaColumn.getRows(); i++) {
            sumArea += areaColumn.getRowAsDouble(i);
        }

        return sumArea <= 0;
    }

    private boolean isIntersectingExact(Ij3dSuiteRoiListData intersectionNonEmpty, MeasuredRoi candidateMeasurements, MeasuredRoi filterMeasurements, ImagePlus intersectionReference, JIPipeExpressionVariablesMap variablesMap) {
        if (!overlapCondition.isEnabled()) {
            // It's enough if we have intersecting filters (empty intersections already filtered out by isEmptyIntersection)
            return !intersectionNonEmpty.isEmpty();
        }

        // Measure the intersection ROI
        ResultsTableData measurements = intersectionNonEmpty.measure(intersectionReference, measurementParameters.measurements, true, measurementParameters.measurePhysicalSizes);
        MeasuredRoi intersectionMeasurements = toMeasuredRoi(intersectionNonEmpty, measurements).getFirst();

        // Store variables that are measured
        putMeasurementsIntoVariable(candidateMeasurements, "Candidate", variablesMap);
        putMeasurementsIntoVariable(filterMeasurements, "Filter", variablesMap);
        putMeasurementsIntoVariable(intersectionMeasurements, "Overlap", variablesMap);

        return overlapCondition.getContent().evaluateToBoolean(variablesMap);
    }

    private Roi createBoundingBoxIntersection(Roi candidate, Roi filter) {
        Rectangle b1 = candidate.getBounds();
        Rectangle b2 = filter.getBounds();

        Rectangle intersection = b1.intersection(b2);
        Roi result = new ShapeRoi(intersection);
        result.copyAttributes(candidate);
        result.setPosition(candidate.getCPosition(), candidate.getZPosition(), candidate.getTPosition());
        return result;
    }

    private boolean isIntersectingBoundingBox(Roi roi1, Roi roi2) {
        Rectangle b1 = roi1.getBounds();
        Rectangle b2 = roi2.getBounds();
        if (b1 == null) {
            return false;
        }
        if (b2 == null) {
            return false;
        }
        return b1.intersects(b2);
    }

    private boolean isZCTVisible(Ij3dSuiteRoi roi1, Ij3dSuiteRoi roi2) {
        int c1 = roi1.getChannel();
        int t1 = roi1.getFrame();
        int c2 = roi2.getChannel();
        int t2 = roi2.getFrame();
        if (ignoreChannel || c1 == 0 || c2 == 0) {
            c1 = 0;
            c2 = 0;
        }
        if (ignoreFrame || t1 == 0 || t2 == 0) {
            t1 = 0;
            t2 = 0;
        }
        return c1 == c2 && t1 == t2;
    }

    private void updateSlots() {
        toggleSlot(SLOT_INPUT_REFERENCE, measurementParameters.referenceMode == ReferenceMode.SameForAll);
        toggleSlot(SLOT_INPUT_REFERENCE_CANDIDATES, measurementParameters.referenceMode == ReferenceMode.PerRoiSet);
        toggleSlot(SLOT_INPUT_REFERENCE_FILTERS, measurementParameters.referenceMode == ReferenceMode.PerRoiSet);
        toggleSlot(SLOT_INPUT_REFERENCE_INTERSECTIONS, measurementParameters.referenceMode == ReferenceMode.PerRoiSet);
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
    public OverlapMode getOverlapMode() {
        return overlapMode;
    }

    @JIPipeParameter("overlap-mode")
    public void setOverlapMode(OverlapMode overlapMode) {
        this.overlapMode = overlapMode;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Overlap condition", description = "Optional expression that allows to customize what is considered an overlap. If the expression returns true, it the tested candidate-filter-overlap triplet is considered an overlap." +
            " If the expression is disabled, any overlap is seen as overlap.")
    @JIPipeParameter(value = "overlap-condition", uiOrder = -90)
    @AddJIPipeExpressionParameterVariable(fromClass = Roi3dOverlapMatchStatisticsVariablesInfo.class)
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
    @AddJIPipeExpressionParameterVariable(fromClass = Roi3dOverlapMatchStatisticsVariablesInfo.class)
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
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (overlapMode == OverlapMode.BoundingBoxOnly && "overlap-condition".equals(access.getKey())) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    public enum OverlapMode {
        ExactOnly("Only exact (slower)"),
        BoundingBoxOnly("Only bounding box (fast, inaccurate)"),
        BoundingBoxPrefilter("Bounding box + exact (default)");

        private final String label;

        OverlapMode(String label) {
            this.label = label;
        }


        @Override
        public String toString() {
            return label;
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

    public record MeasuredRoi(Roi roi, Map<String, Object> measurements) {

    }

    public static class MeasurementParameters extends AbstractJIPipeParameterCollection {
        private Roi3dMeasurementSetParameter measurements = new Roi3dMeasurementSetParameter();
        private boolean measurePhysicalSizes = false;
        private ReferenceMode referenceMode = ReferenceMode.None;

        public MeasurementParameters() {
        }

        public MeasurementParameters(MeasurementParameters other) {
            this.measurements = new Roi3dMeasurementSetParameter(other.measurements);
            this.measurePhysicalSizes = other.measurePhysicalSizes;
            this.referenceMode = other.referenceMode;
        }

        @SetJIPipeDocumentation(name = "Measurements", description = "The measurements that are taken from the candidates, filters, and intersections.")
        @JIPipeParameter("measurements")
        public Roi3dMeasurementSetParameter getMeasurements() {
            return measurements;
        }

        @JIPipeParameter("measurements")
        public void setMeasurements(Roi3dMeasurementSetParameter measurements) {
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
}
