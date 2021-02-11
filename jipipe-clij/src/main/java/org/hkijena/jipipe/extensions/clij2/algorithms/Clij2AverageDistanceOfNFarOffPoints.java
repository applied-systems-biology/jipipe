package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.AverageDistanceOfNFarOffPoints;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.AverageDistanceOfNFarOffPoints}
 */
@JIPipeDocumentation(name = "CLIJ2 Average Distance Of N Far Off Points", description = "Determines the average of the n far off (most distant) points for every point in a distance matrix." + "This corresponds to the average of the n maximum values (rows) for each column of the distance matrix. Works for following image dimensions: 2D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Distance matrix")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "distance_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "indexlist_destination", autoCreate = true)

public class Clij2AverageDistanceOfNFarOffPoints extends JIPipeSimpleIteratingAlgorithm {
    int nPoints;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2AverageDistanceOfNFarOffPoints(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2AverageDistanceOfNFarOffPoints(Clij2AverageDistanceOfNFarOffPoints other) {
        super(other);
        this.nPoints = other.nPoints;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer distance_matrix = dataBatch.getInputData(getInputSlot("distance_matrix"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer indexlist_destination = clij2.create(distance_matrix);
        AverageDistanceOfNFarOffPoints.averageDistanceOfNFarOffPoints(clij2, distance_matrix, indexlist_destination, nPoints);

        dataBatch.addOutputData(getOutputSlot("indexlist_destination"), new CLIJImageData(indexlist_destination), progressInfo);
    }

    @JIPipeParameter("n-points")
    public int getNPoints() {
        return nPoints;
    }

    @JIPipeParameter("n-points")
    public void setNPoints(int value) {
        this.nPoints = value;
    }

}