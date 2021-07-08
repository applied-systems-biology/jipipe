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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AsserterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.nio.file.Path;

/**
 * 3D image
 */
@JIPipeDocumentation(name = "3D image")
@JIPipeOrganization(menuPath = "Images\n3D")
@JIPipeHeavyData
public class ImagePlus3DData extends ImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DData(ImagePlus image) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image));
        ImageJUtils.assert3DImage(image);
    }

    public ImagePlus3DData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image), colorSpace);
        ImageJUtils.assert3DImage(image);
    }

    public ImagePlus3DData(ImageSource source) {
        super(new AsserterWrapperImageSource(new ConverterWrapperImageSource(source, ImageJUtils::convert3ChannelToRGBIfNeeded), ImageJUtils::assert3DImage));
    }

    public ImagePlus3DData(ImageSource source, ColorSpace colorSpace) {
        super(new AsserterWrapperImageSource(new ConverterWrapperImageSource(source, ImageJUtils::convert3ChannelToRGBIfNeeded), ImageJUtils::assert3DImage), colorSpace);
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus3DData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus3DData(data.getImage(), data.getColorSpace());
        } else {
            return new ImagePlus3DData(data.getImageSource());
        }
    }
}
