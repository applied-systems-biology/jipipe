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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.awt.Component;
import java.nio.file.Path;

/**
 * A colored image without dimension.
 * It acts as base and intermediate type between colored images. The convertFrom(data) method copies the color space
 * Conversion works through {@link org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@JIPipeDocumentation(name = "5D Image (Color)")
@JIPipeNode(menuPath = "Images\n5D\nColor")
@JIPipeHeavyData
public class ImagePlus5DColorData extends ImagePlus5DData implements ColoredImagePlusData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * @param image wrapped image
     */
    public ImagePlus5DColorData(ImagePlus image) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image));
    }

    public ImagePlus5DColorData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image), colorSpace);
    }

    public ImagePlus5DColorData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToColorRGBIfNeeded));
    }

    public ImagePlus5DColorData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToColorRGBIfNeeded), colorSpace);
    }

    @Override
    public Component preview(int width, int height) {
        return ImageJUtils.generatePreview(this.getImage(), getColorSpace(), width, height);
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getColorSpace() + " colors]";
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus5DColorData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     * Copies the color space if provided with an {@link ColoredImagePlusData}
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            ImagePlus image = data.getImage();
            if (image.getType() != ImagePlus.COLOR_RGB) {
                // This will go through the standard method (greyscale -> RGB -> HSB)
                return new ImagePlus5DColorData(image);
            } else {
                return new ImagePlus5DColorData(image, ((ColoredImagePlusData) data).getColorSpace());
            }
        } else {
            return new ImagePlus5DColorData(data.getImageSource(), data.getColorSpace());
        }
    }
}
