package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Image processor algorithm that optionally takes a ROI
 */
public abstract class SimpleImageAndRoiIteratingAlgorithm extends JIPipeIteratingAlgorithm {

    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;

    public SimpleImageAndRoiIteratingAlgorithm(JIPipeNodeInfo info,
                                               Class<? extends ImagePlusData> inputClass,
                                               Class<? extends ImagePlusData> outputClass,
                                               String outputInheritance,
                                               Map<Class<? extends JIPipeData>, Class<? extends JIPipeData>> inheritanceConversions) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", inputClass)
                .addOutputSlot("Output", outputClass, outputInheritance, inheritanceConversions)
                .seal()
                .build());
        updateRoiSlot();
    }

    public SimpleImageAndRoiIteratingAlgorithm(SimpleImageAndRoiIteratingAlgorithm other) {
        super(other);
        this.targetArea = other.targetArea;
        updateRoiSlot();
    }

    @JIPipeDocumentation(name = "Only apply to ...", description = "Determines where the algorithm is applied to.")
    @JIPipeParameter("roi:target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("roi:target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        this.targetArea = targetArea;
        updateRoiSlot();
    }

    public ImageProcessor getMask(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = dataBatch.getInputData("Input", ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMask(img.getImage());
            }
            case InsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Input", ImagePlusData.class, progressInfo);
                if(rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                }
                else {
                    return rois.toMask(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0).getProcessor();
                }
            }
            case OutsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Input", ImagePlusData.class, progressInfo);
                if(rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                }
                else {
                    ImageProcessor processor = rois.toMask(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                return dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage().getProcessor();
            }
            case OutsideMask: {
                ImageProcessor processor = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage().getProcessor().duplicate();
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

    private void updateRoiSlot() {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        if(targetArea == ImageROITargetArea.WholeImage) {
            if(slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if(slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        }
        else if(targetArea == ImageROITargetArea.InsideRoi || targetArea == ImageROITargetArea.OutsideRoi) {
            if(!slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input, "ROI"), false);
            }
            if(slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        }
        else if(targetArea == ImageROITargetArea.InsideMask || targetArea == ImageROITargetArea.OutsideMask) {
            if(slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if(!slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.addSlot("Mask", new JIPipeDataSlotInfo(ImagePlus2DGreyscaleMaskData.class, JIPipeSlotType.Input, "Mask"), false);
            }
        }
    }

}
