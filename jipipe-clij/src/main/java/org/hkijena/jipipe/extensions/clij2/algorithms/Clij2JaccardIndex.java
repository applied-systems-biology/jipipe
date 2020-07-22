package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.JaccardIndex;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.JaccardIndex}
 */
@JIPipeDocumentation(name = "CLIJ2 Jaccard Index", description = "Determines the overlap of two binary images using the Jaccard index. " + "A value of 0 suggests no overlap, 1 means perfect overlap." + "The resulting Jaccard index is saved to the results table in the 'Jaccard_Index' column." + "Note that the Sorensen-Dice coefficient can be calculated from the Jaccard index j using this formula:" + "<pre>s = f(j) = 2 j / (j + 1)</pre> Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Binary")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input1", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "input2", autoCreate = true)

public class Clij2JaccardIndex extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2JaccardIndex(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2JaccardIndex(Clij2JaccardIndex other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input1 = dataBatch.getInputData(getInputSlot("input1"), CLIJImageData.class).getImage();
        ClearCLBuffer input2 = clij2.create(input1);
        JaccardIndex.jaccardIndex(clij2, input1, input2);

        dataBatch.addOutputData(getOutputSlot("input2"), new CLIJImageData(input2));
    }

}