package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ReduceStack;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ReduceStack}
 */
@JIPipeDocumentation(name = "CLIJ2 Reduce Stack", description = "Reduces the number of slices in a stack by a given factor." + "With the offset you have control which slices stay: " + "* With factor 3 and offset 0, slices 0, 3, 6,... are kept. * With factor 4 and offset 1, slices 1, 5, 9,... are kept. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Dimensions")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ReduceStack extends JIPipeSimpleIteratingAlgorithm {
    Integer factor;
    Integer offset;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ReduceStack(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ReduceStack(Clij2ReduceStack other) {
        super(other);
        this.factor = other.factor;
        this.offset = other.offset;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ReduceStack.reduceStack(clij2, src, dst, factor, offset);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("factor")
    public Integer getFactor() {
        return factor;
    }

    @JIPipeParameter("factor")
    public void setFactor(Integer value) {
        this.factor = value;
    }

    @JIPipeParameter("offset")
    public Integer getOffset() {
        return offset;
    }

    @JIPipeParameter("offset")
    public void setOffset(Integer value) {
        this.offset = value;
    }

}