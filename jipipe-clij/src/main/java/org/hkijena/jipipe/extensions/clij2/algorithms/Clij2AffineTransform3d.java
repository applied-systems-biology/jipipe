package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.AffineTransform3D;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.AffineTransform3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Affine Transform 3D", description = "Applies an affine transform to a 3D image. " +
        "Individual transforms must be separated by spaces." +
        "Supported transforms:"
        + "<ul><li>center: translate the coordinate origin to the center of the image</li>" +
        "<li>-center: translate the coordinate origin back to the initial origin</li>" +
        "<li>rotate=[angle]: rotate in X/Y plane (around Z-axis) by the given angle in degrees</li>" +
        "<li>rotateX=[angle]: rotate in Y/Z plane (around X-axis) by the given angle in degrees</li>" +
        "<li>rotateY=[angle]: rotate in X/Z plane (around Y-axis) by the given angle in degrees</li>" +
        "<li>rotateZ=[angle]: rotate in X/Y plane (around Z-axis) by the given angle in degrees</li>" +
        "<li>scale=[factor]: isotropic scaling according to given zoom factor</li>" +
        "<li>scaleX=[factor]: scaling along X-axis according to given zoom factor</li>" +
        "<li>scaleY=[factor]: scaling along Y-axis according to given zoom factor</li>" +
        "<li>scaleZ=[factor]: scaling along Z-axis according to given zoom factor</li>" +
        "<li>shearXY=[factor]: shearing along X-axis in XY plane according to given factor</li>" +
        "<li>shearXZ=[factor]: shearing along X-axis in XZ plane according to given factor</li>" +
        "<li>shearYX=[factor]: shearing along Y-axis in XY plane according to given factor</li>" +
        "<li>shearYZ=[factor]: shearing along Y-axis in YZ plane according to given factor</li>" +
        "<li>shearZX=[factor]: shearing along Z-axis in XZ plane according to given factor</li>" +
        "<li>shearZY=[factor]: shearing along Z-axis in YZ plane according to given factor</li>" +
        "<li>translateX=[distance]: translate along X-axis by distance given in pixels</li>" +
        "<li>translateY=[distance]: translate along X-axis by distance given in pixels</li>" +
        "<li>translateZ=[distance]: translate along X-axis by distance given in pixels"
        + "Example transform: <code>transform = 'center scale=2 rotate=45 -center'</code>; " +
        "Works for following image dimensions: 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)
@JIPipeCitation("Developed by Robert Haase based on work by Martin Weigert.")
@JIPipeCitation("Adapted from: https://github.com/maweigert/gputools/blob/master/gputools/transforms/kernels/transformations.cl\"")
public class Clij2AffineTransform3d extends JIPipeSimpleIteratingAlgorithm {
    String transform;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2AffineTransform3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2AffineTransform3d(Clij2AffineTransform3d other) {
        super(other);
        this.transform = other.transform;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer output = clij2.create(input);
        AffineTransform3D.affineTransform3D(clij2, input, output, transform);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output), progressInfo);
    }

    @JIPipeParameter("transform")
    public String getTransform() {
        return transform;
    }

    @JIPipeParameter("transform")
    public void setTransform(String value) {
        this.transform = value;
    }

}