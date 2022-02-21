package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MaximumOctagon;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MaximumOctagon}
 */
@JIPipeDocumentation(name = "CLIJ2 Maximum Octagon", description = "Applies a maximum filter with kernel size 3x3 n times to an image iteratively. " + "Odd iterations are done with box neighborhood, even iterations with a diamond. " + "Thus, with n > 2, the filter shape is an octagon. The given number of iterations makes the filter " + "result very similar to minimum sphere. Approximately:radius = iterations - 2 Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nLocal")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2MaximumOctagon extends JIPipeSimpleIteratingAlgorithm {
    int iterations;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MaximumOctagon(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MaximumOctagon(Clij2MaximumOctagon other) {
        super(other);
        this.iterations = other.iterations;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        MaximumOctagon.maximumOctagon(clij2, src, dst, iterations);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

    @JIPipeParameter("iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("iterations")
    public void setIterations(int value) {
        this.iterations = value;
    }

}