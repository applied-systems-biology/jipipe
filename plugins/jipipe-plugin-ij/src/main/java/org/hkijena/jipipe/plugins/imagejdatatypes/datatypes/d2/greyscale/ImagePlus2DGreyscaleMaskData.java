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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.GreyscaleColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageDimensions;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * 8-bit mask 2D image
 */
@SetJIPipeDocumentation(name = "2D ImageJ image (mask)", description = "An 8-bit binary image")
@ConfigureJIPipeNode(menuPath = "Images\n2D\nGreyscale")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ByteProcessor.class, colorSpace = GreyscaleColorSpace.class, pixelType = Byte.class, bitDepth = 8, numDimensions = 2)
public class ImagePlus2DGreyscaleMaskData extends ImagePlus2DGreyscale8UData {

    public ImagePlus2DGreyscaleMaskData(ImagePlus image) {
        super(ImageJUtils.convertToGreyscale8UIfNeeded(image));
    }

    /**
     * Creates an empty image with given dimensions
     *
     * @param dimensions the dimensions
     */
    public ImagePlus2DGreyscaleMaskData(ImageDimensions dimensions) {
        this(IJ.createHyperStack("Image",
                dimensions.getWidth(),
                dimensions.getHeight(),
                dimensions.getSizeC(),
                dimensions.getSizeZ(),
                dimensions.getSizeT(),
                8));
    }

    public ImagePlus2DGreyscaleMaskData(ImagePlus image, ColorSpace colorSpace) {
        super(ImageJUtils.convertToGreyscale8UIfNeeded(image), colorSpace);
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus2DGreyscaleMaskData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus2DGreyscaleMaskData(data.getImage());
    }
}
