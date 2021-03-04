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
import ij.process.ImageConverter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.nio.file.Path;

/**
 * RGB colored image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@JIPipeDocumentation(name = "Image (RGB)")
@JIPipeOrganization(menuPath = "Images\nColor")
@JIPipeHeavyData
public class ImagePlusColorRGBData extends ImagePlusColorData implements ColoredImagePlusData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    /**
     * The color space of this image
     */
    public static final ColorSpace COLOR_SPACE = new RGBColorSpace();

    /**
     * @param image wrapped image
     */
    public ImagePlusColorRGBData(ImagePlus image) {
        super(ImagePlusColorRGBData.convertIfNeeded(image));
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
            String title = image.getTitle();
            image = image.duplicate();
            image.setTitle(title);
            ImageConverter.setDoScaling(true);
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
        }
        return image;
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlusColorRGBData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        ImagePlus image = data.getImage();
        if (image.getType() != ImagePlus.COLOR_RGB) {
            // Standard method: Greyscale -> RGB
            return new ImagePlusColorRGBData(data.getImage());
        } else if (data instanceof ColoredImagePlusData) {
            ImagePlus copy = data.getDuplicateImage();
            COLOR_SPACE.convert(copy, ((ColoredImagePlusData) data).getColorSpace(), new JIPipeProgressInfo());
            return new ImagePlusColorRGBData(copy);
        } else {
            return new ImagePlusColorRGBData(data.getImage());
        }
    }
}
