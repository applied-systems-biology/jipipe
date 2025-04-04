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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;

/**
 * Image processor algorithm that optionally takes a ROI
 */
public abstract class SimpleImageAndRoiIteratingAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;

    public SimpleImageAndRoiIteratingAlgorithm(JIPipeNodeInfo info,
                                               Class<? extends ImagePlusData> inputClass,
                                               Class<? extends ImagePlusData> outputClass) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", inputClass)
                .addOutputSlot("Output", "", outputClass)
                .seal()
                .build());
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    public SimpleImageAndRoiIteratingAlgorithm(SimpleImageAndRoiIteratingAlgorithm other) {
        super(other);
        this.targetArea = other.targetArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @SetJIPipeDocumentation(name = "Only apply to ...", description = "Determines where the algorithm is applied to.")
    @JIPipeParameter("roi:target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("roi:target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        this.targetArea = targetArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    public ImageProcessor getMask(JIPipeSingleIterationStep iterationStep, ImageSliceIndex sliceIndex, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
            }
            case InsideRoi: {
                ROI2DListData rois = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
                } else {
                    return rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                ROI2DListData rois = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
                } else {
                    ImageProcessor processor = rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                ImagePlus mask = iterationStep.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                if (mask.getStackSize() > 1) {
                    return mask.getStack().getProcessor(sliceIndex.zeroSliceIndexToOneStackIndex(mask));
                } else {
                    return mask.getProcessor();
                }
            }
            case OutsideMask: {
                ImagePlus mask = iterationStep.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                ImageProcessor processor;
                if (mask.getStackSize() > 1) {
                    processor = mask.getStack().getProcessor(sliceIndex.zeroSliceIndexToOneStackIndex(mask)).duplicate();
                } else {
                    processor = mask.getProcessor().duplicate();
                }
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

}
