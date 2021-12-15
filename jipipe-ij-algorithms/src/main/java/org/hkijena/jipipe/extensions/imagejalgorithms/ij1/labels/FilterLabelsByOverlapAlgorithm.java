package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipePercentageProgressInfo;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
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
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils2;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Filter Labels by overlap 2D", description = "Filters the labels by testing for mutual overlap. " +
        "The Labels 1 output contains all Labels 1 input labels that overlap with any of Labels 2. " +
        "The Labels 2 output contains all Labels 2 input labels that overlap with a Labels 1 Labels. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labels")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 1", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 2", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 1", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels 2", autoCreate = true)
public class FilterLabelsByOverlapAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter overlapFilterMeasurements = new ImageStatisticsSetParameter();
    private LabelFilterSettings labels1Settings = new LabelFilterSettings();
    private LabelFilterSettings labels2Settings = new LabelFilterSettings();

    public FilterLabelsByOverlapAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(labels1Settings);
        registerSubParameter(labels2Settings);
        updateSlots();
        getEventBus().register(this);
    }

    public FilterLabelsByOverlapAlgorithm(FilterLabelsByOverlapAlgorithm other) {
        super(other);
        this.labels1Settings = new LabelFilterSettings(other.labels1Settings);
        this.labels2Settings = new LabelFilterSettings(other.labels2Settings);
        this.overlapFilterMeasurements = new ImageStatisticsSetParameter(other.overlapFilterMeasurements);
        registerSubParameter(labels1Settings);
        registerSubParameter(labels2Settings);
        updateSlots();
        getEventBus().register(this);
    }

    private void updateSlots() {
        if (labels1Settings.isEnabled()) {
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
        if (labels2Settings.isEnabled()) {
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
        if (event.getSource() == labels1Settings || event.getSource() == labels2Settings) {
            if ("enabled".equals(event.getKey())) {
                updateSlots();
            }
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ImagePlusGreyscaleData roi1_original = dataBatch.getInputData("Labels 1", ImagePlusGreyscaleData.class, progressInfo);
        ImagePlusGreyscaleData roi2_original = dataBatch.getInputData("Labels 2", ImagePlusGreyscaleData.class, progressInfo);

        if (labels1Settings.isEnabled()) {
            ImagePlus roi1 = roi1_original.getDuplicateImage();
            applyFiltering(roi1,
                    roi2_original.getImage(),
                    "Label1",
                    "Label2",
                    getOutputSlot("Labels 1"),
                    labels1Settings,
                    dataBatch,
                    progressInfo.resolveAndLog("Labels 1 filtering"));
        }
        if (labels2Settings.isEnabled()) {
            ImagePlus roi2 = roi2_original.getDuplicateImage();
            applyFiltering(roi2,
                    roi1_original.getImage(),
                    "Label2",
                    "Label1",
                    getOutputSlot("Labels 2"),
                    labels2Settings,
                    dataBatch,
                    progressInfo.resolveAndLog("Labels 2 filtering"));
        }
    }

    private void applyFiltering(ImagePlus targetLabels, ImagePlus otherLabels, String targetPrefix, String otherPrefix,
                                JIPipeDataSlot outputSlot, LabelFilterSettings settings, JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        boolean withExpression = !StringUtils.isNullOrEmpty(settings.overlapFilter.getExpression());
        ByteProcessor overlap = new ByteProcessor(targetLabels.getWidth(), targetLabels.getHeight());
        ExpressionVariables variables = new ExpressionVariables();

        // Write annotations map
        Map<String, String> annotations = new HashMap<>();
        for (Map.Entry<String, JIPipeAnnotation> entry : dataBatch.getGlobalAnnotations().entrySet()) {
            annotations.put(entry.getKey(), entry.getValue().getValue());
        }
        variables.set("annotations", annotations);

        ImageJUtils.forEachIndexedZCTSlice(targetLabels, (targetLabelProcessor, index) -> {
            ImageProcessor outputProcessor = targetLabelProcessor.duplicate();
            ImageProcessor otherLabelProcessor = ImageJUtils.getClosestSliceZero(otherLabels, index);

            // If we are interested in overlaps, delete the output first
            if(settings.outputOverlaps) {
                outputProcessor.setColor(0);
                outputProcessor.fill();
            }

            ResultsTableData targetLabelMeasurements = null;
            ResultsTableData otherLabelMeasurements = null;
            // Collect where variables are located for a specified label
            TIntIntMap targetLabelMeasurementRows = new TIntIntHashMap();
            TIntIntMap otherLabelMeasurementRows = new TIntIntHashMap();

            if(withExpression) {
                // Apply measurements (global) and store them into the variables
                targetLabelMeasurements = ImageJUtils2.measureLabels(targetLabelProcessor,
                        targetLabelProcessor,
                        overlapFilterMeasurements,
                        index,
                        progressInfo.resolve(index.toString()).resolve(targetPrefix));

                otherLabelMeasurements = ImageJUtils2.measureLabels(otherLabelProcessor,
                        otherLabelProcessor,
                        overlapFilterMeasurements,
                        index,
                        progressInfo.resolve(index.toString()).resolve(otherPrefix));



                for (int row = 0; row < targetLabelMeasurements.getRowCount(); row++) {
                    int labelId = (int) targetLabelMeasurements.getValueAsDouble(row, "label_id");
                    targetLabelMeasurementRows.put(labelId, row);
                }
                for (int row = 0; row < otherLabelMeasurements.getRowCount(); row++) {
                    int labelId = (int) otherLabelMeasurements.getValueAsDouble(row, "label_id");
                    otherLabelMeasurementRows.put(labelId, row);
                }
            }

            TIntSet matchedLabels = new TIntHashSet();
            int[] allTargetLabels = LabelImages.findAllLabels(targetLabelProcessor);
            int[] allOtherLabels = LabelImages.findAllLabels(otherLabelProcessor);

            JIPipePercentageProgressInfo percentageProgressInfo = progressInfo.resolve(index.toString()).percentage("Overlap labels");
            for (int targetLabelIndex = 0; targetLabelIndex < allTargetLabels.length; targetLabelIndex++) {

                // Set variables from table
                if(withExpression) {
                    int row = targetLabelMeasurementRows.get(allTargetLabels[targetLabelIndex]);
                    for (int col = 0; col < targetLabelMeasurements.getColumnCount(); col++) {
                        variables.set(targetPrefix + "." + targetLabelMeasurements.getColumnName(col), targetLabelMeasurements.getValueAt(row, col));
                    }
                }

                for (int otherLabelIndex = 0; otherLabelIndex < allOtherLabels.length; otherLabelIndex++) {
                    percentageProgressInfo.logPercentage(otherLabelIndex + targetLabelIndex * allOtherLabels.length, allTargetLabels.length * allOtherLabels.length);

                    if(progressInfo.isCancelled())
                        return;

                    // Set variables from table
                    if(withExpression) {
                        int row = otherLabelMeasurementRows.get(allOtherLabels[otherLabelIndex]);
                        for (int col = 0; col < otherLabelMeasurements.getColumnCount(); col++) {
                            variables.set(targetPrefix + "." + otherLabelMeasurements.getColumnName(col), otherLabelMeasurements.getValueAt(row, col));
                        }
                    }

                    // Calculate overlap
                    calculateOverlap(overlap,
                            targetLabelProcessor,
                            otherLabelProcessor,
                            allTargetLabels[targetLabelIndex],
                            allOtherLabels[otherLabelIndex]);

                    boolean matched;

                    if(withExpression) {
                        // Apply measurements
                        ResultsTableData overlapMeasurements = ImageJUtils2.measureLabels(overlap, overlap, overlapFilterMeasurements, index, progressInfo);
                        overlapMeasurements.removeColumn("label_id");

                        variables.set("z", index.getZ());
                        variables.set("c", index.getC());
                        variables.set("t", index.getT());

                        if (overlapMeasurements.getRowCount() > 0) {
                            for (int col = 0; col < overlapMeasurements.getColumnCount(); col++) {
                                variables.set("Overlap." + overlapMeasurements.getColumnName(col), overlapMeasurements.getValueAt(0, col));
                            }
                        }
                        matched = overlapMeasurements.getRowCount() > 0 && settings.overlapFilter.test(variables);
                    }
                    else {
                        matched = overlap.getStatistics().max > 0;
//                        System.out.println(allTargetLabels[targetLabelIndex] + "<-> " + allOtherLabels[otherLabelIndex] + " = " + matched);
                    }

                    if (settings.invert)
                        matched = !matched;

                    if (matched && settings.outputOverlaps) {
                        // copy the overlap
                        byte[] overlapBytes = (byte[]) overlap.getPixels();
                        int nPixels = outputProcessor.getWidth() * outputProcessor.getHeight();
                        int label = allTargetLabels[targetLabelIndex];
                        for (int i = 0; i < nPixels; i++) {
                            if ((overlapBytes[i] & 0xff) > 0) {
                                outputProcessor.setf(i, label);
                            }
                        }
                    }

                    // End after first match
                    if(matched) {
                        matchedLabels.add(allTargetLabels[targetLabelIndex]);
                        break;
                    }
                }
            }

            if(!settings.outputOverlaps) {
                ImageJUtils2.removeLabelsExcept(outputProcessor, matchedLabels.toArray());
            }

            // Write pixels
           ImageJUtils.setSliceZero(targetLabels, outputProcessor, index);
        }, progressInfo);

        dataBatch.addOutputData(outputSlot, new ImagePlusGreyscaleData(targetLabels), progressInfo);
    }

    private void calculateOverlap(ByteProcessor overlap, ImageProcessor labels1, ImageProcessor labels2, int label1, int label2) {
        final int numPixels = overlap.getWidth() * overlap.getHeight();
        byte[] overlapPixels = (byte[]) overlap.getPixels();

        if(labels1 instanceof ByteProcessor) {
            byte[] labels1pixels = (byte[]) labels1.getPixels();
            if(labels2 instanceof ByteProcessor) {
                byte[] labels2pixels = (byte[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = labels1pixels[i] & 0xff;
                    int roi2Label = labels2pixels[i] & 0xff;
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else if(labels2 instanceof ShortProcessor) {
                short[] labels2pixels = (short[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = labels1pixels[i] & 0xff;
                    int roi2Label = labels2pixels[i] & 0xffff;
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else if(labels2 instanceof FloatProcessor) {
                float[] labels2pixels = (float[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = labels1pixels[i] & 0xff;
                    int roi2Label = (int) labels2pixels[i];
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        else if(labels1 instanceof ShortProcessor) {
            short[] labels1pixels = (short[]) labels1.getPixels();
            if(labels2 instanceof ByteProcessor) {
                byte[] labels2pixels = (byte[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = labels1pixels[i] & 0xffff;
                    int roi2Label = labels2pixels[i] & 0xff;
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else if(labels2 instanceof ShortProcessor) {
                short[] labels2pixels = (short[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = labels1pixels[i] & 0xffff;
                    int roi2Label = labels2pixels[i] & 0xffff;
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else if(labels2 instanceof FloatProcessor) {
                float[] labels2pixels = (float[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = labels1pixels[i] & 0xffff;
                    int roi2Label = (int) labels2pixels[i];
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        else if(labels1 instanceof FloatProcessor) {
            float[] labels1pixels = (float[]) labels1.getPixels();
            if(labels2 instanceof ByteProcessor) {
                byte[] labels2pixels = (byte[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = (int) labels1pixels[i];
                    int roi2Label = labels2pixels[i] & 0xff;
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else if(labels2 instanceof ShortProcessor) {
                short[] labels2pixels = (short[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = (int) labels1pixels[i];
                    int roi2Label = labels2pixels[i] & 0xffff;
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else if(labels2 instanceof FloatProcessor) {
                float[] labels2pixels = (float[]) labels2.getPixels();
                for (int i = 0; i < numPixels; i++) {
                    int roi1Label = (int) labels1pixels[i];
                    int roi2Label = (int) labels2pixels[i];
                    if(roi1Label == label1 && roi2Label == label2) {
                        overlapPixels[i] = (byte)255;
                    }
                    else {
                        overlapPixels[i] = 0;
                    }
                }
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        else {
            throw new UnsupportedOperationException();
        }

    }

    @JIPipeDocumentation(name = "Labels 1 filter", description = "Use following settings to determine how inputs into <b>Labels 1</b> are filtered " +
            "(by overlapping them with items in <b>Labels 2</b>). " +
            "Filtered Labels will be put into the corresponding <b>Labels 1</b> output.")
    @JIPipeParameter(value = "labels1", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/object-tweak-jitter-color.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/object-tweak-jitter-color.png")
    public LabelFilterSettings getLabels1Settings() {
        return labels1Settings;
    }

    @JIPipeDocumentation(name = "Labels 2 filter", description = "Use following settings to determine how inputs into <b>Labels 2</b> are filtered " +
            "(by overlapping them with items in <b>Labels 1</b>). " +
            "Filtered Labels will be put into the corresponding <b>Labels 2</b> output.")
    @JIPipeParameter(value = "labels2", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/object-tweak-jitter-color.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/object-tweak-jitter-color.png")
    public LabelFilterSettings getLabels2Settings() {
        return labels2Settings;
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

        @JIPipeDocumentation(name = "Overlap filter", description = "This filter is applied to any combination of Labels that have an overlap. You will have three sets of measurements: Labels1, Overlap, and Labels2." +
                "'Labels1'and 'Labels2' correspond to a Labels from the input slots,respectively. 'Overlap' is the overlap between these labels." +
                " Please open the expression builder to see a list of all available variables. If the filter is empty, " +
                "no filtering is applied. " + "Please note that writing a custom expression into this field requires that statistics are obtained from labels and overlapping regions, which has a significant impact on the performance.")
        @JIPipeParameter("overlap-filter")
        @ExpressionParameterSettings(variableSource = LabelOverlapStatisticsVariableSource.class)
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

