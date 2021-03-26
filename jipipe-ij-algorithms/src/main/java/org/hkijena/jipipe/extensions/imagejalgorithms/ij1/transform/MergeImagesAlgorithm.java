package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

@JIPipeDocumentation(name = "Overlay images", description = "Overlays the target image with the source image according to a mask or ROI.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Target", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Source", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Target")
public class MergeImagesAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageROITargetArea targetArea = ImageROITargetArea.InsideMask;

    public MergeImagesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        updateRoiSlot();
    }

    public MergeImagesAlgorithm(MergeImagesAlgorithm other) {
        super(other);
        this.targetArea = other.targetArea;
        updateRoiSlot();
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData targetImageData = dataBatch.getInputData("Target", ImagePlusData.class, progressInfo);
        ImagePlus output = targetImageData.getDuplicateImage();
        ImagePlus src = dataBatch.getInputData("Source", targetImageData.getClass(), progressInfo).getImage();

        if(src.getWidth() != output.getWidth() || src.getHeight() != output.getHeight() || src.getStackSize() != output.getStackSize()) {
            throw new UserFriendlyRuntimeException("Images have different sizes!",
                    "Source and target images have different sizes!",
                    getName(),
                    "You wanted to overlay two images with different sizes (width/height/planes).",
                    "Please check if the images are correct.");
        }

        ImageJUtils.forEachIndexedZCTSlice(output, (ip, index) -> {
            ImageProcessor mask = getMask(dataBatch, index, progressInfo);
            ImageProcessor srcProcessor = ImageJUtils.getSliceZero(src, index);
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    if(mask.get(x, y) > 0) {
                        ip.set(x, y, srcProcessor.get(x, y));
                    }
                }
            }
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(output), progressInfo);
    }

    @JIPipeDocumentation(name = "Overwrite", description = "Determines where pixels are overwritten.")
    @JIPipeParameter("roi:target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("roi:target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        this.targetArea = targetArea;
        updateRoiSlot();
    }

    public ImageProcessor getMask(JIPipeDataBatch dataBatch, ImageSliceIndex sliceIndex, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = dataBatch.getInputData("Target", ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMask(img.getImage());
            }
            case InsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Target", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    return rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Target", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    ImageProcessor processor = rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                if (mask.getStackSize() > 1) {
                    return mask.getStack().getProcessor(sliceIndex.getStackIndex(mask));
                } else {
                    return mask.getProcessor();
                }
            }
            case OutsideMask: {
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                ImageProcessor processor;
                if (mask.getStackSize() > 1) {
                    processor = mask.getStack().getProcessor(sliceIndex.getStackIndex(mask)).duplicate();
                } else {
                    processor = mask.getProcessor().duplicate();
                }
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

    private void updateRoiSlot() {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        if (targetArea == ImageROITargetArea.WholeImage) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (targetArea == ImageROITargetArea.InsideRoi || targetArea == ImageROITargetArea.OutsideRoi) {
            if (!slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, "ROI"), false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (targetArea == ImageROITargetArea.InsideMask || targetArea == ImageROITargetArea.OutsideMask) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (!slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.addSlot("Mask", new JIPipeDataSlotInfo(ImagePlusGreyscaleMaskData.class, JIPipeSlotType.Input, "Mask"), false);
            }
        }
    }
}
