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

@JIPipeDocumentation(name = "Set LUT from color", description = "Generates a LUT from the first to the second color. " +
        "This does not change the pixel data.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "LUT")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class SetLUTFromColorAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean duplicateImage = true;
    private Color firstColor = Color.BLACK;
    private Color secondColor = Color.RED;

    public SetLUTFromColorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetLUTFromColorAlgorithm(SetLUTFromColorAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
        this.firstColor = other.firstColor;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData data = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate();
        data.getImage().setLut(createGradientLUT(firstColor, secondColor));
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

    @JIPipeDocumentation(name = "First color", description = "The color assigned to zero values.")
    @JIPipeParameter("first-color")
    public Color getFirstColor() {
        return firstColor;
    }

    @JIPipeParameter("first-color")
    public void setFirstColor(Color firstColor) {
        this.firstColor = firstColor;
    }

    @JIPipeDocumentation(name = "Second color", description = "The color assigned to maximum values.")
    @JIPipeParameter("second-color")
    public Color getSecondColor() {
        return secondColor;
    }

    @JIPipeParameter("second-color")
    public void setSecondColor(Color secondColor) {
        this.secondColor = secondColor;
    }

    public static LUT createGradientLUT(Color firstColor, Color secondColor) {
        byte[] rLut = new byte[256];
        byte[] gLut = new byte[256];
        byte[] bLut = new byte[256];
        double dr = (secondColor.getRed() - firstColor.getRed()) / 256.0;
        double dg = (secondColor.getGreen() - firstColor.getGreen()) / 256.0;
        double db = (secondColor.getBlue() - firstColor.getBlue()) / 256.0;
        for (int i = 0; i < 256; i++) {
            rLut[i] = (byte)(firstColor.getRed() + dr * i);
            gLut[i] = (byte)(firstColor.getGreen() + dg * i);
            bLut[i] = (byte)(firstColor.getBlue() + db * i);
        }
        return new LUT(rLut, gLut, bLut);
    }
}
