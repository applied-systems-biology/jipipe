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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.HSBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.RGBColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ConverterWrapperImageSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;

import java.awt.*;

/**
 * RGB color 3D image
 */
@JIPipeDocumentation(name = "3D image (HSB)")
@JIPipeNode(menuPath = "Images\n3D\nColor")
@JIPipeHeavyData
@ImageTypeInfo(imageProcessorType = ColorProcessor.class, colorSpace = HSBColorSpace.class, pixelType = Integer.class, bitDepth = 24, numDimensions = 3)
public class ImagePlus3DColorHSBData extends ImagePlus3DColorData  {

    /**
     * @param image wrapped image
     */
    public ImagePlus3DColorHSBData(ImagePlus image) {
        super(ImageJUtils.convertToColorHSBIfNeeded(image));
    }

    public ImagePlus3DColorHSBData(ImagePlus image, ColorSpace ignored) {
        super(ImageJUtils.convertToColorHSBIfNeeded(image));
    }

    public ImagePlus3DColorHSBData(ImageSource source) {
        super(new ConverterWrapperImageSource(source, ImageJUtils::convertToColorHSBIfNeeded));
    }

    public static ImagePlusData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new ImagePlus3DColorHSBData(ImagePlusData.importImagePlusFrom(storage, progressInfo));
    }

    /**
     * Converts the incoming image data into the current format.
     *
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        if (data.hasLoadedImage()) {
            return new ImagePlus3DColorHSBData(ImagePlusColorHSBData.convertFrom(data).getImage());
        } else {
            return new ImagePlus3DColorHSBData(data.getImageSource());
        }
    }

    @Override
    public ColorSpace getColorSpace() {
        return HSBColorSpace.INSTANCE;
    }

    @Override
    public Component preview(int width, int height) {
        return ImageJUtils.generatePreview(this.getImage(), getColorSpace(), width, height);
    }
}
