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

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.nio.file.Path;

/**
 * 16-bit greyscale 5D image
 */
@JIPipeDocumentation(name = "5D image (16 bit)")
@JIPipeNode(menuPath = "Images\n5D\nGreyscale")
@JIPipeHeavyData
public class ImagePlus5DGreyscale16UData extends ImagePlus5DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    public ImagePlus5DGreyscale16UData(ImagePlus image) {
        super(ImageJUtils.convertToGrayscale16UIfNeeded(image));
    }

    public ImagePlus5DGreyscale16UData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGrayscale16UIfNeeded));
    }

    public ImagePlus5DGreyscale16UData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGrayscale16UIfNeeded(image), colorSpace);
    }

    public ImagePlus5DGreyscale16UData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGrayscale16UIfNeeded), colorSpace);
    }

    public static ImagePlusData importFrom(Path storageFolder, JIPipeProgressInfo progressInfo) {
        return new ImagePlus5DGreyscale16UData(ImagePlusData.importImagePlusFrom(storageFolder, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus5DGreyscale16UData(data.getImage());
        } else {
            return new ImagePlus5DGreyscale16UData(data.getImageSource());
        }
    }
}
