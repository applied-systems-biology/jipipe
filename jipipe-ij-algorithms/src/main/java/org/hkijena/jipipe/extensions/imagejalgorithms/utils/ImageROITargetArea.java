package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.jetbrains.annotations.NotNull;

public enum ImageROITargetArea {
    WholeImage,
    InsideRoi,
    OutsideRoi,
    InsideMask,
    OutsideMask;

    /**
     * Gets the appropriate mask for the current setting
     * @param inputImage the input image
     * @param rois the list of Rois (can be null if not InsideRoi and OutsideRoi)
     * @param mask the mask (can be null if not InsideMask and OutsideMask)
     * @return the mask
     */
    public ImageProcessor getMask(ImagePlus inputImage, ROIListData rois, ImagePlus mask) {
        switch (this) {
            case WholeImage: {
                return createWhiteMask(inputImage);
            }
            case InsideRoi: {
                if(rois.isEmpty()) {
                    return createWhiteMask(inputImage);
                }
                else {
                    return rois.toMask(inputImage.getWidth(), inputImage.getHeight(),
                            false, true, 0).getProcessor();
                }
            }
            case OutsideRoi: {
                if(rois.isEmpty()) {
                    return createWhiteMask(inputImage);
                }
                else {
                    ImageProcessor processor = rois.toMask(inputImage.getWidth(), inputImage.getHeight(),
                            false, true, 0).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                return mask.getProcessor();
            }
            case OutsideMask: {
                ImageProcessor processor = mask.getProcessor().duplicate();
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

    @NotNull
    public static ByteProcessor createWhiteMask(ImagePlus img) {
        ByteProcessor processor = new ByteProcessor(img.getWidth(), img.getHeight());
        processor.setValue(255);
        processor.setRoi(0,0, processor.getWidth(), processor.getHeight());
        processor.fill();
        return processor;
    }

    @Override
    public String toString() {
        switch (this) {
            case WholeImage:
                return "Whole image";
            case InsideRoi:
                return "Inside ROI";
            case OutsideRoi:
                return "Outside ROI";
            case InsideMask:
                return "Inside mask";
            case OutsideMask:
                return "Outside mask";
        }
        throw new UnsupportedOperationException();
    }
}
