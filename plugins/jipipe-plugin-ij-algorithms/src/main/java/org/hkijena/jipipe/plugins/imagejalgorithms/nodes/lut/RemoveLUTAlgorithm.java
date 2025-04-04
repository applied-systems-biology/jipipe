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
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Remove LUT", description = "Removes LUT information from the input image.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "LUT")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nLookup Tables", aliasName = "Remove LUT")
public class RemoveLUTAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean duplicateImage = true;
    private OptionalIntegerRange restrictToChannels = new OptionalIntegerRange();

    public RemoveLUTAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveLUTAlgorithm(RemoveLUTAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
        this.restrictToChannels = new OptionalIntegerRange(other.restrictToChannels);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate(progressInfo);
        ImagePlus image = data.getImage();
        Set<Integer> channels = new HashSet<>();
        if (restrictToChannels.isEnabled()) {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
            channels.addAll(restrictToChannels.getContent().getIntegers(0, data.getNChannels() - 1, variables));
        }
        ImageJUtils.removeLUT(image, channels);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }


    @SetJIPipeDocumentation(name = "Duplicate image", description = "As the LUT removal does not change any image data, you can disable creating a duplicate.")
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
