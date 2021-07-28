package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MinimumZProjectionThresholdedBounded;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MinimumZProjectionThresholdedBounded}
 */
@JIPipeDocumentation(name = "CLIJ2 Minimum Z Projection Thresholded Bounded", description = "Determines the minimum intensity projection of all pixels in an image above a given threshold along Z within a given z range. Works for following image dimensions: 3D -> 2D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions\nZ Projection")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_min", autoCreate = true)

public class Clij2MinimumZProjectionThresholdedBounded extends JIPipeSimpleIteratingAlgorithm {
    float threshold_intensity;
    int min_z;
    int max_z;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MinimumZProjectionThresholdedBounded(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MinimumZProjectionThresholdedBounded(Clij2MinimumZProjectionThresholdedBounded other) {
        super(other);
        this.threshold_intensity = other.threshold_intensity;
        this.min_z = other.min_z;
        this.max_z = other.max_z;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst_min = clij2.create(src);
        MinimumZProjectionThresholdedBounded.minimumZProjectionThresholdedBounded(clij2, src, dst_min, threshold_intensity, min_z, max_z);

        dataBatch.addOutputData(getOutputSlot("dst_min"), new CLIJImageData(dst_min), progressInfo);
    }

    @JIPipeParameter("threshold-intensity")
    public float getThreshold_intensity() {
        return threshold_intensity;
    }

    @JIPipeParameter("threshold-intensity")
    public void setThreshold_intensity(float value) {
        this.threshold_intensity = value;
    }

    @JIPipeParameter("min-z")
    public int getMin_z() {
        return min_z;
    }

    @JIPipeParameter("min-z")
    public void setMin_z(int value) {
        this.min_z = value;
    }

    @JIPipeParameter("max-z")
    public int getMax_z() {
        return max_z;
    }

    @JIPipeParameter("max-z")
    public void setMax_z(int value) {
        this.max_z = value;
    }

}