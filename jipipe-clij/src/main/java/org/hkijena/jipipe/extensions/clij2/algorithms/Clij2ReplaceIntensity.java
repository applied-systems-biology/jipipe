package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ReplaceIntensity;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ReplaceIntensity}
 */
@JIPipeDocumentation(name = "CLIJ2 Replace Intensity", description = "Replaces a specific intensity in an image with a given new value. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nReplace")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ReplaceIntensity extends JIPipeSimpleIteratingAlgorithm {
    float in;
    float out;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ReplaceIntensity(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ReplaceIntensity.replaceIntensity(clij2, src, dst, in, out);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("in")
    public float getIn() {
        return in;
    }

    @JIPipeParameter("in")
    public void setIn(float value) {
        this.in = value;
    }

    @JIPipeParameter("out")
    public float getOut() {
        return out;
    }

    @JIPipeParameter("out")
    public void setOut(float value) {
        this.out = value;
    }

}