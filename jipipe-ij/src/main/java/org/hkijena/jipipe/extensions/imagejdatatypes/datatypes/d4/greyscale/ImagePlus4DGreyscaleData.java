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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.GreyscaleColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.GreyscaleImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

/**
 * Greyscale 4D image
 */
@JIPipeDocumentation(name = "4D image (greyscale)")
@JIPipeNode(menuPath = "Images\n4D\nGreyscale")
@JIPipeHeavyData
@ImageTypeInfo(imageProcessorType = FloatProcessor.class, colorSpace = GreyscaleColorSpace.class, pixelType = Float.class, bitDepth = 32, numDimensions = 4)
public class ImagePlus4DGreyscaleData extends ImagePlus4DData implements GreyscaleImageData {

    public ImagePlus4DGreyscaleData(ImagePlus image) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image));
    }

    public ImagePlus4DGreyscaleData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGreyscaleIfNeeded));
    }

    public ImagePlus4DGreyscaleData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image), colorSpace);
    }

    public ImagePlus4DGreyscaleData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGreyscaleIfNeeded), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus4DGreyscaleData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
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
            return new ImagePlus4DGreyscaleData(data.getImage());
        } else {
            return new ImagePlus4DGreyscaleData(data.getImageSource());
        }
    }
}
