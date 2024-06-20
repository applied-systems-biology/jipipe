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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.blur;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;


/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Median filter 3x3 2D (8-bit greyscale)", description = "Applies a 3x3 median filter to 8-bit greyscale images. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Filter", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale8UData.class, name = "Output", create = true)
public class MedianBlurGreyscale8U2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MedianBlurGreyscale8U2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MedianBlurGreyscale8U2DAlgorithm(MedianBlurGreyscale8U2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, ImageProcessor::medianFilter, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale8UData(img), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }
}
