package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MinimumDistanceOfTouchingNeighbors;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MinimumDistanceOfTouchingNeighbors}
 */
@JIPipeDocumentation(name = "CLIJ2 Minimum Distance Of Touching Neighbors", description = "Takes a touch matrix and a distance matrix to determine the shortest distance of touching neighbors for every object. Works for following image dimensions: 2D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Touch matrix")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "distance_matrix", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "minimum_distancelist_destination", autoCreate = true)

public class Clij2MinimumDistanceOfTouchingNeighbors extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MinimumDistanceOfTouchingNeighbors(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MinimumDistanceOfTouchingNeighbors(Clij2MinimumDistanceOfTouchingNeighbors other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer distance_matrix = dataBatch.getInputData(getInputSlot("distance_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer touch_matrix = dataBatch.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer minimum_distancelist_destination = clij2.create(distance_matrix);
        MinimumDistanceOfTouchingNeighbors.minimumDistanceOfTouchingNeighbors(clij2, distance_matrix, touch_matrix, minimum_distancelist_destination);

        dataBatch.addOutputData(getOutputSlot("minimum_distancelist_destination"), new CLIJImageData(minimum_distancelist_destination), progressInfo);
    }

}