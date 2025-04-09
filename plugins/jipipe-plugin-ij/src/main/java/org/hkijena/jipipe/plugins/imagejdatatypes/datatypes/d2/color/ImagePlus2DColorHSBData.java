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

package org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.HSBColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageDimensions;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * RGB colored 2D image
 */
@SetJIPipeDocumentation(name = "2D ImageJ image (HSB)", description = "A colored image (HSB colors)")
@ConfigureJIPipeNode(menuPath = "Images\n2D\nColor")
@LabelAsJIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ColorProcessor.class, colorSpace = HSBColorSpace.class, pixelType = Integer.class, bitDepth = 24, numDimensions = 2)
public class ImagePlus2DColorHSBData extends ImagePlus2DColorData {

    public ImagePlus2DColorHSBData(ImagePlus image) {
        super(ImageJUtils.convertToColorHSBIfNeeded(image));
    }

    public ImagePlus2DColorHSBData(ImagePlus image, ColorSpace ignored) {
        super(ImageJUtils.convertToColorHSBIfNeeded(image));
    }

    /**
     * Creates an empty image with given dimensions
     *
     * @param dimensions the dimensions
     */
    public ImagePlus2DColorHSBData(ImageDimensions dimensions) {
        this(IJ.createHyperStack("Image",
                dimensions.getWidth(),
                dimensions.getHeight(),
                dimensions.getSizeC(),
                dimensions.getSizeZ(),
                dimensions.getSizeT(),
                24));
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus2DColorHSBData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus2DColorHSBData(ImagePlusColorHSBData.convertFrom(data).getImage());
    }

    @Override
    public ColorSpace getColorSpace() {
        return HSBColorSpace.INSTANCE;
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return ImageJUtils.generateThumbnail(this.getImage(), getColorSpace(), width, height);
    }
}
