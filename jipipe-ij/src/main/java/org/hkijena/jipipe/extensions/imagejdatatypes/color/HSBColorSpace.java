package org.hkijena.jipipe.extensions.imagejdatatypes.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

public class HSBColorSpace implements ColorSpace{
    @Override
    public void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ImageJUtils.convertHSBToRGB(img, progressInfo);
    }

    @Override
    public void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo) {
        if(imgSpace.getClass() != HSBColorSpace.class) {
            imgSpace.convertToRGB(img, progressInfo);
            ImageJUtils.convertRGBToHSB(img, progressInfo);
        }
    }

    @Override
    public String toString() {
        return "HSB";
    }
}
