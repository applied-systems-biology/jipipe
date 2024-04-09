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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.lut;

import ij.ImagePlus;
import ij.process.LUT;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Set LUT (image)", description = "Sets the LUT of the image from another image, which should be a RGB image with a width of 256 and a height of 1. " +
        "This does not change the pixel data.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "LUT")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlus2DColorRGBData.class, slotName = "LUT", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nLookup Tables")
public class SetLUTFromImageAlgorithm extends JIPipeIteratingAlgorithm {
    private boolean duplicateImage = true;

    private OptionalIntegerRange restrictToChannels = new OptionalIntegerRange();

    public SetLUTFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetLUTFromImageAlgorithm(SetLUTFromImageAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
        this.restrictToChannels = new OptionalIntegerRange(other.restrictToChannels);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = iterationStep.getInputData("Input", ImagePlusGreyscaleData.class, progressInfo);
        ImagePlusData lutData = iterationStep.getInputData("LUT", ImagePlus2DColorRGBData.class, progressInfo);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate(progressInfo);
        data.ensureComposite();
        LUT lut = ImageJUtils.lutFromImage(lutData.getImage());
        ImagePlus image = data.getImage();
        Set<Integer> channels = new HashSet<>();
        if (restrictToChannels.isEnabled()) {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            channels.addAll(restrictToChannels.getContent().getIntegers(0, data.getNChannels() - 1, variables));
        }
        ImageJUtils.setLut(image, lut, channels);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Duplicate image", description = "As the LUT modification does not change any image data, you can disable creating a duplicate.")
    @JIPipeParameter("duplicate-image")
    public boolean isDuplicateImage() {
        return duplicateImage;
    }

    @JIPipeParameter("duplicate-image")
    public void setDuplicateImage(boolean duplicateImage) {
        this.duplicateImage = duplicateImage;
    }

    @SetJIPipeDocumentation(name = "Restrict to channels", description = "Allows to restrict setting LUT to specific channels")
    @JIPipeParameter("restrict-to-channels")
    public OptionalIntegerRange getRestrictToChannels() {
        return restrictToChannels;
    }

    @JIPipeParameter("restrict-to-channels")
    public void setRestrictToChannels(OptionalIntegerRange restrictToChannels) {
        this.restrictToChannels = restrictToChannels;
    }
}
