package org.hkijena.jipipe.utils;

import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class BufferedImageUtils {

    public static BufferedImage setAlpha(BufferedImage image, BufferedImage mask) {
        if(mask.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            mask = copyBufferedImageToGray(mask);
        }
        if(image.getWidth() != mask.getWidth() || image.getHeight() != mask.getHeight()) {
            throw new JIPipeValidationRuntimeException(new IllegalArgumentException("Images have unequal size!"),
                    "Images have a different size!",
                    "The provided images have a different size",
                    "Check the inputs");
        }
        image = copyBufferedImageToARGB(image);
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] alphaBuffer = ((DataBufferByte) mask.getRaster().getDataBuffer()).getData();
        int[] targetBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < width * height; i++) {
            int a = Byte.toUnsignedInt(alphaBuffer[i]);
            targetBuffer[i] = (a << 24) | (targetBuffer[i] & 0x00FFFFFF);
        }

        return image;
    }

    public static BufferedImage extractAlpha(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage alphaImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] dstBuffer = ((DataBufferByte) alphaImage.getRaster().getDataBuffer()).getData();
//        int[] dstBuffer = new int[width * height];

        if (!src.getColorModel().hasAlpha()) {
            // Just color it white
            Arrays.fill(dstBuffer, (byte)255);
        } else {
            if (src.getType() != BufferedImage.TYPE_INT_ARGB) {
                src = copyBufferedImageToARGB(src);
            }

            int[] srcBuffer = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();

            for (int i = 0; i < width * height; i++) {
                int a = (srcBuffer[i] >> 24) & 0xff;
                dstBuffer[i] = (byte) a;
//                dstBuffer[i] = (byte) (i % width == 0 ? 255 : 0);
            }
        }

//        WritableRaster raster = (WritableRaster) alphaImage.getData();
//        raster.setPixels(0, 0, width, height, dstBuffer);


        return alphaImage;
    }

    public static String getColorModelString(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        int numComponents = cm.getNumComponents();
        boolean hasAlpha = cm.hasAlpha();

        if (numComponents == 1 && !hasAlpha) {
            return "I";
        } else if (numComponents == 2 && hasAlpha) {
            return "IA";
        } else if (numComponents == 3 && !hasAlpha) {
            return "RGB";
        } else if (numComponents == 4 && hasAlpha) {
            return "RGBA";
        } else {
            return "Unknown";
        }
    }

    public static BufferedImage convertAlphaToCheckerboard(Image originalImage, int checkerSize) {
        // Create a new BufferedImage without alpha channel
        int width = originalImage.getWidth(null);
        int height = originalImage.getHeight(null);
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = newImage.createGraphics();

        // Draw the checkerboard pattern
        for (int y = 0; y < height; y += checkerSize) {
            for (int x = 0; x < width; x += checkerSize) {
                if ((x / checkerSize + y / checkerSize) % 2 == 0) {
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(ModernMetalTheme.GRAY);
                }
                g2d.fillRect(x, y, checkerSize, checkerSize);
            }
        }

        // Draw the original image using the alpha channel
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(originalImage, 0, 0, null);

        // Dispose of the graphics object
        g2d.dispose();

        return newImage;
    }

    public static BufferedImage scaleImageToFit(BufferedImage image, int maxWidth, int maxHeight) {
        double scale = 1.0;
        if (maxWidth > 0) {
            scale = 1.0 * maxWidth / image.getWidth();
        }
        if (maxHeight > 0) {
            scale = Math.min(1.0 * maxHeight / image.getHeight(), scale);
        }
        if (scale != 1.0) {
            Image scaledInstance = image.getScaledInstance((int) Math.max(1, image.getWidth() * scale), (int) Math.max(1, image.getHeight() * scale), Image.SCALE_SMOOTH);
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

    /**
     * Copy a {@link BufferedImage}
     *
     * @param bi the image
     * @return the copy
     */
    public static BufferedImage copyBufferedImageToRGB(BufferedImage bi) {
        BufferedImage result = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(bi, 0, 0, null);
        graphics.dispose();
        return result;
    }

    public static BufferedImage copyBufferedImageToGray(BufferedImage bi) {
        BufferedImage result = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(bi, 0, 0, null);
        graphics.dispose();
        return result;
    }

    public static BufferedImage spatialBlurLinear(BufferedImage bi, Point start, Point end, double sigma) {

        BufferedImage maskImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics2D = maskImage.createGraphics();
        LinearGradientPaint paint = new LinearGradientPaint(start.x, start.y, end.x, end.y, new float[]{0f, 1f}, new Color[]{Color.WHITE, Color.BLACK});
        graphics2D.setPaint(paint);
        graphics2D.fillRect(0, 0, bi.getWidth(), bi.getHeight());
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
            double fac = Math.min(1.0, 0.25 + Byte.toUnsignedInt(maskPixels[i]) / 255.0);
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
            int rgb = (r << 16) + (g << 8) + b;
            imgPixels[i] = rgb;
        }

        return img.getBufferedImage();
    }

    public static int lerpColorValue(int x0, int x1, double frac) {
        return (int) (x0 + frac * (x1 - x0));
    }


}
