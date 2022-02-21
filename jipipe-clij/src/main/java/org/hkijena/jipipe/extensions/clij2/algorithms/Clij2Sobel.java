package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Sobel;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Sobel}
 */
@JIPipeDocumentation(name = "CLIJ2 Sobel", description = "Convolve the image with the Sobel kernel. Works for following image dimensions: 2D, 3D. Developed by Ruth Whelan-Jeans, Robert Haase.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Edges")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Sobel extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Sobel(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Sobel(Clij2Sobel other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Sobel.sobel(clij2, src, dst);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

}