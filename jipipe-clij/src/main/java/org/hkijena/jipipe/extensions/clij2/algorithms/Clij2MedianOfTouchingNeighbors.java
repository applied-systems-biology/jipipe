package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MedianOfTouchingNeighbors;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MedianOfTouchingNeighbors}
 */
@JIPipeDocumentation(name = "CLIJ2 Median Of Touching Neighbors", description = "Takes a touch matrix and a vector of values to determine the median value among touching neighbors for every object. " + " Works for following image dimensions: 2D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Touch matrix")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_values", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_values", autoCreate = true)

public class Clij2MedianOfTouchingNeighbors extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MedianOfTouchingNeighbors(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MedianOfTouchingNeighbors(Clij2MedianOfTouchingNeighbors other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_values = dataBatch.getInputData(getInputSlot("src_values"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer touch_matrix = dataBatch.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst_values = clij2.create(src_values);
        MedianOfTouchingNeighbors.medianOfTouchingNeighbors(clij2, src_values, touch_matrix, dst_values);

        dataBatch.addOutputData(getOutputSlot("dst_values"), new CLIJImageData(dst_values), progressInfo);
    }

}