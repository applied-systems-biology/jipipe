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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.measure;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Extract ROI statistics", description = "Generates a results table containing ROI statistics. If a reference image is provided, the statistics are calculated for the reference image. Otherwise, " +
        "an empty reference image is automatically generated.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Measure")
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true, description = "Optional image that is the basis for the measurements. If not set, an empty image is generated.")
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Measurements", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze", aliasName = "Measure (ROI)")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData roi = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlus reference = getReferenceImage(iterationStep, progressInfo);
        if (roi.isEmpty()) {
            iterationStep.addOutputData(getFirstOutputSlot(), new ResultsTableData(), progressInfo);
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

            iterationStep.addOutputData(getFirstOutputSlot(), result, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    private ImagePlus getReferenceImage(JIPipeSingleIterationStep iterationStep, JIPipeProgressInfo progressInfo) {
        ImagePlus reference = null;
        {
            ImagePlusData data = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);
            if (data != null) {
                reference = data.getDuplicateImage();
            }
        }
        return reference;
    }

    @SetJIPipeDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns.<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
    @JIPipeParameter(value = "measurements", important = true)
    public ImageStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(ImageStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @SetJIPipeDocumentation(name = "Generated annotation", description = "Optional. The annotation will contain the image slice position that was " +
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

    @SetJIPipeDocumentation(name = "Apply per slice", description = "If true, the operation is applied for each Z-slice separately. If false, all Z-slices are put together.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @SetJIPipeDocumentation(name = "Apply per channel", description = "If true, the operation is applied for each channel-slice separately. If false, all channel-slices are put together. " +
            "Please note that 'Channel' does not refer to a pixel channel like Red in RGB.")
    @JIPipeParameter("apply-per-channel")
    public boolean isApplyPerChannel() {
        return applyPerChannel;
    }

    @JIPipeParameter("apply-per-channel")
    public void setApplyPerChannel(boolean applyPerChannel) {
        this.applyPerChannel = applyPerChannel;
    }

    @SetJIPipeDocumentation(name = "Apply per frame", description = "If true, the operation is applied for each frame separately. If false, all frames are put together.")
    @JIPipeParameter("apply-per-frame")
    public boolean isApplyPerFrame() {
        return applyPerFrame;
    }

    @JIPipeParameter("apply-per-frame")
    public void setApplyPerFrame(boolean applyPerFrame) {
        this.applyPerFrame = applyPerFrame;
    }

    @SetJIPipeDocumentation(name = "Add ROI name as column", description = "If enabled, a column 'Name' containing the ROI name is created. " +
            "You can modify this name via the 'Change ROI properties' node.")
    @JIPipeParameter("add-name")
    public boolean isAddNameToTable() {
        return addNameToTable;
    }

    @JIPipeParameter("add-name")
    public void setAddNameToTable(boolean addNameToTable) {
        this.addNameToTable = addNameToTable;
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }
}
