package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.CountTouchingNeighbors;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.CountTouchingNeighbors}
 */
@JIPipeDocumentation(name = "CLIJ2 Count Touching Neighbors", description = "Takes a touch matrix as input and delivers a vector with number of touching neighbors per label as a vector. Works for following image dimensions: 2D -> 1D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Math\nCount")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src_touch_matrix", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst_count_list", autoCreate = true)

public class Clij2CountTouchingNeighbors extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2CountTouchingNeighbors(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2CountTouchingNeighbors(Clij2CountTouchingNeighbors other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_touch_matrix = dataInterface.getInputData(getInputSlot("src_touch_matrix"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_count_list = clij2.create(src_touch_matrix);
        CountTouchingNeighbors.countTouchingNeighbors(clij2, src_touch_matrix, dst_count_list);

        dataInterface.addOutputData(getOutputSlot("dst_count_list"), new CLIJImageData(dst_count_list));
    }

}