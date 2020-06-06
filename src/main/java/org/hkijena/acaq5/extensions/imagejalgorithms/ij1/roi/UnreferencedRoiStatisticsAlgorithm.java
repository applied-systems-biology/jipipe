package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.imagejalgorithms.SliceIndex;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.ImageStatisticsParameters;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Extract ROI statistics (unreferenced)", description = "Generates a results table containing ROI statistics. This algorithm does not require a " +
        "reference image and will instead use the output of the unreferenced 'ROI to Mask' algorithm as reference.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Analysis)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output")
public class UnreferencedRoiStatisticsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private UnreferencedRoiToMaskAlgorithm toMaskAlgorithm;
    private ImageStatisticsParameters measurements = new ImageStatisticsParameters();
    private boolean applyPerSlice = false;
    private boolean applyPerChannel = false;
    private boolean applyPerFrame = false;
    private ACAQTraitDeclarationRef indexAnnotation = new ACAQTraitDeclarationRef();

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public UnreferencedRoiStatisticsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ResultsTableData.class, null)
                .seal()
                .build());
        toMaskAlgorithm = ACAQAlgorithm.newInstance("ij1-roi-to-mask-unreferenced");
        registerSubParameter(toMaskAlgorithm);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public UnreferencedRoiStatisticsAlgorithm(UnreferencedRoiStatisticsAlgorithm other) {
        super(other);
        this.measurements = new ImageStatisticsParameters(other.measurements);
        this.applyPerChannel = other.applyPerChannel;
        this.applyPerFrame = other.applyPerFrame;
        this.applyPerSlice = other.applyPerSlice;
        this.toMaskAlgorithm = new UnreferencedRoiToMaskAlgorithm(other.toMaskAlgorithm);
        this.indexAnnotation = new ACAQTraitDeclarationRef(other.indexAnnotation);
        registerSubParameter(toMaskAlgorithm);

    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = dataInterface.getInputData(getFirstInputSlot(), ROIListData.class);
        Map<SliceIndex, List<Roi>> grouped = inputData.groupByPosition(applyPerSlice, applyPerChannel, applyPerFrame);

        for (Map.Entry<SliceIndex, List<Roi>> entry : grouped.entrySet()) {
            ROIListData data = new ROIListData(entry.getValue());

            // Generate reference image
            toMaskAlgorithm.clearSlotData();
            toMaskAlgorithm.getFirstInputSlot().addData(data);
            toMaskAlgorithm.run(subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled);
            ImagePlus reference = toMaskAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();

            ResultsTableData result = data.measure(reference, measurements);
            List<ACAQTrait> annotations = new ArrayList<>();
            if (indexAnnotation.getDeclaration() != null) {
                annotations.add(indexAnnotation.getDeclaration().newInstance(entry.getKey().toString()));
            }

            dataInterface.addOutputData(getFirstOutputSlot(), result, annotations);
        }
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Reference image generator").report(toMaskAlgorithm);
    }

    @ACAQDocumentation(name = "Reference image generator", description = "The measurements are always extracted on images. This algorithm " +
            "generates a reference image by converting the ROIs into masks.")
    @ACAQParameter("reference-generator")
    public UnreferencedRoiToMaskAlgorithm getToMaskAlgorithm() {
        return toMaskAlgorithm;
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
    public ACAQTraitDeclarationRef getIndexAnnotation() {
        return indexAnnotation;
    }

    @ACAQParameter("index-annotation")
    public void setIndexAnnotation(ACAQTraitDeclarationRef indexAnnotation) {
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
