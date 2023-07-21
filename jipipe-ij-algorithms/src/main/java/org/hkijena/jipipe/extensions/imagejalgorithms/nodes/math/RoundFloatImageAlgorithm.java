package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Round float image", description = "Rounds the values in a 32-bit image to a specific number of decimals")
@JIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo).getDuplicateImage();
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Number of decimals", description = "The number of decimals. Set to zero to round to integer numbers. Negative numbers will round to the closest integer.")
    @JIPipeParameter("decimals")
    public int getDecimals() {
        return decimals;
    }

    @JIPipeParameter("decimals")
    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }
}
