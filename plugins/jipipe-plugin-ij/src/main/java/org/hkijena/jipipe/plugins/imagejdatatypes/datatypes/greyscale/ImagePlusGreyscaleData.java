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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.GreyscaleColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * Greyscale image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@SetJIPipeDocumentation(name = "ImageJ Image (greyscale)", description = "A greyscale image")
@ConfigureJIPipeNode(menuPath = "Images\nGreyscale")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = FloatProcessor.class, colorSpace = GreyscaleColorSpace.class, pixelType = Float.class, bitDepth = 32)
public class ImagePlusGreyscaleData extends ImagePlusData implements GreyscaleImageData {

    public ImagePlusGreyscaleData(ImagePlus image) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image));
    }

    public ImagePlusGreyscaleData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGreyscaleIfNeeded(image), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlusGreyscaleData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlusGreyscaleData(data.getImage());
    }

    @Override
    public ColorSpace getColorSpace() {
        return GreyscaleColorSpace.INSTANCE;
    }
}
