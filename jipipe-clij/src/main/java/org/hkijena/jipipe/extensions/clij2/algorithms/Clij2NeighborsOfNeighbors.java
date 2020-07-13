package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.NeighborsOfNeighbors;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.NeighborsOfNeighbors}
 */
@JIPipeDocumentation(name = "CLIJ2 Neighbors Of Neighbors", description = "Determines neighbors of neigbors from touch matrix and saves the result as a new touch matrix. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Touch matrix")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "neighbor_matrix", autoCreate = true)

public class Clij2NeighborsOfNeighbors extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2NeighborsOfNeighbors(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
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
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer touch_matrix = dataInterface.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class).getImage();
        ClearCLBuffer neighbor_matrix = clij2.create(touch_matrix);
        NeighborsOfNeighbors.neighborsOfNeighbors(clij2, touch_matrix, neighbor_matrix);

        dataInterface.addOutputData(getOutputSlot("neighbor_matrix"), new CLIJImageData(neighbor_matrix));
    }

}