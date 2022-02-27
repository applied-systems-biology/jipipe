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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.greyscale;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.GreyscaleColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

/**
 * 8-bit greyscale 5D image
 */
@JIPipeDocumentation(name = "5D image (8 bit)")
@JIPipeNode(menuPath = "Images\n5D\nGreyscale")
@JIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ByteProcessor.class, colorSpace = GreyscaleColorSpace.class, pixelType = Byte.class, bitDepth = 8, numDimensions = 5)
public class ImagePlus5DGreyscale8UData extends ImagePlus5DGreyscaleData {

    public ImagePlus5DGreyscale8UData(ImagePlus image) {
        super(ImageJUtils.convertToGreyscale8UIfNeeded(image));
    }

    public ImagePlus5DGreyscale8UData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGreyscale8UIfNeeded));
    }

    /**
     * Creates an empty image with given dimensions
     * @param dimensions the dimensions
     */
    public ImagePlus5DGreyscale8UData(ImageDimensions dimensions) {
        this(IJ.createHyperStack("Image",
                dimensions.getWidth(),
                dimensions.getHeight(),
                dimensions.getSizeC(),
                dimensions.getSizeZ(),
                dimensions.getSizeT(),
                8));
    }

    public ImagePlus5DGreyscale8UData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGreyscale8UIfNeeded(image), colorSpace);
    }

    public ImagePlus5DGreyscale8UData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGreyscale8UIfNeeded), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus5DGreyscale8UData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus5DGreyscale8UData(data.getImage());
        } else {
            return new ImagePlus5DGreyscale8UData(data.getImageSource());
        }
    }
}
