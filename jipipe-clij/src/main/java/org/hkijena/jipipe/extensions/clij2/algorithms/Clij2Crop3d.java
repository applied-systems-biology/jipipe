package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Crop3D;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Crop3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Crop 3D", description = "Crops a given sub-stack out of a given image stack. " + "Note: If the destination image pre-exists already, it will be overwritten and keep it's dimensions. Works for following image dimensions: 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Crop3d extends JIPipeSimpleIteratingAlgorithm {
    int startX;
    int startY;
    int startZ;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Crop3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Crop3d(Clij2Crop3d other) {
        super(other);
        this.startX = other.startX;
        this.startY = other.startY;
        this.startZ = other.startZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Crop3D.crop(clij2, src, dst, startX, startY, startZ);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

    @JIPipeParameter("start-x")
    public int getStartX() {
        return startX;
    }

    @JIPipeParameter("start-x")
    public void setStartX(int value) {
        this.startX = value;
    }

    @JIPipeParameter("start-y")
    public int getStartY() {
        return startY;
    }

    @JIPipeParameter("start-y")
    public void setStartY(int value) {
        this.startY = value;
    }

    @JIPipeParameter("start-z")
    public int getStartZ() {
        return startZ;
    }

    @JIPipeParameter("start-z")
    public void setStartZ(int value) {
        this.startZ = value;
    }

}