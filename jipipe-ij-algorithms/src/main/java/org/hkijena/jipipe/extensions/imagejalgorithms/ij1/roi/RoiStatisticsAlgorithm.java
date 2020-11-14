/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.SliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Extract ROI statistics", description = "Generates a results table containing ROI statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference")
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements")
public class RoiStatisticsAlgorithm extends ImageRoiProcessorAlgorithm {

    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean applyPerSlice = false;
    private boolean applyPerChannel = false;
    private boolean applyPerFrame = false;
    private OptionalStringParameter indexAnnotation = new OptionalStringParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info, ResultsTableData.class, "Measurements");
        indexAnnotation.setContent("Image index");
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RoiStatisticsAlgorithm(RoiStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ImageStatisticsSetParameter(other.measurements);
        this.applyPerChannel = other.applyPerChannel;
        this.applyPerFrame = other.applyPerFrame;
        this.applyPerSlice = other.applyPerSlice;
        this.indexAnnotation = other.indexAnnotation;

    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        Map<ImagePlusData, ROIListData> groupedByReference = getReferenceImage(dataBatch, progress);
        for (Map.Entry<ImagePlusData, ROIListData> referenceEntry : groupedByReference.entrySet()) {
            Map<SliceIndex, List<Roi>> grouped = referenceEntry.getValue().groupByPosition(applyPerSlice, applyPerChannel, applyPerFrame);
            for (Map.Entry<SliceIndex, List<Roi>> entry : grouped.entrySet()) {
                ROIListData data = new ROIListData(entry.getValue());

                ResultsTableData result = data.measure(referenceEntry.getKey().getImage(), measurements);
                List<JIPipeAnnotation> annotations = new ArrayList<>();
                if (indexAnnotation.isEnabled() && !StringUtils.isNullOrEmpty(indexAnnotation.getContent())) {
                    annotations.add(new JIPipeAnnotation(indexAnnotation.getContent(), entry.getKey().toString()));
                }

                dataBatch.addOutputData(getFirstOutputSlot(), result, annotations, JIPipeAnnotationMergeStrategy.Merge);
            }
        }
    }

    @JIPipeDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns.<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter("measurements")
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "Optional. The annotation will contain the image slice position that was " +
            "used to generate the statistics.")
    @JIPipeParameter("index-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalStringParameter getIndexAnnotation() {
        return indexAnnotation;
    }

    @JIPipeParameter("index-annotation")
    public void setIndexAnnotation(OptionalStringParameter indexAnnotation) {
        this.indexAnnotation = indexAnnotation;
    }

    @JIPipeDocumentation(name = "Apply per slice", description = "If true, the operation is applied for each Z-slice separately. If false, all Z-slices are put together.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @JIPipeDocumentation(name = "Apply per channel", description = "If true, the operation is applied for each channel-slice separately. If false, all channel-slices are put together. " +
            "Please note that 'Channel' does not refer to a pixel channel like Red in RGB.")
    @JIPipeParameter("apply-per-channel")
    public boolean isApplyPerChannel() {
        return applyPerChannel;
    }

    @JIPipeParameter("apply-per-channel")
    public void setApplyPerChannel(boolean applyPerChannel) {
        this.applyPerChannel = applyPerChannel;
    }

    @JIPipeDocumentation(name = "Apply per frame", description = "If true, the operation is applied for each frame separately. If false, all frames are put together.")
    @JIPipeParameter("apply-per-frame")
    public boolean isApplyPerFrame() {
        return applyPerFrame;
    }

    @JIPipeParameter("apply-per-frame")
    public void setApplyPerFrame(boolean applyPerFrame) {
        this.applyPerFrame = applyPerFrame;
    }
}
