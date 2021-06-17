package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesOutOfRange;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ExcludeLabelsWithValuesOutOfRange}
 */
@JIPipeDocumentation(name = "CLIJ2 Exclude Labels With Values Out Of Range", description = "This operation removes labels from a labelmap and renumbers the remaining labels. " + "Hand over a vector of values and a range specifying which labels with which values are eliminated. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labels")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "values", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "label_map_in", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "label_map_out", autoCreate = true)

public class Clij2ExcludeLabelsWithValuesOutOfRange extends JIPipeIteratingAlgorithm {
    float min;
    float max;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ExcludeLabelsWithValuesOutOfRange(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer values = dataBatch.getInputData(getInputSlot("values"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer label_map_in = dataBatch.getInputData(getInputSlot("label_map_in"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer label_map_out = clij2.create(values);
        ExcludeLabelsWithValuesOutOfRange.excludeLabelsWithValuesOutOfRange(clij2, values, label_map_in, label_map_out, min, max);

        dataBatch.addOutputData(getOutputSlot("label_map_out"), new CLIJImageData(label_map_out), progressInfo);
    }

    @JIPipeParameter("min")
    public float getMin() {
        return min;
    }

    @JIPipeParameter("min")
    public void setMin(float value) {
        this.min = value;
    }

    @JIPipeParameter("max")
    public float getMax() {
        return max;
    }

    @JIPipeParameter("max")
    public void setMax(float value) {
        this.max = value;
    }

}