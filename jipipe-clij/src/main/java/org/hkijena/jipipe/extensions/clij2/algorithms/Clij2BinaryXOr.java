package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.BinaryXOr;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.BinaryXOr}
 */
@JIPipeDocumentation(name = "CLIJ2 Binary X Or", description = "Computes a binary image (containing pixel values 0 and 1) from two images X and Y by connecting pairs of" + "pixels x and y with the binary operators AND &, OR | and NOT ! implementing the XOR operator." + "All pixel values except 0 in the input images are interpreted as 1." + "<pre>f(x, y) = (x & !y) | (!x & y)</pre> Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src1", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src2", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2BinaryXOr extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2BinaryXOr(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2BinaryXOr(Clij2BinaryXOr other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src1 = dataBatch.getInputData(getInputSlot("src1"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer src2 = dataBatch.getInputData(getInputSlot("src2"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src1);
        BinaryXOr.binaryXOr(clij2, src1, src2, dst);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

}