package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.NClosestPoints;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.NClosestPoints}
 */
@JIPipeDocumentation(name = "CLIJ2 N Closest Points", description = "Determine the n point indices with shortest distance for all points in a distance matrix. " + "This corresponds to the n row indices with minimum values for each column of the distance matrix. Works for following image dimensions: 2D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Distance matrix")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "distance_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "indexlist_destination", autoCreate = true)

public class Clij2NClosestPoints extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2NClosestPoints(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2NClosestPoints(Clij2NClosestPoints other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer distance_matrix = dataBatch.getInputData(getInputSlot("distance_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer indexlist_destination = clij2.create(distance_matrix);
        NClosestPoints.nClosestPoints(clij2, distance_matrix, indexlist_destination);

        dataBatch.addOutputData(getOutputSlot("indexlist_destination"), new CLIJImageData(indexlist_destination), progressInfo);
    }

}