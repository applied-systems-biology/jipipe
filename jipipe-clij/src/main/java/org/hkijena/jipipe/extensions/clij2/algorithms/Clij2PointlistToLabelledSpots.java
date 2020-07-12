package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.PointlistToLabelledSpots;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.PointlistToLabelledSpots}
 */
@JIPipeDocumentation(name = "CLIJ2 Pointlist To Labelled Spots", description = "Takes a pointlist with dimensions n*d with n point coordinates in d dimensions and a touch matrix of " + "size n*n to draw lines from all points to points if the corresponding pixel in the touch matrix is 1. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Point list")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "pointlist", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "labelledSpots", autoCreate = true)

public class Clij2PointlistToLabelledSpots extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2PointlistToLabelledSpots(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2PointlistToLabelledSpots(Clij2PointlistToLabelledSpots other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer pointlist = dataInterface.getInputData(getInputSlot("pointlist"), CLIJImageData.class).getImage();
        ClearCLBuffer labelledSpots = clij2.create(pointlist);
        PointlistToLabelledSpots.pointlistToLabelledSpots(clij2, pointlist, labelledSpots);

        dataInterface.addOutputData(getOutputSlot("labelledSpots"), new CLIJImageData(labelledSpots));
    }

}