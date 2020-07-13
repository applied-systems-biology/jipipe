package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ExcludeLabelsSubSurface;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ExcludeLabelsSubSurface}
 */
@JIPipeDocumentation(name = "CLIJ2 Exclude Labels Sub Surface", description = "This operation follows a ray from a given position towards a label (or opposite direction) and checks if  there is another label between the label an the image border. " + "If yes, this label is eliminated from the label map. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Labels")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "pointlist", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "label_map_in", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "label_map_out", autoCreate = true)

public class Clij2ExcludeLabelsSubSurface extends JIPipeIteratingAlgorithm {
    Float centerX;
    Float centerY;
    Float centerZ;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2ExcludeLabelsSubSurface(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ExcludeLabelsSubSurface(Clij2ExcludeLabelsSubSurface other) {
        super(other);
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.centerZ = other.centerZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer pointlist = dataInterface.getInputData(getInputSlot("pointlist"), CLIJImageData.class).getImage();
        ClearCLBuffer label_map_in = dataInterface.getInputData(getInputSlot("label_map_in"), CLIJImageData.class).getImage();
        ClearCLBuffer label_map_out = clij2.create(pointlist);
        ExcludeLabelsSubSurface.excludeLabelsSubSurface(clij2, pointlist, label_map_in, label_map_out, centerX, centerY, centerZ);

        dataInterface.addOutputData(getOutputSlot("label_map_out"), new CLIJImageData(label_map_out));
    }

    @JIPipeParameter("center-x")
    public Float getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(Float value) {
        this.centerX = value;
    }

    @JIPipeParameter("center-y")
    public Float getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(Float value) {
        this.centerY = value;
    }

    @JIPipeParameter("center-z")
    public Float getCenterZ() {
        return centerZ;
    }

    @JIPipeParameter("center-z")
    public void setCenterZ(Float value) {
        this.centerZ = value;
    }

}