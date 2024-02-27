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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

@SetJIPipeDocumentation(name = "Insert image (masked)", description = "Overlays the target image with the source image according to a mask or ROI.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Target", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Source", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class MergeImagesAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageROITargetArea targetArea = ImageROITargetArea.InsideMask;

    public MergeImagesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    public MergeImagesAlgorithm(MergeImagesAlgorithm other) {
        super(other);
        this.targetArea = other.targetArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData targetImageData = iterationStep.getInputData("Target", ImagePlusData.class, progressInfo);
        ImagePlus output = targetImageData.getDuplicateImage();
        ImagePlus src = iterationStep.getInputData("Source", targetImageData.getClass(), progressInfo).getImage();

        if (src.getWidth() != output.getWidth() || src.getHeight() != output.getHeight() || src.getStackSize() != output.getStackSize()) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        ImageJUtils.forEachIndexedZCTSlice(output, (ip, index) -> {
            ImageProcessor mask = getMask(iterationStep, index, progressInfo);
            ImageProcessor srcProcessor = ImageJUtils.getSliceZero(src, index);
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    if (mask.get(x, y) > 0) {
                        ip.set(x, y, srcProcessor.get(x, y));
                    }
                }
            }
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(output), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Overwrite", description = "Determines where pixels are overwritten.")
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
                ImagePlusData img = iterationStep.getInputData("Target", ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
            }
            case InsideRoi: {
                ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData("Target", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
                } else {
                    return rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData("Target", ImagePlusData.class, progressInfo);
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
