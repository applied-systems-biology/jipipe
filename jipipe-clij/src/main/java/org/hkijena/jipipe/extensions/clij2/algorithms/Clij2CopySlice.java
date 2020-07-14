package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.CopySlice;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.CopySlice}
 */
@JIPipeDocumentation(name = "CLIJ2 Copy Slice", description = "This method has two purposes: " + "It copies a 2D image to a given slice z position in a 3D image stack or " + "It copies a given slice at position z in an image stack to a 2D image." + "The first case is only available via ImageJ macro. If you are using it, it is recommended that the " + "target 3D image already pre-exists in GPU memory before calling this method. Otherwise, CLIJ create " + "the image stack with z planes. Works for following image dimensions: 3D -> 2D and 2D -> 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Miscellaneous, menuPath = "CLIJ2")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2CopySlice extends JIPipeSimpleIteratingAlgorithm {
    Integer planeIndex;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2CopySlice(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2CopySlice(Clij2CopySlice other) {
        super(other);
        this.planeIndex = other.planeIndex;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        CopySlice.copySlice(clij2, src, dst, planeIndex);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("plane-index")
    public Integer getPlaneIndex() {
        return planeIndex;
    }

    @JIPipeParameter("plane-index")
    public void setPlaneIndex(Integer value) {
        this.planeIndex = value;
    }

}