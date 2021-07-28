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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.convolve;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link Convolver}
 */
@JIPipeDocumentation(name = "Convolve 2D", description = "Applies a convolution with a user-defined filter kernel. The kernel is defined by a second parameter slot." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Convolve", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus2DGreyscale32FData.class, slotName = "Kernel", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class ConvolveByImage2DAlgorithm extends JIPipeIteratingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ConvolveByImage2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ConvolveByImage2DAlgorithm(ConvolveByImage2DAlgorithm other) {
        super(other);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getDuplicateImage();
        ImagePlus imgKernel = dataBatch.getInputData("Kernel", ImagePlus2DGreyscale32FData.class, progressInfo).getDuplicateImage();
        FloatProcessor processor = (FloatProcessor) imgKernel.getProcessor();

        Convolver convolver = new Convolver();
        float[] kernel = new float[processor.getWidth() * processor.getHeight()];
        for (int row = 0; row < processor.getHeight(); row++) {
            for (int col = 0; col < processor.getWidth(); col++) {
                kernel[row * processor.getWidth() + col] = processor.getf(col, row);
            }
        }

        ImageJUtils.forEachSlice(img, imp -> convolver.convolve(imp, kernel, imgKernel.getWidth(), imgKernel.getHeight()), progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }
}
