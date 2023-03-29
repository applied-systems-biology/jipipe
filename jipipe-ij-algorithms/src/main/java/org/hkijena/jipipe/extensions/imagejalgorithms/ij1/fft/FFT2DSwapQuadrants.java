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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.fft;

import ij.ImagePlus;
import ij.process.FHT;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Applies a FFT forward transformation
 */
@JIPipeDocumentation(name = "2D FFT swap quadrants", description = "Swaps the quadrants of a frequency space image.")
@JIPipeNode(menuPath = "FFT", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusFFT2DData.class, autoCreate = true, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusFFT2DData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFFT", aliasName = "Swap Quadrants")
public class FFT2DSwapQuadrants extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public FFT2DSwapQuadrants(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FFT2DSwapQuadrants(FFT2DSwapQuadrants other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus fft = dataBatch.getInputData(getFirstInputSlot(), ImagePlusFFT2DData.class, progressInfo).getDuplicateImage();
        FHT fht = new FHT(new FloatProcessor(1, 1));
        ImageJUtils.forEachSlice(fft, fht::swapQuadrants, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusFFT2DData(fft), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }
}
