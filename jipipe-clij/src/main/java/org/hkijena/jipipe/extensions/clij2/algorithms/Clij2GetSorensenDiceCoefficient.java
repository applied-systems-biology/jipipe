package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GetSorensenDiceCoefficient;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GetSorensenDiceCoefficient}
 */
@JIPipeDocumentation(name = "CLIJ2 Get Sorensen Dice Coefficient", description = "Determines the overlap of two binary images using the Sorensen-Dice coefficent. " + "A value of 0 suggests no overlap, 1 means perfect overlap." + "The Sorensen-Dice coefficient is saved in the colum 'Sorensen_Dice_coefficient'." + "Note that the Sorensen-Dice coefficient s can be calculated from the Jaccard index j using this formula:" + "<pre>s = f(j) = 2 j / (j + 1)</pre> Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Binary")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "input1", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "input2", autoCreate = true)

public class Clij2GetSorensenDiceCoefficient extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2GetSorensenDiceCoefficient(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GetSorensenDiceCoefficient(Clij2GetSorensenDiceCoefficient other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input1 = dataInterface.getInputData(getInputSlot("input1"), CLIJImageData.class).getImage();
        ClearCLBuffer input2 = clij2.create(input1);
        GetSorensenDiceCoefficient.getSorensenDiceCoefficient(clij2, input1, input2);

        dataInterface.addOutputData(getOutputSlot("input2"), new CLIJImageData(input2));
    }

}