package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import ij.ImagePlus;
import mcib3d.image3d.ImageHandler;
import mcib_plugins.Hysteresis_Thresholding;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@JIPipeDocumentation(name = "3D hysteresis segmentation", description = "Will perform 3-levels thresholding based on two thresholds. " +
        "The low threshold will determine background (for values below), " +
        "the high threshold the objects cores (for values above) and an intermediate level for values between low and high threshold. ")
@JIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/3D-Hysteresis-Segmentation/")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class HysteresisSegmentation3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private float highThreshold = 128;
    private float lowThreshold = 50;

    private boolean labeling = false;

    public HysteresisSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public HysteresisSegmentation3DAlgorithm(HysteresisSegmentation3DAlgorithm other) {
        super(other);
        this.highThreshold = other.highThreshold;
        this.lowThreshold = other.lowThreshold;
        this.labeling = other.labeling;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage = IJ3DUtils.forEach3DIn5DGenerate(inputImage, (ih, index, ctProgress) -> {
            Hysteresis_Thresholding hysteresisThresholding = new Hysteresis_Thresholding();
            ImagePlus hysteresis = hysteresisThresholding.hysteresis(ih.getImagePlus(), lowThreshold, highThreshold, false, labeling);
            return ImageHandler.wrap(hysteresis);
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Generate labels", description = "If enabled, generate a label image instead of a mask")
    @JIPipeParameter("labeling")
    public boolean isLabeling() {
        return labeling;
    }

    @JIPipeParameter("labeling")
    public void setLabeling(boolean labeling) {
        this.labeling = labeling;
    }

    @JIPipeDocumentation(name = "High threshold", description = "Determines the object cores (for values above)")
    @JIPipeParameter("high-threshold")
    public float getHighThreshold() {
        return highThreshold;
    }

    @JIPipeParameter("high-threshold")
    public void setHighThreshold(float highThreshold) {
        this.highThreshold = highThreshold;
    }

    @JIPipeDocumentation(name = "Low threshold", description = "Determines the background (for values below)")
    @JIPipeParameter("low-threshold")
    public float getLowThreshold() {
        return lowThreshold;
    }

    @JIPipeParameter("low-threshold")
    public void setLowThreshold(float lowThreshold) {
        this.lowThreshold = lowThreshold;
    }
}
