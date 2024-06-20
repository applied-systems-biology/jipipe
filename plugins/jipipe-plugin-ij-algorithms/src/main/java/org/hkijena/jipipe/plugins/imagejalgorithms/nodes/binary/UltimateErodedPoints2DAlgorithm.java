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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.binary;

import ij.ImagePlus;
import ij.plugin.filter.EDM;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link EDM}
 */
@SetJIPipeDocumentation(name = "Ultimate eroded points 2D", description = "Find the maxima of the Euclidean distance transform. In the output, the points " +
        "are assigned the EDM value, which is equal to the radius of the largest circle " +
        "that fits into the particle, with the UEP as the center. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nBinary", aliasName = "Ultimate Points")
public class UltimateErodedPoints2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public UltimateErodedPoints2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public UltimateErodedPoints2DAlgorithm(UltimateErodedPoints2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        EDM edm = new EDM();
        edm.setup("points", img);
        ImageJUtils.forEachSlice(img, edm::run, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale8UData(img), progressInfo);
    }
}
