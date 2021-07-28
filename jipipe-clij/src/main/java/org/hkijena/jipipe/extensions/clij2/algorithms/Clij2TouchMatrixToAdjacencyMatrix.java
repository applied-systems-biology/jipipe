package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.TouchMatrixToAdjacencyMatrix;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.TouchMatrixToAdjacencyMatrix}
 */
@JIPipeDocumentation(name = "CLIJ2 Touch Matrix To Adjacency Matrix", description = "Converts a touch matrix in an adjacency matrix Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "adjacency_matrix", autoCreate = true)

public class Clij2TouchMatrixToAdjacencyMatrix extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2TouchMatrixToAdjacencyMatrix(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2TouchMatrixToAdjacencyMatrix(Clij2TouchMatrixToAdjacencyMatrix other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer touch_matrix = dataBatch.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer adjacency_matrix = clij2.create(touch_matrix);
        TouchMatrixToAdjacencyMatrix.touchMatrixToAdjacencyMatrix(clij2, touch_matrix, adjacency_matrix);

        dataBatch.addOutputData(getOutputSlot("adjacency_matrix"), new CLIJImageData(adjacency_matrix), progressInfo);
    }

}