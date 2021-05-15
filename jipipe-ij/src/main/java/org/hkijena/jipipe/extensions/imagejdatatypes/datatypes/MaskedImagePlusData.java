package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.exceptions.UserFriendlyNullPointerException;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesSettings;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * A compound image data type that contains both an image and a mask
 */
@JIPipeDocumentation(name = "Masked image", description = "An image associated with a mask")
public class MaskedImagePlusData extends ImagePlusData {

    private ImagePlus mask;

    public MaskedImagePlusData(ImagePlus image, ImagePlus mask) {
        super(image);
        this.mask = ImagePlusGreyscaleMaskData.convertIfNeeded(mask);
        if(image.getWidth() != mask.getWidth() || image.getHeight() != mask.getHeight()) {
            throw new RuntimeException("The image and mask do not have the same size!");
        }
    }

    public MaskedImagePlusData(ImagePlus image, ImagePlus mask, ColorSpace colorSpace) {
        super(image, colorSpace);
        this.mask = ImagePlusGreyscaleMaskData.convertIfNeeded(mask);
        if(image.getWidth() != mask.getWidth() || image.getHeight() != mask.getHeight()) {
            throw new RuntimeException("The image and mask do not have the same size!");
        }
    }

    @Override
    public void makeUnique() {
        super.makeUnique();
        mask = getDuplicateMask();
    }

    /**
     * Returns a duplicate of the contained mask
     *
     * @return the duplicate
     */
    public ImagePlus getDuplicateMask() {
        ImagePlus imp = mask.duplicate();
        imp.setTitle(getMask().getTitle());
        return imp;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        String imageName = forceName ? name + "_image" : "image";
        String maskName = forceName ? name + "_mask" : "mask";

        // Save the image
        super.saveTo(storageFilePath, imageName, forceName, progressInfo);

        // Save the mask
        if (ImageJDataTypesSettings.getInstance().isUseBioFormats() && !(mask.getType() == ImagePlus.COLOR_RGB && ImageJDataTypesSettings.getInstance().isSaveRGBWithImageJ())) {
            Path outputPath = storageFilePath.resolve(maskName + ".ome.tif");
            OMEImageData.simpleOMEExport(mask, outputPath);
        } else {
            Path outputPath = storageFilePath.resolve(maskName + ".tif");
            IJ.saveAsTiff(getMask(), outputPath.toString());
        }
    }

    @Override
    public JIPipeData duplicate() {
        return new MaskedImagePlusData(getImage().duplicate(), getMask().duplicate(), getColorSpace());
    }

    @Override
    public Component preview(int width, int height) {
        double factorX = 1.0 * width / getImage().getWidth();
        double factorY = 1.0 * height / getImage().getHeight();
        double factor = Math.max(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) (getImage().getWidth() * factor);
        int imageHeight = (int) (getImage().getHeight() * factor);
        ImagePlus rgbImage = ImageJUtils.channelsToRGB(getImage());
        ImageProcessor resizedImage = rgbImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        ImageProcessor resizedMask = getMask().getProcessor().resize(imageWidth, imageHeight, smooth);
        int[] imagePixels = (int[])resizedImage.getPixels();
        byte[] maskPixels = (byte[]) resizedMask.getPixels();
        for (int i = 0; i < imagePixels.length; i++) {
            if(maskPixels[i] == 0) {
                int as_rgb = imagePixels[i];
                int r = (as_rgb & 0xff0000) >> 16;
                int g = ((as_rgb & 0xff00) >> 8);
                int b = (as_rgb & 0xff);
                r /= 2;
                g /= 2;
                b /= 2;
                imagePixels[i] = (r << 16) + (g << 8) + b;
            }
        }
        BufferedImage bufferedImage = resizedImage.getBufferedImage();
        return new JLabel(new ImageIcon(bufferedImage));
    }

    public ImagePlus getMask() {
        return mask;
    }

    public static MaskedImagePlusData importFrom(Path storageFilePath) {
        List<Path> files = PathUtils.findFilesByExtensionIn(storageFilePath, ".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp");
        Path imageFile = null;
        Path maskFile = null;
        for (Path file : files) {
            String name = file.getFileName().toString();
            if(name.contains("mask"))
                maskFile = file;
            if(name.contains("image"))
                imageFile = file;
        }
        if(imageFile == null || maskFile == null) {
            throw new UserFriendlyNullPointerException("Could not find a compatible image file in '" + storageFilePath + "'!",
                    "Unable to find file in location '" + storageFilePath + "'",
                    "ImagePlusData loading",
                    "JIPipe needs to load the image from a folder, but it could not find any matching file.",
                    "Please contact the JIPipe developers about this issue.");
        }
        ImagePlus image;
        ImagePlus mask;
        {
            String fileName = imageFile.toString().toLowerCase();
            if ((fileName.endsWith(".tiff") || fileName.endsWith(".tif")) && ImageJDataTypesSettings.getInstance().isUseBioFormats()) {
                OMEImageData omeImageData = OMEImageData.importFrom(storageFilePath);
                image = omeImageData.getImage();
            } else {
                image = IJ.openImage(imageFile.toString());
            }
        }
        {
            String fileName = maskFile.toString().toLowerCase();
            if ((fileName.endsWith(".tiff") || fileName.endsWith(".tif")) && ImageJDataTypesSettings.getInstance().isUseBioFormats()) {
                OMEImageData omeImageData = OMEImageData.importFrom(storageFilePath);
                mask = omeImageData.getImage();
            } else {
                mask = IJ.openImage(maskFile.toString());
            }
        }
        return new MaskedImagePlusData(image, mask);
    }
}
