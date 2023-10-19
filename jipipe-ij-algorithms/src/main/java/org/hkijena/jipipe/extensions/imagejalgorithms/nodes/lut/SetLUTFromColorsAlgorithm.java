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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.lut;

import ij.ImagePlus;
import ij.process.LUT;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Set LUT (two colors)", description = "Generates a LUT from the first to the second color. " +
        "This does not change the pixel data.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "LUT")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nLookup Tables")
public class SetLUTFromColorsAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean duplicateImage = true;
    private Color firstColor = Color.BLACK;
    private Color secondColor = Color.RED;
    private OptionalIntegerRange restrictToChannels = new OptionalIntegerRange();

    public SetLUTFromColorsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetLUTFromColorsAlgorithm(SetLUTFromColorsAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
        this.firstColor = other.firstColor;
        this.secondColor = other.secondColor;
        this.restrictToChannels = new OptionalIntegerRange(other.restrictToChannels);
    }

    public static LUT createGradientLUT(Color firstColor, Color secondColor) {
        byte[] rLut = new byte[256];
        byte[] gLut = new byte[256];
        byte[] bLut = new byte[256];
        double dr = (secondColor.getRed() - firstColor.getRed()) / 256.0;
        double dg = (secondColor.getGreen() - firstColor.getGreen()) / 256.0;
        double db = (secondColor.getBlue() - firstColor.getBlue()) / 256.0;
        for (int i = 0; i < 256; i++) {
            rLut[i] = (byte) (firstColor.getRed() + dr * i);
            gLut[i] = (byte) (firstColor.getGreen() + dg * i);
            bLut[i] = (byte) (firstColor.getBlue() + db * i);
        }
        return new LUT(rLut, gLut, bLut);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate(progressInfo);
        data.ensureComposite();
        LUT lut = createGradientLUT(firstColor, secondColor);
        ImagePlus image = data.getImage();
        Set<Integer> channels = new HashSet<>();
        if (restrictToChannels.isEnabled()) {
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            channels.addAll(restrictToChannels.getContent().getIntegers(0, data.getNChannels() - 1, variables));
        }
        ImageJUtils.setLut(image, lut, channels);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
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

    @JIPipeDocumentation(name = "Restrict to channels", description = "Allows to restrict setting LUT to specific channels")
    @JIPipeParameter("restrict-to-channels")
    public OptionalIntegerRange getRestrictToChannels() {
        return restrictToChannels;
    }

    @JIPipeParameter("restrict-to-channels")
    public void setRestrictToChannels(OptionalIntegerRange restrictToChannels) {
        this.restrictToChannels = restrictToChannels;
    }
}
