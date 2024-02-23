package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import ij.ImagePlus;
import ij.process.AutoThresholder;
import mcib3d.image3d.segment.Segment3DNuclei;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

@SetJIPipeDocumentation(name = "3D nuclei segmentation", description = "This plugin is designed to segment nuclei from cell culture (not from tissues). " +
        "The method is based on a maximum Z-projection followed by a 2D Segmentation. " +
        "The segmentation for the 2D projection is based on a global thresholding. The nuclei are then segmented and separated using ImageJ watershed. ")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Nuclei-Segmentation/")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
public class NucleiSegmentation3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private AutoThresholder.Method autoThresholdMethod = AutoThresholder.Method.Default;

    private OptionalIntegerParameter customThreshold = new OptionalIntegerParameter(false, 0);

    private boolean separateNuclei = true;

    public NucleiSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public NucleiSegmentation3DAlgorithm(NucleiSegmentation3DAlgorithm other) {
        super(other);
        this.autoThresholdMethod = other.autoThresholdMethod;
        this.customThreshold = new OptionalIntegerParameter(other.customThreshold);
        this.separateNuclei = other.separateNuclei;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage = IJ3DUtils.forEach3DIn5DGenerate(inputImage, (ih, index, ctProgress) -> {
            Segment3DNuclei segment3DNuclei = new Segment3DNuclei(ih);
            segment3DNuclei.setMethod(autoThresholdMethod);
            if (customThreshold.isEnabled()) {
                segment3DNuclei.setManual(customThreshold.getContent());
            } else {
                segment3DNuclei.setManual(0);
            }
            segment3DNuclei.setSeparate(separateNuclei);
            return segment3DNuclei.segment();
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Auto threshold method", description = "The auto threshold method (if auto thresholding is enabled)")
    @JIPipeParameter("auto-threshold-method")
    public AutoThresholder.Method getAutoThresholdMethod() {
        return autoThresholdMethod;
    }

    @JIPipeParameter("auto-threshold-method")
    public void setAutoThresholdMethod(AutoThresholder.Method autoThresholdMethod) {
        this.autoThresholdMethod = autoThresholdMethod;
    }

    @SetJIPipeDocumentation(name = "Set custom threshold", description = "If enabled, auto-thresholding is disabled and a custom threshold is utilized")
    @JIPipeParameter("custom-threshold")
    public OptionalIntegerParameter getCustomThreshold() {
        return customThreshold;
    }

    @JIPipeParameter("custom-threshold")
    public void setCustomThreshold(OptionalIntegerParameter customThreshold) {
        this.customThreshold = customThreshold;
    }

    @SetJIPipeDocumentation(name = "Separate nuclei", description = "If enabled, the nuclei are separated using watershed")
    @JIPipeParameter("separate-nuclei")
    public boolean isSeparateNuclei() {
        return separateNuclei;
    }

    @JIPipeParameter("separate-nuclei")
    public void setSeparateNuclei(boolean separateNuclei) {
        this.separateNuclei = separateNuclei;
    }
}
