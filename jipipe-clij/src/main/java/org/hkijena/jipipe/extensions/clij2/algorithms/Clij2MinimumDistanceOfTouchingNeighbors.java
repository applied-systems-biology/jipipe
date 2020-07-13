package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MinimumDistanceOfTouchingNeighbors;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MinimumDistanceOfTouchingNeighbors}
 */
@JIPipeDocumentation(name = "CLIJ2 Minimum Distance Of Touching Neighbors", description = "Takes a touch matrix and a distance matrix to determine the shortest distance of touching neighbors for every object. Works for following image dimensions: 2D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Touch matrix")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "distance_matrix", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "minimum_distancelist_destination", autoCreate = true)

public class Clij2MinimumDistanceOfTouchingNeighbors extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2MinimumDistanceOfTouchingNeighbors(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
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
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer distance_matrix = dataInterface.getInputData(getInputSlot("distance_matrix"), CLIJImageData.class).getImage();
        ClearCLBuffer touch_matrix = dataInterface.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class).getImage();
        ClearCLBuffer minimum_distancelist_destination = clij2.create(distance_matrix);
        MinimumDistanceOfTouchingNeighbors.minimumDistanceOfTouchingNeighbors(clij2, distance_matrix, touch_matrix, minimum_distancelist_destination);

        dataInterface.addOutputData(getOutputSlot("minimum_distancelist_destination"), new CLIJImageData(minimum_distancelist_destination));
    }

}