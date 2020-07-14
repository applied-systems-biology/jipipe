package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MeanZProjectionBounded;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MeanZProjectionBounded}
 */
@JIPipeDocumentation(name = "CLIJ2 Mean Z Projection Bounded", description = "Determines the mean average intensity projection of an image along Z within a given z range. Works for following image dimensions: 3D -> 2D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Dimensions\nZ Projection")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_mean", autoCreate = true)

public class Clij2MeanZProjectionBounded extends JIPipeSimpleIteratingAlgorithm {
    Integer min_z;
    Integer max_z;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2MeanZProjectionBounded(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MeanZProjectionBounded(Clij2MeanZProjectionBounded other) {
        super(other);
        this.min_z = other.min_z;
        this.max_z = other.max_z;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_mean = clij2.create(src);
        MeanZProjectionBounded.meanZProjectionBounded(clij, src, dst_mean, min_z, max_z);

        dataInterface.addOutputData(getOutputSlot("dst_mean"), new CLIJImageData(dst_mean));
    }

    @JIPipeParameter("min-z")
    public Integer getMin_z() {
        return min_z;
    }

    @JIPipeParameter("min-z")
    public void setMin_z(Integer value) {
        this.min_z = value;
    }

    @JIPipeParameter("max-z")
    public Integer getMax_z() {
        return max_z;
    }

    @JIPipeParameter("max-z")
    public void setMax_z(Integer value) {
        this.max_z = value;
    }

}