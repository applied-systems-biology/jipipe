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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.lut;

import ij.ImagePlus;
import ij.process.LUT;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.colors.ColorMap;

@JIPipeDocumentation(name = "Set LUT (image)", description = "Sets the LUT of the image from another image, which should be a RGB image with a width of 256 and a height of 1. " +
        "This does not change the pixel data.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "LUT")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus2DColorRGBData.class, slotName = "LUT", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class SetLUTFromImageAlgorithm extends JIPipeIteratingAlgorithm {
    private boolean duplicateImage = true;
    private boolean applyToAllPlanes = true;

    public SetLUTFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetLUTFromImageAlgorithm(SetLUTFromImageAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
        this.applyToAllPlanes = other.applyToAllPlanes;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = dataBatch.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo);
        ImagePlusData lutData = dataBatch.getInputData("LUT", ImagePlus2DColorRGBData.class, progressInfo);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate();
        LUT lut = ImageJUtils.lutFromImage(lutData.getImage());
        ImagePlus image = data.getImage();
        if (applyToAllPlanes && image.isStack()) {
            ImageSliceIndex original = new ImageSliceIndex(image.getC(), image.getZ(), image.getT());
            for (int z = 0; z < image.getNSlices(); z++) {
                for (int c = 0; c < image.getNChannels(); c++) {
                    for (int t = 0; t < image.getNFrames(); t++) {
                        image.setPosition(c, z, t);
                        image.getProcessor().setLut(lut);
                    }
                }
            }
            image.setPosition(original.getC(), original.getZ(), original.getT());
        } else {
            image.getProcessor().setLut(lut);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Duplicate image", description = "As the LUT modification does not change any image data, you can disable creating a duplicate.")
    @JIPipeParameter("duplicate-image")
    public boolean isDuplicateImage() {
        return duplicateImage;
    }

    @JIPipeParameter("duplicate-image")
    public void setDuplicateImage(boolean duplicateImage) {
        this.duplicateImage = duplicateImage;
    }

    @JIPipeDocumentation(name = "Apply to all planes", description = "If enabled, all LUT are modified, not only the one of the current plane.")
    @JIPipeParameter("apply-to-all-planes")
    public boolean isApplyToAllPlanes() {
        return applyToAllPlanes;
    }

    @JIPipeParameter("apply-to-all-planes")
    public void setApplyToAllPlanes(boolean applyToAllPlanes) {
        this.applyToAllPlanes = applyToAllPlanes;
    }
}
