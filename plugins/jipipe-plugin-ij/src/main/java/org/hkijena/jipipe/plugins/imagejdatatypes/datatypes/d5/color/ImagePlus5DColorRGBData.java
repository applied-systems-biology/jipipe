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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d5.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageDimensions;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * RGB color 5D image
 */
@SetJIPipeDocumentation(name = "5D ImageJ image (RGB)", description = "A color image (RGB colors)")
@ConfigureJIPipeNode(menuPath = "Images\n5D\nColor")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ColorProcessor.class, colorSpace = RGBColorSpace.class, pixelType = Integer.class, bitDepth = 24, numDimensions = 5)
public class ImagePlus5DColorRGBData extends ImagePlus5DColorData {

    /**
     * @param image wrapped image
     */
    public ImagePlus5DColorRGBData(ImagePlus image) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image));
    }

    public ImagePlus5DColorRGBData(ImagePlus image, ColorSpace ignored) {
        super(ImageJUtils.convertToColorRGBIfNeeded(image));
    }

    /**
     * Creates an empty image with given dimensions
     *
     * @param dimensions the dimensions
     */
    public ImagePlus5DColorRGBData(ImageDimensions dimensions) {
        this(IJ.createHyperStack("Image",
                dimensions.getWidth(),
                dimensions.getHeight(),
                dimensions.getSizeC(),
                dimensions.getSizeZ(),
                dimensions.getSizeT(),
                24));
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus5DColorRGBData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus5DColorRGBData(ImagePlusColorRGBData.convertFrom(data).getImage());
    }

    @Override
    public ColorSpace getColorSpace() {
        return RGBColorSpace.INSTANCE;
    }
}
