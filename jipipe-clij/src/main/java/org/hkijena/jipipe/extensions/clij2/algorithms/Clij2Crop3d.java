package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Crop3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Crop3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Crop 3D", description = "Crops a given sub-stack out of a given image stack. " + "Note: If the destination image pre-exists already, it will be overwritten and keep it's dimensions. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Transform")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Crop3d extends JIPipeSimpleIteratingAlgorithm {
    Integer startX;
    Integer startY;
    Integer startZ;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2Crop3d(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Crop3d(Clij2Crop3d other) {
        super(other);
        this.startX = other.startX;
        this.startY = other.startY;
        this.startZ = other.startZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Crop3D.crop(clij2, src, dst, startX, startY, startZ);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("start-x")
    public Integer getStartX() {
        return startX;
    }

    @JIPipeParameter("start-x")
    public void setStartX(Integer value) {
        this.startX = value;
    }

    @JIPipeParameter("start-y")
    public Integer getStartY() {
        return startY;
    }

    @JIPipeParameter("start-y")
    public void setStartY(Integer value) {
        this.startY = value;
    }

    @JIPipeParameter("start-z")
    public Integer getStartZ() {
        return startZ;
    }

    @JIPipeParameter("start-z")
    public void setStartZ(Integer value) {
        this.startZ = value;
    }

}