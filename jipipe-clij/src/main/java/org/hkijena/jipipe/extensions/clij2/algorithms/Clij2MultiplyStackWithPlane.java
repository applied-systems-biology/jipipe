package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MultiplyStackWithPlane;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MultiplyStackWithPlane}
 */
@JIPipeDocumentation(name = "CLIJ2 Multiply Stack With Plane", description = "Multiplies all pairs of pixel values x and y from an image stack X and a 2D image Y. " + "x and y are at " + "the same spatial position within a plane." + "<pre>f(x, y) = x * y</pre> Works for following image dimensions: 3D (first parameter), 2D (second parameter), 3D (result).")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input3d", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input2d", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output3d", autoCreate = true)

public class Clij2MultiplyStackWithPlane extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MultiplyStackWithPlane(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MultiplyStackWithPlane(Clij2MultiplyStackWithPlane other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input3d = dataBatch.getInputData(getInputSlot("input3d"), CLIJImageData.class).getImage();
        ClearCLBuffer input2d = dataBatch.getInputData(getInputSlot("input2d"), CLIJImageData.class).getImage();
        ClearCLBuffer output3d = clij2.create(input3d);
        MultiplyStackWithPlane.multiplyStackWithPlane(clij2, input3d, input2d, output3d);

        dataBatch.addOutputData(getOutputSlot("output3d"), new CLIJImageData(output3d));
    }

}