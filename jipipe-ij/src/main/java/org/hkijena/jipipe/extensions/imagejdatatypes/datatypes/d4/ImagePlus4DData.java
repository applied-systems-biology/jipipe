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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * 4D image
 */
@SetJIPipeDocumentation(name = "4D ImageJ image", description = "A 4D image")
@ConfigureJIPipeNode(menuPath = "Images\n4D")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(numDimensions = 4)
public class ImagePlus4DData extends ImagePlusData implements Image4DData {

    /**
     * @param image wrapped image
     */
    public ImagePlus4DData(ImagePlus image) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image));
        ImageJUtils.assert4DImage(getImage());
    }

    public ImagePlus4DData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convert3ChannelToRGBIfNeeded(image), colorSpace);
        ImageJUtils.assert4DImage(getImage());
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus4DData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus4DData(data.getImage(), data.getColorSpace());
    }
}
