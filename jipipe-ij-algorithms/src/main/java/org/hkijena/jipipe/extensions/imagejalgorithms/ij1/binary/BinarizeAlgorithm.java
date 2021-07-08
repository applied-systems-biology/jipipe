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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.binary;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.ManualThreshold8U2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.ranges.IntNumberRangeParameter;

@JIPipeDocumentation(name = "Binarize", description = "Converts a greyscale image into a binary image. All pixels with a value larger than zero are set to 255.")
@JIPipeOrganization(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
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

    @JIPipeDocumentation(name = "Invert", description = "If ")
    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ManualThreshold8U2DAlgorithm algorithm = JIPipe.createNode(ManualThreshold8U2DAlgorithm.class);
        if (!invert)
            algorithm.setThreshold(new IntNumberRangeParameter(1, 256));
        else
            algorithm.setThreshold(new IntNumberRangeParameter(256, 1));
        algorithm.getFirstInputSlot().addData(dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo), progressInfo);
        algorithm.run(progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), algorithm.getFirstOutputSlot().getData(0, ImagePlusGreyscaleMaskData.class, progressInfo), progressInfo);
    }
}
