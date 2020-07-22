package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GenerateDistanceMatrix;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GenerateDistanceMatrix}
 */
@JIPipeDocumentation(name = "CLIJ2 Generate Distance matrix", description = "Takes two images containing coordinates and builds up a matrix containing distance between the points. " + "Convention: image width represents number of points, height represents dimensionality (2D, 3D, ... 10D). The result image has width the first input image and height equals to the width of the second input image. Works for following image dimensions: 2D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Generate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_pointlist1", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_pointlist2", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_distance_matrix", autoCreate = true)

public class Clij2GenerateDistanceMatrix extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2GenerateDistanceMatrix(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GenerateDistanceMatrix(Clij2GenerateDistanceMatrix other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_pointlist1 = dataBatch.getInputData(getInputSlot("src_pointlist1"), CLIJImageData.class).getImage();
        ClearCLBuffer src_pointlist2 = dataBatch.getInputData(getInputSlot("src_pointlist2"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_distance_matrix = clij2.create(src_pointlist1);
        GenerateDistanceMatrix.generateDistanceMatrix(clij2, src_pointlist1, src_pointlist2, dst_distance_matrix);

        dataBatch.addOutputData(getOutputSlot("dst_distance_matrix"), new CLIJImageData(dst_distance_matrix));
    }

}