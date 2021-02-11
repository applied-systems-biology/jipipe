package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.NClosestDistances;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.NClosestDistances}
 */
@JIPipeDocumentation(name = "CLIJ2 N Closest Distances", description = "Determine the n point indices with shortest distance for all points in a distance matrix. " + "This corresponds to the n row indices with minimum values for each column of the distance matrix.Returns the n shortest distances in one image and the point indices in another image. Works for following image dimensions: 2D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Distance matrix")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "distance_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "distancelist_destination", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "indexlist_destination", autoCreate = true)

public class Clij2NClosestDistances extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2NClosestDistances(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2NClosestDistances(Clij2NClosestDistances other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer distance_matrix = dataBatch.getInputData(getInputSlot("distance_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer distancelist_destination = clij2.create(distance_matrix);
        ClearCLBuffer indexlist_destination = clij2.create(distance_matrix);
        NClosestDistances.nClosestDistances(clij2, distance_matrix, distancelist_destination, indexlist_destination);

        dataBatch.addOutputData(getOutputSlot("distancelist_destination"), new CLIJImageData(distancelist_destination), progressInfo);
        dataBatch.addOutputData(getOutputSlot("indexlist_destination"), new CLIJImageData(indexlist_destination), progressInfo);
    }

}