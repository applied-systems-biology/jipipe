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
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Invert LUT", description = "Inverts the current LUT. If no LUT is set, a white-to-black LUT is generated. " +
        "This does not change the pixel data.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "LUT")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class LUTInverterAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean duplicateImage = true;

    public LUTInverterAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LUTInverterAlgorithm(LUTInverterAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData data = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate();
        if(data.getImage().getLuts().length == 0) {
            data.getImage().setLut(LUT.createLutFromColor(Color.WHITE).createInvertedLut());
        }
        else {
            LUT lut = data.getImage().getLuts()[0];
            if(lut != null) {
                data.getImage().setLut(lut.createInvertedLut());
            }
            else {
                data.getImage().setLut(LUT.createLutFromColor(Color.WHITE).createInvertedLut());
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data);
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
}
