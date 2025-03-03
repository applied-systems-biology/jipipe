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
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DRelationMeasurementExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.ROI3DRelationMeasurementSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

@SetJIPipeDocumentation(name = "Filter 3D ROI by overlap", description = "Filters the 3D ROI lists by testing for mutual overlap. The ROI1 output contains all ROI1 input ROI that overlap with any of ROI2. " +
        "The ROI2 output contains all ROI2 input ROI that overlap with a ROI1 ROI.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "ROI 1", create = true)
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "ROI 2", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, description = "An optional reference image", optional = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "ROI 1", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "ROI 2", create = true)
public class FilterRoi3DByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    private ROI3DRelationMeasurementSetParameter overlapFilterMeasurements = new ROI3DRelationMeasurementSetParameter();
    private ROIFilterSettings roi1Settings = new ROIFilterSettings();
    private ROIFilterSettings roi2Settings = new ROIFilterSettings();

    public FilterRoi3DByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
    }

    public FilterRoi3DByOverlapAlgorithm(FilterRoi3DByOverlapAlgorithm other) {
        super(other);
        this.roi1Settings = new ROIFilterSettings(other.roi1Settings);
        this.roi2Settings = new ROIFilterSettings(other.roi2Settings);
        this.overlapFilterMeasurements = new ROI3DRelationMeasurementSetParameter(other.overlapFilterMeasurements);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
    }

    private void updateSlots() {
        if (roi1Settings.isEnabled()) {
            if (!getOutputSlotMap().containsKey("ROI 1")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addOutputSlot("ROI 1", "The first set of ROI", ROI3DListData.class, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("ROI 1")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeOutputSlot("ROI 1", false);
            }
        }
        if (roi2Settings.isEnabled()) {
            if (!getOutputSlotMap().containsKey("ROI 2")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addOutputSlot("ROI 2", "The second set of ROI", ROI3DListData.class, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("ROI 2")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeOutputSlot("ROI 2", false);
            }
        }
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if (event.getSource() == roi1Settings || event.getSource() == roi2Settings) {
            if ("enabled".equals(event.getKey())) {
                updateSlots();
            }
        }
    }


    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi1List = iterationStep.getInputData("ROI 1", ROI3DListData.class, progressInfo);
        ROI3DListData roi2List = iterationStep.getInputData("ROI 2", ROI3DListData.class, progressInfo);
        ImageHandler imageHandler = IJ3DUtils.wrapImage(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));

        if (roi1Settings.isEnabled()) {

            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

            ROI3DListData copy1 = new ROI3DListData();
            ROI3DListData copy2 = new ROI3DListData();
            copy1.addAll(roi1List);
            copy2.addAll(roi2List);
            ROI3DListData filtered = applyFilter(copy1, copy2, roi1Settings, imageHandler, variables, progressInfo.resolve("Filter ROI 1"));
            iterationStep.addOutputData("ROI 1", filtered, progressInfo);
        }
        if (roi2Settings.isEnabled()) {

            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

            ROI3DListData copy1 = new ROI3DListData();
            ROI3DListData copy2 = new ROI3DListData();
            copy1.addAll(roi1List);
            copy2.addAll(roi2List);
            ROI3DListData filtered = applyFilter(copy2, copy1, roi2Settings, imageHandler, variables, progressInfo.resolve("Filter ROI 2"));
            iterationStep.addOutputData("ROI 1", filtered, progressInfo);
        }
    }

    private ROI3DListData applyFilter(ROI3DListData roi1List, ROI3DListData roi2List, ROIFilterSettings settings, ImageHandler imageHandler, JIPipeExpressionVariablesMap variables, JIPipeProgressInfo progressInfo) {
        ROI3DListData output = new ROI3DListData();
        ResultsTableData measurements = new ResultsTableData();
        IJ3DUtils.measureRoi3dRelation(imageHandler,
                roi1List,
                roi2List,
                overlapFilterMeasurements.getNativeValue(),
                settings.isMeasureInPhysicalUnits(),
                settings.isRequireColocalization(),
                settings.isPreciseColocalization(),
                settings.isIgnoreC(),
                settings.isIgnoreT(),
                "",
                measurements,
                progressInfo.resolve("Measure Overlaps"));

        progressInfo.log("Processing measurements ...");
        Multimap<Integer, Integer> roi1To2Overlaps = HashMultimap.create();

        for (int row = 0; row < measurements.getRowCount(); row++) {
            int roi1Index = (int) measurements.getValueAsDouble(row, "Current.Index");
            int roi2Index = (int) measurements.getValueAsDouble(row, "Other.Index");

            if (StringUtils.isNullOrEmpty(settings.overlapFilter.getExpression())) {
                if (settings.requireColocalization && settings.preciseColocalization) {
                    // Already fulfilled
                    roi1To2Overlaps.put(roi1Index, roi2Index);
                } else if (measurements.containsColumn("Colocalization")) {
                    if (measurements.getValueAsDouble(row, "Colocalization") > 0) {
                        roi1To2Overlaps.put(roi1Index, roi2Index);
                    }
                } else {
                    ROI3D roi1 = roi1List.get(roi1Index);
                    ROI3D roi2 = roi2List.get((int) measurements.getValueAsDouble(row, "Roi2.Index"));
                    if (roi1.getObject3D().hasOneVoxelColoc(roi2.getObject3D())) {
                        roi1To2Overlaps.put(roi1Index, roi2Index);
                    }
                }
            } else {
                for (int col = 0; col < measurements.getColumnCount(); col++) {
                    variables.set(measurements.getColumnName(col), measurements.getValueAt(row, col));
                }
                if (settings.overlapFilter.test(variables)) {
                    roi1To2Overlaps.put(roi1Index, roi2Index);
                }
            }
        }

        TIntSet consumedRoi2 = new TIntHashSet();
        for (int i = 0; i < roi1List.size(); i++) {
            TIntSet overlappingRoi2 = new TIntHashSet(roi1To2Overlaps.get(i));
            if (settings.consumeOnOverlap) {
                overlappingRoi2.removeAll(consumedRoi2);
            }
            boolean canOutput;
            if (settings.invert) {
                canOutput = overlappingRoi2.isEmpty();
            } else {
                canOutput = !overlappingRoi2.isEmpty();
            }
            if (settings.consumeOnOverlap && canOutput && !overlappingRoi2.isEmpty()) {
                consumedRoi2.add(overlappingRoi2.iterator().next());
            }
            if (canOutput) {
                output.add(roi1List.get(i));
            }
        }

        return output;
    }

    @SetJIPipeDocumentation(name = "ROI 1 filter", description = "Use following settings to determine how inputs into <b>ROI 1</b> are filtered " +
            "(by overlapping them with items in <b>ROI 2</b>). " +
            "Filtered ROI will be put into the corresponding <b>ROI 1</b> output.")
    @JIPipeParameter(value = "roi1", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/roi.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/roi.png")
    public ROIFilterSettings getRoi1Settings() {
        return roi1Settings;
    }

    @SetJIPipeDocumentation(name = "ROI 2 filter", description = "Use following settings to determine how inputs into <b>ROI 2</b> are filtered " +
            "(by overlapping them with items in <b>ROI 1</b>). " +
            "Filtered ROI will be put into the corresponding <b>ROI 2</b> output.")
    @JIPipeParameter(value = "roi2", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/roi.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/roi.png")
    public ROIFilterSettings getRoi2Settings() {
        return roi2Settings;
    }

    @SetJIPipeDocumentation(name = "Overlap filter measurements", description = "Measurements extracted for the overlap filter.")
    @JIPipeParameter("overlap-filter-measurements")
    public ROI3DRelationMeasurementSetParameter getOverlapFilterMeasurements() {
        return overlapFilterMeasurements;
    }

    @JIPipeParameter("overlap-filter-measurements")
    public void setOverlapFilterMeasurements(ROI3DRelationMeasurementSetParameter overlapFilterMeasurements) {
        this.overlapFilterMeasurements = overlapFilterMeasurements;
    }

    public static class ROIFilterSettings extends AbstractJIPipeParameterCollection {
        private boolean enabled = true;
        private boolean invert = false;
        private boolean outputOverlaps = false;
        private JIPipeExpressionParameter overlapFilter = new JIPipeExpressionParameter();
        private boolean consumeOnOverlap = false;

        private boolean measureInPhysicalUnits = true;

        private boolean requireColocalization = true;

        private boolean preciseColocalization = true;

        private boolean ignoreC = true;
        private boolean ignoreT = true;

        public ROIFilterSettings() {
        }

        public ROIFilterSettings(ROIFilterSettings other) {
            this.enabled = other.enabled;
            this.invert = other.invert;
            this.outputOverlaps = other.outputOverlaps;
            this.overlapFilter = new JIPipeExpressionParameter(other.overlapFilter);
            this.consumeOnOverlap = other.consumeOnOverlap;
            this.measureInPhysicalUnits = other.measureInPhysicalUnits;
            this.requireColocalization = other.requireColocalization;
            this.preciseColocalization = other.preciseColocalization;
            this.ignoreC = other.ignoreC;
            this.ignoreT = other.ignoreT;
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

        @SetJIPipeDocumentation(name = "Enabled", description = "You can use this setting to disable generating this output.")
        @JIPipeParameter("enabled")
        public boolean isEnabled() {
            return enabled;
        }

        @JIPipeParameter("enabled")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @SetJIPipeDocumentation(name = "Invert", description = "If enabled, ROI are stored into the output if they do not overlap.")
        @JIPipeParameter("invert")
        public boolean isInvert() {
            return invert;
        }

        @JIPipeParameter("invert")
        public void setInvert(boolean invert) {
            this.invert = invert;
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

        @SetJIPipeDocumentation(name = "Overlap filter", description = "This filter is applied to any combination of ROIs that have an overlap. You will have three sets of measurements: ROI1, Overlap, and ROI2." +
                "'ROI1'and 'ROI2' correspond to a ROI from the input slots,respectively. 'Overlap' is the overlap between these ROI." +
                " Please open the expression builder to see a list of all available variables. If the filter is empty, " +
                "no filtering is applied.")
        @JIPipeParameter("overlap-filter")
        @JIPipeExpressionParameterSettings(hint = "per overlapping ROI")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = ROI3DRelationMeasurementExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
        public JIPipeExpressionParameter getOverlapFilter() {
            return overlapFilter;
        }

        @JIPipeParameter("overlap-filter")
        public void setOverlapFilter(JIPipeExpressionParameter overlapFilter) {
            this.overlapFilter = overlapFilter;
        }

        @SetJIPipeDocumentation(name = "Output overlapping regions", description = "If enabled, the overlapping regions, instead of the ROI are extracted.")
        @JIPipeParameter("output-overlaps")
        public boolean isOutputOverlaps() {
            return outputOverlaps;
        }

        @JIPipeParameter("output-overlaps")
        public void setOutputOverlaps(boolean outputOverlaps) {
            this.outputOverlaps = outputOverlaps;
        }

        @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available. " +
                "Measurements will be in the physical sizes of the reference image.")
        @JIPipeParameter("measure-in-physical-units")
        public boolean isMeasureInPhysicalUnits() {
            return measureInPhysicalUnits;
        }

        @JIPipeParameter("measure-in-physical-units")
        public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
            this.measureInPhysicalUnits = measureInPhysicalUnits;
        }

        @SetJIPipeDocumentation(name = "Only measure if objects co-localize", description = "If enabled, only co-localizing objects are measured")
        @JIPipeParameter("require-colocalization")
        public boolean isRequireColocalization() {
            return requireColocalization;
        }

        @JIPipeParameter("require-colocalization")
        public void setRequireColocalization(boolean requireColocalization) {
            this.requireColocalization = requireColocalization;
        }

        @SetJIPipeDocumentation(name = "Precise colocalization", description = "If enabled, the object co-localization for the 'Only measure if objects co-localize' setting tests for voxel colocalization (slower)." +
                " Otherwise, only the bounding boxes are compared (faster).")
        @JIPipeParameter("precise-colocalization")
        public boolean isPreciseColocalization() {
            return preciseColocalization;
        }

        @JIPipeParameter("precise-colocalization")
        public void setPreciseColocalization(boolean preciseColocalization) {
            this.preciseColocalization = preciseColocalization;
        }
    }
}
