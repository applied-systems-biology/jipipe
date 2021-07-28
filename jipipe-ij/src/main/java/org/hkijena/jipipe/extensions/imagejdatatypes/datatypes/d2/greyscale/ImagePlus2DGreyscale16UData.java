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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.nio.file.Path;

/**
 * 16-bit greyscale 2D image
 */
@JIPipeDocumentation(name = "2D image (16 bit)")
@JIPipeNode(menuPath = "Images\n2D\nGreyscale")
@JIPipeHeavyData
public class ImagePlus2DGreyscale16UData extends ImagePlus2DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    public ImagePlus2DGreyscale16UData(ImagePlus image) {
        super(ImageJUtils.convertToGrayscale16UIfNeeded(image));
    }

    public ImagePlus2DGreyscale16UData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGrayscale16UIfNeeded));
    }

    public ImagePlus2DGreyscale16UData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGrayscale16UIfNeeded(image), colorSpace);
    }

    public ImagePlus2DGreyscale16UData(ImageSource source, ColorSpace colorSpace) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToGrayscale16UIfNeeded), colorSpace);
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus2DGreyscale16UData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus2DGreyscale16UData(data.getImage());
        } else {
            return new ImagePlus2DGreyscale16UData(data.getImageSource());
        }
    }
}
