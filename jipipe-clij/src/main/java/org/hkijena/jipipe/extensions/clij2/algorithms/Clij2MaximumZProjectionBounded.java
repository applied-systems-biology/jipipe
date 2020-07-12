package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MaximumZProjectionBounded;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MaximumZProjectionBounded}
 */
@JIPipeDocumentation(name = "CLIJ2 Maximum Z Projection Bounded", description = "Determines the maximum intensity projection of an image along Z within a given z range. Works for following image dimensions: 3D -> 2D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Dimensions\nZ Projection")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst_max", autoCreate = true)

public class Clij2MaximumZProjectionBounded extends JIPipeSimpleIteratingAlgorithm {
    Integer min_z;
    Integer max_z;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2MaximumZProjectionBounded(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MaximumZProjectionBounded(Clij2MaximumZProjectionBounded other) {
        super(other);
        this.min_z = other.min_z;
        this.max_z = other.max_z;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_max = clij2.create(src);
        MaximumZProjectionBounded.maximumZProjectionBounded(clij2, src, dst_max, min_z, max_z);

        dataInterface.addOutputData(getOutputSlot("dst_max"), new CLIJImageData(dst_max));
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