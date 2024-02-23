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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.binary;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold.ManualThreshold8U2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.library.ranges.IntNumberRangeParameter;

@SetJIPipeDocumentation(name = "Binarize", description = "Converts a greyscale image into a binary image. All pixels with a value larger than zero are set to 255.")
@ConfigureJIPipeNode(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nBinary", aliasName = "Convert to Mask")
public class BinarizeAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean invert = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public BinarizeAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public BinarizeAlgorithm(BinarizeAlgorithm other) {
        super(other);
        this.invert = other.invert;
    }

    @SetJIPipeDocumentation(name = "Invert", description = "If ")
    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ManualThreshold8U2DAlgorithm algorithm = JIPipe.createNode(ManualThreshold8U2DAlgorithm.class);
        if (!invert)
            algorithm.setThreshold(new IntNumberRangeParameter(1, 256));
        else
            algorithm.setThreshold(new IntNumberRangeParameter(256, 1));
        algorithm.getFirstInputSlot().addData(iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo), progressInfo);
        algorithm.run(runContext, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), algorithm.getFirstOutputSlot().getData(0, ImagePlusGreyscaleMaskData.class, progressInfo), progressInfo);
    }
}
