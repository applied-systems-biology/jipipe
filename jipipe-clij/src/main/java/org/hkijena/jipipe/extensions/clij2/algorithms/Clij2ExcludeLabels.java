package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ExcludeLabels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ExcludeLabels}
 */
@JIPipeDocumentation(name = "CLIJ2 Exclude Labels", description = "This operation removes labels from a labelmap and renumbers the remaining labels. " + "Hand over a binary flag list vector starting with a flag for the background, continuing with label1, label2, ..." + "For example if you pass 0,1,0,0,1: Labels 1 and 4 will be removed (those with a 1 in the vector will be excluded). Labels 2 and 3 will be kept and renumbered to 1 and 2. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labels")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "flaglist", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "label_map_in", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "label_map_out", autoCreate = true)

public class Clij2ExcludeLabels extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ExcludeLabels(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ExcludeLabels(Clij2ExcludeLabels other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer flaglist = dataBatch.getInputData(getInputSlot("flaglist"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer label_map_in = dataBatch.getInputData(getInputSlot("label_map_in"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer label_map_out = clij2.create(flaglist);
        ExcludeLabels.excludeLabels(clij2, flaglist, label_map_in, label_map_out);

        dataBatch.addOutputData(getOutputSlot("label_map_out"), new CLIJImageData(label_map_out), progressInfo);
    }

}