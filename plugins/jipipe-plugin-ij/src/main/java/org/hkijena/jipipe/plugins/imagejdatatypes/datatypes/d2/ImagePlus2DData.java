/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * 2D image
 */
@SetJIPipeDocumentation(name = "2D ImageJ image", description = "A 2D image")
@ConfigureJIPipeNode(menuPath = "Images\n2D")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(numDimensions = 2)
public class ImagePlus2DData extends ImagePlusData implements Image2DData {

    /**
     * @param image wrapped image
     */
    public ImagePlus2DData(ImagePlus image) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image));
        ImageJUtils.assert2DImage(getImage());
    }

    public ImagePlus2DData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image), colorSpace);
        ImageJUtils.assert2DImage(getImage());
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus2DData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus2DData(data.getImage(), data.getColorSpace());
    }
}
