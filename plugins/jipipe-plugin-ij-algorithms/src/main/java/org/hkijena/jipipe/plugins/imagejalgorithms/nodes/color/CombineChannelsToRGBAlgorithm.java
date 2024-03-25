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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.color;

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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Combine channel slices to RGB", description = "If the input image has 3 channel slices, those channels are merged into one RGB slice. Useful if you imported images via Bio-Formats or other importers that automatically split channels.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nColor")
public class CombineChannelsToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public CombineChannelsToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CombineChannelsToRGBAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus toRGB = ImageJUtils.channelsToRGB(image.getImage());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(toRGB), progressInfo);
    }
}
