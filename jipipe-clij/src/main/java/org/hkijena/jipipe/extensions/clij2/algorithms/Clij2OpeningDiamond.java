package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.OpeningDiamond;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.OpeningDiamond}
 */
@JIPipeDocumentation(name = "CLIJ2 Opening Diamond", description = "Apply a binary opening to the input image by calling n erosions and n dilations subsequenntly. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ProcessorNodeTypeCategory.class, menuPath = "Morphology\nOpening")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2OpeningDiamond extends JIPipeSimpleIteratingAlgorithm {
    Integer radius;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2OpeningDiamond(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2OpeningDiamond(Clij2OpeningDiamond other) {
        super(other);
        this.radius = other.radius;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        OpeningDiamond.openingDiamond(clij2, input, output, radius);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("radius")
    public Integer getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(Integer value) {
        this.radius = value;
    }

}