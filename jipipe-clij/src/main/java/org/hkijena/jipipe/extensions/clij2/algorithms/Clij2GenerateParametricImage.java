package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GenerateParametricImage;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GenerateParametricImage}
 */
@JIPipeDocumentation(name = "CLIJ2 Generate Parametric Image", description = "Take a labelmap and a vector of values to replace label 1 with the 1st value in the vector. " + "Note that indexing in the vector starts at zero. The 0th entry corresponds to background in the label map.Internally this method just calls ReplaceIntensities." + " Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Generate")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "label_map", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "parameter_value_vector", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "parametric_image_destination", autoCreate = true)

public class Clij2GenerateParametricImage extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2GenerateParametricImage(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GenerateParametricImage(Clij2GenerateParametricImage other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer label_map = dataInterface.getInputData(getInputSlot("label_map"), CLIJImageData.class).getImage();
        ClearCLBuffer parameter_value_vector = dataInterface.getInputData(getInputSlot("parameter_value_vector"), CLIJImageData.class).getImage();
        ClearCLBuffer parametric_image_destination = clij2.create(label_map);
        GenerateParametricImage.generateParametricImage(clij2, label_map, parameter_value_vector, parametric_image_destination);

        dataInterface.addOutputData(getOutputSlot("parametric_image_destination"), new CLIJImageData(parametric_image_destination));
    }

}