package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesOutOfRange;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesOutOfRange}
 */
@JIPipeDocumentation(name = "CLIJ2 Exclude Labels With Values Out Of Range", description = "This operation removes labels from a labelmap and renumbers the remaining labels. " + "Hand over a vector of values and a range specifying which labels with which values are eliminated. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Labels")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "values", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "label_map_in", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "label_map_out", autoCreate = true)

public class Clij2ExcludeLabelsWithValuesOutOfRange extends JIPipeIteratingAlgorithm {
    Float min;
    Float max;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2ExcludeLabelsWithValuesOutOfRange(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ExcludeLabelsWithValuesOutOfRange(Clij2ExcludeLabelsWithValuesOutOfRange other) {
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
        ExcludeLabelsWithValuesOutOfRange.excludeLabelsWithValuesOutOfRange(clij2, values, label_map_in, label_map_out, min, max);

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