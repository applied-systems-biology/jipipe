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

import ij.ImagePlus;
import ij.plugin.filter.EDM;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link EDM}
 */
@JIPipeDocumentation(name = "Ultimate eroded points 2D", description = "Find the maxima of the Euclidean distance transform. In the output, the points " +
        "are assigned the EDM value, which is equal to the radius of the largest circle " +
        "that fits into the particle, with the UEP as the center. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Binary", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nBinary", aliasName = "Ultimate Points")
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        EDM edm = new EDM();
        edm.setup("points", img);
        ImageJUtils.forEachSlice(img, edm::run, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale8UData(img), progressInfo);
    }
}
