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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.lut;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.colors.ColorMap;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Set LUT (color map)", description = "Sets the LUT of the image from a predefined color map. " +
        "This does not change the pixel data.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "LUT")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nLookup Tables")
public class SetLUTFromColorMapAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean duplicateImage = true;
    private ColorMap colorMap = ColorMap.viridis;
    private OptionalIntegerRange restrictToChannels = new OptionalIntegerRange();

    public SetLUTFromColorMapAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetLUTFromColorMapAlgorithm(SetLUTFromColorMapAlgorithm other) {
        super(other);
        this.duplicateImage = other.duplicateImage;
        this.colorMap = other.colorMap;
        this.restrictToChannels = new OptionalIntegerRange(other.restrictToChannels);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData data = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo);
        if (duplicateImage)
            data = (ImagePlusData) data.duplicate(progressInfo);
        data.ensureComposite();
        Set<Integer> channels = new HashSet<>();
        if (restrictToChannels.isEnabled()) {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            channels.addAll(restrictToChannels.getContent().getIntegers(0, data.getNChannels() - 1, variables));
        }
        ImageJUtils.setLutFromColorMap(data.getImage(), colorMap, channels);
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

    @SetJIPipeDocumentation(name = "Color map", description = "The color map that will be used as LUT")
    @JIPipeParameter("color-map")
    public ColorMap getColorMap() {
        return colorMap;
    }

    @JIPipeParameter("color-map")
    public void setColorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
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
