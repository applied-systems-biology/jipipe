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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.GreyscaleColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.GreyscaleImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

/**
 * Greyscale 2D image
 */
@JIPipeDocumentation(name = "2D image (greyscale)")
@JIPipeNode(menuPath = "Images\n2D\nGreyscale")
@JIPipeHeavyData
@ImageTypeInfo(imageProcessorType = FloatProcessor.class, colorSpace = GreyscaleColorSpace.class, pixelType = Float.class, bitDepth = 32, numDimensions = 2)
public class ImagePlus2DGreyscaleData extends ImagePlus2DData implements GreyscaleImageData {

    public ImagePlus2DGreyscaleData(ImagePlus image) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image));
    }

    public ImagePlus2DGreyscaleData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGreyscaleIfNeeded));
    }

    public ImagePlus2DGreyscaleData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image), colorSpace);
    }

    public ImagePlus2DGreyscaleData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGreyscaleIfNeeded), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus2DGreyscaleData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    @Override
    public ColorSpace getColorSpace() {
        return GreyscaleColorSpace.INSTANCE;
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus2DGreyscaleData(data.getImage());
        } else {
            return new ImagePlus2DGreyscaleData(data.getImageSource());
        }
    }
}
