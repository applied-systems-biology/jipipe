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

package org.hkijena.jipipe.extensions.ij3d.nodes.features;

import ij.ImagePlus;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.MaximaFinder;
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
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@SetJIPipeDocumentation(name = "3D maxima finder", description = "Detects local maxima with high contrast. " +
        "Local maxima in a specified radius are computed and sorted by intensity. " +
        "Starting from local maximum with highest intensity, a 3D flooding is performed, " +
        "all connected pixels with values above the value of (LocalMaxima - NoiseValue) are marked as zone of the local maximum." +
        " All other local maxima from these zones are removed. " +
        "The next local maximum is processed, this step is repeated until there are no more local maxima to process. " +
        "The final image is created with local maxima, with their original intensity value.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
public class FindMaxima3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private float minThreshold = 0;
    private float noise = 100;
    private float radiusXY = 1.5f;
    private float radiusZ = 1.5f;

    public FindMaxima3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FindMaxima3DAlgorithm(FindMaxima3DAlgorithm other) {
        super(other);
        this.minThreshold = other.minThreshold;
        this.noise = other.noise;
        this.radiusXY = other.radiusXY;
        this.radiusZ = other.radiusZ;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus outputImage = IJ3DUtils.forEach3DIn5DGenerate(inputImage, (ih, index, ctProgress) -> {
            ctProgress.log("Removing peaks below " + minThreshold);
            ImageHandler thresholded = ih.duplicate();
            thresholded.thresholdCut(minThreshold, false, true);
            MaximaFinder maximaFinder = new MaximaFinder(thresholded, noise);
            maximaFinder.setRadii(radiusXY, radiusZ);
            return maximaFinder.getImagePeaks();
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Minimum threshold", description = "Peaks below this value are removed")
    @JIPipeParameter("min-threshold")
    public float getMinThreshold() {
        return minThreshold;
    }

    @JIPipeParameter("min-threshold")
    public void setMinThreshold(float minThreshold) {
        this.minThreshold = minThreshold;
    }

    @SetJIPipeDocumentation(name = "Noise", description = "All connected pixels with values above the value of (LocalMaxima - NoiseValue) are marked as zone of the local maximum")
    @JIPipeParameter("noise")
    public float getNoise() {
        return noise;
    }

    @JIPipeParameter("noise")
    public void setNoise(float noise) {
        this.noise = noise;
    }

    @SetJIPipeDocumentation(name = "Radius (X/Y)", description = "The radius in the X/Y plane")
    @JIPipeParameter("radius-xy")
    public float getRadiusXY() {
        return radiusXY;
    }

    @JIPipeParameter("radius-xy")
    public void setRadiusXY(float radiusXY) {
        this.radiusXY = radiusXY;
    }

    @SetJIPipeDocumentation(name = "Radius (Z)", description = "Radius in the Z direction")
    @JIPipeParameter("radius-z")
    public float getRadiusZ() {
        return radiusZ;
    }

    @JIPipeParameter("radius-z")
    public void setRadiusZ(float radiusZ) {
        this.radiusZ = radiusZ;
    }
}
