package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.CountTouchingNeighbors;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.CountTouchingNeighbors}
 */
@JIPipeDocumentation(name = "CLIJ2 Count Touching Neighbors", description = "Takes a touch matrix as input and delivers a vector with number of touching neighbors per label as a vector. Works for following image dimensions: 2D -> 1D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nCount")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_touch_matrix", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_count_list", autoCreate = true)

public class Clij2CountTouchingNeighbors extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2CountTouchingNeighbors(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_touch_matrix = dataBatch.getInputData(getInputSlot("src_touch_matrix"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_count_list = clij2.create(src_touch_matrix);
        CountTouchingNeighbors.countTouchingNeighbors(clij2, src_touch_matrix, dst_count_list);

        dataBatch.addOutputData(getOutputSlot("dst_count_list"), new CLIJImageData(dst_count_list));
    }

}