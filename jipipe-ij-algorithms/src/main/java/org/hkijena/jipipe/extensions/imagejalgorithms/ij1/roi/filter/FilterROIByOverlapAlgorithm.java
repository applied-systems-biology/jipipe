package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.filter;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.RoiOverlapStatisticsVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

@JIPipeDocumentation(name = "Filter ROI by overlap", description = "Filters the ROI lists by testing for mutual overlap. The ROI1 output contains all ROI1 input ROI that overlap with any of ROI2. " +
        "The ROI2 output contains all ROI2 input ROI that overlap with a ROI1 ROI.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI 1", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI 2", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, description = "An optional reference image", optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI 1", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI 2", autoCreate = true)
public class FilterROIByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter overlapFilterMeasurements = new ImageStatisticsSetParameter();
    private ROIFilterSettings roi1Settings = new ROIFilterSettings();
    private ROIFilterSettings roi2Settings = new ROIFilterSettings();

    public FilterROIByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
        getEventBus().register(this);
    }

    public FilterROIByOverlapAlgorithm(FilterROIByOverlapAlgorithm other) {
        super(other);
        this.roi1Settings = new ROIFilterSettings(other.roi1Settings);
        this.roi2Settings = new ROIFilterSettings(other.roi2Settings);
        this.overlapFilterMeasurements = new ImageStatisticsSetParameter(other.overlapFilterMeasurements);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
        getEventBus().register(this);
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

    @Subscribe
    public void onParameterChanged(ParameterChangedEvent event) {
        if (event.getSource() == roi1Settings || event.getSource() == roi2Settings) {
            if ("enabled".equals(event.getKey())) {
                updateSlots();
            }
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ROIListData roi1_original = dataBatch.getInputData("ROI 1", ROIListData.class, progressInfo);
        ROIListData roi2_original = dataBatch.getInputData("ROI 2", ROIListData.class, progressInfo);

        ImagePlus referenceImage = null;
        {
            ImagePlusData reference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
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
                    dataBatch,
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
                    dataBatch,
                    progressInfo.resolveAndLog("ROI 2 filtering"));
        }
    }

    private void applyFiltering(ROIListData first, ROIListData second, String firstPrefix, String secondPrefix, JIPipeDataSlot outputSlot, ImagePlus referenceImage, ROIFilterSettings settings, JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        boolean withFiltering = !StringUtils.isNullOrEmpty(settings.getOverlapFilter().getExpression());
        ExpressionVariables variableSet = new ExpressionVariables();
        ROIListData temp = new ROIListData();
        ROIListData result = new ROIListData();

        // Write annotations map
        Map<String, String> annotations = new HashMap<>();
        for (Map.Entry<String, JIPipeTextAnnotation> entry : dataBatch.getMergedTextAnnotations().entrySet()) {
            annotations.put(entry.getKey(), entry.getValue().getValue());
        }
        variableSet.set("annotations", annotations);

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
                    overlap = calculateOverlap(temp, roi, roi2, settings.isFastMode());
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
                    }
                }
                if (overlap != null) {
                    overlaps.add(overlap);
                    if (settings.isConsumeOnOverlap()) {
                        // We consumed this overlap. Remove Roi2
                        second.remove(overlappingRoi);
                    }
                    overlapSuccess = true;
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
        dataBatch.addOutputData(outputSlot, result, progressInfo);
    }

    private void putMeasurementsIntoVariable(Roi first, String firstPrefix, Roi second, String secondPrefix, ExpressionVariables variableSet, Roi overlap, ImagePlus referenceImage, ROIListData temp, boolean measureInPhysicalUnits) {

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

        // Measure overlap
        temp.clear();
        temp.add(overlap);
        ResultsTableData overlapMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false, measureInPhysicalUnits);
        for (int col = 0; col < overlapMeasurements.getColumnCount(); col++) {
            variableSet.set("Overlap." + overlapMeasurements.getColumnName(col), overlapMeasurements.getValueAt(0, col));
        }
    }

    private Roi calculateOverlap(ROIListData temp, Roi roi1, Roi roi2, boolean fastMode) {
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

    @JIPipeDocumentation(name = "ROI 1 filter", description = "Use following settings to determine how inputs into <b>ROI 1</b> are filtered " +
            "(by overlapping them with items in <b>ROI 2</b>). " +
            "Filtered ROI will be put into the corresponding <b>ROI 1</b> output.")
    @JIPipeParameter(value = "roi1", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/roi.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/roi.png")
    public ROIFilterSettings getRoi1Settings() {
        return roi1Settings;
    }

    @JIPipeDocumentation(name = "ROI 2 filter", description = "Use following settings to determine how inputs into <b>ROI 2</b> are filtered " +
            "(by overlapping them with items in <b>ROI 1</b>). " +
            "Filtered ROI will be put into the corresponding <b>ROI 2</b> output.")
    @JIPipeParameter(value = "roi2", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/roi.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/roi.png")
    public ROIFilterSettings getRoi2Settings() {
        return roi2Settings;
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

    public static class ROIFilterSettings implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();

        private boolean enabled = true;
        private boolean invert = false;
        private boolean outputOverlaps = false;
        private DefaultExpressionParameter overlapFilter = new DefaultExpressionParameter();
        private boolean consumeOnOverlap = false;
        private boolean fastMode = false;

        private boolean measureInPhysicalUnits = true;

        public ROIFilterSettings() {
        }

        public ROIFilterSettings(ROIFilterSettings other) {
            this.enabled = other.enabled;
            this.invert = other.invert;
            this.outputOverlaps = other.outputOverlaps;
            this.overlapFilter = new DefaultExpressionParameter(other.overlapFilter);
            this.consumeOnOverlap = other.consumeOnOverlap;
            this.fastMode = other.fastMode;
            this.measureInPhysicalUnits = other.measureInPhysicalUnits;
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Enabled", description = "You can use this setting to disable generating this output.")
        @JIPipeParameter("enabled")
        public boolean isEnabled() {
            return enabled;
        }

        @JIPipeParameter("enabled")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @JIPipeDocumentation(name = "Invert", description = "If enabled, ROI are stored into the output if they do not overlap.")
        @JIPipeParameter("invert")
        public boolean isInvert() {
            return invert;
        }

        @JIPipeParameter("invert")
        public void setInvert(boolean invert) {
            this.invert = invert;
        }

        @JIPipeDocumentation(name = "Consume on overlap", description = "If enabled, ROI are consumed if an overlap is detected, meaning " +
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

        @JIPipeDocumentation(name = "Overlap filter", description = "This filter is applied to any combination of ROIs that have an overlap. You will have three sets of measurements: ROI1, Overlap, and ROI2." +
                "'ROI1'and 'ROI2' correspond to a ROI from the input slots,respectively. 'Overlap' is the overlap between these ROI." +
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

        @JIPipeDocumentation(name = "Fast mode", description = "If enabled, only the bounding box is used to calculate overlaps. " +
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

        @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available. " +
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
