package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MinimumImageAndScalar;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MinimumImageAndScalar}
 */
@JIPipeDocumentation(name = "CLIJ2 Minimum Image And Scalar", description = "Computes the minimum of a constant scalar s and each pixel value x in a given image X." + "<pre>f(x, s) = min(x, s)</pre> Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Math\nCalculate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2MinimumImageAndScalar extends JIPipeSimpleIteratingAlgorithm {
    Float valueB;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2MinimumImageAndScalar(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MinimumImageAndScalar(Clij2MinimumImageAndScalar other) {
        super(other);
        this.valueB = other.valueB;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        MinimumImageAndScalar.minimumImageAndScalar(clij2, src, dst, valueB);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("value-b")
    public Float getValueB() {
        return valueB;
    }

    @JIPipeParameter("value-b")
    public void setValueB(Float value) {
        this.valueB = value;
    }

}