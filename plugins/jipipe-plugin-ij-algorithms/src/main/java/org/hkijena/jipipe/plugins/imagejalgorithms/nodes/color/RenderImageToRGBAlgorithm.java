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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Render to RGB", description = "Converts the incoming image to RGB. This applies the LUT if any is set. " +
        "Compared to the image converter node, this operation renders the image into a representation equivalent to an image viewer, meaning " +
        "that contrast settings are preserved.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nType", aliasName = "RGB Color")
@AddJIPipeNodeAlias(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert", aliasName = "Convert image to RGB colors (Render)")
public class RenderImageToRGBAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public RenderImageToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RenderImageToRGBAlgorithm(RenderImageToRGBAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ImagePlus outputImage = ImageJUtils.renderToRGBWithLUTIfNeeded(inputImage, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);
    }

}
