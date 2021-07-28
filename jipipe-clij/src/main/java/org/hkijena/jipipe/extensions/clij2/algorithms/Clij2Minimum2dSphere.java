package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Minimum2DSphere;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Minimum2DSphere}
 */
@JIPipeDocumentation(name = "CLIJ2 Minimum 2D Sphere", description = "Computes the local minimum of a pixels ellipsoidal neighborhood. " + "The ellipses size is specified by " + "its half-width and half-height (radius). Works for following image dimensions: 2D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nLocal")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Minimum2dSphere extends JIPipeSimpleIteratingAlgorithm {
    int radiusX;
    int radiusY;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Minimum2dSphere(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Minimum2dSphere(Clij2Minimum2dSphere other) {
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
        Minimum2DSphere.minimum2DSphere(clij2, src, dst, radiusX, radiusY);

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