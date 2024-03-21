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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.GreyscaleColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.GreyscaleImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Greyscale 4D image
 */
@SetJIPipeDocumentation(name = "4D ImageJ image (greyscale)", description = "A greyscale image")
@ConfigureJIPipeNode(menuPath = "Images\n4D\nGreyscale")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = FloatProcessor.class, colorSpace = GreyscaleColorSpace.class, pixelType = Float.class, bitDepth = 32, numDimensions = 4)
public class ImagePlus4DGreyscaleData extends ImagePlus4DData implements GreyscaleImageData {

    public ImagePlus4DGreyscaleData(ImagePlus image) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image));
    }

    public ImagePlus4DGreyscaleData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus4DGreyscaleData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus4DGreyscaleData(data.getImage());
    }

    @Override
    public ColorSpace getColorSpace() {
        return GreyscaleColorSpace.INSTANCE;
    }
}
