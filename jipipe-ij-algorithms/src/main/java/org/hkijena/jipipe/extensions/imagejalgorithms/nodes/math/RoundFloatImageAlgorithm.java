package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Round float image", description = "Rounds the values in a 32-bit image to a specific number of decimals")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", create = true)
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
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
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
