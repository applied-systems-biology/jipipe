package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.CentroidsOfLabels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.CentroidsOfLabels}
 */
@JIPipeDocumentation(name = "CLIJ2 Centroids Of Labels", description = "Determines the centroids of all labels in a label image or image stack. " + "It writes the resulting  coordinates in a pointlist image. Depending on the dimensionality d of the labelmap and the number  of labels n, the pointlist image will have n*d pixels. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Features")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "labelMap", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "pointlist", autoCreate = true)

public class Clij2CentroidsOfLabels extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2CentroidsOfLabels(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2CentroidsOfLabels(Clij2CentroidsOfLabels other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer labelMap = dataBatch.getInputData(getInputSlot("labelMap"), CLIJImageData.class).getImage();
        ClearCLBuffer pointlist = clij2.create(labelMap);
        CentroidsOfLabels.centroidsOfLabels(clij2, labelMap, pointlist);

        dataBatch.addOutputData(getOutputSlot("pointlist"), new CLIJImageData(pointlist));
    }

}