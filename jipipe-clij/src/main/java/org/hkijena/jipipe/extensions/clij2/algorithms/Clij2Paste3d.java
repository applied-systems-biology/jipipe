package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Paste3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Paste3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Paste 3D", description = "Pastes an image into another image at a given position. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Paste3d extends JIPipeSimpleIteratingAlgorithm {
    Integer destination_x;
    Integer destination_y;
    Integer destination_z;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Paste3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Paste3d(Clij2Paste3d other) {
        super(other);
        this.destination_x = other.destination_x;
        this.destination_y = other.destination_y;
        this.destination_z = other.destination_z;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Paste3D.paste(clij2, src, dst, destination_x, destination_y, destination_z);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("destination-x")
    public Integer getDestination_x() {
        return destination_x;
    }

    @JIPipeParameter("destination-x")
    public void setDestination_x(Integer value) {
        this.destination_x = value;
    }

    @JIPipeParameter("destination-y")
    public Integer getDestination_y() {
        return destination_y;
    }

    @JIPipeParameter("destination-y")
    public void setDestination_y(Integer value) {
        this.destination_y = value;
    }

    @JIPipeParameter("destination-z")
    public Integer getDestination_z() {
        return destination_z;
    }

    @JIPipeParameter("destination-z")
    public void setDestination_z(Integer value) {
        this.destination_z = value;
    }

}