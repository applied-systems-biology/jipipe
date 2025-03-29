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

package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import ij.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.plugin.filter.AVI_Writer;
import ij.plugin.filter.Convolver;
import ij.process.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeImageThumbnailData;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.BitDepth;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.colors.ColorMap;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2dParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility functions for ImageJ
 */
public class ImageJUtils {


    public static ImageProcessor extractFromProcessor(ImageProcessor processor, Rectangle source) {
        try {
            processor.setRoi(source);
            return processor.crop();
        } finally {
            processor.setRoi((Roi) null);
        }
    }

    public static Quantity getPixelSizeX(ImagePlus imp) {
        if (imp.getCalibration() != null) {
            return new Quantity(imp.getCalibration().pixelWidth, StringUtils.orElse(imp.getCalibration().getXUnit(), Quantity.UNIT_PIXELS));
        } else {
            return new Quantity(1, Quantity.UNIT_PIXELS);
        }
    }

    public static Quantity getPixelSizeY(ImagePlus imp) {
        if (imp.getCalibration() != null) {
            return new Quantity(imp.getCalibration().pixelHeight, StringUtils.orElse(imp.getCalibration().getYUnit(), Quantity.UNIT_PIXELS));
        } else {
            return new Quantity(1, Quantity.UNIT_PIXELS);
        }
    }

    public static Quantity getPixelSizeZ(ImagePlus imp) {
        if (imp.getCalibration() != null) {
            return new Quantity(imp.getCalibration().pixelDepth, StringUtils.orElse(imp.getCalibration().getZUnit(), Quantity.UNIT_PIXELS));
        } else {
            return new Quantity(1, Quantity.UNIT_PIXELS);
        }
    }

    /**
     * Returns the persistent properties of a {@link ImagePlus} as map
     *
     * @param imagePlus the image
     * @return the properties
     */
    public static Map<String, String> getImageProperties(ImagePlus imagePlus) {
        HashMap<String, String> map = new HashMap<>();
        if (imagePlus.getImageProperties() != null) {
            for (Map.Entry<Object, Object> entry : imagePlus.getImageProperties().entrySet()) {
                map.put("" + entry.getKey(), "" + entry.getValue());
            }
        }
        return map;
    }

    /**
     * Sets the properties of a {@link ImagePlus} from a map
     *
     * @param imagePlus  the image
     * @param properties the properties
     */
    public static void setImageProperties(ImagePlus imagePlus, Map<String, String> properties) {
        ImmutableList<Map.Entry<String, String>> copyOf = ImmutableList.copyOf(properties.entrySet());
        String[] props = new String[copyOf.size() * 2];
        for (int i = 0; i < copyOf.size(); i++) {
            Map.Entry<String, String> entry = copyOf.get(i);
            props[2 * i] = StringUtils.nullToEmpty(entry.getKey()).replace(' ', '_').replace('=', '_').replace(':', '_');
            props[2 * i + 1] = entry.getValue();
        }
        imagePlus.setProperties(props);
    }

    /**
     * Returns the lowest ImageJ image type that can contain all images.
     * If any of the images is RGB, 24 bit will be returned
     *
     * @param images the images
     * @return the consensus bit depth. 0 if images is empty
     */
    public static int getConsensusBitDepth(Collection<ImagePlus> images) {
        int type = 0;
        for (ImagePlus image : images) {
            if (image.getBitDepth() == 24) {
                type = 24;
                break;
            } else {
                type = Math.max(type, image.getBitDepth());
            }
        }
        return type;
    }

    public static List<ImagePlus> convertToConsensusBitDepthIfNeeded(Collection<ImagePlus> images) {
        List<ImagePlus> result = new ArrayList<>();
        int bitDepth = getConsensusBitDepth(images);
        for (ImagePlus image : images) {
            result.add(convertToBitDepthIfNeeded(image, bitDepth));
        }
        return result;
    }

    public static ImagePlus convertToBitDepthIfNeeded(ImagePlus imagePlus, int bitDepth) {
        switch (bitDepth) {
            case 8:
                return convertToGreyscale8UIfNeeded(imagePlus);
            case 16:
                return convertToGrayscale16UIfNeeded(imagePlus);
            case 32:
                return convertToGrayscale32FIfNeeded(imagePlus);
            case 24:
                return convertToColorRGBIfNeeded(imagePlus);
            default:
                return imagePlus;

        }
    }

    /**
     * Returns the general {@link ImagePlusData} class for a bit depth
     *
     * @param bitDepth the bit depth
     * @return the class. if bit depth is invalid, {@link ImagePlusData} is returned
     */
    public static Class<? extends ImagePlusData> bitDepthToImagePlusDataType(int bitDepth) {
        switch (bitDepth) {
            case 8:
                return ImagePlusGreyscale8UData.class;
            case 16:
                return ImagePlusGreyscale16UData.class;
            case 32:
                return ImagePlusGreyscale32FData.class;
            case 24:
                return ImagePlusColorRGBData.class;
            default:
                return ImagePlusData.class;
        }
    }

    /**
     * Faster version of the duplicate() method in {@link ImagePlus}.
     * The reason behind this is that {@link ij.plugin.Duplicator} uses an expensive crop() operation instead of Java's array copy
     * to allow making duplicates of ROI
     *
     * @param imp the image
     * @return a copy of the image
     */
    public static ImagePlus duplicate(ImagePlus imp) {
        ImageStack stack = imp.getStack();
        boolean virtualStack = stack.isVirtual();
        double min = imp.getDisplayRangeMin();
        double max = imp.getDisplayRangeMax();
        ImageStack copyStack = new ImageStack(imp.getWidth(), imp.getHeight(), imp.getProcessor().getColorModel());
        int n = stack.size();
        for (int i = 1; i <= n; i++) {
            ImageProcessor sourceProcessor = stack.getProcessor(i);
            ImageProcessor targetProcessor;
            if (sourceProcessor instanceof ByteProcessor) {
                sourceProcessor.setSnapshotCopyMode(false);
                targetProcessor = new ByteProcessor(imp.getWidth(), imp.getHeight(), (byte[]) sourceProcessor.getPixelsCopy());
            } else if (sourceProcessor instanceof ShortProcessor) {
                sourceProcessor.setSnapshotCopyMode(false);
                targetProcessor = new ShortProcessor(imp.getWidth(), imp.getHeight(), (short[]) sourceProcessor.getPixelsCopy(), sourceProcessor.getColorModel());
            } else if (sourceProcessor instanceof ColorProcessor) {
                sourceProcessor.setSnapshotCopyMode(false);
                targetProcessor = new ColorProcessor(imp.getWidth(), imp.getHeight(), (int[]) sourceProcessor.getPixelsCopy());
            } else if (sourceProcessor instanceof FloatProcessor) {
                sourceProcessor.setSnapshotCopyMode(false);
                targetProcessor = new FloatProcessor(imp.getWidth(), imp.getHeight(), (float[]) sourceProcessor.getPixelsCopy());
            } else {
                sourceProcessor.setRoi((Roi) null);
                targetProcessor = sourceProcessor.crop();
            }
            copyStack.addSlice(stack.getSliceLabel(i), targetProcessor);
        }
        ImagePlus imp2 = imp.createImagePlus();
        imp2.setStack("DUP_" + imp.getTitle(), copyStack);
        String info = (String) imp.getProperty("Info");
        if (info != null)
            imp2.setProperty("Info", info);
        imp2.setProperties(imp.getPropertiesAsArray());
        imp2.setCalibration(imp.getCalibration());
        int[] dim = imp.getDimensions();
        imp2.setDimensions(dim[2], dim[3], dim[4]);
        if (imp.isComposite()) {
            imp2 = new CompositeImage(imp2, 0);
            ((CompositeImage) imp2).copyLuts(imp);
        }
        if (virtualStack)
            imp2.setDisplayRange(min, max);
        if (imp.isHyperStack())
            imp2.setOpenAsHyperStack(true);
        Overlay overlay = imp.getOverlay();
        if (overlay != null && !imp.getHideOverlay())
            imp2.setOverlay(overlay);

        // Copy the LUT
        if (imp.getType() != ImagePlus.COLOR_RGB) {
            imp2.setLut(imp.getProcessor().getLut());
            if (imp2.hasImageStack()) {
                imp2.getStack().setColorModel(imp.getStack().getColorModel());
            }
        }

        // Calibrate
        calibrate(imp2, ImageJCalibrationMode.Custom, min, max);

        return imp2;
    }

    public static boolean imagesHaveSameSize(ImagePlus... images) {
        if (images.length < 2)
            return true;
        ImagePlus referenceImage = images[0];
        int width = referenceImage.getWidth();
        int height = referenceImage.getHeight();
        int nZ = referenceImage.getNSlices();
        int nC = referenceImage.getNChannels();
        int nT = referenceImage.getNFrames();
        for (ImagePlus image : images) {
            if (image.getWidth() != width || image.getHeight() != height ||
                    image.getNFrames() != nT || image.getNChannels() != nC || image.getNSlices() != nZ) {
                return false;
            }
        }
        return true;
    }

