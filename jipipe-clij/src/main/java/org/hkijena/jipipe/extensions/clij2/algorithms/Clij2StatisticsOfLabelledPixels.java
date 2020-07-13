package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.StatisticsOfLabelledPixels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.StatisticsOfLabelledPixels}
 */
@JIPipeDocumentation(name = "CLIJ2 Statistics Of Labelled Pixels", description = "Determines bounding box, area (in pixels/voxels), min, max and mean intensity " + " of labelled objects in a label map and corresponding pixels in the original image. " + "Instead of a label map, you can also use a binary image as a binary image is a label map with just one label." + "This method is executed on the CPU and not on the GPU/OpenCL device. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Analysis, menuPath = "Statistics")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "inputImage", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "inputLabelMap", autoCreate = true)

public class Clij2StatisticsOfLabelledPixels extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2StatisticsOfLabelledPixels(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2StatisticsOfLabelledPixels(Clij2StatisticsOfLabelledPixels other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer inputImage = dataInterface.getInputData(getInputSlot("inputImage"), CLIJImageData.class).getImage();
        ClearCLBuffer inputLabelMap = clij2.create(inputImage);
        StatisticsOfLabelledPixels.statisticsOfLabelledPixels(clij2, inputImage, inputLabelMap);

        dataInterface.addOutputData(getOutputSlot("inputLabelMap"), new CLIJImageData(inputLabelMap));
    }

}