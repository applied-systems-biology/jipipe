package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.CloseIndexGapsInLabelMap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.CloseIndexGapsInLabelMap}
 */
@JIPipeDocumentation(name = "CLIJ2 Close Index Gaps In Label Map", description = "Analyses a label map and if there are gaps in the indexing (e.g. label 5 is not present) all " + "subsequent labels will be relabelled. " + "Thus, afterwards number of labels and maximum label index are equal." + "This operation is mostly performed on the CPU. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Morphology")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2CloseIndexGapsInLabelMap extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2CloseIndexGapsInLabelMap(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2CloseIndexGapsInLabelMap(Clij2CloseIndexGapsInLabelMap other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer output = clij2.create(input);
        CloseIndexGapsInLabelMap.closeIndexGapsInLabelMap(clij2, input, output);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output), progressInfo);
    }

}