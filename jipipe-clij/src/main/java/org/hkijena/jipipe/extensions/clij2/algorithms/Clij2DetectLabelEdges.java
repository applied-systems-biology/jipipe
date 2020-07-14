package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.DetectLabelEdges;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.DetectLabelEdges}
 */
@JIPipeDocumentation(name = "CLIJ2 Detect Label Edges", description = "Takes a labelmap and returns an image where all pixels on label edges are set to 1 and all other pixels to 0. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Edges")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src_label_map", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_edge_image", autoCreate = true)

public class Clij2DetectLabelEdges extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2DetectLabelEdges(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2DetectLabelEdges(Clij2DetectLabelEdges other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_label_map = dataBatch.getInputData(getInputSlot("src_label_map"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_edge_image = clij2.create(src_label_map);
        DetectLabelEdges.detectLabelEdges(clij2, src_label_map, dst_edge_image);

        dataBatch.addOutputData(getOutputSlot("dst_edge_image"), new CLIJImageData(dst_edge_image));
    }

}