package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Mask;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Mask}
 */
@JIPipeDocumentation(name = "CLIJ2 Mask", description = "Computes a masked image by applying a binary mask to an image. " + "All pixel values x of image X will be copied" + "to the destination image in case pixel value m at the same position in the mask image is not equal to " + "zero." + "<pre>f(x,m) = (x if (m != 0); (0 otherwise))</pre> Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Mask")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "mask", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Mask extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Mask(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Mask(Clij2Mask other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer mask = dataBatch.getInputData(getInputSlot("mask"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Mask.mask(clij2, src, mask, dst);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

}