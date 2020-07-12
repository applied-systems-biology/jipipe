package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GenerateJaccardIndexMatrix;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GenerateJaccardIndexMatrix}
 */
@JIPipeDocumentation(name = "CLIJ2 Generate Jaccard Index Matrix", description = "Takes two labelmaps with n and m labels_2 and generates a (n+1)*(m+1) matrix where all labels_1 are set to 0 exept those where labels_2 overlap between the label maps. " + "For the remaining labels_1, the value will be between 0 and 1 indicating the overlap as measured by the Jaccard Index." + "Major parts of this operation run on the CPU. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src_label_map1", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src_label_map2", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst_jaccard_index_matrix", autoCreate = true)

public class Clij2GenerateJaccardIndexMatrix extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2GenerateJaccardIndexMatrix(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GenerateJaccardIndexMatrix(Clij2GenerateJaccardIndexMatrix other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src_label_map1 = dataInterface.getInputData(getInputSlot("src_label_map1"), CLIJImageData.class).getImage();
        ClearCLBuffer src_label_map2 = dataInterface.getInputData(getInputSlot("src_label_map2"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_jaccard_index_matrix = clij2.create(src_label_map1);
        GenerateJaccardIndexMatrix.generateJaccardIndexMatrix(clij2, src_label_map1, src_label_map2, dst_jaccard_index_matrix);

        dataInterface.addOutputData(getOutputSlot("dst_jaccard_index_matrix"), new CLIJImageData(dst_jaccard_index_matrix));
    }

}