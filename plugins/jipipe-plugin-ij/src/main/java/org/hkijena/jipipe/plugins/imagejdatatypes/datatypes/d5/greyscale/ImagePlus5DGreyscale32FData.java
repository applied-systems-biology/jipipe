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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d5.greyscale;

import ij.IJ;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * 32-bit floating point greyscale image
 */
@SetJIPipeDocumentation(name = "5D ImageJ image (float)", description = "A 32-bit greyscale image")
@ConfigureJIPipeNode(menuPath = "Images\n5D\nGreyscale")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = FloatProcessor.class, colorSpace = GreyscaleColorSpace.class, pixelType = Float.class, bitDepth = 32, numDimensions = 5)
public class ImagePlus5DGreyscale32FData extends ImagePlus5DGreyscaleData {

    public ImagePlus5DGreyscale32FData(ImagePlus image) {
        super(ImageJUtils.convertToGrayscale32FIfNeeded(image));
    }

    /**
     * Creates an empty image with given dimensions
     *
     * @param dimensions the dimensions
     */
    public ImagePlus5DGreyscale32FData(ImageDimensions dimensions) {
        this(IJ.createHyperStack("Image",
                dimensions.getWidth(),
                dimensions.getHeight(),
                dimensions.getSizeC(),
                dimensions.getSizeZ(),
                dimensions.getSizeT(),
                32));
    }

    public ImagePlus5DGreyscale32FData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGrayscale32FIfNeeded(image), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus5DGreyscale32FData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus5DGreyscale32FData(data.getImage());
    }
}
