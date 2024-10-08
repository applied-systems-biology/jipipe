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

package org.hkijena.jipipe.plugins.ij3d.nodes.binary;

import ij.ImagePlus;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.distanceMap3d.EDT;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ij3d.IJ3DUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Eroded Volume Fraction 3D", description = "The EVF (Eroded Volume Fraction) can be regarded as a normalized EDT (Euclidean Distance Map). " +
        "The calculated distances will be in the calibrated unit.")
@AddJIPipeCitation("Read about EVF: https://onlinelibrary.wiley.com/doi/full/10.1002/jcb.21823")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nDistance map")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", description = "Mask for the EVF", optional = true, create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
public class ErodedVolumeFraction3DAlgorithm extends JIPipeIteratingAlgorithm {

    private int threshold = 0;

    private boolean inverse;

    public ErodedVolumeFraction3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ErodedVolumeFraction3DAlgorithm(ErodedVolumeFraction3DAlgorithm other) {
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
        ImagePlus inputImage = iterationStep.getInputData("Input", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImagePlus maskImage = ImageJUtils.unwrap(iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo));
        ImagePlus outputImage = IJ3DUtils.forEach3DIn5DGenerate(inputImage, (img, index, ctProgress) -> {
            ctProgress.log("Computing Distance Map (EDT) ...");
            ImageFloat r = EDT.run(img, threshold, inverse, 0);
            ImageFloat r2 = r.duplicate();
            ImageHandler imgMask = img.thresholdAboveExclusive(threshold);
            if (maskImage != null) {
                imgMask = ImageHandler.wrap(maskImage);
            }
            if (inverse) { // do not invert if other image
                if (maskImage == null) {
                    imgMask = imgMask.duplicate();
                    imgMask.invert();
                }
            }
            ctProgress.log("Normalizing Distance Map (EVF) ...");
            // TEST
            r2 = (ImageFloat) EDT.normaliseLabel((ImageInt) img, r2);
            //EDT.normalizeDistanceMap(r2, imgMask, true);
            if (maskImage != null) {
                r2.intersectMask(imgMask);
            }
            return r2;
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
