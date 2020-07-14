package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MaskLabel;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MaskLabel}
 */
@JIPipeDocumentation(name = "CLIJ2 Mask Label", description = "Computes a masked image by applying a label mask to an image. " + "All pixel values x of image X will be copied" + "to the destination image in case pixel value m at the same position in the label_map image has the right index value i." + "f(x,m,i) = (x if (m == i); (0 otherwise)) Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Mask")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "label_map", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2MaskLabel extends JIPipeIteratingAlgorithm {
    Float index;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MaskLabel(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MaskLabel(Clij2MaskLabel other) {
        super(other);
        this.index = other.index;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer label_map = dataBatch.getInputData(getInputSlot("label_map"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        MaskLabel.maskLabel(clij2, input, label_map, output, index);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("index")
    public Float getIndex() {
        return index;
    }

    @JIPipeParameter("index")
    public void setIndex(Float value) {
        this.index = value;
    }

}