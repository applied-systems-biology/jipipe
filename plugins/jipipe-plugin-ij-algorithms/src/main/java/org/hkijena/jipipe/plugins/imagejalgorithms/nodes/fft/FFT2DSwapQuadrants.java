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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.fft;

import ij.ImagePlus;
import ij.process.FHT;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.FilteringFunction;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.fft.ImagePlusFFT2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;

/**
 * Applies a FFT forward transformation
 */
@SetJIPipeDocumentation(name = "2D FFT swap quadrants", description = "Swaps the quadrants of a frequency space image.")
@ConfigureJIPipeNode(function = FilteringFunction.class, framework = ImageJ1Framework.class, spatialDimensions = {SPATIAL_2D})
@AddJIPipeInputSlot(value = ImagePlusFFT2DData.class, create = true, name = "Input")
@AddJIPipeOutputSlot(value = ImagePlusFFT2DData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus fft = iterationStep.getInputData(getFirstInputSlot(), ImagePlusFFT2DData.class, progressInfo).getDuplicateImage();
        FHT fht = new FHT(new FloatProcessor(1, 1));
        ImageJIterationUtils.forEachSlice(fft, fht::swapQuadrants, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusFFT2DData(fft), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }
}
