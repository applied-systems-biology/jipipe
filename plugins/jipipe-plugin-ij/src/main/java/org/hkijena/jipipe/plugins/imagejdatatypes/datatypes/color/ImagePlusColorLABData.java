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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.LABColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.converters.ImplicitImageTypeConverter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageDimensions;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * LAB colored image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link ImplicitImageTypeConverter}
 */
@SetJIPipeDocumentation(name = "ImageJ Image (LAB)", description = "A colored image (LAB colors)")
@ConfigureJIPipeNode(menuPath = "Images\nColor")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ColorProcessor.class, colorSpace = LABColorSpace.class, pixelType = Integer.class, bitDepth = 24)
public class ImagePlusColorLABData extends ImagePlusColorData {

    public ImagePlusColorLABData(ImagePlus image) {
        super(ImageJUtils.convertToColorLABIfNeeded(image));
    }

    public ImagePlusColorLABData(ImagePlus image, ColorSpace ignored) {
        super(ImageJUtils.convertToColorLABIfNeeded(image));
    }

    /**
     * Creates an empty image with given dimensions
     *
     * @param dimensions the dimensions
     */
    public ImagePlusColorLABData(ImageDimensions dimensions) {
        this(IJ.createHyperStack("Image",
                dimensions.getWidth(),
                dimensions.getHeight(),
                dimensions.getSizeC(),
                dimensions.getSizeZ(),
                dimensions.getSizeT(),
                24));
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
        ImagePlus image = data.getImage();
        if (image.getType() != ImagePlus.COLOR_RGB) {
            // Standard method: Greyscale -> RGB
            return new ImagePlusColorLABData(data.getImage());
        } else {
            if (data.getColorSpace() instanceof LABColorSpace) {
                // No conversion needed
                return new ImagePlusColorLABData(data.getImage());
            } else {
                ImagePlus copy = data.getDuplicateImage();
                LABColorSpace.INSTANCE.convert(copy, data.getColorSpace(), new JIPipeProgressInfo());
                return new ImagePlusColorLABData(copy);
            }
        }
    }

    @Override
    public ColorSpace getColorSpace() {
        return LABColorSpace.INSTANCE;
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return ImageJUtils.generateThumbnail(this.getImage(), getColorSpace(), width, height);
    }
}
