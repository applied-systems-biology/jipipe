/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ij3d.nodes.segmentation;

import ij.ImagePlus;
import mcib3d.image3d.ImageHandler;
import mcib_plugins.Hysteresis_Thresholding;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@SetJIPipeDocumentation(name = "3D hysteresis segmentation", description = "Will perform 3-levels thresholding based on two thresholds. " +
        "The low threshold will determine background (for values below), " +
        "the high threshold the objects cores (for values above) and an intermediate level for values between low and high threshold. ")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/3D-Hysteresis-Segmentation/")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
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

    @SetJIPipeDocumentation(name = "Generate labels", description = "If enabled, generate a label image instead of a mask")
    @JIPipeParameter("labeling")
    public boolean isLabeling() {
        return labeling;
    }

    @JIPipeParameter("labeling")
    public void setLabeling(boolean labeling) {
        this.labeling = labeling;
    }

    @SetJIPipeDocumentation(name = "High threshold", description = "Determines the object cores (for values above)")
    @JIPipeParameter("high-threshold")
    public float getHighThreshold() {
        return highThreshold;
    }

    @JIPipeParameter("high-threshold")
    public void setHighThreshold(float highThreshold) {
        this.highThreshold = highThreshold;
    }

    @SetJIPipeDocumentation(name = "Low threshold", description = "Determines the background (for values below)")
    @JIPipeParameter("low-threshold")
    public float getLowThreshold() {
        return lowThreshold;
    }

    @JIPipeParameter("low-threshold")
    public void setLowThreshold(float lowThreshold) {
        this.lowThreshold = lowThreshold;
    }
}
