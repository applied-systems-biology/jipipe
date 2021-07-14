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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.nio.file.Path;

/**
 * 32-bit floating point greyscale 4D image
 */
@JIPipeDocumentation(name = "4D image (float)")
@JIPipeOrganization(menuPath = "Images\n4D\nGreyscale")
@JIPipeHeavyData
public class ImagePlus4DGreyscale32FData extends ImagePlus4DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    public ImagePlus4DGreyscale32FData(ImagePlus image) {
        super(ImageJUtils.convertToGrayscale32FIfNeeded(image));
    }

    public ImagePlus4DGreyscale32FData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGrayscale32FIfNeeded));
    }

    public ImagePlus4DGreyscale32FData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGrayscale32FIfNeeded(image), colorSpace);
    }

    public ImagePlus4DGreyscale32FData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGrayscale32FIfNeeded), colorSpace);
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus4DGreyscale32FData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus4DGreyscale32FData(data.getImage());
        } else {
            return new ImagePlus4DGreyscale32FData(data.getImageSource());
        }
    }
}
