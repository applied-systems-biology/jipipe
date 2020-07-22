package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MultiplyImageAndCoordinate;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MultiplyImageAndCoordinate}
 */
@JIPipeDocumentation(name = "CLIJ2 Multiply Image And Coordinate", description = "Multiplies all pixel intensities with the x, y or z coordinate, depending on specified dimension. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ProcessorNodeTypeCategory.class, menuPath = "Math\nLocal")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2MultiplyImageAndCoordinate extends JIPipeSimpleIteratingAlgorithm {
    Integer dimension;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MultiplyImageAndCoordinate(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MultiplyImageAndCoordinate(Clij2MultiplyImageAndCoordinate other) {
        super(other);
        this.dimension = other.dimension;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        MultiplyImageAndCoordinate.multiplyImageAndCoordinate(clij2, src, dst, dimension);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("dimension")
    public Integer getDimension() {
        return dimension;
    }

    @JIPipeParameter("dimension")
    public void setDimension(Integer value) {
        this.dimension = value;
    }

}