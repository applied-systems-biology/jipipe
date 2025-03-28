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
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Invert RGB colors", description = "Inverts the colors by subtracting 255 from each RGB color channel.")
@ConfigureJIPipeNode(menuPath = "Colors", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusColorRGBData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Edit", aliasName = "Invert (RGB)")
public class InvertRGBColorsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public InvertRGBColorsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public InvertRGBColorsAlgorithm(InvertRGBColorsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusColorRGBData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusColorRGBData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJIterationUtils.forEachSlice(img, ImageProcessor::invert, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(img), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }
}
