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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.fft;

import ij.ImagePlus;
import ij.plugin.FFT;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT2DData;

/**
 * Applies a FFT forward transformation
 */
@JIPipeDocumentation(name = "2D FFT inverse transformation", description = "Converts a frequency space image into an image in real space.")
@JIPipeNode(menuPath = "FFT", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusFFT2DData.class, autoCreate = true, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlus2DData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFFT", aliasName = "Inverse FFT")
public class FFT2DInverseTransform extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public FFT2DInverseTransform(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FFT2DInverseTransform(FFT2DInverseTransform other) {
        super(other);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusFFT2DData.class, progressInfo).getImage();
        ImagePlus fft = FFT.inverse(img);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(fft), progressInfo);
    }
}