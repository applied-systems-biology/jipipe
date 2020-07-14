package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesWithinRange;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesWithinRange}
 */
@JIPipeDocumentation(name = "CLIJ2 Exclude Labels With Values Within Range", description = "This operation removes labels from a labelmap and renumbers the remaining labels. " + "Hand over a vector of values and a range specifying which labels with which values are eliminated. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Labels")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "values", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "label_map_in", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "label_map_out", autoCreate = true)

public class Clij2ExcludeLabelsWithValuesWithinRange extends JIPipeIteratingAlgorithm {
    Float min;
    Float max;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ExcludeLabelsWithValuesWithinRange(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ExcludeLabelsWithValuesWithinRange(Clij2ExcludeLabelsWithValuesWithinRange other) {
        super(other);
        this.min = other.min;
        this.max = other.max;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer values = dataInterface.getInputData(getInputSlot("values"), CLIJImageData.class).getImage();
        ClearCLBuffer label_map_in = dataInterface.getInputData(getInputSlot("label_map_in"), CLIJImageData.class).getImage();
        ClearCLBuffer label_map_out = clij2.create(values);
        ExcludeLabelsWithValuesWithinRange.excludeLabelsWithValuesWithinRange(clij2, values, label_map_in, label_map_out, min, max);

        dataInterface.addOutputData(getOutputSlot("label_map_out"), new CLIJImageData(label_map_out));
    }

    @JIPipeParameter("min")
    public Float getMin() {
        return min;
    }

    @JIPipeParameter("min")
    public void setMin(Float value) {
        this.min = value;
    }

    @JIPipeParameter("max")
    public Float getMax() {
        return max;
    }

    @JIPipeParameter("max")
    public void setMax(Float value) {
        this.max = value;
    }

}