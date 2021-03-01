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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.scijava.Priority;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An algorithm that processes {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData} to
 * or {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData} and {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData}
 * to the output data
 */
public abstract class ImageRoiProcessorAlgorithm extends JIPipeIteratingAlgorithm {

    public static final String ROI_PROCESSOR_DESCRIPTION = "This algorithm can process ROI lists with or without a reference image. " +
            "The reference image provides information about the area where the ROI are located and can be used to extract statistics. " +
            "You can choose to disable the reference image slot via the 'Require reference image' parameter.\n\n" + ITERATING_ALGORITHM_DESCRIPTION;

    private boolean overrideReferenceImage = true;
    private UnreferencedRoiToMaskAlgorithm toMaskAlgorithm;
    private boolean preferAssociatedImage = true;

    /**
     * Creates a new instance
     *
     * @param info       algorithm info
     * @param output     the generated output
     * @param outputName name of the output slot
     */
    public ImageRoiProcessorAlgorithm(JIPipeNodeInfo info, Class<? extends JIPipeData> output, String outputName) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("ROI", ROIListData.class)
                .addOutputSlot(outputName, output, null)
                .seal()
                .build());
        toMaskAlgorithm = JIPipe.createNode("ij1-roi-to-mask-unreferenced", UnreferencedRoiToMaskAlgorithm.class);
        registerSubParameter(toMaskAlgorithm);
        toMaskAlgorithm.setAllSlotsVirtual(false, false, null);
        updateSlots();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImageRoiProcessorAlgorithm(ImageRoiProcessorAlgorithm other) {
        super(other);
        this.setOverrideReferenceImage(other.overrideReferenceImage);
        this.toMaskAlgorithm = new UnreferencedRoiToMaskAlgorithm(other.toMaskAlgorithm);
        this.preferAssociatedImage = other.preferAssociatedImage;
        registerSubParameter(toMaskAlgorithm);
        toMaskAlgorithm.setAllSlotsVirtual(false, false, null);
        updateSlots();
    }

    /**
     * Extracts or generates the reference image.
     * Please note that the reference is not safe to modify
     *
     * @param dataBatch    the input data
     * @param progressInfo progress
     * @return map of reference image to ROI list. The key can be null!
     */
    protected Map<ImagePlusData, ROIListData> getReferenceImage(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (overrideReferenceImage) {
            ImagePlusData reference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
            Map<ImagePlusData, ROIListData> result = new HashMap<>();
            result.put(reference, dataBatch.getInputData("ROI", ROIListData.class, progressInfo));
            return result;
        } else {
            Map<Optional<ImagePlus>, ROIListData> byReferenceImage = dataBatch.getInputData("ROI", ROIListData.class, progressInfo).groupByReferenceImage();
            Map<ImagePlusData, ROIListData> result = new HashMap<>();
            if (preferAssociatedImage) {
                for (Map.Entry<Optional<ImagePlus>, ROIListData> entry : byReferenceImage.entrySet()) {
                    if (entry.getKey().isPresent()) {
                        result.put(new ImagePlusData(entry.getKey().get()), entry.getValue());
                    } else {
                        toMaskAlgorithm.clearSlotData();
                        toMaskAlgorithm.getFirstInputSlot().addData(entry.getValue(), progressInfo);
                        toMaskAlgorithm.run(progressInfo);
                        ImagePlusData reference = toMaskAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo);
                        result.put(reference, entry.getValue());
                    }
                }
            } else {
                ROIListData inputRois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
//                toMaskAlgorithm.clearSlotData();
//                toMaskAlgorithm.getFirstInputSlot().addData(inputRois, progressInfo);
//                toMaskAlgorithm.run(progressInfo);
//                ImagePlusData reference = toMaskAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo);
//                ImagePlusData reference = new ImagePlusData(IJ.createImage("empty", "8-bit", 1, 1, 1));
                result.put(null, inputRois);
            }
            return result;
        }
    }

    @JIPipeDocumentation(name = "Override reference image", description = "If enabled, this algorithm requires a manually set reference image for its calculations." +
            " If disabled, the reference image is generated automatically or extracted from the ROI itself.")
    @JIPipeParameter(value = "override-reference-image", priority = Priority.HIGH)
    public boolean isOverrideReferenceImage() {
        return overrideReferenceImage;
    }

    @JIPipeParameter(value = "override-reference-image")
    public void setOverrideReferenceImage(boolean overrideReferenceImage) {
        this.overrideReferenceImage = overrideReferenceImage;
        updateSlots();
    }

    @JIPipeDocumentation(name = "Reference image generator", description = "Only relevant if 'Require reference image' is disabled. " +
            "The measurements are always extracted on images. This algorithm " +
            "generates a reference image by converting the ROIs into masks.")
    @JIPipeParameter(value = "reference-generator", uiExcludeSubParameters = {"jipipe:data-batch-generation", "jipipe:parameter-slot-algorithm"})
    public UnreferencedRoiToMaskAlgorithm getToMaskAlgorithm() {
        return toMaskAlgorithm;
    }

    @JIPipeDocumentation(name = "Prefer ROI-associated images", description = "Only relevant if 'Require reference image' is disabled. " +
            "ROI can carry a reference to an image (e.g. the thresholding input). With this option enabled, this image is preferred to generating " +
            "a mask based on the pure ROIs.")
    @JIPipeParameter("prefer-associated-image")
    public boolean isPreferAssociatedImage() {
        return preferAssociatedImage;
    }

    @JIPipeParameter("prefer-associated-image")
    public void setPreferAssociatedImage(boolean preferAssociatedImage) {
        this.preferAssociatedImage = preferAssociatedImage;
    }

    private void updateSlots() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        if (overrideReferenceImage && !getInputSlotMap().containsKey("Reference")) {
            slotConfiguration.addSlot("Reference", new JIPipeDataSlotInfo(ImagePlusData.class, JIPipeSlotType.Input, null), false);
        } else if (!overrideReferenceImage && getInputSlotMap().containsKey("Reference")) {
            slotConfiguration.removeInputSlot("Reference", false);
        }
    }
}
