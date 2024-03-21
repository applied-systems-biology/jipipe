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

package org.hkijena.jipipe.extensions.ij3d.nodes.binary;

import ij.ImagePlus;
import mcib3d.image3d.distanceMap3d.EDT;
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
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

@SetJIPipeDocumentation(name = "Euclidean Distance Map 3D", description = "Calculates the Euclidean distance map of a 3D image. " +
        "The calculated distances will be in the calibrated unit.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nDistance map")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
public class DistanceMap3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int threshold = 0;

    private boolean inverse;

    public DistanceMap3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DistanceMap3DAlgorithm(DistanceMap3DAlgorithm other) {
        super(other);
        this.threshold = other.threshold;
        this.inverse = other.inverse;
    }

    @SetJIPipeDocumentation(name = "Threshold", description = "Threshold value for the mask")
    @JIPipeParameter("threshold")
    public int getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @SetJIPipeDocumentation(name = "Inverse", description = "If enabled, the threshold is applied on the inverse iamge")
    @JIPipeParameter("inverse")
    public boolean isInverse() {
        return inverse;
    }

    @JIPipeParameter("inverse")
    public void setInverse(boolean inverse) {
        this.inverse = inverse;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImagePlus outputImage = IJ3DUtils.forEach3DIn5DGenerate(inputImage, (ih, index, ctProgress) -> EDT.run(ih, threshold, inverse, 0), progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
