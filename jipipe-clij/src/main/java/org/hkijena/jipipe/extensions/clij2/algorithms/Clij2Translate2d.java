package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Translate2D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Translate2D}
 */
@JIPipeDocumentation(name = "CLIJ2 Translate 2D", description = "Translate an image stack in X and Y. Works for following image dimensions: 2D.")
@JIPipeOrganization(nodeTypeCategory = ProcessorNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Translate2d extends JIPipeSimpleIteratingAlgorithm {
    Float translateX;
    Float translateY;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Translate2d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Translate2d(Clij2Translate2d other) {
        super(other);
        this.translateX = other.translateX;
        this.translateY = other.translateY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        Translate2D.translate2D(clij2, input, output, translateX, translateY);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("translate-x")
    public Float getTranslateX() {
        return translateX;
    }

    @JIPipeParameter("translate-x")
    public void setTranslateX(Float value) {
        this.translateX = value;
    }

    @JIPipeParameter("translate-y")
    public Float getTranslateY() {
        return translateY;
    }

    @JIPipeParameter("translate-y")
    public void setTranslateY(Float value) {
        this.translateY = value;
    }

}