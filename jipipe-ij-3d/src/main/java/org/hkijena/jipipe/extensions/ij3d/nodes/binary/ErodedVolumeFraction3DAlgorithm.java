package org.hkijena.jipipe.extensions.ij3d.nodes.binary;

import ij.ImagePlus;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.distanceMap3d.EDT;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Eroded Volume Fraction 3D", description = "The EVF (Eroded Volume Fraction) can be regarded as a normalized EDT (Euclidean Distance Map). " +
        "The calculated distances will be in the calibrated unit.")
@JIPipeCitation("Read about EVF: https://onlinelibrary.wiley.com/doi/full/10.1002/jcb.21823")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nDistance map")
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", description = "Mask for the EVF", optional = true, autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
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

    @JIPipeDocumentation(name = "Threshold", description = "Threshold value for the mask")
    @JIPipeParameter("threshold")
    public int getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @JIPipeDocumentation(name = "Inverse", description = "If enabled, the threshold is applied on the inverse iamge")
    @JIPipeParameter("inverse")
    public boolean isInverse() {
        return inverse;
    }

    @JIPipeParameter("inverse")
    public void setInverse(boolean inverse) {
        this.inverse = inverse;
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData("Input", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImagePlus maskImage = ImageJUtils.unwrap(dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo));
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
