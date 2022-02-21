package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.AdjacencyMatrixToTouchMatrix;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.AdjacencyMatrixToTouchMatrix}
 */
@JIPipeDocumentation(name = "CLIJ2 Adjacency Matrix To Touch Matrix", description = "Converts a adjacency matrix in a touch matrix. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "adjacency_matrix", autoCreate = true)

public class Clij2AdjacencyMatrixToTouchMatrix extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2AdjacencyMatrixToTouchMatrix(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2AdjacencyMatrixToTouchMatrix(Clij2AdjacencyMatrixToTouchMatrix other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer touch_matrix = dataBatch.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer adjacency_matrix = clij2.create(touch_matrix);
        AdjacencyMatrixToTouchMatrix.adjacencyMatrixToTouchMatrix(clij2, touch_matrix, adjacency_matrix);

        dataBatch.addOutputData(getOutputSlot("adjacency_matrix"), new CLIJImageData(adjacency_matrix), progressInfo);
    }

}