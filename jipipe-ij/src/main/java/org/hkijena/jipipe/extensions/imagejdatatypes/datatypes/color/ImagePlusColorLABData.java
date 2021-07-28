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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.LABColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.awt.Component;
import java.nio.file.Path;

/**
 * LAB colored image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@JIPipeDocumentation(name = "Image (LAB)")
@JIPipeNode(menuPath = "Images\nColor")
@JIPipeHeavyData
public class ImagePlusColorLABData extends ImagePlusColorData implements ColoredImagePlusData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    /**
     * The color space of this image
     */
    public static final ColorSpace COLOR_SPACE = new LABColorSpace();

    /**
     * @param image wrapped image
     */
    public ImagePlusColorLABData(ImagePlus image) {
        super(ImageJUtils.convertToColorLABIfNeeded(image));
    }

    public ImagePlusColorLABData(ImageSource source) {
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
        return new ImagePlusColorLABData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            ImagePlus image = data.getImage();
            if (image.getType() != ImagePlus.COLOR_RGB) {
                // Standard method: Greyscale -> RGB
                return new ImagePlusColorLABData(data.getImage());
            } else if (data instanceof ColoredImagePlusData) {
                ImagePlus copy = data.getDuplicateImage();
                COLOR_SPACE.convert(copy, ((ColoredImagePlusData) data).getColorSpace(), new JIPipeProgressInfo());
                return new ImagePlusColorLABData(copy);
            } else {
                return new ImagePlusColorLABData(data.getImage());
            }
        } else {
            return new ImagePlusColorLABData(data.getImageSource());
        }
    }
}
