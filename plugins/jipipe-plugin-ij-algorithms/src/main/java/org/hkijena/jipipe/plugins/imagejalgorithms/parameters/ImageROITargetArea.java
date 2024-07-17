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

package org.hkijena.jipipe.plugins.imagejalgorithms.parameters;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;


public enum ImageROITargetArea {
    WholeImage,
    InsideRoi,
    OutsideRoi,
    InsideMask,
    OutsideMask;

    public static ByteProcessor createWhiteMaskProcessor(ImagePlus img) {
        ByteProcessor processor = new ByteProcessor(img.getWidth(), img.getHeight());
        processor.setValue(255);
        processor.setRoi(0, 0, processor.getWidth(), processor.getHeight());
        processor.fill();
        return processor;
    }

    public static ImagePlus createWhiteMask(ImagePlus img) {
        ImagePlus result = IJ.createImage(img.getTitle(), img.getWidth(), img.getHeight(), img.getStackSize(), 8);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        ImageJUtils.forEachIndexedZCTSlice(result, (processor, index) -> {
            processor.setValue(255);
            processor.setRoi(0, 0, processor.getWidth(), processor.getHeight());
            processor.fill();
        }, new JIPipeProgressInfo());
        return result;
    }

    /**
     * Gets the appropriate mask for the current setting
     *
     * @param inputImage the input image
     * @param rois       the list of Rois (can be null if not InsideRoi and OutsideRoi)
     * @param mask       the mask (can be null if not InsideMask and OutsideMask)
     * @return the mask
     */
    public ImageProcessor getMask(ImagePlus inputImage, ROI2DListData rois, ImagePlus mask) {
        switch (this) {
            case WholeImage: {
                return createWhiteMaskProcessor(inputImage);
            }
            case InsideRoi: {
                if (rois.isEmpty()) {
                    return createWhiteMaskProcessor(inputImage);
                } else {
                    return rois.toMask(inputImage.getWidth(), inputImage.getHeight(),
                            false, true, 0).getProcessor();
                }
            }
            case OutsideRoi: {
                if (rois.isEmpty()) {
                    return createWhiteMaskProcessor(inputImage);
                } else {
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
