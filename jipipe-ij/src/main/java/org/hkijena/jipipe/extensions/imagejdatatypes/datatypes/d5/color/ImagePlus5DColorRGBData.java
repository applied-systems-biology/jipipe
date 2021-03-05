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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

import java.nio.file.Path;

/**
 * RGB color 5D image
 */
@JIPipeDocumentation(name = "5D image (RGB)")
@JIPipeOrganization(menuPath = "Images\n5D\nColor")
@JIPipeHeavyData
public class ImagePlus5DColorRGBData extends ImagePlus5DColorData implements ColoredImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * The color space of this image
     */
    public static final ColorSpace COLOR_SPACE = new RGBColorSpace();

    /**
     * @param image wrapped image
     */
    public ImagePlus5DColorRGBData(ImagePlus image) {
        super(ImagePlusColorRGBData.convertIfNeeded(image));
    }

    @Override
    public ColorSpace getColorSpace() {
        return COLOR_SPACE;
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus5DColorRGBData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus5DColorRGBData(ImagePlusColorRGBData.convertFrom(data).getImage());
    }
}
