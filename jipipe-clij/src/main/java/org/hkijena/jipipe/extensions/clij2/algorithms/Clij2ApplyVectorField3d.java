package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ApplyVectorField3D;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ApplyVectorField3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Apply Vector Field 3D", description = "Deforms an image stack according to distances provided in the given vector image stacks." + "It is recommended to use 32-bit image stacks for input, output and vector image stacks.  Works for following image dimensions: 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "vectorX", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "vectorY", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "vectorZ", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ApplyVectorField3d extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ApplyVectorField3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ApplyVectorField3d(Clij2ApplyVectorField3d other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer vectorX = dataBatch.getInputData(getInputSlot("vectorX"), CLIJImageData.class).getImage();
        ClearCLBuffer vectorY = dataBatch.getInputData(getInputSlot("vectorY"), CLIJImageData.class).getImage();
        ClearCLBuffer vectorZ = dataBatch.getInputData(getInputSlot("vectorZ"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ApplyVectorField3D.applyVectorField(clij2, src, vectorX, vectorY, vectorZ, dst);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

}