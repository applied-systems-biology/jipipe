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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.filter;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
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
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.RoiOverlapStatisticsVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

@SetJIPipeDocumentation(name = "Filter ROI by overlap", description = "Filters the ROI lists by testing for mutual overlap. The ROI1 output contains all ROI1 input ROI that overlap with any of ROI2. " +
        "The ROI2 output contains all ROI2 input ROI that overlap with a ROI1 ROI.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI 1", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI 2", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true, description = "An optional reference image", optional = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI 1", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI 2", create = true)
public class FilterROIByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter overlapFilterMeasurements = new ImageStatisticsSetParameter();
    private ROIFilterSettings roi1Settings = new ROIFilterSettings();
    private ROIFilterSettings roi2Settings = new ROIFilterSettings();

    public FilterROIByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
    }

    public FilterROIByOverlapAlgorithm(FilterROIByOverlapAlgorithm other) {
        super(other);
        this.roi1Settings = new ROIFilterSettings(other.roi1Settings);
        this.roi2Settings = new ROIFilterSettings(other.roi2Settings);
        this.overlapFilterMeasurements = new ImageStatisticsSetParameter(other.overlapFilterMeasurements);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
    }

    private void updateSlots() {
        if (roi1Settings.isEnabled()) {
            if (!getOutputSlotMap().containsKey("ROI 1")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addOutputSlot("ROI 1", "The first set of ROI", ROIListData.class, null, false);
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
                slotConfiguration.addOutputSlot("ROI 2", "The second set of ROI", ROIListData.class, null, false);
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

        ROIListData roi1_original = iterationStep.getInputData("ROI 1", ROIListData.class, progressInfo);
        ROIListData roi2_original = iterationStep.getInputData("ROI 2", ROIListData.class, progressInfo);

        ImagePlus referenceImage = null;
        {
            ImagePlusData reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);
            if (reference != null) {
                referenceImage = reference.getDuplicateImage(); // Measurements tend to break the image
            }
        }
        if (referenceImage == null) {
            referenceImage = ROIListData.createDummyImageFor(Arrays.asList(roi1_original, roi2_original));
        }
        if (roi1Settings.isEnabled()) {
            ROIListData roi1 = new ROIListData(roi1_original);
            ROIListData roi2 = new ROIListData(roi2_original);
            applyFiltering(roi1,
                    roi2,
                    "ROI1",
                    "ROI2",
                    getOutputSlot("ROI 1"),
                    referenceImage,
                    roi1Settings,
                    iterationStep,
                    progressInfo.resolveAndLog("ROI 1 filtering"));
        }
        if (roi2Settings.isEnabled()) {
            ROIListData roi1 = new ROIListData(roi1_original);
            ROIListData roi2 = new ROIListData(roi2_original);
            applyFiltering(roi2,
                    roi1,
                    "ROI2",
                    "ROI1",
                    getOutputSlot("ROI 2"),
                    referenceImage,
                    roi2Settings,
                    iterationStep,
                    progressInfo.resolveAndLog("ROI 2 filtering"));
        }
    }

    private void applyFiltering(ROIListData first, ROIListData second, String firstPrefix, String secondPrefix, JIPipeOutputDataSlot outputSlot, ImagePlus referenceImage, ROIFilterSettings settings, JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo) {
        boolean withFiltering = !StringUtils.isNullOrEmpty(settings.getOverlapFilter().getExpression());
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        ROIListData temp = new ROIListData();
        ROIListData result = new ROIListData();

        // Write annotations map
        Map<String, String> annotations = new HashMap<>();
        for (Map.Entry<String, JIPipeTextAnnotation> entry : iterationStep.getMergedTextAnnotations().entrySet()) {
            annotations.put(entry.getKey(), entry.getValue().getValue());
        }
        variableSet.set("annotations", annotations);
        getDefaultCustomExpressionVariables().writeToVariables(variableSet);

        // Apply comparison
        for (int i = 0; i < first.size(); i++) {
            if (i % 100 == 0)
                progressInfo.resolveAndLog("ROI", i, first.size());
            Roi roi = first.get(i);
            List<Roi> overlaps = new ArrayList<>();
            boolean overlapSuccess = false;
            {
                Roi overlap = null;
                Roi overlappingRoi = null;
                for (Roi roi2 : second) {

                    overlappingRoi = roi2;
                    overlap = calculateOverlap(temp, roi, roi2, settings.isFastMode(), settings.ignoreC, settings.ignoreT);
                    if (overlap != null) {
                        if (withFiltering) {
                            putMeasurementsIntoVariable(roi, firstPrefix, roi2, secondPrefix, variableSet, overlap, referenceImage, temp, settings.measureInPhysicalUnits);
                            if (settings.getOverlapFilter().test(variableSet))
                                break;
                            else
                                overlap = null;
                        } else {
                            break;
                        }
                    } else if (!settings.enforceOverlap && withFiltering) {
                        putMeasurementsIntoVariable(roi, firstPrefix, roi2, secondPrefix, variableSet, overlap, referenceImage, temp, settings.measureInPhysicalUnits);
                        if (settings.getOverlapFilter().test(variableSet)) {
                            overlapSuccess = true;
                            break;
                        }
                    }
                }
                if (overlap != null) {
                    overlaps.add(overlap);
                    if (settings.isConsumeOnOverlap()) {
                        // We consumed this overlap. Remove Roi2
                        second.remove(overlappingRoi);
                    }
                    overlapSuccess = true;
                } else if (overlapSuccess) {
                    if (settings.isConsumeOnOverlap()) {
                        // We consumed this overlap. Remove Roi2
                        second.remove(overlappingRoi);
                    }
                }
            }
            if (settings.isInvert())
                overlapSuccess = !overlapSuccess;
            if (overlapSuccess) {
                if (settings.isOutputOverlaps()) {
                    result.addAll(overlaps);
                } else {
                    result.add(roi);
                }
            }
        }

        // Finished. Save to output
        iterationStep.addOutputData(outputSlot, result, progressInfo);
    }

    private void putMeasurementsIntoVariable(Roi first, String firstPrefix, Roi second, String secondPrefix, JIPipeExpressionVariablesMap variableSet, Roi overlap, ImagePlus referenceImage, ROIListData temp, boolean measureInPhysicalUnits) {

        variableSet.set(firstPrefix + ".z", first.getZPosition());
        variableSet.set(firstPrefix + ".c", first.getCPosition());
        variableSet.set(firstPrefix + ".t", first.getTPosition());
        variableSet.set(firstPrefix + ".name", StringUtils.nullToEmpty(first.getName()));
        variableSet.set(secondPrefix + ".z", second.getZPosition());
        variableSet.set(secondPrefix + ".c", second.getCPosition());
        variableSet.set(secondPrefix + ".t", second.getTPosition());
        variableSet.set(secondPrefix + ".name", StringUtils.nullToEmpty(second.getName()));

        // Add first ROI info
        temp.clear();
        temp.add(first);
        ResultsTableData firstMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false, measureInPhysicalUnits);
        for (int col = 0; col < firstMeasurements.getColumnCount(); col++) {
            variableSet.set(firstPrefix + "." + firstMeasurements.getColumnName(col), firstMeasurements.getValueAt(0, col));
        }

        // Add second ROI info
        temp.clear();
        temp.add(second);
        ResultsTableData secondMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false, measureInPhysicalUnits);
        for (int col = 0; col < secondMeasurements.getColumnCount(); col++) {
            variableSet.set(secondPrefix + "." + secondMeasurements.getColumnName(col), secondMeasurements.getValueAt(0, col));
        }

        if (overlap != null) {
            // Measure overlap
            temp.clear();
            temp.add(overlap);
            ResultsTableData overlapMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false, measureInPhysicalUnits);
            for (int col = 0; col < overlapMeasurements.getColumnCount(); col++) {
                variableSet.set("Overlap." + overlapMeasurements.getColumnName(col), overlapMeasurements.getValueAt(0, col));
            }
        }
    }

    private Roi calculateOverlap(ROIListData temp, Roi roi1, Roi roi2, boolean fastMode, boolean ignoreC, boolean ignoreT) {

        if (!ignoreC) {
            if (roi1.getCPosition() != roi2.getCPosition() && (roi1.getCPosition() > 0 || roi1.getCPosition() > 0)) {
                return null;
            }
        }
        if (!ignoreT) {
            if (roi1.getTPosition() != roi2.getTPosition() && (roi1.getTPosition() > 0 || roi1.getTPosition() > 0)) {
                return null;
            }
        }

        if (fastMode) {
            Rectangle intersection = roi1.getBounds().intersection(roi2.getBounds());
            if (intersection.isEmpty())
                return null;
            else
                return new Roi(intersection);
        }

        temp.clear();
        // BB check for optimization
        if (!roi1.getBounds().intersects(roi2.getBounds()))
            return null;
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

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
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
    public ImageStatisticsSetParameter getOverlapFilterMeasurements() {
        return overlapFilterMeasurements;
    }

    @JIPipeParameter("overlap-filter-measurements")
    public void setOverlapFilterMeasurements(ImageStatisticsSetParameter overlapFilterMeasurements) {
        this.overlapFilterMeasurements = overlapFilterMeasurements;
    }

    public static class ROIFilterSettings extends AbstractJIPipeParameterCollection {
        private boolean enabled = true;
        private boolean invert = false;
        private boolean outputOverlaps = false;
        private JIPipeExpressionParameter overlapFilter = new JIPipeExpressionParameter();
        private boolean consumeOnOverlap = false;

        private boolean enforceOverlap = true;
        private boolean fastMode = false;

        private boolean measureInPhysicalUnits = true;
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
            this.fastMode = other.fastMode;
            this.measureInPhysicalUnits = other.measureInPhysicalUnits;
            this.enforceOverlap = other.enforceOverlap;
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

        @SetJIPipeDocumentation(name = "Overlap filter: enforce overlap", description = "If enabled, a pair of ROI is not considered for custom filtering if it does not overlap. Disable this setting if you want to implement special behavior.")
        @JIPipeParameter("enforce-overlap")
        public boolean isEnforceOverlap() {
            return enforceOverlap;
        }

        @JIPipeParameter("enforce-overlap")
        public void setEnforceOverlap(boolean enforceOverlap) {
            this.enforceOverlap = enforceOverlap;
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
        @JIPipeExpressionParameterSettings(variableSource = RoiOverlapStatisticsVariablesInfo.class, hint = "per overlapping ROI")
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

        @SetJIPipeDocumentation(name = "Fast mode", description = "If enabled, only the bounding box is used to calculate overlaps. " +
                "This includes overlaps generated with 'Output overlapping regions'. This is faster than the logical AND operation " +
                "applied by default, but will lead to problems if the ROI are close to each other or very non-box-like.")
        @JIPipeParameter("fast-mode")
        public boolean isFastMode() {
            return fastMode;
        }

        @JIPipeParameter("fast-mode")
        public void setFastMode(boolean fastMode) {
            this.fastMode = fastMode;
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
    }
}
