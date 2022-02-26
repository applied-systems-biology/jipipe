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
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.awt.*;

/**
 * A colored image without dimension.
 * It acts as base and intermediate type between colored images. The convertFrom(data) method copies the color space
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@JIPipeDocumentation(name = "Image (Color)")
@JIPipeNode(menuPath = "Images\nColor")
@JIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ColorProcessor.class, colorSpace = RGBColorSpace.class, pixelType = Integer.class, bitDepth = 24)
public class ImagePlusColorData extends ImagePlusData implements ColorImageData {

    /**
     * @param image wrapped image
     */
    public ImagePlusColorData(ImagePlus image) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image));
    }

    public ImagePlusColorData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image), colorSpace);
    }

    public ImagePlusColorData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToColorRGBIfNeeded));
    }

    public ImagePlusColorData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToColorRGBIfNeeded), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlusColorData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            ImagePlus image = data.getImage();
            if (image.getType() != ImagePlus.COLOR_RGB) {
                // This will go through the standard method (greyscale -> RGB -> HSB)
                return new ImagePlusColorData(image);
            } else {
                return new ImagePlusColorData(image, data.getColorSpace());
            }
        } else {
            return new ImagePlusColorData(data.getImageSource(), data.getColorSpace());
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
