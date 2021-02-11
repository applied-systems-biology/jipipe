package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.NeighborsOfNeighbors;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.NeighborsOfNeighbors}
 */
@JIPipeDocumentation(name = "CLIJ2 Neighbors Of Neighbors", description = "Determines neighbors of neigbors from touch matrix and saves the result as a new touch matrix. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Touch matrix")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "neighbor_matrix", autoCreate = true)

public class Clij2NeighborsOfNeighbors extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2NeighborsOfNeighbors(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2NeighborsOfNeighbors(Clij2NeighborsOfNeighbors other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer touch_matrix = dataBatch.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer neighbor_matrix = clij2.create(touch_matrix);
        NeighborsOfNeighbors.neighborsOfNeighbors(clij2, touch_matrix, neighbor_matrix);

        dataBatch.addOutputData(getOutputSlot("neighbor_matrix"), new CLIJImageData(neighbor_matrix), progressInfo);
    }

}