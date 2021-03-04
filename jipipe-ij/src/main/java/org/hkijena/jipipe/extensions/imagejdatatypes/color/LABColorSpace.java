package org.hkijena.jipipe.extensions.imagejdatatypes.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

public class LABColorSpace implements ColorSpace{
    @Override
    public void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ImageJUtils.convertLABToRGB(img, progressInfo);
    }

    @Override
    public void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo) {
        if(imgSpace.getClass() != LABColorSpace.class) {
            imgSpace.convertToRGB(img, progressInfo);
            ImageJUtils.convertRGBToLAB(img, progressInfo);
        }
    }

    @Override
    public String toString() {
        return "L*a*b*";
    }
}
