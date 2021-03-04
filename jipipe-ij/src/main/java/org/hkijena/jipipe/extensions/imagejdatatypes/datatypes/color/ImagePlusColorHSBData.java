/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.HSBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color.ImagePlus3DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.color.ImagePlus4DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color.ImagePlus5DColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * HSV colored image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@JIPipeDocumentation(name = "Image (HSB)")
@JIPipeOrganization(menuPath = "Images\nColor")
@JIPipeHeavyData
public class ImagePlusColorHSBData extends ImagePlusData implements ColoredImagePlusData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    /**
     * The color space of this image
     */
    public static final ColorSpace COLOR_SPACE = new HSBColorSpace();

    /**
     * @param image wrapped image
     */
    public ImagePlusColorHSBData(ImagePlus image) {
        super(ImagePlusColorHSBData.convertIfNeeded(image));
    }

    @Override
    public ColorSpace getColorSpace() {
        return COLOR_SPACE;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.COLOR_RGB) {
            // A copy is guaranteed here
            ImagePlus copy = ImagePlusColorRGBData.convertIfNeeded(image);
            ImageJUtils.convertRGBToHSB(copy, new JIPipeProgressInfo());
            return copy;
        }
        // ImageJ does not differentiate between color spaces, so we cannot convert. The convertFrom() method will handle this correctly.
        return image;
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlusColorHSBData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        ImagePlus image = data.getImage();
        if (image.getType() != ImagePlus.COLOR_RGB) {
            // This will go through the standard method (greyscale -> RGB -> HSB)
            return new ImagePlusColorHSBData(image);
        }
        else if(data instanceof ColoredImagePlusData) {
            ImagePlus copy = data.getDuplicateImage();
            COLOR_SPACE.convert(copy, ((ColoredImagePlusData) data).getColorSpace(), new JIPipeProgressInfo());
            return new ImagePlusColorHSBData(copy);
        }
        else {
            return new ImagePlusColorHSBData(image);
        }
    }

    @Override
    public Component preview(int width, int height) {
        return generatePreview(this.getImage(), width, height);
    }

    /**
     * Generates a preview for a HSB image
     * @param image the image
     * @param width the width
     * @param height the height
     * @return the preview
     */
    public static Component generatePreview(ImagePlus image, int width, int height) {
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        ImagePlus firstSliceImage= new ImagePlus("Preview", image.getProcessor().duplicate());
        ImageJUtils.convertHSBToRGB(firstSliceImage, new JIPipeProgressInfo());
        ImageProcessor resized = firstSliceImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        BufferedImage bufferedImage = resized.getBufferedImage();
        return new JLabel(new ImageIcon(bufferedImage));
    }
}
