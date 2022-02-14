package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.LabelToMask;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.LabelToMask}
 */
@JIPipeDocumentation(name = "CLIJ2 Label To Mask", description = "Masks a single label in a label map. " + "Sets all pixels in the target image to 1, where the given label index was present in the label map. Other pixels are set to 0. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "labelMap", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "maskOutput", autoCreate = true)

public class Clij2LabelToMask extends JIPipeSimpleIteratingAlgorithm {
    float index;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2LabelToMask(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2LabelToMask(Clij2LabelToMask other) {
        super(other);
        this.index = other.index;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer labelMap = dataBatch.getInputData(getInputSlot("labelMap"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer maskOutput = clij2.create(labelMap);
        LabelToMask.labelToMask(clij2, labelMap, maskOutput, index);

        dataBatch.addOutputData(getOutputSlot("maskOutput"), new CLIJImageData(maskOutput), progressInfo);
    }

    @JIPipeParameter("index")
    public float getIndex() {
        return index;
    }

    @JIPipeParameter("index")
    public void setIndex(float value) {
        this.index = value;
    }

}