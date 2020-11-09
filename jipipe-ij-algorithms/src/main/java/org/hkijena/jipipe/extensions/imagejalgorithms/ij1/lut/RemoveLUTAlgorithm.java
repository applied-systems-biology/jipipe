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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.SliceIndex;

import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Remove LUT", description = "Removes LUT information from the input image.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Miscellaneous")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class RemoveLUTAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean duplicateImage = true;
    private boolean applyToAllPlanes = true;

    public RemoveLUTAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveLUTAlgorithm(RemoveLUTAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
        this.applyToAllPlanes = other.applyToAllPlanes;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData data = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate();
        ImagePlus image = data.getImage();
        if(applyToAllPlanes && image.isStack()) {
            SliceIndex original = new SliceIndex(image.getZ(), image.getC(), image.getT());
            for (int z = 0; z < image.getNSlices(); z++) {
                for (int c = 0; c < image.getNChannels(); c++) {
                    for (int t = 0; t < image.getNFrames(); t++) {
                        image.setPosition(c, z, t);
                        image.getProcessor().setLut(null);
                    }
                }
            }
            image.setPosition(original.getC(), original.getZ(), original.getT());
        }
        else {
            image.getProcessor().setLut(null);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data);
    }


    @JIPipeDocumentation(name = "Duplicate image", description = "As the LUT removal does not change any image data, you can disable creating a duplicate.")
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
