package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.TopHatBox;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.TopHatBox}
 */
@JIPipeDocumentation(name = "CLIJ2 Top Hat Box", description = "Applies a top-hat filter for background subtraction to the input image. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Morphology\nTop Hat")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2TopHatBox extends JIPipeSimpleIteratingAlgorithm {
    int radiusX;
    int radiusY;
    int radiusZ;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2TopHatBox(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2TopHatBox(Clij2TopHatBox other) {
        super(other);
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer output = clij2.create(input);
        TopHatBox.topHatBox(clij2, input, output, radiusX, radiusY, radiusZ);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output), progressInfo);
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

    @JIPipeParameter("radius-z")
    public int getRadiusZ() {
        return radiusZ;
    }

    @JIPipeParameter("radius-z")
    public void setRadiusZ(int value) {
        this.radiusZ = value;
    }

}