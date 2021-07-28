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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

@JIPipeDocumentation(name = "Adjust displayed contrast", description = "Re-calibrates the incoming image, so its color range is displayed differently by ImageJ. " +
        "This does not change the pixel data.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class DisplayRangeCalibrationAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ImageJCalibrationMode calibrationMode = ImageJCalibrationMode.AutomaticImageJ;
    private double customMin = 0;
    private double customMax = 1;
    private boolean duplicateImage = true;
    private boolean applyToAllPlanes = true;

    public DisplayRangeCalibrationAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DisplayRangeCalibrationAlgorithm(DisplayRangeCalibrationAlgorithm other) {
        super(other);
        this.calibrationMode = other.calibrationMode;
        this.customMin = other.customMin;
        this.customMax = other.customMax;
        this.duplicateImage = other.duplicateImage;
        this.applyToAllPlanes = other.applyToAllPlanes;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate();
        ImagePlus image = data.getImage();
        if (applyToAllPlanes && image.isStack()) {
            ImageSliceIndex original = new ImageSliceIndex(image.getC(), image.getZ(), image.getT());
            for (int z = 0; z < image.getNSlices(); z++) {
                for (int c = 0; c < image.getNChannels(); c++) {
                    for (int t = 0; t < image.getNFrames(); t++) {
                        image.setPosition(c, z, t);
                        ImageJUtils.calibrate(image, calibrationMode, customMin, customMax);
                    }
                }
            }
            image.setPosition(original.getC(), original.getZ(), original.getT());
        } else {
            ImageJUtils.calibrate(image, calibrationMode, customMin, customMax);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Calibration method", description = "The method to apply for calibration.")
    @JIPipeParameter("calibration-mode")
    public ImageJCalibrationMode getCalibrationMode() {
        return calibrationMode;
    }

    @JIPipeParameter("calibration-mode")
    public void setCalibrationMode(ImageJCalibrationMode calibrationMode) {
        this.calibrationMode = calibrationMode;
    }

    @JIPipeDocumentation(name = "Custom min", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom minimum value.")
    @JIPipeParameter("custom-min")
    public double getCustomMin() {
        return customMin;
    }

    @JIPipeParameter("custom-min")
    public void setCustomMin(double customMin) {
        this.customMin = customMin;
    }

    @JIPipeDocumentation(name = "Custom max", description = "Used if 'Calibration' method is set to 'Custom'. Sets custom maximum value.")
    @JIPipeParameter("custom-max")
    public double getCustomMax() {
        return customMax;
    }

    @JIPipeParameter("custom-max")
    public void setCustomMax(double customMax) {
        this.customMax = customMax;
    }

    @JIPipeDocumentation(name = "Duplicate image", description = "As the calibration does not change any image data, you can disable creating a duplicate.")
    @JIPipeParameter("duplicate-image")
    public boolean isDuplicateImage() {
        return duplicateImage;
    }

    @JIPipeParameter("duplicate-image")
    public void setDuplicateImage(boolean duplicateImage) {
        this.duplicateImage = duplicateImage;
    }

    @JIPipeDocumentation(name = "Apply to all planes", description = "If enabled, all image planes are recalibrated, not only the one of the current plane.")
    @JIPipeParameter("apply-to-all-planes")
    public boolean isApplyToAllPlanes() {
        return applyToAllPlanes;
    }

    @JIPipeParameter("apply-to-all-planes")
    public void setApplyToAllPlanes(boolean applyToAllPlanes) {
        this.applyToAllPlanes = applyToAllPlanes;
    }


}
