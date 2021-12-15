package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.RoiOverlapStatisticsVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.utils.ResourceUtils;

@JIPipeDocumentation(name = "Filter Labels by overlap", description = "Filters the labels by testing for mutual overlap. The Labels1 output contains all Labels1 input labels that overlap with any of Labels2. " +
        "The Labels2 output contains all Labels2 input labels that overlap with a Labels1 Labels.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labels")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 1", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 2", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 1", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 2", autoCreate = true)
public class FilterLabelsByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter overlapFilterMeasurements = new ImageStatisticsSetParameter();
    private LabelFilterSettings roi1Settings = new LabelFilterSettings();
    private LabelFilterSettings roi2Settings = new LabelFilterSettings();

    public FilterLabelsByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
        getEventBus().register(this);
    }

    public FilterLabelsByOverlapAlgorithm(FilterLabelsByOverlapAlgorithm other) {
        super(other);
        this.roi1Settings = new LabelFilterSettings(other.roi1Settings);
        this.roi2Settings = new LabelFilterSettings(other.roi2Settings);
        this.overlapFilterMeasurements = new ImageStatisticsSetParameter(other.overlapFilterMeasurements);
        registerSubParameter(roi1Settings);
        registerSubParameter(roi2Settings);
        updateSlots();
        getEventBus().register(this);
    }

    private void updateSlots() {
        if (roi1Settings.isEnabled()) {
            if (!getOutputSlotMap().containsKey("Labels 1")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addOutputSlot("Labels 1", ImagePlusGreyscaleData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Labels 1")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeOutputSlot("Labels 1", false);
            }
        }
        if (roi2Settings.isEnabled()) {
            if (!getOutputSlotMap().containsKey("Labels 2")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addOutputSlot("Labels 2", ImagePlusGreyscaleData.class, null, false);
            }
        } else {
            if (getOutputSlotMap().containsKey("Labels 2")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeOutputSlot("Labels 2", false);
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

//        ImagePlusGreyscaleData roi1_original = dataBatch.getInputData("Labels 1", ImagePlusGreyscaleData.class, progressInfo);
//        ImagePlusGreyscaleData roi2_original = dataBatch.getInputData("Labels 2", ImagePlusGreyscaleData.class, progressInfo);
//
//        ImagePlus referenceImage = ImagePlusGreyscaleData.createDummyImageFor(Arrays.asList(roi1_original, roi2_original));
//
//        if (roi1Settings.isEnabled()) {
//            ImagePlus roi1 = roi1_original.getDuplicateImage();
//            ImagePlus roi2 = roi2_original.getDuplicateImage();
//            applyFiltering(roi1,
//                    roi2,
//                    "Labels1",
//                    "Labels2",
//                    getOutputSlot("Labels 1"),
//                    referenceImage,
//                    roi1Settings,
//                    dataBatch,
//                    progressInfo.resolveAndLog("Labels 1 filtering"));
//        }
//        if (roi2Settings.isEnabled()) {
//            ImagePlus roi1 = roi1_original.getDuplicateImage();
//            ImagePlus roi2 = roi2_original.getDuplicateImage();
//            applyFiltering(roi2,
//                    roi1,
//                    "Labels2",
//                    "Labels1",
//                    getOutputSlot("Labels 2"),
//                    referenceImage,
//                    roi2Settings,
//                    dataBatch,
//                    progressInfo.resolveAndLog("Labels 2 filtering"));
//        }
    }

//    private void applyFiltering(ImagePlusGreyscaleData first, ImagePlusGreyscaleData second, String firstPrefix, String secondPrefix, JIPipeDataSlot outputSlot, ImagePlus referenceImage, LabelFilterSettings settings, JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
//        boolean withFiltering = !StringUtils.isNullOrEmpty(settings.getOverlapFilter().getExpression());
//        ExpressionVariables variableSet = new ExpressionVariables();
//        ImagePlusGreyscaleData temp = new ImagePlusGreyscaleData();
//        ImagePlusGreyscaleData result = new ImagePlusGreyscaleData();
//
//        // Apply comparison
//        for (int i = 0; i < first.size(); i++) {
//            if (i % 100 == 0)
//                progressInfo.resolveAndLog("Labels", i, first.size());
//            Roi roi = first.get(i);
//            List<Roi> overlaps = new ArrayList<>();
//            boolean overlapSuccess = false;
//            {
//                Roi overlap = null;
//                Roi overlappingRoi = null;
//                for (Roi roi2 : second) {
//                    overlappingRoi = roi2;
//                    overlap = calculateOverlap(temp, roi, roi2, settings.isFastMode());
//                    if (overlap != null) {
//                        if (withFiltering) {
//                            putMeasurementsIntoVariable(roi, firstPrefix, roi2, secondPrefix, variableSet, overlap, referenceImage, temp);
//                            if (settings.getOverlapFilter().test(variableSet))
//                                break;
//                            else
//                                overlap = null;
//                        } else {
//                            break;
//                        }
//                    }
//                }
//                if (overlap != null) {
//                    overlaps.add(overlap);
//                    if (settings.isConsumeOnOverlap()) {
//                        // We consumed this overlap. Remove Roi2
//                        second.remove(overlappingRoi);
//                    }
//                    overlapSuccess = true;
//                }
//            }
//            if (settings.isInvert())
//                overlapSuccess = !overlapSuccess;
//            if (overlapSuccess) {
//                if (settings.isOutputOverlaps()) {
//                    result.addAll(overlaps);
//                } else {
//                    result.add(roi);
//                }
//            }
//        }
//
//        // Finished. Save to output
//        dataBatch.addOutputData(outputSlot, result, progressInfo);
//    }
//
//    private void putMeasurementsIntoVariable(Roi first, String firstPrefix, Roi second, String secondPrefix, ExpressionVariables variableSet, Roi overlap, ImagePlus referenceImage, ImagePlusGreyscaleData temp) {
//
//        variableSet.set(firstPrefix + ".z", first.getZPosition());
//        variableSet.set(firstPrefix + ".c", first.getCPosition());
//        variableSet.set(firstPrefix + ".t", first.getTPosition());
//        variableSet.set(firstPrefix + ".name", StringUtils.nullToEmpty(first.getName()));
//        variableSet.set(secondPrefix + ".z", second.getZPosition());
//        variableSet.set(secondPrefix + ".c", second.getCPosition());
//        variableSet.set(secondPrefix + ".t", second.getTPosition());
//        variableSet.set(secondPrefix + ".name", StringUtils.nullToEmpty(second.getName()));
//
//        // Add first Labels info
//        temp.clear();
//        temp.add(first);
//        ResultsTableData firstMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false);
//        for (int col = 0; col < firstMeasurements.getColumnCount(); col++) {
//            variableSet.set(firstPrefix + "." + firstMeasurements.getColumnName(col), firstMeasurements.getValueAt(0, col));
//        }
//
//        // Add second Labels info
//        temp.clear();
//        temp.add(second);
//        ResultsTableData secondMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false);
//        for (int col = 0; col < secondMeasurements.getColumnCount(); col++) {
//            variableSet.set(secondPrefix + "." + secondMeasurements.getColumnName(col), secondMeasurements.getValueAt(0, col));
//        }
//
//        // Measure overlap
//        temp.clear();
//        temp.add(overlap);
//        ResultsTableData overlapMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false);
//        for (int col = 0; col < overlapMeasurements.getColumnCount(); col++) {
//            variableSet.set("Overlap." + overlapMeasurements.getColumnName(col), overlapMeasurements.getValueAt(0, col));
//        }
//    }

    private void calculateOverlap(ByteProcessor overlap, ImageProcessor roi1, ImageProcessor roi2, int label) {
        final int numPixels = overlap.getWidth() * overlap.getHeight();
        byte[] overlapPixels = (byte[]) overlap.getPixels();
        for (int i = 0; i < numPixels; i++) {
            int roi1Label = (int) roi1.getf(i);
            int roi2Label = (int) roi2.getf(i);
            if(roi1Label == label && roi2Label == roi1Label) {
                overlapPixels[i] = (byte)255;
            }
            else {
                overlapPixels[i] = 0;
            }
        }
    }

    @JIPipeDocumentation(name = "Labels 1 filter", description = "Use following settings to determine how inputs into <b>Labels 1</b> are filtered " +
            "(by overlapping them with items in <b>Labels 2</b>). " +
            "Filtered Labels will be put into the corresponding <b>Labels 1</b> output.")
    @JIPipeParameter(value = "roi1", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/roi.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/roi.png")
    public LabelFilterSettings getRoi1Settings() {
        return roi1Settings;
    }

    @JIPipeDocumentation(name = "Labels 2 filter", description = "Use following settings to determine how inputs into <b>Labels 2</b> are filtered " +
            "(by overlapping them with items in <b>Labels 1</b>). " +
            "Filtered Labels will be put into the corresponding <b>Labels 2</b> output.")
    @JIPipeParameter(value = "roi2", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/roi.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/roi.png")
    public LabelFilterSettings getRoi2Settings() {
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

    public static class LabelFilterSettings implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();

        private boolean enabled = true;
        private boolean invert = false;
        private boolean outputOverlaps = false;
        private DefaultExpressionParameter overlapFilter = new DefaultExpressionParameter();

        public LabelFilterSettings() {
        }

        public LabelFilterSettings(LabelFilterSettings other) {
            this.enabled = other.enabled;
            this.invert = other.invert;
            this.outputOverlaps = other.outputOverlaps;
            this.overlapFilter = new DefaultExpressionParameter(other.overlapFilter);
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

        @JIPipeDocumentation(name = "Invert", description = "If enabled, labels are stored into the output if they do not overlap.")
        @JIPipeParameter("invert")
        public boolean isInvert() {
            return invert;
        }

        @JIPipeParameter("invert")
        public void setInvert(boolean invert) {
            this.invert = invert;
        }

        @JIPipeDocumentation(name = "Overlap filter", description = "This filter is applied to any combination of Labelss that have an overlap. You will have three sets of measurements: Labels1, Overlap, and Labels2." +
                "'Labels1'and 'Labels2' correspond to a Labels from the input slots,respectively. 'Overlap' is the overlap between these labels." +
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

        @JIPipeDocumentation(name = "Output overlapping regions", description = "If enabled, the overlapping regions, instead of the Labels are extracted.")
        @JIPipeParameter("output-overlaps")
        public boolean isOutputOverlaps() {
            return outputOverlaps;
        }

        @JIPipeParameter("output-overlaps")
        public void setOutputOverlaps(boolean outputOverlaps) {
            this.outputOverlaps = outputOverlaps;
        }
    }
}
