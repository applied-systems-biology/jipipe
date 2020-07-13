package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.LabelledSpotsToPointList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.LabelledSpotsToPointList}
 */
@JIPipeDocumentation(name = "CLIJ2 Labelled Spots To Point List", description = "Generates a coordinate list of points in a labelled spot image. " + "Transforms a labelmap of spots (single pixels with values 1, 2, ..., n for n spots) as resulting " + "from connected components analysis in an image where every column contains d " + "pixels (with d = dimensionality of the original image) with the coordinates of the maxima/minima. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Analysis, menuPath = "Labels")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input_labelmap", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2LabelledSpotsToPointList extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2LabelledSpotsToPointList(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2LabelledSpotsToPointList(Clij2LabelledSpotsToPointList other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input_labelmap = dataInterface.getInputData(getInputSlot("input_labelmap"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input_labelmap);
        LabelledSpotsToPointList.labelledSpotsToPointList(clij2, input_labelmap, output);

        dataInterface.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

}