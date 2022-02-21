package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.PointlistToLabelledSpots;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.PointlistToLabelledSpots}
 */
@JIPipeDocumentation(name = "CLIJ2 Pointlist To Labelled Spots", description = "Takes a pointlist with dimensions n*d with n point coordinates in d dimensions and a touch matrix of " + "size n*n to draw lines from all points to points if the corresponding pixel in the touch matrix is 1. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Point list")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "pointlist", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "labelledSpots", autoCreate = true)

public class Clij2PointlistToLabelledSpots extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2PointlistToLabelledSpots(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2PointlistToLabelledSpots(Clij2PointlistToLabelledSpots other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer pointlist = dataBatch.getInputData(getInputSlot("pointlist"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer labelledSpots = clij2.create(pointlist);
        PointlistToLabelledSpots.pointlistToLabelledSpots(clij2, pointlist, labelledSpots);

        dataBatch.addOutputData(getOutputSlot("labelledSpots"), new CLIJImageData(labelledSpots), progressInfo);
    }

}