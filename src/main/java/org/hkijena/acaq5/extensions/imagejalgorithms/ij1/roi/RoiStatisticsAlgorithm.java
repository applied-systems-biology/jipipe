package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.extensions.imagejalgorithms.SliceIndex;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.ImageStatisticsParameters;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi.ImageRoiProcessorAlgorithm.ROI_PROCESSOR_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Extract ROI statistics", description = "Generates a results table containing ROI statistics." + "\n\n" + ROI_PROCESSOR_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Analysis)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Reference")
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Measurements")
public class RoiStatisticsAlgorithm extends ImageRoiProcessorAlgorithm {

    private ImageStatisticsParameters measurements = new ImageStatisticsParameters();
    private boolean applyPerSlice = false;
    private boolean applyPerChannel = false;
    private boolean applyPerFrame = false;
    private String indexAnnotation = "Image index";

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RoiStatisticsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ResultsTableData.class, "Measurements");
    }

    /**
     * Instantiates a new algorithm.
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = dataInterface.getInputData("ROI", ROIListData.class);
        Map<SliceIndex, List<Roi>> grouped = inputData.groupByPosition(applyPerSlice, applyPerChannel, applyPerFrame);
        ImagePlus reference = getReferenceImage(dataInterface, subProgress.resolve("Generate refence image"), algorithmProgress, isCancelled);

        for (Map.Entry<SliceIndex, List<Roi>> entry : grouped.entrySet()) {
            ROIListData data = new ROIListData(entry.getValue());

            ResultsTableData result = data.measure(reference, measurements);
            List<ACAQAnnotation> annotations = new ArrayList<>();
            if (!StringUtils.isNullOrEmpty(indexAnnotation)) {
                annotations.add(new ACAQAnnotation(indexAnnotation, entry.getKey().toString()));
            }

            dataInterface.addOutputData(getFirstOutputSlot(), result, annotations);
        }
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Extracted measurements", description = "Please select which measurements should be extracted. " +
            "Each measurement will be assigned to one or multiple output table columns. Please refer to the " +
            "individual measurement documentations for the column names.")
    @ACAQParameter("measurements")
    public ImageStatisticsParameters getMeasurements() {
        return measurements;
    }

    @ACAQDocumentation(name = "Generated annotation", description = "Optional. The annotation will contain the image slice position that was " +
            "used to generate the statistics.")
    @ACAQParameter("index-annotation")
    public String getIndexAnnotation() {
        return indexAnnotation;
    }

    @ACAQParameter("index-annotation")
    public void setIndexAnnotation(String indexAnnotation) {
        this.indexAnnotation = indexAnnotation;
    }

    @ACAQDocumentation(name = "Apply per slice", description = "If true, the operation is applied for each Z-slice separately. If false, all Z-slices are put together.")
    @ACAQParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @ACAQParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @ACAQDocumentation(name = "Apply per channel", description = "If true, the operation is applied for each channel-slice separately. If false, all channel-slices are put together. " +
            "Please note that 'Channel' does not refer to a pixel channel like Red in RGB.")
    @ACAQParameter("apply-per-channel")
    public boolean isApplyPerChannel() {
        return applyPerChannel;
    }

    @ACAQParameter("apply-per-channel")
    public void setApplyPerChannel(boolean applyPerChannel) {
        this.applyPerChannel = applyPerChannel;
    }

    @ACAQDocumentation(name = "Apply per frame", description = "If true, the operation is applied for each frame separately. If false, all frames are put together.")
    @ACAQParameter("apply-per-frame")
    public boolean isApplyPerFrame() {
        return applyPerFrame;
    }

    @ACAQParameter("apply-per-frame")
    public void setApplyPerFrame(boolean applyPerFrame) {
        this.applyPerFrame = applyPerFrame;
    }
}
