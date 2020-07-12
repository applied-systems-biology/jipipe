package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ReplaceIntensity;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ReplaceIntensity}
 */
@JIPipeDocumentation(name = "CLIJ2 Replace Intensity", description = "Replaces a specific intensity in an image with a given new value. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ReplaceIntensity extends JIPipeSimpleIteratingAlgorithm {
    Float in;
    Float out;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2ReplaceIntensity(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ReplaceIntensity(Clij2ReplaceIntensity other) {
        super(other);
        this.in = other.in;
        this.out = other.out;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ReplaceIntensity.replaceIntensity(clij2, src, dst, in, out);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("in")
    public Float getIn() {
        return in;
    }

    @JIPipeParameter("in")
    public void setIn(Float value) {
        this.in = value;
    }

    @JIPipeParameter("out")
    public Float getOut() {
        return out;
    }

    @JIPipeParameter("out")
    public void setOut(Float value) {
        this.out = value;
    }

}