package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.CountNonZeroPixelsSliceBySliceSphere;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.CountNonZeroPixelsSliceBySliceSphere}
 */
@JIPipeDocumentation(name = "CLIJ2 Count Non Zero Pixels Slice By Slice Sphere", description = "Counts non-zero pixels in a sphere around every pixel slice by slice in a stack. " + " It puts the resulting number in the destination image stack. Works for following image dimensions: 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nCount")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2CountNonZeroPixelsSliceBySliceSphere extends JIPipeSimpleIteratingAlgorithm {
    int radiusX;
    int radiusY;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2CountNonZeroPixelsSliceBySliceSphere(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2CountNonZeroPixelsSliceBySliceSphere(Clij2CountNonZeroPixelsSliceBySliceSphere other) {
        super(other);
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        CountNonZeroPixelsSliceBySliceSphere.countNonZeroPixelsSliceBySliceSphere(clij2, src, dst, radiusX, radiusY);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

    @JIPipeParameter("radius-x")
    public int getRadiusX() {
        return radiusX;
    }

    @JIPipeParameter("radius-x")
    public void setRadiusX(int value) {
        this.radiusX = value;
    }

    @JIPipeParameter("radius-y")
    public int getRadiusY() {
        return radiusY;
    }

    @JIPipeParameter("radius-y")
    public void setRadiusY(int value) {
        this.radiusY = value;
    }

}