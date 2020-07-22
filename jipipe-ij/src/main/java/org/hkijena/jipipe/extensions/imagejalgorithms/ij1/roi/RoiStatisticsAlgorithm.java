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
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.measure.ImageStatisticsParameters;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.SliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private ImageStatisticsParameters measurements = new ImageStatisticsParameters();
    private boolean applyPerSlice = false;
    private boolean applyPerChannel = false;
    private boolean applyPerFrame = false;
    private String indexAnnotation = "Image index";

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info, ResultsTableData.class, "Measurements");
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RoiStatisticsAlgorithm(RoiStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ImageStatisticsParameters(other.measurements);
        this.applyPerChannel = other.applyPerChannel;
        this.applyPerFrame = other.applyPerFrame;
        this.applyPerSlice = other.applyPerSlice;
        this.indexAnnotation = other.indexAnnotation;

    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Map<ImagePlusData, ROIListData> groupedByReference = getReferenceImage(dataBatch, subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled);
        for (Map.Entry<ImagePlusData, ROIListData> referenceEntry : groupedByReference.entrySet()) {
            Map<SliceIndex, List<Roi>> grouped = referenceEntry.getValue().groupByPosition(applyPerSlice, applyPerChannel, applyPerFrame);
            for (Map.Entry<SliceIndex, List<Roi>> entry : grouped.entrySet()) {
                ROIListData data = new ROIListData(entry.getValue());

                ResultsTableData result = data.measure(referenceEntry.getKey().getImage(), measurements);
                List<JIPipeAnnotation> annotations = new ArrayList<>();
                if (!StringUtils.isNullOrEmpty(indexAnnotation)) {
                    annotations.add(new JIPipeAnnotation(indexAnnotation, entry.getKey().toString()));
                }

                dataBatch.addOutputData(getFirstOutputSlot(), result, annotations);
            }
        }
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns. Please refer to the " +
            "individual measurement documentations for the column names.")
    @JIPipeParameter("measurements")
    public ImageStatisticsParameters getMeasurements() {
        return measurements;
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "Optional. The annotation will contain the image slice position that was " +
            "used to generate the statistics.")
    @JIPipeParameter("index-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getIndexAnnotation() {
        return indexAnnotation;
    }

    @JIPipeParameter("index-annotation")
    public void setIndexAnnotation(String indexAnnotation) {
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
