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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.converters.ImplicitImageTypeConverter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ColorImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d5.ImagePlus5DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * A colored image without dimension.
 * It acts as base and intermediate type between colored images. The convertFrom(data) method copies the color space
 * Conversion works through {@link ImplicitImageTypeConverter}
 */
@SetJIPipeDocumentation(name = "5D ImageJ image (Color)", description = "A color image")
@ConfigureJIPipeNode(menuPath = "Images\n5D\nColor")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ColorProcessor.class, colorSpace = RGBColorSpace.class, pixelType = Integer.class, bitDepth = 24, numDimensions = 5)
public class ImagePlus5DColorData extends ImagePlus5DData implements ColorImageData {

    /**
     * @param image wrapped image
     */
    public ImagePlus5DColorData(ImagePlus image) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image));
    }

    public ImagePlus5DColorData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus5DColorData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
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
            // This will go through the standard method (greyscale -> RGB -> HSB)
            return new ImagePlus5DColorData(image);
        } else {
            return new ImagePlus5DColorData(image, data.getColorSpace());
        }
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return ImageJUtils.generateThumbnail(this.getImage(), getColorSpace(), width, height);
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getColorSpace() + " colors]";
    }
}
