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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d5;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AsserterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

/**
 * 5D image
 */
@JIPipeDocumentation(name = "5D image")
@JIPipeNode(menuPath = "Images\n5D")
@JIPipeHeavyData
@ImageTypeInfo(numDimensions = 5)
public class ImagePlus5DData extends ImagePlusData {

    /**
     * @param image wrapped image
     */
    public ImagePlus5DData(ImagePlus image) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image));
        ImageJUtils.assert5DImage(getImage());
    }

    public ImagePlus5DData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image), colorSpace);
        ImageJUtils.assert5DImage(getImage());
    }

    public ImagePlus5DData(ImageSource source) {
        super(new AsserterWrapperImageSource(new ConverterWrapperImageSource(source, ImageJUtils::convert3ChannelToRGBIfNeeded), ImageJUtils::assert5DImage));
    }

    public ImagePlus5DData(ImageSource source, ColorSpace colorSpace) {
        super(new AsserterWrapperImageSource(new ConverterWrapperImageSource(source, ImageJUtils::convert3ChannelToRGBIfNeeded), ImageJUtils::assert5DImage), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus5DData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus5DData(data.getImage(), data.getColorSpace());
        } else {
            return new ImagePlus5DData(data.getImageSource(), data.getColorSpace());
        }
    }
}
