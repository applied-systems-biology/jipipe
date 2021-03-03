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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

import java.awt.*;
import java.nio.file.Path;

/**
 * RGB colored 2D image
 */
@JIPipeDocumentation(name = "2D image (HSB)")
@JIPipeOrganization(menuPath = "Images\n2D\nColor")
@JIPipeHeavyData
public class ImagePlus2DColorHSBData extends ImagePlus2DColorData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * @param image wrapped image
     */
    public ImagePlus2DColorHSBData(ImagePlus image) {
        super(ImagePlusColorHSBData.convertIfNeeded(image));
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus2DColorHSBData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    @Override
    public Component preview(int width, int height) {
        return ImagePlusColorHSBData.generatePreview(this.getImage(), width, height);
    }

    /**
     * Converts the incoming image data into the current format.
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus2DColorHSBData(ImagePlusColorHSBData.convertFrom(data).getImage());
    }
}
