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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotDefinition;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.scijava.Priority;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that processes {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData} to
 * or {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData} and {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData}
 * to the output data
 */
public abstract class ImageRoiProcessorAlgorithm extends JIPipeIteratingAlgorithm {

    public static final String ROI_PROCESSOR_DESCRIPTION = "This algorithm can process ROI lists with or without a reference image. " +
            "The reference image provides information about the area where the ROI are located and can be used to extract statistics. " +
            "You can choose to disable the reference image slot via the 'Require reference image' parameter.\n\n" + ITERATING_ALGORITHM_DESCRIPTION;

    private boolean requireReferenceImage = true;
    private UnreferencedRoiToMaskAlgorithm toMaskAlgorithm;

    /**
     * Creates a new instance
     *
     * @param info       algorithm info
     * @param output     the generated output
     * @param outputName name of the output slot
     */
    public ImageRoiProcessorAlgorithm(JIPipeNodeInfo info, Class<? extends JIPipeData> output, String outputName) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("ROI", ROIListData.class)
                .addInputSlot("Reference", ImagePlusData.class)
                .addOutputSlot(outputName, output, null)
                .seal()
                .build());
        toMaskAlgorithm = JIPipeAlgorithm.newInstance("ij1-roi-to-mask-unreferenced");
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
     * @param dataBatch         the input data
     * @param subProgress       progress
     * @param algorithmProgress progress
     * @param isCancelled       if cancelled
     * @return reference image
     */
    protected ImagePlus getReferenceImage(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (requireReferenceImage) {
            return dataBatch.getInputData("Reference", ImagePlusData.class).getImage();
        } else {
            toMaskAlgorithm.clearSlotData();
            toMaskAlgorithm.getFirstInputSlot().addData(dataBatch.getInputData("ROI", ROIListData.class));
            toMaskAlgorithm.run(subProgress.resolve("Generate reference image"), algorithmProgress, isCancelled);
            return toMaskAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
        }
    }

    @JIPipeDocumentation(name = "Require reference image", description = "If enabled, this algorithm requires a reference image for its calculations." +
            " If disabled, the reference image is generated automatically.")
    @JIPipeParameter(value = "require-reference-image", priority = Priority.HIGH)
    public boolean isRequireReferenceImage() {
        return requireReferenceImage;
    }

    @JIPipeParameter(value = "require-reference-image")
    public void setRequireReferenceImage(boolean requireReferenceImage) {
        this.requireReferenceImage = requireReferenceImage;
        updateSlots();
    }

    @JIPipeDocumentation(name = "Reference image generator", description = "Only relevant if 'Require reference image' is disabled. " +
            "The measurements are always extracted on images. This algorithm " +
            "generates a reference image by converting the ROIs into masks.")
    @JIPipeParameter(value = "reference-generator", uiExcludeSubParameters = {"jipipe:data-batch-generation", "jipipe:parameter-slot-algorithm"})
    public UnreferencedRoiToMaskAlgorithm getToMaskAlgorithm() {
        return toMaskAlgorithm;
    }

    private void updateSlots() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        if (requireReferenceImage && !getInputSlotMap().containsKey("Reference")) {
            slotConfiguration.addSlot("Reference", new JIPipeSlotDefinition(ImagePlusData.class, JIPipeSlotType.Input, null), false);
        } else if (!requireReferenceImage && getInputSlotMap().containsKey("Reference")) {
            slotConfiguration.removeInputSlot("Reference", false);
        }
    }
}
