package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.BinaryFillHoles;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.BinaryFillHoles}
 */
@JIPipeDocumentation(name = "CLIJ2 Binary Fill Holes", description = "Fills holes (pixels with value 0 surrounded by pixels with value 1) in a binary image. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2BinaryFillHoles extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2BinaryFillHoles(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2BinaryFillHoles(Clij2BinaryFillHoles other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        BinaryFillHoles.binaryFillHoles(clij2, src, dst);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

}