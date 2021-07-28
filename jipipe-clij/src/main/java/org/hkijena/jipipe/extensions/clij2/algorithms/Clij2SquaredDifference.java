package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.SquaredDifference;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.SquaredDifference}
 */
@JIPipeDocumentation(name = "CLIJ2 Squared Difference", description = "Determines the squared difference pixel by pixel between two images. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nCalculate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "buffer1", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "buffer2", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "result", autoCreate = true)

public class Clij2SquaredDifference extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2SquaredDifference(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2SquaredDifference(Clij2SquaredDifference other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer buffer1 = dataBatch.getInputData(getInputSlot("buffer1"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer buffer2 = dataBatch.getInputData(getInputSlot("buffer2"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer result = clij2.create(buffer1);
        SquaredDifference.squaredDifference(clij2, buffer1, buffer2, result);

        dataBatch.addOutputData(getOutputSlot("result"), new CLIJImageData(result), progressInfo);
    }

}