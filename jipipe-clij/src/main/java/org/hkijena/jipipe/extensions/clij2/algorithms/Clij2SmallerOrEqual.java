package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.SmallerOrEqual;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.SmallerOrEqual}
 */
@JIPipeDocumentation(name = "CLIJ2 Smaller Or Equal", description = "Determines if two images A and B smaller or equal pixel wise." + "f(a, b) = 1 if a <= b; 0 otherwise.  Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nCompare")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src1", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src2", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2SmallerOrEqual extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2SmallerOrEqual(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2SmallerOrEqual(Clij2SmallerOrEqual other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src1 = dataBatch.getInputData(getInputSlot("src1"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer src2 = dataBatch.getInputData(getInputSlot("src2"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src1);
        SmallerOrEqual.smallerOrEqual(clij2, src1, src2, dst);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

}