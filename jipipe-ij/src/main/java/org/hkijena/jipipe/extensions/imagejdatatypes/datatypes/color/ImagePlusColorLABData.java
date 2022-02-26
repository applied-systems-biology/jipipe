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
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.LABColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.awt.*;

/**
 * LAB colored image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@JIPipeDocumentation(name = "Image (LAB)")
@JIPipeNode(menuPath = "Images\nColor")
@JIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ColorProcessor.class, colorSpace = LABColorSpace.class, pixelType = Integer.class, bitDepth = 24)
public class ImagePlusColorLABData extends ImagePlusColorData  {

    public ImagePlusColorLABData(ImagePlus image) {
        super(ImageJUtils.convertToColorLABIfNeeded(image));
    }

    public ImagePlusColorLABData(ImagePlus image, ColorSpace ignored) {
        super(ImageJUtils.convertToColorLABIfNeeded(image));
    }

    public ImagePlusColorLABData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToColorLABIfNeeded));
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlusColorLABData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
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
                // Standard method: Greyscale -> RGB
                return new ImagePlusColorLABData(data.getImage());
            } else {
                if(data.getColorSpace() instanceof LABColorSpace) {
                    // No conversion needed
                    return new ImagePlusColorLABData(data.getImage());
                }
                else {
                    ImagePlus copy = data.getDuplicateImage();
                    LABColorSpace.INSTANCE.convert(copy, data.getColorSpace(), new JIPipeProgressInfo());
                    return new ImagePlusColorLABData(copy);
                }
            }
        } else {
            return new ImagePlusColorLABData(data.getImageSource());
        }
    }

    @Override
    public ColorSpace getColorSpace() {
        return LABColorSpace.INSTANCE;
    }

    @Override
    public Component preview(int width, int height) {
        return ImageJUtils.generatePreview(this.getImage(), getColorSpace(), width, height);
    }
}
