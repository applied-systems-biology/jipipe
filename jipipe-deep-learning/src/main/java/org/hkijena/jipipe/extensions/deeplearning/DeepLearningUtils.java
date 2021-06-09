package org.hkijena.jipipe.extensions.deeplearning;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.deeplearning.configs.DeepLearningModelConfiguration;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.TransformScale2DAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.roi.OptionalIntModificationParameter;

public class DeepLearningUtils {

    private DeepLearningUtils() {

    }

    /**
     * Scales the input image to the model parameters if it does not fit
     * @param image the image
     * @param modelConfiguration the model configuration
     * @param scale2DAlgorithm the scaling algorithm
     * @return scaled image
     */
    public static ImagePlus scaleToModel(ImagePlus image, DeepLearningModelConfiguration modelConfiguration, TransformScale2DAlgorithm scale2DAlgorithm, JIPipeProgressInfo progressInfo) {
        if(image.getWidth() != modelConfiguration.getImageWidth() || image.getHeight() != modelConfiguration.getImageHeight()) {
            // Apply 2D scaling
            scale2DAlgorithm.clearSlotData();
            scale2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(image), progressInfo);
            scale2DAlgorithm.getxAxis().getContent().setExpression("" + modelConfiguration.getImageWidth());
            scale2DAlgorithm.getyAxis().getContent().setExpression("" + modelConfiguration.getImageHeight());
            scale2DAlgorithm.run(progressInfo);
            image = scale2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
        }
        if(modelConfiguration.getImageDimensions() < image.getNDimensions()) {
            throw new RuntimeException("Image has too many dimensions: Model only accepts " + modelConfiguration.getImageDimensions() + ", while " +
                    "image has " + image.getNDimensions() + " dimensions");
        }
        // TODO: Handle dimensions
        return image;
    }
}
