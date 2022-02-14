package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Paste2D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Paste2D}
 */
@JIPipeDocumentation(name = "CLIJ2 Paste 2D", description = "Pastes an image into another image at a given position. Works for following image dimensions: 2D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Paste2d extends JIPipeSimpleIteratingAlgorithm {
    int destination_x;
    int destination_y;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Paste2d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Paste2d(Clij2Paste2d other) {
        super(other);
        this.destination_x = other.destination_x;
        this.destination_y = other.destination_y;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Paste2D.paste(clij2, src, dst, destination_x, destination_y);

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

}