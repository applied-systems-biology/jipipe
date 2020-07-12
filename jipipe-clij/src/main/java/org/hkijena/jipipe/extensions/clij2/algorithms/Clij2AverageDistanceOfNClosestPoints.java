package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.AverageDistanceOfNClosestPoints;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.AverageDistanceOfNClosestPoints}
 */
@JIPipeDocumentation(name = "CLIJ2 Average Distance Of N Closest Points", description = "Determines the average of the n closest points for every point in a distance matrix." + "This corresponds to the average of the n minimum values (rows) for each column of the distance matrix. Works for following image dimensions: 2D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Distance Matrix")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "distance_matrix", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "indexlist_destination", autoCreate = true)

public class Clij2AverageDistanceOfNClosestPoints extends JIPipeSimpleIteratingAlgorithm {
    Integer nPoints;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2AverageDistanceOfNClosestPoints(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2AverageDistanceOfNClosestPoints(Clij2AverageDistanceOfNClosestPoints other) {
        super(other);
        this.nPoints = other.nPoints;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer distance_matrix = dataInterface.getInputData(getInputSlot("distance_matrix"), CLIJImageData.class).getImage();
        ClearCLBuffer indexlist_destination = clij2.create(distance_matrix);
        AverageDistanceOfNClosestPoints.averageDistanceOfNClosestPoints(clij2, distance_matrix, indexlist_destination, nPoints);

        dataInterface.addOutputData(getOutputSlot("indexlist_destination"), new CLIJImageData(indexlist_destination));
    }

    @JIPipeParameter("n-points")
    public Integer getNPoints() {
        return nPoints;
    }

    @JIPipeParameter("n-points")
    public void setNPoints(Integer value) {
        this.nPoints = value;
    }

}