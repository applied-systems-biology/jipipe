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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT2DData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies a FFT forward transformation
 */
@JIPipeDocumentation(name = "2D FFT swap quadrants", description = "Swaps the quadrants of a frequency space image.")
@JIPipeOrganization(menuPath = "FFT", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(ImagePlusFFT2DData.class)
@JIPipeOutputSlot(ImagePlusFFT2DData.class)
public class FFT2DSwapQuadrants extends JIPipeSimpleIteratingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public FFT2DSwapQuadrants(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusFFT2DData.class)
                .addOutputSlot("Output", ImagePlusFFT2DData.class, null)
                .allowOutputSlotInheritance(false)
                .seal()
                .build());
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
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus fft = dataInterface.getInputData(getFirstInputSlot(), ImagePlusFFT2DData.class).getImage().duplicate();
        FHT fht = new FHT(new FloatProcessor(1, 1));
        ImageJUtils.forEachSlice(fft, fht::swapQuadrants);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusFFT2DData(fft));
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {

    }
}
