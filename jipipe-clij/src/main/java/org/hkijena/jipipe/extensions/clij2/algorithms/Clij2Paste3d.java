package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Paste3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Paste3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Paste 3D", description = "Pastes an image into another image at a given position. Works for following image dimensions: 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Paste3d extends JIPipeSimpleIteratingAlgorithm {
    int destination_x;
    int destination_y;
    int destination_z;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Paste3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Paste3d(Clij2Paste3d other) {
        super(other);
        this.destination_x = other.destination_x;
        this.destination_y = other.destination_y;
        this.destination_z = other.destination_z;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Paste3D.paste(clij2, src, dst, destination_x, destination_y, destination_z);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

    @JIPipeParameter("destination-x")
    public int getDestination_x() {
        return destination_x;
    }

    @JIPipeParameter("destination-x")
    public void setDestination_x(int value) {
        this.destination_x = value;
    }

    @JIPipeParameter("destination-y")
    public int getDestination_y() {
        return destination_y;
    }

    @JIPipeParameter("destination-y")
    public void setDestination_y(int value) {
        this.destination_y = value;
    }

    @JIPipeParameter("destination-z")
    public int getDestination_z() {
        return destination_z;
    }

    @JIPipeParameter("destination-z")
    public void setDestination_z(int value) {
        this.destination_z = value;
    }

}