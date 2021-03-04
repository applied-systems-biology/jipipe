package org.hkijena.jipipe.extensions.imagejdatatypes.color;

import ij.ImagePlus;
import ij.process.ColorSpaceConverter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

public class LABColorSpace implements ColorSpace{

    private static final ColorSpaceConverter CONVERTER = new ColorSpaceConverter();

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
    public int convert(int pixel, ColorSpace imgSpace) {
        if(imgSpace.getClass() != LABColorSpace.class) {
            pixel = imgSpace.convertToRGB(pixel);
            double[] lab = CONVERTER.RGBtoLAB(pixel);
            int l = (int)Math.max(0, Math.min(255, lab[0]));
            int a = (int)Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[1])) - (int)Byte.MIN_VALUE;
            int b = (int)Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[2])) - (int)Byte.MIN_VALUE;
            pixel = (l << 16) + (a << 8) + b;
        }
        return pixel;
    }

    @Override
    public int convertToRGB(int pixel) {
        double[] lab = new double[3];
        int l = (pixel&0xff0000)>>16;
        int a = ((pixel&0xff00)>>8) + Byte.MIN_VALUE;
        int b = (pixel&0xff) + Byte.MIN_VALUE;
        lab[0] = l;
        lab[1] = a;
        lab[2] = b;
        int[] rgb = CONVERTER.LABtoRGB(lab);
        return (rgb[0] << 16) + (rgb[1] << 8) + rgb[2];
    }

    @Override
    public String toString() {
        return "L*a*b*";
    }
}
