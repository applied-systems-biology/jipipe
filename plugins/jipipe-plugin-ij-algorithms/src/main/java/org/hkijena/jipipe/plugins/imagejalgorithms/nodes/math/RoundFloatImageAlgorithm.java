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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import org.apache.commons.math3.util.Precision;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;

@SetJIPipeDocumentation(name = "Round float image", description = "Rounds the values in a 32-bit image to a specific number of decimals")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
public class RoundFloatImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int decimals = 3;

    public RoundFloatImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RoundFloatImageAlgorithm(RoundFloatImageAlgorithm other) {
        super(other);
        this.decimals = other.decimals;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo).getDuplicateImage();
        ImageJIterationUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            float[] pixels = (float[]) ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                float v = pixels[i];
                if (decimals >= 0) {
                    v = Precision.round(v, decimals);
                } else {
                    v = Math.round(v / -decimals) * -decimals;
                }
                pixels[i] = v;
            }
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Number of decimals", description = "The number of decimals. Set to zero to round to integer numbers. Negative numbers will round to the closest integer.")
    @JIPipeParameter("decimals")
    public int getDecimals() {
        return decimals;
    }

    @JIPipeParameter("decimals")
    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }
}
