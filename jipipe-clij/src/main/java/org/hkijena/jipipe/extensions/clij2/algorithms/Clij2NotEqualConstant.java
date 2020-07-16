package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.NotEqualConstant;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.NotEqualConstant}
 */
@JIPipeDocumentation(name = "CLIJ2 Not Equal Constant", description = "Determines if two images A and B equal pixel wise." + "f(a, b) = 1 if a != b; 0 otherwise.  Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Math\nCompare")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src1", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2NotEqualConstant extends JIPipeSimpleIteratingAlgorithm {
    Float scalar;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2NotEqualConstant(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2NotEqualConstant(Clij2NotEqualConstant other) {
        super(other);
        this.scalar = other.scalar;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src1 = dataBatch.getInputData(getInputSlot("src1"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src1);
        NotEqualConstant.notEqualConstant(clij2, src1, dst, scalar);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("scalar")
    public Float getScalar() {
        return scalar;
    }

    @JIPipeParameter("scalar")
    public void setScalar(Float value) {
        this.scalar = value;
    }

}