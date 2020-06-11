package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.scijava.Priority;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that processes {@link org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData} to
 * or {@link org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData} and {@link org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData}
 * to the output data
 */
public abstract class ImageRoiProcessorAlgorithm extends ACAQIteratingAlgorithm {

    public static final String ROI_PROCESSOR_DESCRIPTION = "This algorithm can process ROI lists with or without a reference image. " +
            "The reference image provides information about the area where the ROI are located and can be used to extract statistics. " +
            "You can choose to disable the reference image slot via the 'Require reference image' parameter.\n\n" + ITERATING_ALGORITHM_DESCRIPTION;

    private boolean requireReferenceImage = true;
    private UnreferencedRoiToMaskAlgorithm toMaskAlgorithm;

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     * @param output      the generated output
     * @param outputName  name of the output slot
     */
    public ImageRoiProcessorAlgorithm(ACAQAlgorithmDeclaration declaration, Class<? extends ACAQData> output, String outputName) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("ROI", ROIListData.class)
                .addInputSlot("Reference", ImagePlusData.class)
                .addOutputSlot(outputName, output, null)
                .seal()
                .build());
        toMaskAlgorithm = ACAQAlgorithm.newInstance("ij1-roi-to-mask-unreferenced");
        registerSubParameter(toMaskAlgorithm);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImageRoiProcessorAlgorithm(ImageRoiProcessorAlgorithm other) {
        super(other);
        this.setRequireReferenceImage(other.requireReferenceImage);
        this.toMaskAlgorithm = new UnreferencedRoiToMaskAlgorithm(other.toMaskAlgorithm);
        registerSubParameter(toMaskAlgorithm);
    }

    /**
     * Extracts or generates the reference image.
     * Please note that the reference is not safe to modify
     *
     * @param dataInterface     the input data
     * @param subProgress       progress
     * @param algorithmProgress progress
     * @param isCancelled       if cancelled
     * @return reference image
     */
    protected ImagePlus getReferenceImage(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (requireReferenceImage) {
            return dataInterface.getInputData("Reference", ImagePlusData.class).getImage();
        } else {
            toMaskAlgorithm.clearSlotData();
            toMaskAlgorithm.getFirstInputSlot().addData(dataInterface.getInputData("ROI", ROIListData.class));
            toMaskAlgorithm.run(subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled);
            return toMaskAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
        }
    }

    @ACAQDocumentation(name = "Require reference image", description = "If enabled, this algorithm requires a reference image for its calculations." +
            " If disabled, the reference image is generated automatically.")
    @ACAQParameter(value = "require-reference-image", priority = Priority.HIGH)
    public boolean isRequireReferenceImage() {
        return requireReferenceImage;
    }

    @ACAQParameter(value = "require-reference-image")
    public void setRequireReferenceImage(boolean requireReferenceImage) {
        this.requireReferenceImage = requireReferenceImage;
        updateSlots();
    }

    @ACAQDocumentation(name = "Reference image generator", description = "Only relevant if 'Require reference image' is disabled. " +
            "The measurements are always extracted on images. This algorithm " +
            "generates a reference image by converting the ROIs into masks.")
    @ACAQParameter("reference-generator")
    public UnreferencedRoiToMaskAlgorithm getToMaskAlgorithm() {
        return toMaskAlgorithm;
    }

    private void updateSlots() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getSlotConfiguration();
        if (requireReferenceImage && !getSlots().containsKey("Reference")) {
            slotConfiguration.addSlot("Reference", new ACAQSlotDefinition(ImagePlusData.class, ACAQDataSlot.SlotType.Input, null), false);
        } else if (!requireReferenceImage && getSlots().containsKey("Reference")) {
            slotConfiguration.removeSlot("Reference", false);
        }
    }
}
