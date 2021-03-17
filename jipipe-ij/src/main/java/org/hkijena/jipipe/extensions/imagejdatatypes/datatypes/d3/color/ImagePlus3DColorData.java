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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.*;
import java.nio.file.Path;

/**
 * A colored image without dimension.
 * It acts as base and intermediate type between colored images. The convertFrom(data) method copies the color space
 * Conversion works through {@link org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@JIPipeDocumentation(name = "3D Image (Color)")
@JIPipeOrganization(menuPath = "Images\n3D\nColor")
@JIPipeHeavyData
public class ImagePlus3DColorData extends ImagePlus3DData implements ColoredImagePlusData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DColorData(ImagePlus image) {
        super(ImagePlus3DColorData.convertIfNeeded(image));
    }

    public ImagePlus3DColorData(ImagePlus image, ColorSpace colorSpace) {
        super(ImagePlus3DColorData.convertIfNeeded(image), colorSpace);
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
        return new ImagePlus3DColorData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     * Copies the color space if provided with an {@link ColoredImagePlusData}
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        ImagePlus image = data.getImage();
        if (image.getType() != ImagePlus.COLOR_RGB) {
            // This will go through the standard method (greyscale -> RGB -> HSB)
            return new ImagePlus3DColorData(image);
        } else {
            return new ImagePlus3DColorData(image, ((ColoredImagePlusData) data).getColorSpace());
        }
    }

    @Override
    public Component preview(int width, int height) {
        return ImageJUtils.generatePreview(this.getImage(), getColorSpace(), width, height);
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getColorSpace() + " colors]";
    }
}
