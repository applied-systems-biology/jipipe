package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GenerateTouchCountMatrix;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GenerateTouchCountMatrix}
 */
@JIPipeDocumentation(name = "CLIJ2 Generate Touch Count Matrix", description = "Takes a label map with n labels and generates a (n+1)*(n+1) matrix where all pixels are set the number of pixels where labels touch (diamond neighborhood). " + "Major parts of this operation run on the CPU. Works for following image dimensions: 2D, 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_label_map1", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_touch_count_matrix", autoCreate = true)

public class Clij2GenerateTouchCountMatrix extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2GenerateTouchCountMatrix(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GenerateTouchCountMatrix(Clij2GenerateTouchCountMatrix other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_label_map1 = dataBatch.getInputData(getInputSlot("src_label_map1"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst_touch_count_matrix = clij2.create(src_label_map1);
        GenerateTouchCountMatrix.generateTouchCountMatrix(clij2, src_label_map1, dst_touch_count_matrix);

        dataBatch.addOutputData(getOutputSlot("dst_touch_count_matrix"), new CLIJImageData(dst_touch_count_matrix), progressInfo);
    }

}