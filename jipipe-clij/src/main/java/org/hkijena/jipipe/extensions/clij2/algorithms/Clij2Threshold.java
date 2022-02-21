package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Threshold;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Threshold}
 */
@JIPipeDocumentation(name = "CLIJ2 Threshold", description = "Computes a binary image with pixel values 0 and 1. " + "All pixel values x of a given input image with " + "value larger or equal to a given threshold t will be set to 1." + "f(x,t) = (1 if (x >= t); (0 otherwise))" + "This plugin is comparable to setting a raw threshold in ImageJ and using the 'Convert to Mask' menu. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Threshold extends JIPipeSimpleIteratingAlgorithm {
    float threshold;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Threshold(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Threshold(Clij2Threshold other) {
        super(other);
        this.threshold = other.threshold;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer output = clij2.create(input);
        Threshold.threshold(clij2, input, output, threshold);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output), progressInfo);
    }

    @JIPipeParameter("threshold")
    public float getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(float value) {
        this.threshold = value;
    }

}