    public static boolean imagesHaveSameSize(Collection<ImagePlus> images) {
        if (images.size() < 2)
            return true;
        ImagePlus referenceImage = images.iterator().next();
        int width = referenceImage.getWidth();
        int height = referenceImage.getHeight();
        int nZ = referenceImage.getNSlices();
        int nC = referenceImage.getNChannels();
        int nT = referenceImage.getNFrames();
        for (ImagePlus image : images) {
            if (image.getWidth() != width || image.getHeight() != height ||
                    image.getNFrames() != nT || image.getNChannels() != nC || image.getNSlices() != nZ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generates a preview for a colored image with color space
     *
     * @param image  the image
     * @param width  the width
     * @param height the height
     * @return the preview
     */
    public static Component generatePreview(ImagePlus image, ColorSpace colorSpace, int width, int height) {
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        ImagePlus firstSliceImage = new ImagePlus("Preview", image.getProcessor().duplicate());
        colorSpace.convertToRGB(firstSliceImage, new JIPipeProgressInfo());
        ImageProcessor resized = firstSliceImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        BufferedImage bufferedImage = resized.getBufferedImage();
        return new JLabel(new ImageIcon(bufferedImage));
    }

    /**
     * Generates a thumbnail for a colored image with color space
     *
     * @param image  the image
     * @param width  the width
     * @param height the height
     * @return the preview
     */
    public static JIPipeImageThumbnailData generateThumbnail(ImagePlus image, ColorSpace colorSpace, int width, int height) {
        double factorX = 1.0 * width / image.getWidth();
        double factorY = 1.0 * height / image.getHeight();
        double factor = Math.max(factorX, factorY);
        boolean smooth = factor < 0;
        int imageWidth = (int) (image.getWidth() * factor);
        int imageHeight = (int) (image.getHeight() * factor);
        ImagePlus firstSliceImage = new ImagePlus("Preview", image.getProcessor().duplicate());
        colorSpace.convertToRGB(firstSliceImage, new JIPipeProgressInfo());
        ImageProcessor resized = firstSliceImage.getProcessor().resize(imageWidth, imageHeight, smooth);
        return new JIPipeImageThumbnailData(resized);
    }

    /**
     * Converts an RGB image to LAB
     * L is stored unsigned (0-255)
     * A is stored signed (-128-127)
     * B is stored signed (-128-127)
     *
     * @param img          the input image (must be RGB)
     * @param progressInfo the progress info
     */
    public static void convertRGBToLAB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ColorSpaceConverter converter = new ColorSpaceConverter();
        ImageJIterationUtils.forEachSlice(img, ip -> {
            int width = ip.getWidth();
            int height = ip.getHeight();
            int[] pixels = (int[]) ip.getPixels();
            for (int i = 0; i < width * height; i++) {
                int c = pixels[i];
                double[] lab = converter.RGBtoLAB(c);
                int l = (int) Math.max(0, Math.min(255, lab[0]));
                int a = (int) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[1])) - (int) Byte.MIN_VALUE;
                int b = (int) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[2])) - (int) Byte.MIN_VALUE;
                pixels[i] = (l << 16) + (a << 8) + b;
            }
        }, progressInfo);
    }

    /**
     * Converts an LAB image to RGB
     * L is stored unsigned (0-255)
     * A is stored signed (-128-127)
     * B is stored signed (-128-127)
     *
     * @param img          the input image (must be RGB)
     * @param progressInfo the progress info
     */
    public static void convertLABToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ColorSpaceConverter converter = new ColorSpaceConverter();
        ImageJIterationUtils.forEachSlice(img, ip -> {
            int width = ip.getWidth();
            int height = ip.getHeight();
            int[] pixels = (int[]) ip.getPixels();
            double[] lab = new double[3];
            for (int i = 0; i < width * height; i++) {
                int c = pixels[i];
                int l = (c & 0xff0000) >> 16;
                int a = ((c & 0xff00) >> 8) + Byte.MIN_VALUE;
                int b = (c & 0xff) + Byte.MIN_VALUE;
                lab[0] = l;
                lab[1] = a;
                lab[2] = b;
                int[] rgb = converter.LABtoRGB(lab);
                pixels[i] = (rgb[0] << 16) + (rgb[1] << 8) + rgb[2];
            }
        }, progressInfo);
    }

    /**
     * Converts an RGB image to HSB (re-using the same components)
     *
     * @param img          the input image (must be RGB)
     * @param progressInfo the progress info
     */
    public static void convertRGBToHSB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ImageJIterationUtils.forEachSlice(img, ip -> {
            int width = ip.getWidth();
            int height = ip.getHeight();
            int c, r, g, b;
            float[] hsb = new float[3];
            int[] pixels = (int[]) ip.getPixels();
            for (int i = 0; i < width * height; i++) {
                c = pixels[i];
                r = (c & 0xff0000) >> 16;
                g = (c & 0xff00) >> 8;
                b = c & 0xff;
                Color.RGBtoHSB(r, g, b, hsb);
                int H = ((int) (hsb[0] * 255.0));
                int S = ((int) (hsb[1] * 255.0));
                int B = ((int) (hsb[2] * 255.0));
                pixels[i] = (H << 16) + (S << 8) + B;
            }
        }, progressInfo);
    }

    /**
     * Converts an HSB image to RGB (re-using the same components)
     *
     * @param img          the input image (must be RGB)
     * @param progressInfo the progress info
     */
    public static void convertHSBToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ImageJIterationUtils.forEachSlice(img, ip -> {
            int width = ip.getWidth();
            int height = ip.getHeight();
            int c, H, S, B;
            int[] pixels = (int[]) ip.getPixels();
            for (int i = 0; i < width * height; i++) {
                c = pixels[i];
                H = (c & 0xff0000) >> 16;
                S = (c & 0xff00) >> 8;
                B = c & 0xff;
                int rgb = Color.HSBtoRGB(H / 255.0f, S / 255.0f, B / 255.0f);
                pixels[i] = rgb;
            }
        }, progressInfo);
    }

    /**
     * Converts a zero-based slice index to a safe index that can be found within the image
     *
     * @param img   the image
     * @param index the index
     * @return the safe index
     */
    public static ImageSliceIndex toSafeZeroIndex(ImagePlus img, ImageSliceIndex index) {
        return new ImageSliceIndex(
                Math.max(0, Math.min(img.getNChannels() - 1, index.getC())),
                Math.max(0, Math.min(img.getNSlices() - 1, index.getZ())),
                Math.max(0, Math.min(img.getNFrames() - 1, index.getT()))
        );
    }

    /**
     * Gets slice from image.
     * Will not trigger an {@link IndexOutOfBoundsException}
     *
     * @param img   the image
     * @param index the index (zero-based)
     * @return the processor
     */
    public static ImageProcessor getSliceZeroSafe(ImagePlus img, ImageSliceIndex index) {
        if (img.hasImageStack()) {
            return img.getStack().getProcessor(img.getStackIndex(Math.min(index.getC() + 1, img.getNChannels()),
                    Math.min(index.getZ() + 1, img.getNSlices()),
                    Math.min(index.getT() + 1, img.getNFrames())));
        } else {
            return img.getProcessor();
        }
    }

    /**
     * Gets slice from image
     * Will not trigger an {@link IndexOutOfBoundsException}
     *
     * @param img the image
     * @return the processor
     */
    public static ImageProcessor getSliceZeroSafe(ImagePlus img, int c, int z, int t) {
        if (img.hasImageStack()) {
            return img.getStack().getProcessor(img.getStackIndex(Math.min(c + 1, img.getNChannels()),
                    Math.min(z + 1, img.getNSlices()),
                    Math.min(t + 1, img.getNFrames())));
        } else {
            return img.getProcessor();
        }
    }

    /**
     * Gets slice from image
     *
     * @param img   the image
     * @param index the index (zero-based)
     * @return the processor
     */
    public static ImageProcessor getSliceZero(ImagePlus img, ImageSliceIndex index) {
        if (img.hasImageStack()) {
            return img.getStack().getProcessor(img.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1));
        } else {
            if (index.getZ() != 0 || index.getC() != 0 || index.getT() != 0)
                throw new IndexOutOfBoundsException("Accessing stack index " + index);
            return img.getProcessor();
        }
    }

    /**
     * Gets slice from image
     *
     * @param img the image
     * @return the processor
     */
    public static ImageProcessor getSliceZero(ImagePlus img, int c, int z, int t) {
        if (img.hasImageStack()) {
            return img.getStack().getProcessor(img.getStackIndex(c + 1, z + 1, t + 1));
        } else {
            if (z != 0 || c != 0 || t != 0)
                throw new IndexOutOfBoundsException("Accessing stack index z=" + z + ", c=" + c + ", t=" + t);
            return img.getProcessor();
        }
    }

    /**
     * Copies the hyperstack dimensions from one image to another
     * src and target must have the same number of slices!
     *
     * @param src    the source image
     * @param target the target image
     */
    public static void copyHyperstackDimensions(ImagePlus src, ImagePlus target) {
        target.setDimensions(src.getNChannels(), src.getNSlices(), src.getNFrames());
    }

    /**
     * Applies copy-scaling to ensure that the target image has the same size as the reference image
     * Will silently drop slices that are outside the range of the reference image
     *
     * @param target     the target image
     * @param reference  the image
     * @param copySlices if slices should be copied. otherwise, empty (black) slices are created
     * @return image that has the same size as the reference. returns the same target if the size matches
     */
    public static ImagePlus ensureEqualSize(ImagePlus target, ImagePlus reference, boolean copySlices) {
        if (imagesHaveSameSize(reference, target))
            return target;
        if (reference.getStackSize() == 1) {
            return new ImagePlus(target.getTitle(), target.getProcessor());
        } else {
            ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), reference.getNChannels() * reference.getNFrames() * reference.getNSlices());
            ImageJIterationUtils.forEachIndexedZCTSlice(reference, (referenceProcessor, referenceIndex) -> {
                if (copySlices) {
                    int z = Math.min(target.getNSlices() - 1, referenceIndex.getZ());
                    int c = Math.min(target.getNChannels() - 1, referenceIndex.getC());
                    int t = Math.min(target.getNFrames() - 1, referenceIndex.getT());
                    ImageProcessor processor = ImageJUtils.getSliceZero(target, c, z, t);
                    int stackIndex = referenceIndex.zeroSliceIndexToOneStackIndex(reference);
                    stack.setProcessor(processor, stackIndex);
                } else {
                    if (referenceIndex.getZ() < target.getNSlices() && referenceIndex.getC() < target.getNChannels() && referenceIndex.getT() < target.getNFrames()) {
                        ImageProcessor processor = ImageJUtils.getSliceZero(target, referenceIndex);
                        stack.setProcessor(processor, referenceIndex.zeroSliceIndexToOneStackIndex(reference));
                    } else {
                        ImageProcessor processor;
                        switch (target.getBitDepth()) {
                            case 8:
                                processor = new ByteProcessor(target.getWidth(), target.getHeight());
                                break;
                            case 16:
                                processor = new ShortProcessor(target.getWidth(), target.getHeight());
                                break;
                            case 24:
                                processor = new ColorProcessor(target.getWidth(), target.getHeight());
                                break;
                            case 32:
                                processor = new FloatProcessor(target.getWidth(), target.getHeight());
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                        stack.setProcessor(processor, referenceIndex.zeroSliceIndexToOneStackIndex(reference));
                    }
                }
            }, new JIPipeProgressInfo());
            ImagePlus newMask = new ImagePlus(target.getTitle(), stack);
            newMask.setDimensions(reference.getNChannels(), reference.getNSlices(), reference.getNFrames());
            return newMask;
        }
    }

    /**
     * Creates a dummy canvas that contains the appropriate magnification settings
     *
     * @param image      the image
     * @param renderArea the area where the image will be rendered
     * @return the canvas
     */
    public static ImageCanvas createZoomedDummyCanvas(ImagePlus image, Rectangle renderArea) {
        double magnification = 1.0 * renderArea.width / image.getWidth();
        return createZoomedDummyCanvas(image, magnification);
    }

    /**
     * Creates a dummy canvas that contains the appropriate magnification settings
     *
     * @param image         the image
     * @param magnification the magnification. See {@link ImageCanvas} for the minimum and maximum values
     * @return the canvas
     */
    public static ImageCanvas createZoomedDummyCanvas(ImagePlus image, double magnification) {
        ImageCanvas canvas = new ImageCanvas(image);
        canvas.setMagnification(magnification);
        canvas.setSize((int) (image.getWidth() * magnification), (int) (image.getHeight() * magnification));
        return canvas;
    }

    /**
     * Returns an image that has the specified size by copying
     *
     * @param target     the target image
     * @param nChannels  number of channels
     * @param nSlices    number of slices
     * @param nFrames    number of frames
     * @param copySlices if slices should be copied. otherwise, empty (black) slices are created
     * @return image that has the specified size. returns the same target if the size matches
     */
    public static ImagePlus ensureSize(ImagePlus target, int nChannels, int nSlices, int nFrames, boolean copySlices) {
        if (target.getNChannels() == nChannels && target.getNSlices() == nSlices && target.getNFrames() == nFrames)
            return target;
        if (target.getStackSize() == nChannels * nSlices * nFrames) {
            // Use the native ImageJ function
            target.setDimensions(nChannels, nSlices, nFrames);
            return target;
        }
        if (nChannels * nSlices * nFrames == 1) {
            return new ImagePlus(target.getTitle(), target.getProcessor());
        } else {
            ImageStack stack = new ImageStack(target.getWidth(), target.getHeight(), nChannels * nSlices * nFrames);
            for (int z = 0; z < nSlices; z++) {
                for (int c = 0; c < nChannels; c++) {
                    for (int t = 0; t < nFrames; t++) {
                        if (copySlices) {
                            int z_ = Math.min(target.getNSlices() - 1, z);
                            int c_ = Math.min(target.getNChannels() - 1, c);
                            int t_ = Math.min(target.getNFrames() - 1, t);
                            ImageProcessor processor = ImageJUtils.getSliceZero(target, c_, z_, t_);
                            stack.setProcessor(processor, zeroSliceIndexToOneStackIndex(c, z, t, nChannels, nSlices, nFrames));
                        } else {
                            if (z < target.getNSlices() && c < target.getNChannels() && t < target.getNFrames()) {
                                ImageProcessor processor = ImageJUtils.getSliceZero(target, c, z, t);
                                stack.setProcessor(processor, zeroSliceIndexToOneStackIndex(c, z, t, nChannels, nSlices, nFrames));
                            } else {
                                ImageProcessor processor;
                                switch (target.getBitDepth()) {
                                    case 8:
                                        processor = new ByteProcessor(target.getWidth(), target.getHeight());
                                        break;
                                    case 16:
                                        processor = new ShortProcessor(target.getWidth(), target.getHeight());
                                        break;
                                    case 24:
                                        processor = new ColorProcessor(target.getWidth(), target.getHeight());
                                        break;
                                    case 32:
                                        processor = new FloatProcessor(target.getWidth(), target.getHeight());
                                        break;
                                    default:
                                        throw new UnsupportedOperationException();
                                }
                                stack.setProcessor(processor, zeroSliceIndexToOneStackIndex(c, z, t, nChannels, nSlices, nFrames));
                            }
                        }
                    }
                }
            }
            ImagePlus newMask = new ImagePlus(target.getTitle(), stack);
            newMask.setDimensions(nChannels, nSlices, nFrames);
            return newMask;
        }
    }

    /**
     * Rotates the image by a specified amount of degrees to the right
     *
     * @param img          the image
     * @param degrees      the degrees
     * @param expandCanvas if the canvas should be expanded
     * @param background   the background (if non-90-degree angles)
     * @param addRoi       if the image should be overlaid with ROI indicating the area of the content
     * @param progressInfo the progress
     * @return the image
     */
    public static ImagePlus rotate(ImagePlus img, double degrees, boolean expandCanvas, Color background, boolean addRoi, JIPipeProgressInfo progressInfo) {
        if (degrees == 0)
            return duplicate(img);
        if (degrees % 90 == 0) {
            ImageStack stack;
            int width;
            int height;
            if (degrees < 0) {
                int rotations = (int) (degrees) / -90;
                width = (rotations % 2 == 0) ? img.getWidth() : img.getHeight();
                height = (rotations % 2 == 0) ? img.getHeight() : img.getWidth();
                stack = new ImageStack(width, height, img.getStack().getColorModel());
                ImageJIterationUtils.forEachSlice(img, ip -> {
                    ImageProcessor processor = ip;
                    for (int i = 0; i < rotations; i++) {
                        processor = processor.rotateLeft();
                    }
                    stack.addSlice(processor);
                }, progressInfo);
            } else {
                int rotations = (int) (degrees) / 90;
                width = (rotations % 2 == 0) ? img.getWidth() : img.getHeight();
                height = (rotations % 2 == 0) ? img.getHeight() : img.getWidth();
                stack = new ImageStack(width, height, img.getStack().getColorModel());
                ImageJIterationUtils.forEachSlice(img, ip -> {
                    ImageProcessor processor = ip;
                    for (int i = 0; i < rotations; i++) {
                        processor = processor.rotateRight();
                    }
                    stack.addSlice(processor);
                }, progressInfo);
            }
            ImagePlus imagePlus = new ImagePlus(img.getTitle() + "_Rotated" + degrees, stack);
            if (addRoi)
                imagePlus.setRoi(new Rectangle(0, 0, width, height));
            imagePlus.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
            return imagePlus;
        } else {
            // Find the ROI and canvas size
            Rectangle originalRoi = new Rectangle(0, 0, img.getWidth(), img.getHeight());
            Rectangle rotatedBoundingRectangle;
            PolygonRoi roi;
            {
                AffineTransform affineTransform = new AffineTransform();
                affineTransform.rotate(2 * Math.PI * (degrees / 360), originalRoi.getCenterX(), originalRoi.getCenterY());
                Shape transformedShape = affineTransform.createTransformedShape(originalRoi);
                rotatedBoundingRectangle = transformedShape.getBounds();
                PathIterator pathIterator = transformedShape.getPathIterator(null);
                double[] coords = new double[2];
                TIntList xCoords = new TIntArrayList();
                TIntList yCoords = new TIntArrayList();
                while (!pathIterator.isDone()) {
                    pathIterator.currentSegment(coords);
                    pathIterator.next();
                    int x = (int) coords[0];
                    int y = (int) coords[1];
                    if (expandCanvas) {
                        x -= rotatedBoundingRectangle.x;
                        y -= rotatedBoundingRectangle.y;
                    }
                    xCoords.add(x);
                    yCoords.add(y);
                }
                roi = new PolygonRoi(xCoords.toArray(), yCoords.toArray(), xCoords.size(), PolygonRoi.POLYGON);
            }
            if (expandCanvas) {
//                System.out.println(rotatedBoundingRectangle);
                int newWidth = rotatedBoundingRectangle.width;
                int newHeight = rotatedBoundingRectangle.height;

                // Expand the image
                ImagePlus imagePlus = expandImageCanvas(img, background, newWidth, newHeight, Anchor.CenterCenter);
                ImageJIterationUtils.forEachSlice(imagePlus, ip -> {
                    ip.setColor(background);
                    ip.rotate(degrees);
                    ip.fillOutside(roi);
                }, progressInfo);
                imagePlus.setTitle(img.getTitle() + "_Rotated" + degrees);
                if (addRoi)
                    imagePlus.setRoi(roi);
                return imagePlus;
            } else {
                ImagePlus imagePlus = duplicate(img);
                ImageJIterationUtils.forEachSlice(imagePlus, ip -> {
                    ip.setColor(background);
                    ip.rotate(degrees);
                    ip.fillOutside(roi);
                }, progressInfo);
                imagePlus.setTitle(img.getTitle() + "_Rotated" + degrees);
                if (addRoi)
                    imagePlus.setRoi(roi);
                return imagePlus;
            }
        }
    }

    /**
     * Stack index (one-based)
     *
     * @param channel   one-based channel
     * @param slice     one-based slice
     * @param frame     one-based frame
     * @param imagePlus reference image
     * @return one-based stack index
     */
    public static int oneSliceIndexToOneStackIndex(int channel, int slice, int frame, ImagePlus imagePlus) {
        return oneSliceIndexToOneStackIndex(channel, slice, frame, imagePlus.getNChannels(), imagePlus.getNSlices(), imagePlus.getNFrames());
    }

    /**
     * Stack index (zero-based)
     *
     * @param channel   one-based channel
     * @param slice     one-based slice
     * @param frame     one-based frame
     * @param imagePlus reference image
     * @return one-based stack index
     */
    public static int zeroSliceIndexToOneStackIndex(int channel, int slice, int frame, ImagePlus imagePlus) {
        return oneSliceIndexToOneStackIndex(channel + 1, slice + 1, frame + 1, imagePlus.getNChannels(), imagePlus.getNSlices(), imagePlus.getNFrames());
    }

    /**
     * Copies a mask into a {@link BufferedImage}
     *
     * @param mask       the mask
     * @param target     the target image. Must be ABGR
     * @param foreground color for pixels larger than zero
     * @param background color for zero pixels
     */
    public static void maskToBufferedImage(ImageProcessor mask, BufferedImage target, Color foreground, Color background) {
        if (mask.getWidth() != target.getWidth() || mask.getHeight() != target.getHeight()) {
            mask = mask.resize(target.getWidth(), target.getHeight(), false);
        }
        byte[] sourcePixels = (byte[]) mask.getPixels();
        byte[] targetPixels = ((DataBufferByte) target.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < sourcePixels.length; i++) {
            if (Byte.toUnsignedInt(sourcePixels[i]) > 0) {
                targetPixels[i * 4] = (byte) foreground.getAlpha(); // A
                targetPixels[i * 4 + 1] = (byte) foreground.getBlue(); // B
                targetPixels[i * 4 + 2] = (byte) foreground.getGreen(); // G
                targetPixels[i * 4 + 3] = (byte) foreground.getRed(); // R
            } else {
                targetPixels[i * 4] = (byte) background.getAlpha(); // A
                targetPixels[i * 4 + 1] = (byte) background.getBlue(); // B
                targetPixels[i * 4 + 2] = (byte) background.getGreen(); // G
                targetPixels[i * 4 + 3] = (byte) background.getRed(); // R
            }
        }
    }

    /**
     * Copies all data from the source to the target image
     *
     * @param src    the source
     * @param target the target
     */
    public static void copyPixelsBetweenImages(ImagePlus src, ImagePlus target, JIPipeProgressInfo progressInfo) {
        if (!ImageJUtils.imagesHaveSameSize(src, target) || src.getBitDepth() != target.getBitDepth()) {
            throw new IllegalArgumentException("Source and target must have same size and bit depth!");
        }
        ImageJIterationUtils.forEachIndexedZCTSlice(target, (targetProcessor, index) -> {
            ImageProcessor sourceProcessor = ImageJUtils.getSliceZero(src, index);
            targetProcessor.copyBits(sourceProcessor, 0, 0, Blitter.COPY);
        }, progressInfo);
    }

    /**
     * Stack index (one-based)
     *
     * @param channel   one-based channel
     * @param slice     one-based slice
     * @param frame     one-based frame
     * @param nChannels number of channels
     * @param nSlices   number of slices
     * @param nFrames   number of frames
     * @return one-based stack index
     */
    public static int oneSliceIndexToOneStackIndex(int channel, int slice, int frame, int nChannels, int nSlices, int nFrames) {
        if (channel < 1) {
            throw new IndexOutOfBoundsException("Channel < 1");
        }
        if (channel > nChannels) {
            throw new IndexOutOfBoundsException("Channel (" + channel + ")  > nChannels (" + nChannels + ")");
        }
        if (slice < 1) {
            throw new IndexOutOfBoundsException("Slice < 1");
        }
        if (slice > nSlices) {
            throw new IndexOutOfBoundsException("Slice (" + slice + ") > nSlices (" + nSlices + ")");
        }
        if (frame < 1) {
            throw new IndexOutOfBoundsException("Frame < 1");
        }
        if (frame > nFrames) {
            throw new IndexOutOfBoundsException("Frame (" + frame + ") > nFrames (" + nFrames + ")");
        }
        return (frame - 1) * nChannels * nSlices + (slice - 1) * nChannels + channel;
    }

    /**
     * Stack index (zero-based)
     *
     * @param channel   zero-based channel
     * @param slice     zero-based slice
     * @param frame     zero-based frame
     * @param nChannels number of channels
     * @param nSlices   number of slices
     * @param nFrames   number of frames
     * @return one-based stack index
     */
    public static int zeroSliceIndexToOneStackIndex(int channel, int slice, int frame, int nChannels, int nSlices, int nFrames) {
        return oneSliceIndexToOneStackIndex(channel + 1, slice + 1, frame + 1, nChannels, nSlices, nFrames);
    }

    /**
     * Combines the slices in the map into one image. The slice indices can be discontinuous.
     * The new image is built up in order T, Z, C, which implies constraints on the discontinuations:
     * 1. If a T is present, all slices of this T must be present (same length per frame)
     * 2. If a Z is present, all slices of this Z must be present (same channels per Z)
     *
     * @param slices the slices. Must all have the same size. Can be discontinuous.
     * @return the output image
     */
    public static ImagePlus combineSlices(Map<ImageSliceIndex, ImageProcessor> slices) {
        ImageProcessor reference = slices.values().iterator().next();

        // Re-map the slices to continuous indices
        TIntIntMap mappingT = new TIntIntHashMap();
        TIntIntMap mappingZ = new TIntIntHashMap();
        TIntIntMap mappingC = new TIntIntHashMap();

        List<Integer> distinctT = slices.keySet().stream().map(ImageSliceIndex::getT).sorted().distinct().collect(Collectors.toList());
        List<Integer> distinctZ = slices.keySet().stream().map(ImageSliceIndex::getZ).sorted().distinct().collect(Collectors.toList());
        List<Integer> distinctC = slices.keySet().stream().map(ImageSliceIndex::getC).sorted().distinct().collect(Collectors.toList());

        if (slices.size() != distinctZ.size() * distinctC.size() * distinctT.size()) {
            throw new IllegalArgumentException(slices.size() + " slices cannot be distributed into an hyperstack with size ZCT "
                    + distinctZ.size() + " x " + distinctC.size() + " x " + distinctT.size() + "!");
        }

        for (int i = 0; i < distinctT.size(); i++) {
            mappingT.put(distinctT.get(i), i);
        }
        for (int i = 0; i < distinctZ.size(); i++) {
            mappingZ.put(distinctZ.get(i), i);
        }
        for (int i = 0; i < distinctC.size(); i++) {
            mappingC.put(distinctC.get(i), i);
        }

        // Build the stack
        ImageProcessor[] array = new ImageProcessor[slices.size()];
        for (Map.Entry<ImageSliceIndex, ImageProcessor> entry : slices.entrySet()) {
            int t = mappingT.get(entry.getKey().getT()) + 1;
            int z = mappingZ.get(entry.getKey().getZ()) + 1;
            int c = mappingC.get(entry.getKey().getC()) + 1;
            int index = oneSliceIndexToOneStackIndex(c, z, t, distinctC.size(), distinctZ.size(), distinctT.size()) - 1;
            array[index] = entry.getValue();
        }

        ImageStack stack = new ImageStack(reference.getWidth(), reference.getHeight());
        for (ImageProcessor processor : array) {
            stack.addSlice(processor);
        }
        ImagePlus combined = new ImagePlus("combined", stack);
        combined.setDimensions(distinctC.size(), distinctZ.size(), distinctT.size());
        return combined;
    }

    /**
     * Merges mapped slices into one image
     *
     * @param sliceMap         the slice map
     * @param equalizeBitDepth if true, find the consensus bit depth and convert all slices
     * @return the merged image
     */
    public static ImagePlus mergeMappedSlices(Map<ImageSliceIndex, ImageProcessor> sliceMap, boolean equalizeBitDepth) {
        int maxZ = Integer.MIN_VALUE;
        int maxC = Integer.MIN_VALUE;
        int maxT = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int minC = Integer.MAX_VALUE;
        int minT = Integer.MAX_VALUE;
        for (ImageSliceIndex index : sliceMap.keySet()) {
            maxZ = Math.max(maxZ, index.getZ());
            maxC = Math.max(maxC, index.getC());
            maxT = Math.max(maxT, index.getT());
            minZ = Math.min(minZ, index.getZ());
            minC = Math.min(minC, index.getC());
            minT = Math.min(minT, index.getT());
        }

        // Remapping
        List<Integer> zRemapping = new ArrayList<>(Arrays.asList(new Integer[maxZ - minZ + 1]));
        List<Integer> cRemapping = new ArrayList<>(Arrays.asList(new Integer[maxC - minC + 1]));
        List<Integer> tRemapping = new ArrayList<>(Arrays.asList(new Integer[maxT - minT + 1]));

        for (Map.Entry<ImageSliceIndex, ImageProcessor> entry : sliceMap.entrySet()) {
            ImageSliceIndex index = entry.getKey();
            int z = index.getZ() - minZ;
            int c = index.getC() - minC;
            int t = index.getT() - minT;
            zRemapping.set(z, index.getZ());
            cRemapping.set(c, index.getC());
            tRemapping.set(t, index.getT());
        }

        // Cleanup the empty locations within the map
        Iterables.removeIf(zRemapping, Objects::isNull);
        Iterables.removeIf(cRemapping, Objects::isNull);
        Iterables.removeIf(tRemapping, Objects::isNull);

        int consensusBitDepth = equalizeBitDepth ? ImageJUtils.getConsensusBitDepth(sliceMap.values().stream().map(ip -> new ImagePlus("", ip)).collect(Collectors.toList())) : -1;
        ImageProcessor referenceProcessor = sliceMap.values().iterator().next();
        ImageStack outputImageStack = new ImageStack(referenceProcessor.getWidth(), referenceProcessor.getHeight(), zRemapping.size() * cRemapping.size() * tRemapping.size());

        for (Map.Entry<ImageSliceIndex, ImageProcessor> entry : sliceMap.entrySet()) {
            int z = zRemapping.indexOf(entry.getKey().getZ());
            int c = cRemapping.indexOf(entry.getKey().getC());
            int t = tRemapping.indexOf(entry.getKey().getT());
            ImageSliceIndex targetIndex = new ImageSliceIndex(c, z, t);
            ImagePlus converted;
            if (consensusBitDepth > 0) {
                converted = ImageJUtils.convertToBitDepthIfNeeded(new ImagePlus("", entry.getValue()), consensusBitDepth);
            } else {
                converted = new ImagePlus("", entry.getValue()); // No conversion
            }
            outputImageStack.setProcessor(converted.getProcessor(), targetIndex.zeroSliceIndexToOneStackIndex(cRemapping.size(), zRemapping.size(), tRemapping.size()));
        }

        for (int i = 0; i < outputImageStack.size(); i++) {
            if (outputImageStack.getProcessor(i + 1) == null) {
                throw new NullPointerException("Not all slices set! Missing index: " + i);
            }
        }

        ImagePlus outputImage = new ImagePlus("Merged", outputImageStack);
        outputImage.setDimensions(cRemapping.size(), zRemapping.size(), tRemapping.size());
        return outputImage;
    }

    public static ImagePlus mergeMappedSlices(Map<ImageSliceIndex, ImageProcessor> sliceMap) {
        return mergeMappedSlices(sliceMap, true);
    }

    public static ImagePlus extractCTStack(ImagePlus img, int c, int t) {
        Map<ImageSliceIndex, ImageProcessor> sliceMap = new HashMap<>();
        for (int z = 0; z < img.getNSlices(); z++) {
            ImageProcessor processor = getSliceZero(img, c, z, t);
            sliceMap.put(new ImageSliceIndex(0, z, 0), processor);
        }
        ImagePlus result = mergeMappedSlices(sliceMap);
        result.copyScale(img);
        return result;
    }

    public static ImagePlus extractZTStack(ImagePlus img, int z, int t) {
        Map<ImageSliceIndex, ImageProcessor> sliceMap = new HashMap<>();
        for (int c = 0; c < img.getNChannels(); c++) {
            ImageProcessor processor = getSliceZero(img, c, z, t);
            sliceMap.put(new ImageSliceIndex(c, 0, 0), processor);
        }
        ImagePlus result = mergeMappedSlices(sliceMap);
        result.copyScale(img);
        return result;
    }

    public static ImagePlus extractTHyperStack(ImagePlus img, int t) {
        Map<ImageSliceIndex, ImageProcessor> sliceMap = new HashMap<>();
        for (int c = 0; c < img.getNChannels(); c++) {
            for (int z = 0; z < img.getNSlices(); z++) {
                ImageProcessor processor = getSliceZero(img, c, z, t);
                sliceMap.put(new ImageSliceIndex(c, z, 0), processor);
            }
        }
        ImagePlus result = mergeMappedSlices(sliceMap);
        result.copyScale(img);
        return result;
    }


    /**
     * Sets the slice of an image to the processor based on the index. Handles all configurations of images.
     *
     * @param image     the image
     * @param processor the processor
     * @param index     the index
     */
    public static void setSliceZero(ImagePlus image, ImageProcessor processor, ImageSliceIndex index) {
        if (image.getStackSize() == 1) {
            if (index.getC() != 0 && index.getT() != 0 && index.getZ() != 0)
                throw new IndexOutOfBoundsException("Out of range: " + index + " in " + image);
            image.setProcessor(processor);
        } else {
            image.getStack().setProcessor(processor, image.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1));
        }
    }

    /**
     * Runs a command on an ImageJ image
     *
     * @param img        the image
     * @param command    the command
     * @param parameters command parameters
     * @return Result image
     */
    public static ImagePlus runOnImage(ImagePlus img, String command, Object... parameters) {
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(img);
        IJ.run(command, params);
        WindowManager.setTempCurrentImage(null);
        return img;
    }

    /**
     * Runs a command on a copy of an ImageJ image
     *
     * @param img        the image
     * @param command    the command
     * @param parameters the command parameters
     * @return Result image
     */
    public static ImagePlus runOnNewImage(ImagePlus img, String command, Object... parameters) {
        ImagePlus copy = duplicate(img);
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(copy);
        IJ.run(command, params);
        WindowManager.setTempCurrentImage(null);
        return copy;
    }

    /**
     * Runs a command on a copy of an ImageJ image
     *
     * @param img        the image
     * @param plugin     the command
     * @param parameters the command parameters
     * @return Result image
     */
    public static ImagePlus runOnImage(ImagePlus img, PlugIn plugin, Object... parameters) {
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(img);
        plugin.run(params);
        WindowManager.setTempCurrentImage(null);
        return img;
    }

    /**
     * Runs a command on a copy of an ImageJ image
     *
     * @param img        the image
     * @param plugin     the command
     * @param parameters the command parameters
     * @return Result image
     */
    public static ImagePlus runOnNewImage(ImagePlus img, PlugIn plugin, Object... parameters) {
        ImagePlus copy = duplicate(img);
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(copy);
        plugin.run(params);
        WindowManager.setTempCurrentImage(null);
        return copy;
    }

    /**
     * Converts a list of parameters into a space-delimited
     *
     * @param parameters the parameters
     * @return Joined string
     */
    public static String toParameterString(Object... parameters) {
        return Arrays.stream(parameters).map(Object::toString).collect(Collectors.joining(" "));
    }

    /**
     * Convert a RGB image to a grayscale image where the RGB channels are split into individual planes
     *
     * @param image the image
     * @return split image
     */
    public static ImagePlus rgbToChannels(ImagePlus image) {
        if (image.getType() != ImagePlus.COLOR_RGB)
            return image;
        int nChannels = (image.getNChannels() + 2);
        ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNFrames() * image.getNSlices() * nChannels);
        for (int z = 0; z < image.getNSlices(); z++) {
            for (int t = 0; t < image.getNFrames(); t++) {
                for (int c = 0; c < image.getNChannels(); c++) {
                    ColorProcessor rgbProcessor = (ColorProcessor) image.getStack().getProcessor(image.getStackIndex(c, z, t));
                    ImageProcessor rProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
                    ImageProcessor gProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
                    ImageProcessor bProcessor = new ByteProcessor(image.getWidth(), image.getHeight());
                    for (int y = 0; y < rgbProcessor.getHeight(); y++) {
                        for (int x = 0; x < rgbProcessor.getWidth(); x++) {
                            int rgb = rgbProcessor.get(x, y);
                            int r = (rgb >> 16) & 0xFF;
                            int g = (rgb >> 8) & 0xFF;
                            int b = rgb & 0xFF;
                            rProcessor.set(x, y, r);
                            gProcessor.set(x, y, g);
                            bProcessor.set(x, y, b);
                        }
                    }
                    int rProcessorIndex = t * nChannels * image.getNSlices() + z * nChannels + c + 1;
                    int gProcessorIndex = t * nChannels * image.getNSlices() + z * nChannels + c + 2;
                    int bProcessorIndex = t * nChannels * image.getNSlices() + z * nChannels + c + 3;
                    stack.setProcessor(rProcessor, rProcessorIndex);
                    stack.setProcessor(gProcessor, gProcessorIndex);
                    stack.setProcessor(bProcessor, bProcessorIndex);
                }
            }
        }
        ImagePlus result = new ImagePlus(image.getTitle() + "_split", stack);
        result.setDimensions(nChannels, image.getNSlices(), image.getNFrames());
        result.copyScale(image);
        return result;
    }

    /**
     * Converted a 3-channel image into RGB (reducing the dimensionality)
     *
     * @param image the image
     * @return RGB image if needed
     */
    public static ImagePlus channelsToRGB(ImagePlus image) {
        if (image.getType() == ImagePlus.COLOR_RGB || image.getType() == ImagePlus.COLOR_256)
            return image;
        if (image.getNChannels() == 3) {
            image = duplicate(image);
            if (image.getType() != ImagePlus.GRAY8) {
                ImageConverter ic = new ImageConverter(image);
                ic.convertToGray8();
            }
            ImageStack stack = new ImageStack(image.getWidth(), image.getHeight(), image.getNFrames() * image.getNSlices());
            for (int z = 0; z < image.getNSlices(); z++) {
                for (int t = 0; t < image.getNFrames(); t++) {
                    ImageProcessor r = image.getStack().getProcessor(image.getStackIndex(1, z + 1, t + 1));
                    ImageProcessor g = image.getStack().getProcessor(image.getStackIndex(2, z + 1, t + 1));
                    ImageProcessor b = image.getStack().getProcessor(image.getStackIndex(3, z + 1, t + 1));

                    int newPosition = t * image.getNSlices() + z;
                    ColorProcessor processor = new ColorProcessor(r.getWidth(), r.getHeight());
                    for (int y = 0; y < r.getHeight(); y++) {
                        for (int x = 0; x < r.getWidth(); x++) {
                            int rxy = r.get(x, y) << 16;
                            int gxy = g.get(x, y) << 8;
                            int bxy = b.get(x, y);
                            int rgb = rxy + gxy + bxy;
                            processor.set(x, y, rgb);
                        }
                    }

                    stack.setProcessor(processor, newPosition + 1);
                }
            }
            ImagePlus resultImage = new ImagePlus(image.getTitle() + "_RGB", stack);
            resultImage.setDimensions(1, image.getNSlices(), image.getNFrames());
            resultImage.copyScale(image);
            return resultImage;
        }
        return image;
    }

    public static LUT createGrayscaleLUTFromGradient(List<ColorUtils.GradientStop> stops) {
        stops.sort(Comparator.naturalOrder());
        if (stops.get(0).getPosition() > 0)
            stops.add(0, new ColorUtils.GradientStop(0, stops.get(0).getColor()));
        if (stops.get(stops.size() - 1).getPosition() < 1)
            stops.add(new ColorUtils.GradientStop(1, stops.get(stops.size() - 1).getColor()));
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];
        int currentFirstStop = 0;
        int currentLastStop = 1;
        int startIndex = 0;
        int endIndex = (int) (255 * stops.get(currentLastStop).getPosition());
        for (int i = 0; i < 256; i++) {
            if (i != 255 && i >= endIndex) {
                startIndex = i;
                ++currentFirstStop;
                ++currentLastStop;
                endIndex = (int) (255 * stops.get(currentLastStop).getPosition());
            }
            Color currentStart = stops.get(currentFirstStop).getColor();
            Color currentEnd = stops.get(currentLastStop).getColor();
            int r0 = currentStart.getRed();
            int g0 = currentStart.getGreen();
            int b0 = currentStart.getBlue();
            int r1 = currentEnd.getRed();
            int g1 = currentEnd.getGreen();
            int b1 = currentEnd.getBlue();
            int r = (int) (r0 + (r1 - r0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int g = (int) (g0 + (g1 - g0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int b = (int) (b0 + (b1 - b0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            reds[i] = (byte) ((r + g + b) / 3);
            greens[i] = (byte) ((r + g + b) / 3);
            blues[i] = (byte) ((r + g + b) / 3);
        }
        return new LUT(reds, greens, blues);
    }

    public static LUT createLUTFromGradient(List<ColorUtils.GradientStop> stops) {
//        MultipleGradientPaint paint = new LinearGradientPaint(0, 0, 128, 1, fractions, colors);
//        BufferedImage img = new BufferedImage(256,1, BufferedImage.TYPE_INT_RGB);
//        Graphics2D graphics = (Graphics2D) img.getGraphics();
//        graphics.setPaint(paint);
//        graphics.fillRect(0,0,256,1);
//        try {
//            ImageIO.write(img, "bmp", new File("lut.bmp"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        stops.sort(Comparator.naturalOrder());
        if (stops.get(0).getPosition() > 0)
            stops.add(0, new ColorUtils.GradientStop(0, stops.get(0).getColor()));
        if (stops.get(stops.size() - 1).getPosition() < 1)
            stops.add(new ColorUtils.GradientStop(1, stops.get(stops.size() - 1).getColor()));
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];
        int currentFirstStop = 0;
        int currentLastStop = 1;
        int startIndex = 0;
        int endIndex = (int) (255 * stops.get(currentLastStop).getPosition());
        for (int i = 0; i < 256; i++) {
            if (i != 255 && i >= endIndex) {
                startIndex = i;
                ++currentFirstStop;
                ++currentLastStop;
                endIndex = (int) (255 * stops.get(currentLastStop).getPosition());
            }
            Color currentStart = stops.get(currentFirstStop).getColor();
            Color currentEnd = stops.get(currentLastStop).getColor();
            int r0 = currentStart.getRed();
            int g0 = currentStart.getGreen();
            int b0 = currentStart.getBlue();
            int r1 = currentEnd.getRed();
            int g1 = currentEnd.getGreen();
            int b1 = currentEnd.getBlue();
            int r = (int) (r0 + (r1 - r0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int g = (int) (g0 + (g1 - g0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int b = (int) (b0 + (b1 - b0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            reds[i] = (byte) r;
            greens[i] = (byte) g;
            blues[i] = (byte) b;
        }
        return new LUT(reds, greens, blues);
    }

    public static Map<ImageSliceIndex, DisplayRange> readCalibration(ImagePlus imagePlus) {
        Map<ImageSliceIndex, DisplayRange> result = new HashMap<>();
        ImageJIterationUtils.forEachIndexedZCTSlice(imagePlus, (ip, index) -> {
            result.put(index, new DisplayRange(ip.getMin(), ip.getMax()));
        }, new JIPipeProgressInfo());
        return result;
    }

    public static void writeCalibration(ImagePlus imagePlus, Map<ImageSliceIndex, DisplayRange> calibrationMap) {
        ImageJIterationUtils.forEachIndexedZCTSlice(imagePlus, (ip, index) -> {
            DisplayRange displayRange = calibrationMap.getOrDefault(index, null);
            if (displayRange != null) {
                ip.setMinAndMax(displayRange.getDisplayedMin(), displayRange.getDisplayedMax());
            }
        }, new JIPipeProgressInfo());
    }

    public static Vector2dParameter calibrate(ImageProcessor imp, ImageJCalibrationMode calibrationMode, double customMin, double customMax, ImageStatistics stats) {
        double min = calibrationMode.getMin();
        double max = calibrationMode.getMax();
        if (calibrationMode == ImageJCalibrationMode.Custom) {
            min = customMin;
            max = customMax;
        } else if (calibrationMode == ImageJCalibrationMode.AutomaticImageJ) {
            int limit = stats.pixelCount / 10;
            int[] histogram = stats.histogram;
            int threshold = stats.pixelCount / 5000;
            int i = -1;
            boolean found = false;
            int count;
            do {
                i++;
                count = histogram[i];
                if (count > limit) count = 0;
                found = count > threshold;
            } while (!found && i < 255);
            int hmin = i;
            i = 256;
            do {
                i--;
                count = histogram[i];
                if (count > limit) count = 0;
                found = count > threshold;
            } while (!found && i > 0);
            int hmax = i;
            if (hmax >= hmin) {
                min = stats.histMin + hmin * stats.binSize;
                max = stats.histMin + hmax * stats.binSize;
                if (min == max) {
                    min = stats.min;
                    max = stats.max;
                }
            } else {
                int bitDepth = imp.getBitDepth();
                if (bitDepth == 16 || bitDepth == 32) {
                    min = imp.getMin();
                    max = imp.getMax();
                }
            }
        } else if (calibrationMode == ImageJCalibrationMode.MinMax) {
            min = stats.min;
            max = stats.max;
        }
        imp.setMinAndMax(min, max);
        return new Vector2dParameter(min, max);
    }

    public static void writeCalibrationToPixels(ImageProcessor ip, double min, double max) {
        if (ip instanceof ByteProcessor) {
            min = Math.max(0, min);
            max = Math.min(255, max);
            byte[] pixels = (byte[]) ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                int value = Byte.toUnsignedInt(pixels[i]);
                double k = (value - min) / (max - min);
                k = Math.max(Math.min(k, 1), 0);
                value = (int) (min + k * (max - min));
                pixels[i] = (byte) value;
            }
        } else if (ip instanceof ShortProcessor) {
            min = Math.max(0, min);
            max = Math.min(65535, max);
            short[] pixels = (short[]) ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                int value = Short.toUnsignedInt(pixels[i]);
                double k = (value - min) / (max - min);
                k = Math.max(Math.min(k, 1), 0);
                value = (int) (min + k * (max - min));
                pixels[i] = (short) value;
            }
        } else if (ip instanceof FloatProcessor) {
            if (Double.isInfinite(min) || Double.isInfinite(max)) {
                // Unable to resolve
                return;
            }
            float[] pixels = (float[]) ip.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                float value = pixels[i];
                if (!Float.isNaN(value)) {
                    double k = (value - min) / (max - min);
                    value = (float) (min + k * (max - min));
                    pixels[i] = value;
                }
            }
        } else if (ip instanceof ColorProcessor) {
            // Do nothing
        } else {
            throw new UnsupportedOperationException("Unsupported processor: " + ip);
        }
    }

    public static double[] calculateCalibration(ImageProcessor imp, ImageJCalibrationMode calibrationMode, double customMin, double customMax, ImageStatistics stats) {
        double min = calibrationMode.getMin();
        double max = calibrationMode.getMax();
        if (calibrationMode == ImageJCalibrationMode.Custom) {
            min = customMin;
            max = customMax;
        } else if (calibrationMode == ImageJCalibrationMode.AutomaticImageJ) {
            int limit = stats.pixelCount / 10;
            int[] histogram = stats.histogram;
            int threshold = stats.pixelCount / 5000;
            int i = -1;
            boolean found = false;
            int count;
            do {
                i++;
                count = histogram[i];
                if (count > limit) count = 0;
                found = count > threshold;
            } while (!found && i < 255);
            int hmin = i;
            i = 256;
            do {
                i--;
                count = histogram[i];
                if (count > limit) count = 0;
                found = count > threshold;
            } while (!found && i > 0);
            int hmax = i;
            if (hmax >= hmin) {
                min = stats.histMin + hmin * stats.binSize;
                max = stats.histMin + hmax * stats.binSize;
                if (min == max) {
                    min = stats.min;
                    max = stats.max;
                }
            } else {
                int bitDepth = imp.getBitDepth();
                if (bitDepth == 16 || bitDepth == 32) {
                    min = imp.getMin();
                    max = imp.getMax();
                }
            }
        } else if (calibrationMode == ImageJCalibrationMode.MinMax) {
            min = stats.min;
            max = stats.max;
        }
        return new double[]{min, max};
    }

    /**
     * Calibrates the image (all processors). Ported from {@link ij.plugin.frame.ContrastAdjuster}
     *
     * @param imp             the image
     * @param calibrationMode the calibration mode
     * @param customMin       custom min value (only used if calibrationMode is Custom)
     * @param customMax       custom max value (only used if calibrationMode is Custom)
     * @return the new min and max
     */
    public static Vector2dParameter calibrate(ImagePlus imp, ImageJCalibrationMode calibrationMode, double customMin, double customMax) {
        ImageProcessor ip = imp.getProcessor();
        Vector2dParameter result = ImageJUtils.calibrate(ip, calibrationMode, customMin, customMax, ip.getStats());
        if (imp.hasImageStack()) {
            imp.getImageStack().update(ip);
        }
        return result;
    }

    public static ImageStack expandImageStackCanvas(ImageStack stackOld, Color backgroundColor, int wNew, int hNew, int xOff, int yOff) {
        int nFrames = stackOld.getSize();
        ImageProcessor ipOld = stackOld.getProcessor(1);

        ImageStack stackNew = new ImageStack(wNew, hNew, stackOld.getColorModel());
        ImageProcessor ipNew;

        for (int i = 1; i <= nFrames; i++) {
            IJ.showProgress((double) i / nFrames);
            ipNew = ipOld.createProcessor(wNew, hNew);
            ipNew.setColor(backgroundColor);
            ipNew.fill();
            ipNew.insert(stackOld.getProcessor(i), xOff, yOff);
            stackNew.addSlice(stackOld.getSliceLabel(i), ipNew);
        }
        return stackNew;
    }

    public static ImageProcessor expandImageProcessorCanvas(ImageProcessor ipOld, Color backgroundColor, int wNew, int hNew, int xOff, int yOff) {
        ImageProcessor ipNew = ipOld.createProcessor(wNew, hNew);
        ipNew.setColor(backgroundColor);
        ipNew.fill();
        ipNew.insert(ipOld, xOff, yOff);
        return ipNew;
    }

    /**
     * Expands the canvas of an image
     *
     * @param imp             the image
     * @param backgroundColor the background color
     * @param newWidth        the new width
     * @param newHeight       the new height
     * @param anchor          the anchor that determines where to expand from
     * @return expanded image
     */
    public static ImagePlus expandImageCanvas(ImagePlus imp, Color backgroundColor, int newWidth, int newHeight, Anchor anchor) {
        int wOld = imp.getWidth();
        int hOld = imp.getHeight();
        int xOff, yOff;
        int xC = (newWidth - wOld) / 2;    // offset for centered
        int xR = (newWidth - wOld);        // offset for right
        int yC = (newHeight - hOld) / 2;    // offset for centered
        int yB = (newHeight - hOld);        // offset for bottom

        switch (anchor) {
            case TopLeft:    // TL
                xOff = 0;
                yOff = 0;
                break;
            case TopCenter:    // TC
                xOff = xC;
                yOff = 0;
                break;
            case TopRight:    // TR
                xOff = xR;
                yOff = 0;
                break;
            case CenterLeft: // CL
                xOff = 0;
                yOff = yC;
                break;
            case CenterCenter: // C
                xOff = xC;
                yOff = yC;
                break;
            case CenterRight:    // CR
                xOff = xR;
                yOff = yC;
                break;
            case BottomLeft: // BL
                xOff = 0;
                yOff = yB;
                break;
            case BottomCenter: // BC
                xOff = xC;
                yOff = yB;
                break;
            case BottomRight: // BR
                xOff = xR;
                yOff = yB;
                break;
            default: // center
                xOff = xC;
                yOff = yC;
                break;
        }
        if (imp.hasImageStack()) {
            ImagePlus resultImage = new ImagePlus(imp.getTitle() + "_Expanded", expandImageStackCanvas(imp.getImageStack(), backgroundColor, newWidth, newHeight, xOff, yOff));
            resultImage.copyScale(imp);
            return resultImage;
        } else {
            ImagePlus resultImage = new ImagePlus(imp.getTitle() + "_Expanded", expandImageProcessorCanvas(imp.getProcessor(), backgroundColor, newWidth, newHeight, xOff, yOff));
            resultImage.copyScale(imp);
            return resultImage;
        }
    }

    /**
     * Converts this image to the same type as the other one if needed
     *
     * @param reference the other image
     * @param doScaling apply scaling for greyscale conversions
     */
    public static ImagePlus convertToSameTypeIfNeeded(ImagePlus image, ImagePlus reference, boolean doScaling) {
        if (reference.getType() != image.getType()) {
            image = duplicate(image);
            ImageConverter converter = new ImageConverter(image);
            ImageConverter.setDoScaling(doScaling);
            if (reference.getType() == ImagePlus.GRAY8)
                converter.convertToGray8();
            else if (reference.getType() == ImagePlus.GRAY16)
                converter.convertToGray16();
            else if (reference.getType() == ImagePlus.GRAY32)
                converter.convertToGray32();
            else if (reference.getType() == ImagePlus.COLOR_RGB)
                converter.convertToRGB();
            else if (reference.getType() == ImagePlus.COLOR_256) {
                converter.convertToRGB();
                converter.convertRGBtoIndexedColor(256);
            }
        }
        return image;
    }

    public static void getMaskedPixels_Slow(ImageProcessor ip, ImageProcessor mask, List<Float> target) {
        byte[] maskBytes = mask != null ? (byte[]) mask.getPixels() : null;
        for (int i = 0; i < ip.getWidth() * ip.getHeight(); i++) {
            if (mask == null || Byte.toUnsignedInt(maskBytes[i]) > 0) {
                target.add(ip.getf(i));
            }
        }
    }

    public static void getMaskedPixels_8U(ImageProcessor ip, ImageProcessor mask, TByteArrayList target) {
        byte[] imageBytes = (byte[]) ip.getPixels();
        byte[] maskBytes = mask != null ? (byte[]) mask.getPixels() : null;
        for (int i = 0; i < imageBytes.length; i++) {
            if (mask == null || Byte.toUnsignedInt(maskBytes[i]) > 0) {
                target.add(imageBytes[i]);
            }
        }
    }

    public static void getMaskedPixels_16U(ImageProcessor ip, ImageProcessor mask, TShortArrayList target) {
        short[] imageBytes = (short[]) ip.getPixels();
        byte[] maskBytes = mask != null ? (byte[]) mask.getPixels() : null;
        for (int i = 0; i < imageBytes.length; i++) {
            if (mask == null || Byte.toUnsignedInt(maskBytes[i]) > 0) {
                target.add(imageBytes[i]);
            }
        }
    }

    public static void getMaskedPixels_32F(ImageProcessor ip, ImageProcessor mask, TFloatArrayList target) {
        float[] imageBytes = (float[]) ip.getPixels();
        byte[] maskBytes = mask != null ? (byte[]) mask.getPixels() : null;
        for (int i = 0; i < imageBytes.length; i++) {
            if (mask == null || Byte.toUnsignedInt(maskBytes[i]) > 0) {
                target.add(imageBytes[i]);
            }
        }
    }

    public static void writeImageToMovie(ImagePlus image, HyperstackDimension followedDimension, double fps, Path outputFile, AVICompression compression, int jpegQuality, JIPipeProgressInfo progressInfo) {
        ImageStack generatedStack = new ImageStack(image.getWidth(), image.getHeight());

        if (followedDimension == HyperstackDimension.Depth) {
            progressInfo.setMaxProgress(image.getNSlices());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int z = 0; z < image.getNSlices(); z++) {
                if (progressInfo.isCancelled())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("z = " + z);
                ImageProcessor ip = ImageJUtils.getSliceZero(image, 0, z, 0);
                generatedStack.addSlice(new ColorProcessor(ip.getBufferedImage()));
            }
        } else if (followedDimension == HyperstackDimension.Channel) {
            progressInfo.setMaxProgress(image.getNChannels());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int c = 0; c < image.getNChannels(); c++) {
                if (progressInfo.isCancelled())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("c = " + c);
                ImageProcessor ip = ImageJUtils.getSliceZero(image, c, 0, 0);
                generatedStack.addSlice(new ColorProcessor(ip.getBufferedImage()));
            }
        } else if (followedDimension == HyperstackDimension.Frame) {
            progressInfo.setMaxProgress(image.getNFrames());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int t = 0; t < image.getNFrames(); t++) {
                if (progressInfo.isCancelled())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("t = " + t);
                ImageProcessor ip = ImageJUtils.getSliceZero(image, 0, 0, t);
                generatedStack.addSlice(new ColorProcessor(ip.getBufferedImage()));
            }
        }

        ImagePlus combined = new ImagePlus("video", generatedStack);
        combined.getCalibration().fps = fps;
        progressInfo.log("Writing AVI with " + Math.round(combined.getCalibration().fps) + "FPS");
        AVI_Writer writer = new AVI_Writer();
        try {
            writer.writeImage(combined, outputFile.toString(), compression.getNativeValue(), jpegQuality);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeLUT(ImagePlus image, Set<Integer> channels) {
        ImageSliceIndex original = new ImageSliceIndex(image.getC(), image.getZ(), image.getT());
        for (int c = 0; c < image.getNChannels(); c++) {
            if (channels == null || channels.isEmpty() || channels.contains(c)) {
                for (int z = 0; z < image.getNSlices(); z++) {
                    for (int t = 0; t < image.getNFrames(); t++) {
                        image.setPosition(c + 1, z + 1, t + 1);
                        image.getProcessor().setLut(null);
                    }
                }
            }
        }

        image.setPosition(original.getC(), original.getZ(), original.getT());
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertToGreyscaleIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.GRAY8 &&
                image.getType() != ImagePlus.GRAY16 &&
                image.getType() != ImagePlus.GRAY32) {
            image = duplicate(image);
            ImageConverter.setDoScaling(true);
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray32();
        }
        return image;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertToGreyscale8UIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.GRAY8) {
            image = duplicate(image);
            ImageConverter.setDoScaling(true);
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray8();
        }
        return image;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertToGrayscale32FIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.GRAY32) {
            image = duplicate(image);
            ImageConverter.setDoScaling(true);
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray32();
        }
        return image;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertToGrayscale16UIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.GRAY16) {
            image = duplicate(image);
            ImageConverter.setDoScaling(true);
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray16();
        }
        return image;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertToColorRGBIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.COLOR_RGB) {
            String title = image.getTitle();
            image = duplicate(image);
            image.setTitle(title);
            ImageConverter.setDoScaling(true);
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
        }
        return image;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertToColorLABIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.COLOR_RGB) {
            String title = image.getTitle();
            image = duplicate(image);
            image.setTitle(title);
            ImageConverter.setDoScaling(true);
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
            convertRGBToLAB(image, new JIPipeProgressInfo());
        }
        return image;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertToColorHSBIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.COLOR_RGB) {
            // A copy is guaranteed here
            ImagePlus copy = convertToColorRGBIfNeeded(image);
            convertRGBToHSB(copy, new JIPipeProgressInfo());
            return copy;
        }
        // ImageJ does not differentiate between color spaces, so we cannot convert. The convertFrom() method will handle this correctly.
        return image;
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * If this function encounters a 3-channel 3D image, it will assume that it is an RGB image and convert it
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convert3ChannelToRGBIfNeeded(ImagePlus image) {
        image = channelsToRGB(image);
        return image;
    }

    public static void assertImageDimensions(ImagePlus image, int maxDimensions) {
        if (image.getNDimensions() > maxDimensions) {
            throw new JIPipeValidationRuntimeException(new IllegalArgumentException("Trying to fit higher-dimensional data into " + maxDimensions + "D data!"),
                    "Trying to fit higher-dimensional data into " + maxDimensions + "D data!",
                    image.getNDimensions() + "D data was supplied, but it was requested that it should fit into " + maxDimensions + "D data. " +
                            "This is not trivial. This can be caused by selecting the wrong data slot type or applying a conversion" +
                            " from N-dimensional data into data with a defined dimensionality.",
                    "Try to check if the data slots have the correct data types. You can also check the input of the offending algorithm via " +
                            "the quick run to see if they fit the assumptions. If you cannot find the reason behind this error," +
                            " try to contact the JIPipe or plugin developers.");
        }
    }

    public static void removeOverlay(ImagePlus image) {
        image.setOverlay(null);
    }

    /**
     * Converts an image into a LUT. LUT values are extracted from the first row.
     *
     * @param image the image. Must be RGB.
     * @return the lut
     */
    public static LUT lutFromImage(ImagePlus image) {
        byte[] rLut = new byte[256];
        byte[] gLut = new byte[256];
        byte[] bLut = new byte[256];
        ImageProcessor processor = image.getProcessor();
        for (int i = 0; i < 256; i++) {
            int lutIndex = (int) Math.floor(1.0 * i / 256 * image.getWidth());
            Color color = new Color(processor.get(lutIndex, 0));
            rLut[i] = (byte) color.getRed();
            gLut[i] = (byte) color.getGreen();
            bLut[i] = (byte) color.getBlue();
        }
        return new LUT(rLut, gLut, bLut);
    }

    /**
     * Converts a LUT into an RGB image
     *
     * @param lut    the lut
     * @param width  width of the image
     * @param height height of the image
     * @return the image
     */
    public static ImagePlus lutToImage(LUT lut, int width, int height) {
        ImagePlus img = IJ.createImage("LUT", width, height, 1, 24);
        ColorProcessor processor = (ColorProcessor) img.getProcessor();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int lutIndex = (int) Math.floor(1.0 * x / img.getWidth() * 256);
                processor.set(x, y, lut.getRGB(lutIndex));
            }
        }
        return img;
    }

    /**
     * Gets the slice closest to the requested slice index (zero-based).
     * If there are fewer slices than requested, the last slice is chosen
     *
     * @param image the image
     * @param index the slice index (zero-based)
     * @return the slice
     */
    public static ImageProcessor getClosestSliceZero(ImagePlus image, ImageSliceIndex index) {
        int z = Math.min(index.getZ(), image.getNSlices() - 1);
        int c = Math.min(index.getZ(), image.getNChannels() - 1);
        int t = Math.min(index.getZ(), image.getNFrames() - 1);
        return getSliceZero(image, c, z, t);
    }

    public static void assert4DImage(ImagePlus image) {
        assertImageDimensions(image, 4);
    }

    public static void assert3DImage(ImagePlus image) {
        assertImageDimensions(image, 3);
    }

    public static void assert2DImage(ImagePlus image) {
        assertImageDimensions(image, 2);
    }

    public static void assert5DImage(ImagePlus image) {
        assertImageDimensions(image, 5);
    }

    public static void drawCenteredStringLabel(ImagePlus imp, ImageProcessor ip, String label, Rectangle r, Color foreground, Color background, Font font, boolean drawBackground) {
        int size = font.getSize();
        ImageCanvas ic = imp.getCanvas();
        if (ic != null) {
            double mag = ic.getMagnification();
            if (mag < 1.0)
                size /= mag;
        }
        if (size == 9 && r.width > 50 && r.height > 50)
            size = 12;
        ip.setFont(new Font(font.getName(), font.getStyle(), size));
        int w = ip.getStringWidth(label);
        int x = r.x + r.width / 2 - w / 2;
        int y = r.y + r.height / 2 + Math.max(size / 2, 6);
        FontMetrics metrics = ip.getFontMetrics();
        int h = metrics.getHeight();
        if (drawBackground) {
            ip.setColor(background);
            ip.setRoi(x - 1, y - h + 2, w + 1, h - 3);
            ip.fill();
            ip.resetRoi();
        }
        ip.setColor(foreground);
        ip.drawString(label, x, y);
    }

    public static void drawAnchoredStringLabel(ImageProcessor ip, String label, Rectangle availableSpace, Color foreground, Color background, Font font, boolean drawBackground, Anchor anchor) {
        int fontSize = font.getSize();
        ip.setFont(new Font(font.getName(), font.getStyle(), fontSize));
        int stringWidth = ip.getStringWidth(label);
        final int x, y;
        int m = drawBackground ? 1 : 0;
        int fsc = Math.max(fontSize / 2, 6);
        FontMetrics metrics = ip.getFontMetrics();
        int h = metrics.getHeight();

        switch (anchor) {
            case TopLeft:
                x = availableSpace.x + m;
                y = availableSpace.y + m + h - 2;
                break;
            case TopCenter:
                x = availableSpace.x + availableSpace.width / 2 - stringWidth / 2;
                y = availableSpace.y + m + h - 2;
                break;
            case TopRight:
                x = availableSpace.x + availableSpace.width - stringWidth - m;
                y = availableSpace.y + m + h - 2;
                break;
            case CenterLeft:
                x = availableSpace.x + m;
                y = availableSpace.y + availableSpace.height / 2 + fsc;
                break;
            case CenterCenter:
                x = availableSpace.x + availableSpace.width / 2 - stringWidth / 2;
                y = availableSpace.y + availableSpace.height / 2 + fsc;
                break;
            case CenterRight:
                x = availableSpace.x + availableSpace.width - stringWidth - m;
                y = availableSpace.y + availableSpace.height / 2 + fsc;
                break;
            case BottomLeft:
                x = availableSpace.x + m;
                y = availableSpace.y + availableSpace.height - 1 - m;
                break;
            case BottomCenter:
                x = availableSpace.x + availableSpace.width / 2 - stringWidth / 2;
                y = availableSpace.y + availableSpace.height - 1 - m;
                break;
            case BottomRight:
                x = availableSpace.x + availableSpace.width - stringWidth - m;
                y = availableSpace.y + availableSpace.height - 1 - m;
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (drawBackground) {
            ip.setColor(background);
            ip.setRoi(x - 1, y - h + 2, stringWidth + 1, h - 3);
            ip.fill();
            ip.resetRoi();
        }
        ip.setColor(foreground);
        ip.drawString(label, x, y);
    }

    /**
     * Renders a non-RGB image to RGB, including LUT
     *
     * @param inputImage   the input image
     * @param progressInfo the progress info
     * @return the output image
     */
    public static ImagePlus renderToRGBWithLUTIfNeeded(ImagePlus inputImage, JIPipeProgressInfo progressInfo) {
        if (inputImage.getType() == ImagePlus.COLOR_RGB)
            return inputImage;
        ImageStack stack = new ImageStack(inputImage.getWidth(), inputImage.getHeight(), inputImage.getStackSize());
        ImageJIterationUtils.forEachIndexedZCTSlice(inputImage, (ip, slice) -> {
            ColorProcessor colorProcessor = new ColorProcessor(ip.getBufferedImage());
            stack.setProcessor(colorProcessor, slice.zeroSliceIndexToOneStackIndex(inputImage));
        }, progressInfo);
        ImagePlus outputImage = new ImagePlus(inputImage.getTitle(), stack);
        outputImage.copyScale(inputImage);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        return outputImage;
//        return ImageJUtils.convertToColorRGBIfNeeded(inputImage);
    }

    public static void convolveSlice(Convolver convolver, int kernelWidth, int kernelHeight, float[] kernel, ImageProcessor imp) {
        if (imp instanceof ColorProcessor) {
            // Split into channels and convolve individually
            FloatProcessor c0 = imp.toFloat(0, null);
            FloatProcessor c1 = imp.toFloat(1, null);
            FloatProcessor c2 = imp.toFloat(2, null);
            convolver.convolve(c0, kernel, kernelWidth, kernelHeight);
            convolver.convolve(c1, kernel, kernelWidth, kernelHeight);
            convolver.convolve(c2, kernel, kernelWidth, kernelHeight);
            imp.setPixels(0, c0);
            imp.setPixels(1, c1);
            imp.setPixels(2, c2);
        } else if (imp instanceof FloatProcessor) {
            convolver.convolve(imp, kernel, kernelWidth, kernelHeight);
        } else {
            // Convolve directly
            FloatProcessor c0 = imp.toFloat(0, null);
            convolver.convolve(c0, kernel, kernelWidth, kernelHeight);
            imp.setPixels(0, c0);
        }
    }

    public static int rgbPixelLerp(int A, int B, double opacity) {
        int rs = (A & 0xff0000) >> 16;
        int gs = (A & 0xff00) >> 8;
        int bs = A & 0xff;
        int rt = (B & 0xff0000) >> 16;
        int gt = (B & 0xff00) >> 8;
        int bt = B & 0xff;
        int r = Math.min(255, Math.max((int) ((1 - opacity) * rs + opacity * rt), 0));
        int g = Math.min(255, Math.max((int) ((1 - opacity) * gs + opacity * gt), 0));
        int b = Math.min(255, Math.max((int) ((1 - opacity) * bs + opacity * bt), 0));
        return b + (g << 8) + (r << 16);
    }

    public static ImagePlus unwrap(ImagePlusData data) {
        if (data != null) {
            return data.getImage();
        } else {
            return null;
        }
    }

    public static LUT[] getChannelLUT(ImagePlus image) {
        ImageSliceIndex original = new ImageSliceIndex(image.getC(), image.getZ(), image.getT());
        LUT[] luts = new LUT[image.getNChannels()];
        for (int c = 0; c < image.getNChannels(); c++) {
            if (image.isComposite()) {
                CompositeImage compositeImage = (CompositeImage) image;
                LUT lut = compositeImage.getChannelLut(c + 1);
                if (lut == null) {
                    luts[c] = LUT.createLutFromColor(Color.WHITE);
                } else {
                    luts[c] = lut;
                }
            } else {
                image.setPosition(c + 1, 1, 1);
                LUT lut = image.getProcessor().getLut();
                if (lut == null) {
                    luts[c] = LUT.createLutFromColor(Color.WHITE);
                } else {
                    luts[c] = lut;
                }
            }
        }
        image.setPosition(original.getC(), original.getZ(), original.getT());
        return luts;
    }

    public static void setChannelLUT(ImagePlus image, LUT[] luts) {
        for (int c = 0; c < image.getNChannels(); c++) {
            setLut(image, luts[c], Collections.singleton(c));
        }
    }

    public static void setLutFromColorMap(ImagePlus image, ColorMap colorMap, Set<Integer> channels) {
        LUT lut = colorMap.toLUT();
        setLut(image, lut, channels);
    }

    /**
     * Sets the lut of the specified channels
     * @param image the image
     * @param lut the LUT
     * @param channels the channels (zero-based). If empty or null, all channels will be modified.
     */
    public static void setLut(ImagePlus image, LUT lut, Set<Integer> channels) {
        // Standard LUT
        ImageSliceIndex original = new ImageSliceIndex(image.getC(), image.getZ(), image.getT());
        for (int c = 0; c < image.getNChannels(); c++) {
            if (channels == null || channels.isEmpty() || channels.contains(c)) {
                if (image.isComposite()) {
                    CompositeImage compositeImage = (CompositeImage) image;
                    compositeImage.setChannelLut(lut, c + 1);
                }
                for (int z = 0; z < image.getNSlices(); z++) {
                    for (int t = 0; t < image.getNFrames(); t++) {
                        image.setPosition(c + 1, z + 1, t + 1);
                        image.getProcessor().setLut(lut);
//                        getSliceZero(image, c, z, t).setLut(lut);
                    }
                }
            }
        }

        image.setPosition(original.getC(), original.getZ(), original.getT());
    }

    public static Map<ImageSliceIndex, ImageProcessor> splitIntoSlices(ImagePlus image) {
        Map<ImageSliceIndex, ImageProcessor> result = new HashMap<>();
        for (int c = 0; c < image.getNChannels(); c++) {
            for (int z = 0; z < image.getNSlices(); z++) {
                for (int t = 0; t < image.getNFrames(); t++) {
                    result.put(new ImageSliceIndex(c, z, t), getSliceZero(image, c, z, t));
                }
            }
        }
        return result;
    }

    public static Map<ImageSliceIndex, ImageProcessor> deduplicateSliceMappingByOverwriting(Map<ImageProcessor, ImageSliceIndex> sliceMappings, boolean silentlyOverwriteDuplicates) {
        Map<ImageSliceIndex, ImageProcessor> result = new HashMap<>();
        for (Map.Entry<ImageProcessor, ImageSliceIndex> entry : sliceMappings.entrySet()) {
            if (!silentlyOverwriteDuplicates && result.containsKey(entry.getValue())) {
                throw new UnsupportedOperationException("Duplicate image index: " + entry.getValue());
            }
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    public static ImageProcessor createProcessor(int width, int height, int bitDepth) {
        if (bitDepth == 8) {
            return new ByteProcessor(width, height);
        } else if (bitDepth == 16) {
            return new ShortProcessor(width, height);
        } else if (bitDepth == 32) {
            return new FloatProcessor(width, height);
        } else if (bitDepth == 24) {
            return new ColorProcessor(width, height);
        } else {
            throw new UnsupportedOperationException("Unsupported bitDepth: " + bitDepth);
        }
    }

    public static ImagePlus newBlankOf(ImagePlus reference) {
        return IJ.createHyperStack("Blank",
                reference.getWidth(),
                reference.getHeight(),
                reference.getNChannels(),
                reference.getNSlices(),
                reference.getNFrames(),
                reference.getBitDepth());
    }

    public static ImagePlus newBlankOf(ImagePlus reference, BitDepth bitDepth) {
        return IJ.createHyperStack("Blank",
                reference.getWidth(),
                reference.getHeight(),
                reference.getNChannels(),
                reference.getNSlices(),
                reference.getNFrames(),
                bitDepth.getBitDepth());
    }

    /**
     * Cropping as implemented in ImageJ.
     * Cannot handle negative X and Y in the rectangle
     *
     * @param img          the image
     * @param cropped      the cropping ROI
     * @param progressInfo the progress info
     * @return the cropped image
     */
    public static ImagePlus cropLegacy(ImagePlus img, Rectangle cropped, JIPipeProgressInfo progressInfo) {
        ImagePlus croppedImg;
        if (img.hasImageStack()) {
            ImageStack result = new ImageStack(cropped.width, cropped.height, img.getStackSize());
            ImageJIterationUtils.forEachIndexedZCTSlice(img, (imp, index) -> {
                imp.setRoi(cropped);
                ImageProcessor croppedImage = imp.crop();
                imp.resetRoi();
                result.setProcessor(croppedImage, index.zeroSliceIndexToOneStackIndex(img));
            }, progressInfo);
            croppedImg = new ImagePlus("Cropped", result);
            croppedImg.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
            croppedImg.copyScale(img);
        } else {
            ImageProcessor imp = img.getProcessor();
            imp.setRoi(cropped);
            ImageProcessor croppedImage = imp.crop();
            imp.resetRoi();
            croppedImg = new ImagePlus("Cropped", croppedImage);
            croppedImg.copyScale(img);
        }
        return croppedImg;
    }

    /**
     * Cropping that can handle negative X and Y
     *
     * @param img          the image
     * @param cropped      the cropping ROI
     * @param progressInfo the progress info
     * @return the cropped image
     */
    public static ImagePlus crop(ImagePlus img, Rectangle cropped, JIPipeProgressInfo progressInfo) {
        ImagePlus result = IJ.createHyperStack(img.getTitle(),
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                img.getBitDepth());

        result.copyScale(img);
        return result;
    }

    //    public static Roi makeBand(Roi roi, double size) {
//        int dxy = (int) (size * 1.5);
//        ROI2DListData listData = new ROI2DListData();
//        ImagePlus dummyImage = listData.createDummyImage();
//        dummyImage = expandImageCanvas(dummyImage, Color.BLACK, dummyImage.getWidth() + 2 * dxy,  dummyImage.getHeight() + 2 * dxy, Anchor.CenterCenter);
//
//        listData.toMask(true, true, 1, )
//    }

    public static ImageJHistogram computeGrayscaleHistogram(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int size = width * height;

        long[] histogram;
        double min, max;
        int value;

        if (ip instanceof ByteProcessor) {
            histogram = new long[256];
            min = 0;
            max = 255;
            for (int i = 0; i < size; i++) {
                value = ip.get(i) & 0xff;
                histogram[value]++;
            }
        } else if (ip instanceof ShortProcessor) {
            histogram = new long[65536];
            min = 0;
            max = 65535;
            for (int i = 0; i < size; i++) {
                value = ip.get(i) & 0xffff;
                histogram[value]++;
            }
        } else if (ip instanceof FloatProcessor) {
            float[] pixels = (float[]) ip.getPixels();
            min = Double.POSITIVE_INFINITY;
            max = Double.NEGATIVE_INFINITY;
            for (float f : pixels) {
                if (f < min) min = f;
                if (f > max) max = f;
            }

            if (min == max) {
                histogram = new long[1];
                histogram[0] = size;
            } else {
                histogram = new long[256];
                double scale = 255.0 / (max - min);
                for (float f : pixels) {
                    int bin = (int) ((f - min) * scale);
                    if (bin < 0) bin = 0;
                    if (bin > 255) bin = 255;
                    histogram[bin]++;
                }
            }
        } else if (ip instanceof ColorProcessor) {
            histogram = new long[256];
            min = 0;
            max = 255;
            for (int i = 0; i < size; i++) {
                int c = ((ColorProcessor) ip).get(i);
                int r = (c >> 16) & 0xff;
                int g = (c >> 8) & 0xff;
                int b = c & 0xff;
                int gray = (r + g + b) / 3;
                histogram[gray]++;
            }
        } else {
            throw new IllegalArgumentException("Unsupported ImageProcessor type: " + ip.getClass());
        }

        return new ImageJHistogram(min, max, histogram);
    }

    public static void copyAttributes(ImagePlus src, ImagePlus target) {
        target.copyAttributes(src);
    }

    public static void copyLUTs(ImagePlus src, ImagePlus target) {
        LUT[] luts = ImageJUtils.getChannelLUT(src);
        for (int c = 0; c < Math.min(luts.length, target.getNChannels()); c++) {
            setLut(target, luts[c], Collections.singleton(c));
        }
    }

    public static ImagePlus copyLUTsIfNeeded(ImagePlus img, ImagePlus projected) {
        if(img.isComposite()) {
            if(!projected.isComposite()) {
                projected = new CompositeImage(projected);
            }
            copyLUTs(img, projected);
        }
        else {
            copyLUTs(img, projected);
        }
        return projected;
    }
}

