package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

@JIPipeDocumentation(name = "Filter ROI by overlap", description = "Filters the ROI lists by testing for mutual overlap. The nodes filters the ROI of all input slots and puts filtered ROI into their " +
        "corresponding output. If you have more than one input, overlaps are connected according to the logical operation. If only one input is present, no filtering is applied.")
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(ROIListData.class)
@JIPipeOutputSlot(ROIListData.class)
public class FilterROIByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean invert = false;
    private boolean outputOverlaps = false;
    private LogicalOperation logicalOperation = LogicalOperation.LogicalAnd;
    private DefaultExpressionParameter overlapFilter = new DefaultExpressionParameter();
    private ImageStatisticsSetParameter overlapFilterMeasurements = new ImageStatisticsSetParameter();
    private OutputSlotMapParameterCollection deactivatedOutputs;

    public FilterROIByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info, generateSlotConfiguration());
        this.deactivatedOutputs = new OutputSlotMapParameterCollection(Boolean.class, this, () -> false, true);
        this.deactivatedOutputs.getEventBus().register(this);
    }

    public FilterROIByOverlapAlgorithm(FilterROIByOverlapAlgorithm other) {
        super(other);
        this.invert = other.invert;
        this.logicalOperation = other.logicalOperation;
        this.outputOverlaps = other.outputOverlaps;
        this.overlapFilter = new DefaultExpressionParameter(other.overlapFilter);
        this.overlapFilterMeasurements = new ImageStatisticsSetParameter(other.overlapFilterMeasurements);
        this.deactivatedOutputs = new OutputSlotMapParameterCollection(Boolean.class, this, () -> false, true);
        other.deactivatedOutputs.copyTo(this.deactivatedOutputs);
        this.deactivatedOutputs.getEventBus().register(this);
    }

    private static JIPipeSlotConfiguration generateSlotConfiguration() {
        JIPipeIOSlotConfiguration configuration = new JIPipeIOSlotConfiguration();
        configuration.setAllowedInputSlotTypes(Collections.singleton(ROIListData.class));
        configuration.setAllowedOutputSlotTypes(Collections.singleton(ROIListData.class));
        configuration.addSlot("ROI 1", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, null), true);
        configuration.addSlot("ROI 2", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, null), true);
        return configuration;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<String, ROIListData> roiMap = new HashMap<>();
        Map<String, ROIListData> originalRoiMap = new HashMap<>();
        for (JIPipeDataSlot slot : getNonParameterInputSlots()) {
            ROIListData data = dataBatch.getInputData(slot, ROIListData.class, progressInfo);
            originalRoiMap.put(slot.getName(), data);

            Boolean deactivated = deactivatedOutputs.getParameter(slot.getName()).get(Boolean.class);
            if (deactivated) {
                progressInfo.log("Output will be skipped: " + slot.getName());
                continue;
            }
            roiMap.put(slot.getName(), (ROIListData) data.duplicate());
        }
        ImagePlus referenceImage = ROIListData.createDummyImageFor(originalRoiMap.values());

        boolean withFiltering = !StringUtils.isNullOrEmpty(overlapFilter.getExpression());
        ExpressionParameters variableSet = new ExpressionParameters();
        ROIListData temp = new ROIListData();
        for (Map.Entry<String, ROIListData> entry : roiMap.entrySet()) {
            JIPipeProgressInfo subProgress = progressInfo.resolveAndLog("Testing overlaps for input '" + entry.getKey() + "'");
            ROIListData here = entry.getValue();
            List<ROIListData> others = new ArrayList<>();
            for (Map.Entry<String, ROIListData> entry2 : originalRoiMap.entrySet()) {
                if (entry.getKey().equals(entry2.getKey()))
                    continue;
                others.add(entry2.getValue());
            }
            for (int i = 0; i < here.size(); i++) {
                subProgress.resolveAndLog("ROI", i, here.size());
                Roi roi = here.get(i);
                List<Roi> overlaps = new ArrayList<>();
                List<Boolean> overlapSuccesses = new ArrayList<>();
                for (ROIListData other : others) {
                    Roi overlap = null;
                    for (Roi roi2 : other) {
                        overlap = calculateOverlap(temp, roi, roi2);
                        if (overlap != null) {
                            if (withFiltering) {
                                putMeasurementsIntoVariable(roi, roi2, variableSet, overlap, referenceImage, temp);
                                if (overlapFilter.test(variableSet))
                                    break;
                                else
                                    overlap = null;
                            } else {
                                break;
                            }
                        }
                    }
                    if (overlap != null) {
                        overlaps.add(overlap);
                        overlapSuccesses.add(true);
                    } else {
                        overlapSuccesses.add(false);
                    }
                }
                boolean success = logicalOperation.apply(overlapSuccesses);
                if (invert)
                    success = !success;
                if (success) {
                    if (outputOverlaps) {
                        temp.clear();
                        temp.addAll(overlaps);
                        switch (logicalOperation) {
                            case LogicalOr:
                                temp.logicalOr();
                                break;
                            case LogicalXor:
                                temp.logicalXor();
                                break;
                            case LogicalAnd:
                                temp.logicalAnd();
                                break;
                        }
                        here.set(i, temp.get(0));
                    }
                } else {
                    here.set(i, null);
                }
            }
            here.removeIf(Objects::isNull);
        }

        // Save outputs
        for (Map.Entry<String, ROIListData> entry : roiMap.entrySet()) {
            dataBatch.addOutputData(entry.getKey(), entry.getValue(), progressInfo);
        }
    }

    private void putMeasurementsIntoVariable(Roi first, Roi second, ExpressionParameters variableSet, Roi overlap, ImagePlus referenceImage, ROIListData temp) {

        variableSet.set("First.z", first.getZPosition());
        variableSet.set("First.c", first.getCPosition());
        variableSet.set("First.t", first.getTPosition());
        variableSet.set("First.name", StringUtils.nullToEmpty(first.getName()));
        variableSet.set("Second.z", second.getZPosition());
        variableSet.set("Second.c", second.getCPosition());
        variableSet.set("Second.t", second.getTPosition());
        variableSet.set("Second.name", StringUtils.nullToEmpty(second.getName()));

        // Add first ROI info
        temp.clear();
        temp.add(first);
        ResultsTableData firstMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false);
        for (int col = 0; col < firstMeasurements.getColumnCount(); col++) {
            variableSet.set("First." + firstMeasurements.getColumnName(col), firstMeasurements.getValueAt(0, col));
        }

        // Add second ROI info
        temp.clear();
        temp.add(second);
        ResultsTableData secondMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false);
        for (int col = 0; col < secondMeasurements.getColumnCount(); col++) {
            variableSet.set("Second." + secondMeasurements.getColumnName(col), secondMeasurements.getValueAt(0, col));
        }

        // Measure overlap
        temp.clear();
        temp.add(overlap);
        ResultsTableData overlapMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false);
        for (int col = 0; col < overlapMeasurements.getColumnCount(); col++) {
            variableSet.set("Overlap." + overlapMeasurements.getColumnName(col), overlapMeasurements.getValueAt(0, col));
        }
    }

    private Roi calculateOverlap(ROIListData temp, Roi roi1, Roi roi2) {
        temp.clear();
        temp.add(roi1);
        temp.add(roi2);
        temp.logicalAnd();
        if (!temp.isEmpty()) {
            Roi roi = temp.get(0);
            if (roi.getBounds().isEmpty())
                return null;
            return roi;
        }
        return null;
    }

    @JIPipeDocumentation(name = "Deactivated outputs", description = "Here you can disable the ROI overlapping for the " +
            "specified output slots. This will increase the performance.")
    @JIPipeParameter("deactivated-outputs")
    public OutputSlotMapParameterCollection getDeactivatedOutputs() {
        return deactivatedOutputs;
    }

    @JIPipeDocumentation(name = "Invert", description = "If enabled, ROIs are output if there is no overlap to other ROI sets.")
    @JIPipeParameter("invert")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @JIPipeDocumentation(name = "Multi-input operation", description = "Operation applied to the overlap test if there are more than two inputs. " +
            "For example, you can test for overlaps of Input 1 with 2 or 3.")
    @JIPipeParameter("logical-operation")
    public LogicalOperation getLogicalOperation() {
        return logicalOperation;
    }

    @JIPipeParameter("logical-operation")
    public void setLogicalOperation(LogicalOperation logicalOperation) {
        this.logicalOperation = logicalOperation;
    }

    @JIPipeDocumentation(name = "Overlap filter", description = "This filter is applied to any combination of ROIs that have an overlap. You will have three sets of measurements: First, Overlap, and Second." +
            "'First' is the currently filtered ROI item. 'Second' is the tested item. 'Overlap' is the overlap between these ROI." +
            " Please open the expression builder to see a list of all available variables. If the filter is empty, " +
            "no filtering is applied.")
    @JIPipeParameter("overlap-filter")
    @ExpressionParameterSettings(variableSource = RoiOverlapStatisticsVariableSource.class)
    public DefaultExpressionParameter getOverlapFilter() {
        return overlapFilter;
    }

    @JIPipeParameter("overlap-filter")
    public void setOverlapFilter(DefaultExpressionParameter overlapFilter) {
        this.overlapFilter = overlapFilter;
    }

    @JIPipeDocumentation(name = "Output overlapping regions", description = "If enabled, the overlapping regions, instead of the ROI are extracted.")
    @JIPipeParameter("output-overlaps")
    public boolean isOutputOverlaps() {
        return outputOverlaps;
    }

    @JIPipeParameter("output-overlaps")
    public void setOutputOverlaps(boolean outputOverlaps) {
        this.outputOverlaps = outputOverlaps;
    }

    @JIPipeDocumentation(name = "Overlap filter measurements", description = "Measurements extracted for the overlap filter.")
    @JIPipeParameter("overlap-filter-measurements")
    public ImageStatisticsSetParameter getOverlapFilterMeasurements() {
        return overlapFilterMeasurements;
    }

    @JIPipeParameter("overlap-filter-measurements")
    public void setOverlapFilterMeasurements(ImageStatisticsSetParameter overlapFilterMeasurements) {
        this.overlapFilterMeasurements = overlapFilterMeasurements;
    }

}
