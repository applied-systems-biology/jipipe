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

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Extract ROI statistics", description = "Generates a results table containing ROI statistics.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", autoCreate = true)
public class RoiStatisticsAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageStatisticsSetParameter measurements = new ImageStatisticsSetParameter();
    private boolean applyPerSlice = false;
    private boolean applyPerChannel = false;
    private boolean applyPerFrame = false;
    private boolean addNameToTable = true;
    private OptionalStringParameter indexAnnotation = new OptionalStringParameter();

    private boolean measureInPhysicalUnits = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiStatisticsAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
        this.addNameToTable = other.addNameToTable;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData roi = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlus reference = getReferenceImage(dataBatch, progressInfo);
        if (roi.isEmpty()) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ResultsTableData(), progressInfo);
            return;
        }
        Map<ImageSliceIndex, List<Roi>> grouped = roi.groupByPosition(applyPerSlice, applyPerChannel, applyPerFrame);
        for (Map.Entry<ImageSliceIndex, List<Roi>> entry : grouped.entrySet()) {
            ROIListData data = new ROIListData(entry.getValue());
            ResultsTableData result = data.measure(reference, measurements, addNameToTable, measureInPhysicalUnits);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (indexAnnotation.isEnabled() && !StringUtils.isNullOrEmpty(indexAnnotation.getContent())) {
                annotations.add(new JIPipeTextAnnotation(indexAnnotation.getContent(), entry.getKey().toString()));
            }

            dataBatch.addOutputData(getFirstOutputSlot(), result, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    private ImagePlus getReferenceImage(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus reference = null;
        {
            ImagePlusData data = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
            if(data != null) {
                reference = data.getDuplicateImage();
            }
        }
        return reference;
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

    @JIPipeDocumentation(name = "Add ROI name as column", description = "If enabled, a column 'Name' containing the ROI name is created. " +
            "You can modify this name via the 'Change ROI properties' node.")
    @JIPipeParameter("add-name")
    public boolean isAddNameToTable() {
        return addNameToTable;
    }

    @JIPipeParameter("add-name")
    public void setAddNameToTable(boolean addNameToTable) {
        this.addNameToTable = addNameToTable;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }
}
