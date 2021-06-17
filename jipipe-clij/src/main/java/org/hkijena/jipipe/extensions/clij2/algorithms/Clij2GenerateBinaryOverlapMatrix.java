package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GenerateBinaryOverlapMatrix;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GenerateBinaryOverlapMatrix}
 */
@JIPipeDocumentation(name = "CLIJ2 Generate Binary Overlap Matrix", description = "Takes two labelmaps with n and m labels and generates a (n+1)*(m+1) matrix where all pixels are set to 0 exept those where labels overlap between the label maps. " + "For example, if labels 3 in labelmap1 and 4 in labelmap2 are touching then the pixel (3,4) in the matrix will be set to 1. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_label_map1", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_label_map2", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_touch_matrix", autoCreate = true)

public class Clij2GenerateBinaryOverlapMatrix extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2GenerateBinaryOverlapMatrix(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GenerateBinaryOverlapMatrix(Clij2GenerateBinaryOverlapMatrix other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_label_map1 = dataBatch.getInputData(getInputSlot("src_label_map1"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer src_label_map2 = dataBatch.getInputData(getInputSlot("src_label_map2"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst_touch_matrix = clij2.create(src_label_map1);
        GenerateBinaryOverlapMatrix.generateBinaryOverlapMatrix(clij2, src_label_map1, src_label_map2, dst_touch_matrix);

        dataBatch.addOutputData(getOutputSlot("dst_touch_matrix"), new CLIJImageData(dst_touch_matrix), progressInfo);
    }

}