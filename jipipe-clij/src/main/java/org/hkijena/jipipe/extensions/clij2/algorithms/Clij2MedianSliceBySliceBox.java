package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MedianSliceBySliceBox;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MedianSliceBySliceBox}
 */
@JIPipeDocumentation(name = "CLIJ2 Median Slice By Slice Box", description = "Computes the local median of a pixels rectangular neighborhood. " + "This is done slice-by-slice in a 3D " + "image stack. The rectangle is specified by its half-width and half-height (radius)." + "For technical reasons, the area of the rectangle must have less than 1000 pixels. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2MedianSliceBySliceBox extends JIPipeSimpleIteratingAlgorithm {
    Integer radiusX;
    Integer radiusY;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2MedianSliceBySliceBox(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MedianSliceBySliceBox(Clij2MedianSliceBySliceBox other) {
        super(other);
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        MedianSliceBySliceBox.median3DSliceBySliceBox(clij2, src, dst, radiusX, radiusY);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("radius-x")
    public Integer getRadiusX() {
        return radiusX;
    }

    @JIPipeParameter("radius-x")
    public void setRadiusX(Integer value) {
        this.radiusX = value;
    }

    @JIPipeParameter("radius-y")
    public Integer getRadiusY() {
        return radiusY;
    }

    @JIPipeParameter("radius-y")
    public void setRadiusY(Integer value) {
        this.radiusY = value;
    }

}