package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.TouchMatrixToMesh;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.TouchMatrixToMesh}
 */
@JIPipeDocumentation(name = "CLIJ2 Touch Matrix To Mesh", description = "Takes a pointlist with dimensions n*d with n point coordinates in d dimensions and a touch matrix of " + "size n*n to draw lines from all points to points if the corresponding pixel in the touch matrix is 1. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "pointlist", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "touch_matrix", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "mesh", autoCreate = true)

public class Clij2TouchMatrixToMesh extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2TouchMatrixToMesh(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2TouchMatrixToMesh(Clij2TouchMatrixToMesh other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer pointlist = dataInterface.getInputData(getInputSlot("pointlist"), CLIJImageData.class).getImage();
        ClearCLBuffer touch_matrix = dataInterface.getInputData(getInputSlot("touch_matrix"), CLIJImageData.class).getImage();
        ClearCLBuffer mesh = clij2.create(pointlist);
        TouchMatrixToMesh.touchMatrixToMesh(clij2, pointlist, touch_matrix, mesh);

        dataInterface.addOutputData(getOutputSlot("mesh"), new CLIJImageData(mesh));
    }

}