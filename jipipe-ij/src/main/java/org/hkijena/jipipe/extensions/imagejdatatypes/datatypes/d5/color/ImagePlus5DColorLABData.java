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
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.LABColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.awt.Component;
import java.nio.file.Path;

/**
 * RGB color 5D image
 */
@JIPipeDocumentation(name = "5D image (LAB)")
@JIPipeNode(menuPath = "Images\n5D\nColor")
@JIPipeHeavyData
public class ImagePlus5DColorLABData extends ImagePlus5DColorData implements ColoredImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * The color space of this image
     */
    public static final ColorSpace COLOR_SPACE = new LABColorSpace();

    /**
     * @param image wrapped image
     */
    public ImagePlus5DColorLABData(ImagePlus image) {
        super(ImageJUtils.convertToColorLABIfNeeded(image));
    }

    public ImagePlus5DColorLABData(ImagePlus image, ColorSpace ignored) {
        super(ImageJUtils.convertToColorLABIfNeeded(image));
    }

    public ImagePlus5DColorLABData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToColorLABIfNeeded));
    }

    @Override
    public ColorSpace getColorSpace() {
        return COLOR_SPACE;
    }

    @Override
    public Component preview(int width, int height) {
        return ImageJUtils.generatePreview(this.getImage(), getColorSpace(), width, height);
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus5DColorLABData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus5DColorLABData(ImagePlusColorLABData.convertFrom(data).getImage());
        } else {
            return new ImagePlus5DColorLABData(data.getImageSource());
        }
    }
}
