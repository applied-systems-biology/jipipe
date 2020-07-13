package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.LabelToMask;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.LabelToMask}
 */
@JIPipeDocumentation(name = "CLIJ2 Label To Mask", description = "Masks a single label in a label map. " + "Sets all pixels in the target image to 1, where the given label index was present in the label map. Other pixels are set to 0. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "labelMap", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "maskOutput", autoCreate = true)

public class Clij2LabelToMask extends JIPipeSimpleIteratingAlgorithm {
    Float index;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2LabelToMask(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2LabelToMask(Clij2LabelToMask other) {
        super(other);
        this.index = other.index;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer labelMap = dataInterface.getInputData(getInputSlot("labelMap"), CLIJImageData.class).getImage();
        ClearCLBuffer maskOutput = clij2.create(labelMap);
        LabelToMask.labelToMask(clij2, labelMap, maskOutput, index);

        dataInterface.addOutputData(getOutputSlot("maskOutput"), new CLIJImageData(maskOutput));
    }

    @JIPipeParameter("index")
    public Float getIndex() {
        return index;
    }

    @JIPipeParameter("index")
    public void setIndex(Float value) {
        this.index = value;
    }

}