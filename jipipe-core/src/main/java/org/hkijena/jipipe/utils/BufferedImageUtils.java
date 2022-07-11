package org.hkijena.jipipe.utils;

import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BufferedImageUtils {
    public static BufferedImage scaleImageToFit(BufferedImage image, int maxWidth, int maxHeight) {
        double scale = 1.0;
        if (maxWidth > 0) {
            scale = 1.0 * maxWidth / image.getWidth();
        }
        if (maxHeight > 0) {
            scale = Math.min(1.0 * maxHeight / image.getHeight(), scale);
        }
        if (scale != 1.0) {
            Image scaledInstance = image.getScaledInstance((int) (image.getWidth() * scale), (int) (image.getHeight() * scale), Image.SCALE_SMOOTH);
            image = toBufferedImage(scaledInstance, BufferedImage.TYPE_INT_ARGB);
        }
        return image;
    }

    public static String imageToBase64(BufferedImage image, String type) throws IOException {
        String imageString;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, type, bos);
            byte[] imageBytes = bos.toByteArray();
            BASE64Encoder encoder = new BASE64Encoder();
            imageString = encoder.encode(imageBytes);
        }
        return imageString;
    }

    public static BufferedImage base64ToImage(String imageString) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] bytes = decoder.decodeBuffer(imageString);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(bis);
        }
    }

    /**
     * Converts a given Image into a BufferedImage
     * Applies conversion if the types do not match (useful for greyscale conversion)
     *
     * @param img  The Image to be converted
     * @param type the output image type
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img, int type) {
        if (img instanceof BufferedImage && ((BufferedImage) img).getType() == type) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), type);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    /**
     * Get an image off the system clipboard.
     *
     * @param type the image type
     * @return Returns an Image if successful; otherwise returns null.
     */
    public static BufferedImage getImageFromClipboard(int type) {
        Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                Image image = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                return toBufferedImage(image, type);
            } catch (UnsupportedFlavorException | IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Copy a {@link BufferedImage}
     *
     * @param bi the image
     * @return the copy
     */
    public static BufferedImage copyBufferedImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Copy a {@link BufferedImage}
     *
     * @param bi the image
     * @return the copy
     */
    public static BufferedImage copyBufferedImageToARGB(BufferedImage bi) {
        BufferedImage result = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(bi, 0, 0, null);
        graphics.dispose();
        return result;
    }

    public static BufferedImage spatialBlurLinear(BufferedImage bi, Point start, Point end, double sigma) {

        BufferedImage maskImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics2D = maskImage.createGraphics();
        LinearGradientPaint paint = new LinearGradientPaint(start.x, start.y, end.x, end.y, new float[] {0f, 1f}, new Color[] {Color.WHITE, Color.BLACK});
        graphics2D.setPaint(paint);
        graphics2D.fillRect(0,0,bi.getWidth(), bi.getHeight());
        graphics2D.dispose();

        ByteProcessor mask = new ByteProcessor(maskImage);
        ColorProcessor img = new ColorProcessor(bi);
        ColorProcessor blurred = new ColorProcessor(bi);
        GaussianBlur blur = new GaussianBlur();
        blur.blurGaussian(blurred, sigma);
        byte[] maskPixels = (byte[]) mask.getPixels();
        int[] imgPixels = (int[]) img.getPixels();
        int[] blurredPixels = (int[]) blurred.getPixels();
        for (int i = 0; i < maskPixels.length; i++) {
            double fac = Byte.toUnsignedInt(maskPixels[i]) / 255.0;
            int src1 = imgPixels[i];
            int src2 = blurredPixels[i];
            int r1 = (src1 & 0xff0000) >> 16;
            int g1 = ((src1 & 0xff00) >> 8);
            int b1 = (src1 & 0xff);
            int r2 = (src2 & 0xff0000) >> 16;
            int g2 = ((src2 & 0xff00) >> 8);
            int b2 = (src2 & 0xff);
            int r = lerpColorValue(r1, r2, fac);
            int g = lerpColorValue(g1, g2, fac);
            int b = lerpColorValue(b1, b2, fac);
            int rgb =  (r << 16) + (g << 8) + b;
            imgPixels[i] = rgb;
        }

        return img.getBufferedImage();
    }

    public static int lerpColorValue(int x0, int x1, double frac) {
        return (int) (x0 + frac * (x1 - x0));
    }

}
