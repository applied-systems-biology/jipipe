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

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import com.google.common.collect.ImmutableList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import ij.*;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.filter.AVI_Writer;
import ij.plugin.filter.Convolver;
import ij.process.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TriConsumer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility functions for ImageJ
 */
public class ImageJUtils {

    /**
     * Returns the properties of a {@link Roi} as map
     *
     * @param roi the roi
     * @return the properties
     */
    public static Map<String, String> getRoiProperties(Roi roi) {
        Properties props = new Properties();
        String properties = roi.getProperties();
        if (!StringUtils.isNullOrEmpty(properties)) {
            try {
                InputStream is = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
                props.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put("" + entry.getKey(), "" + entry.getValue());
        }
        return map;
    }

    /**
     * Returns the persistent properties of a {@link ImagePlus} as map
     *
     * @param imagePlus the image
     * @return the properties
     */
    public static Map<String, String> getImageProperties(ImagePlus imagePlus) {
        HashMap<String, String> map = new HashMap<>();
        if(imagePlus.getImageProperties() != null) {
            for (Map.Entry<Object, Object> entry : imagePlus.getImageProperties().entrySet()) {
                map.put("" + entry.getKey(), "" + entry.getValue());
            }
        }
        return map;
    }

    /**
     * Sets the properties of a {@link Roi} from a map
     *
     * @param roi        the roi
     * @param properties the properties
     */
    public static void setRoiProperties(Roi roi, Map<String, String> properties) {
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            props.setProperty(StringUtils.nullToEmpty(entry.getKey()).replace(' ', '_').replace('=', '_').replace(':', '_'), entry.getValue());
        }
        try {
            StringWriter writer = new StringWriter();
            props.store(writer, null);
            writer.flush();
            roi.setProperties(writer.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the properties of a {@link ImagePlus} from a map
     *
     * @param imagePlus        the image
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
        IJ.showProgress(1.0);
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
        forEachSlice(img, ip -> {
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
        forEachSlice(img, ip -> {
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
        forEachSlice(img, ip -> {
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
        forEachSlice(img, ip -> {
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
            forEachIndexedZCTSlice(reference, (referenceProcessor, referenceIndex) -> {
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
                forEachSlice(img, ip -> {
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
                forEachSlice(img, ip -> {
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
                System.out.println(rotatedBoundingRectangle);
                int newWidth = rotatedBoundingRectangle.width;
                int newHeight = rotatedBoundingRectangle.height;

                // Expand the image
                ImagePlus imagePlus = expandImageCanvas(img, background, newWidth, newHeight, Anchor.CenterCenter);
                forEachSlice(imagePlus, ip -> {
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
                forEachSlice(imagePlus, ip -> {
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
     * Runs the function for each slice
     *
     * @param img          the image
     * @param function     the function
     * @param progressInfo the progress
     */
    public static void forEachSlice(ImagePlus img, Consumer<ImageProcessor> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            for (int i = 0; i < img.getImageStackSize(); ++i) {
                ImageProcessor ip = img.getImageStack().getProcessor(i + 1);
                progressInfo.resolveAndLog("Slice", i, img.getImageStackSize());
                function.accept(ip);
            }
        } else {
            function.accept(img.getProcessor());
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
    public static void copyBetweenImages(ImagePlus src, ImagePlus target, JIPipeProgressInfo progressInfo) {
        if (!ImageJUtils.imagesHaveSameSize(src, target) || src.getBitDepth() != target.getBitDepth()) {
            throw new IllegalArgumentException("Source and target must have same size and bit depth!");
        }
        ImageJUtils.forEachIndexedZCTSlice(target, (targetProcessor, index) -> {
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
     * Runs the function for each slice
     *
     * @param img          the image
     * @param function     the function
     * @param progressInfo the progress
     */
    public static void forEachIndexedSlice(ImagePlus img, BiConsumer<ImageProcessor, Integer> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            for (int i = 0; i < img.getStack().size(); ++i) {
                if (progressInfo.isCancelled())
                    return;
                ImageProcessor ip = img.getImageStack().getProcessor(i + 1);
                progressInfo.resolveAndLog("Slice", i, img.getStackSize());
                function.accept(ip, i);
            }
        } else {
            function.accept(img.getProcessor(), 0);
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZCTSlice(ImagePlus img, BiConsumer<ImageProcessor, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return;
                        int index = img.getStackIndex(c + 1, z + 1, t + 1);
                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        ImageProcessor processor = img.getImageStack().getProcessor(index);
                        function.accept(processor, new ImageSliceIndex(c, z, t));
                    }
                }
            }
        } else {
            function.accept(img.getProcessor(), new ImageSliceIndex(0, 0, 0));
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZCTSliceWithProgress(ImagePlus img, TriConsumer<ImageProcessor, ImageSliceIndex, JIPipeProgressInfo> function, JIPipeProgressInfo progressInfo) {
        if (img.hasImageStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return;
                        int index = img.getStackIndex(c + 1, z + 1, t + 1);
                        JIPipeProgressInfo stackProgress = progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).resolve("z=" + z + ", c=" + c + ", t=" + t);
                        ImageProcessor processor = img.getImageStack().getProcessor(index);
                        function.accept(processor, new ImageSliceIndex(c, z, t), stackProgress);
                    }
                }
            }
        } else {
            function.accept(img.getProcessor(), new ImageSliceIndex(0, 0, 0), progressInfo);
        }
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
     * Runs the function for each Z, C, and T slice.
     *
     * @param sourceImage  the image
     * @param function     the function. The indices are ZERO-based. Should return the result slice for this index
     * @param progressInfo the progress
     */
    public static ImagePlus generateForEachIndexedZCTSlice(ImagePlus sourceImage, BiFunction<ImageProcessor, ImageSliceIndex, ImageProcessor> function, JIPipeProgressInfo progressInfo) {
        if (sourceImage.hasImageStack()) {
            ImageStack stack = new ImageStack(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getStackSize());
            int iterationIndex = 0;
            for (int t = 0; t < sourceImage.getNFrames(); t++) {
                for (int z = 0; z < sourceImage.getNSlices(); z++) {
                    for (int c = 0; c < sourceImage.getNChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return null;
                        int index = sourceImage.getStackIndex(c + 1, z + 1, t + 1);
                        progressInfo.resolveAndLog("Slice", iterationIndex++, sourceImage.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        ImageProcessor processor = sourceImage.getImageStack().getProcessor(index);
                        ImageProcessor resultProcessor = function.apply(processor, new ImageSliceIndex(c, z, t));
                        stack.setProcessor(resultProcessor, index);
                    }
                }
            }
            ImagePlus resultImage = new ImagePlus(sourceImage.getTitle(), stack);
            resultImage.setDimensions(sourceImage.getNChannels(), sourceImage.getNSlices(), sourceImage.getNFrames());
            resultImage.copyScale(sourceImage);
            return resultImage;
        } else {
            ImageProcessor processor = function.apply(sourceImage.getProcessor(), new ImageSliceIndex(0, 0, 0));
            ImagePlus resultImage = new ImagePlus(sourceImage.getTitle(), processor);
            resultImage.copyScale(sourceImage);
            return resultImage;
        }
    }

    /**
     * Runs the function for each Z and T slice.
     * The function consumes a map from channel index to the channel slice.
     * The slice index channel is always set to -1
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    @Deprecated
    public static void forEachIndexedZTSlice(ImagePlus img, BiConsumer<Map<Integer, ImageProcessor>, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        int iterationIndex = 0;
        for (int t = 0; t < img.getNFrames(); t++) {
            for (int z = 0; z < img.getNSlices(); z++) {
                Map<Integer, ImageProcessor> channels = new HashMap<>();
                for (int c = 0; c < img.getNChannels(); c++) {
                    if (progressInfo.isCancelled())
                        return;
                    progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                    channels.put(c, img.getStack().getProcessor(img.getStackIndex(c + 1, z + 1, t + 1)));
                }
                function.accept(channels, new ImageSliceIndex(-1, z, t));
            }
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
        ImageJUtils.forEachIndexedZCTSlice(imagePlus, (ip, index) -> {
            result.put(index, new DisplayRange(ip.getMin(), ip.getMax()));
        }, new JIPipeProgressInfo());
        return result;
    }

    public static void writeCalibration(ImagePlus imagePlus, Map<ImageSliceIndex, DisplayRange> calibrationMap) {
        ImageJUtils.forEachIndexedZCTSlice(imagePlus, (ip, index) -> {
            DisplayRange displayRange = calibrationMap.getOrDefault(index, null);
            if(displayRange != null) {
                ip.setMinAndMax(displayRange.getDisplayedMin(), displayRange.getDisplayedMax());
            }
        }, new JIPipeProgressInfo());
    }

    public static void calibrate(ImageProcessor imp, ImageJCalibrationMode calibrationMode, double customMin, double customMax, ImageStatistics stats) {
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
     */
    public static void calibrate(ImagePlus imp, ImageJCalibrationMode calibrationMode, double customMin, double customMax) {
        if(imp.getType() == ImagePlus.COLOR_RGB)
            return;
        ImageProcessor ip = imp.getProcessor();
        ImageJUtils.calibrate(ip, calibrationMode, customMin, customMax, ip.getStats());
        if(imp.hasImageStack()) {
          imp.getImageStack().update(ip);
        }
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

    public static void writeImageToMovie(ImagePlus image, HyperstackDimension followedDimension, int timePerFrame, Path outputFile, AVICompression compression, int jpegQuality, JIPipeProgressInfo progressInfo) {
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
        combined.getCalibration().fps = 1.0 / timePerFrame * 1000;
        progressInfo.log("Writing AVI with " + Math.round(combined.getCalibration().fps) + "FPS");
        AVI_Writer writer = new AVI_Writer();
        try {
            writer.writeImage(combined, outputFile.toString(), compression.getNativeValue(), jpegQuality);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeLUT(ImagePlus image, boolean applyToAllPlanes) {
        if (applyToAllPlanes && image.hasImageStack()) {
            ImageSliceIndex original = new ImageSliceIndex(image.getC(), image.getZ(), image.getT());
            for (int z = 0; z < image.getNSlices(); z++) {
                for (int c = 0; c < image.getNChannels(); c++) {
                    for (int t = 0; t < image.getNFrames(); t++) {
                        image.setPosition(c, z, t);
                        image.getProcessor().setLut(null);
                    }
                }
            }
            image.setPosition(original.getC(), original.getZ(), original.getT());
        } else {
            image.getProcessor().setLut(null);
        }
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
            throw new UserFriendlyRuntimeException(new IllegalArgumentException("Trying to fit higher-dimensional data into " + maxDimensions + "D data!"),
                    "Trying to fit higher-dimensional data into " + maxDimensions + "D data!",
                    "ImageJ integration internals",
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

    public static void drawStringLabel(ImagePlus imp, ImageProcessor ip, String label, Rectangle r, Color foreground, Color background, Font font, boolean drawBackground) {
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

    /**
     * Uses reflection to manually set the canvas of a {@link Roi}
     * Please note that the image of the Roi will be set.
     *
     * @param roi       the ROI
     * @param imagePlus the image
     * @param canvas    the canvas
     */
    public static void setRoiCanvas(Roi roi, ImagePlus imagePlus, ImageCanvas canvas) {
        // First set the image
        roi.setImage(imagePlus);
        // We have to set the canvas or overlay rendering will fail
        try {
            Field field = Roi.class.getDeclaredField("ic");
            field.setAccessible(true);
            field.set(roi, canvas);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
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
        forEachIndexedZCTSlice(inputImage, (ip, slice) -> {
            ColorProcessor colorProcessor = new ColorProcessor(ip.getBufferedImage());
            stack.setProcessor(colorProcessor, slice.zeroSliceIndexToOneStackIndex(inputImage));
        }, progressInfo);
        ImagePlus outputImage = new ImagePlus(inputImage.getTitle(), stack);
        outputImage.copyScale(inputImage);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        return outputImage;
    }

    public static void convolveSlice(Convolver convolver, int kernelWidth, int kernelHeight, float[] kernel, ImageProcessor imp) {
        if(imp instanceof ColorProcessor) {
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
        }
        else if(imp instanceof FloatProcessor) {
            convolver.convolve(imp, kernel, kernelWidth, kernelHeight);
        }
        else {
            // Convolve directly
            FloatProcessor c0 = imp.toFloat(0, null);
            convolver.convolve(c0, kernel, kernelWidth, kernelHeight);
            imp.setPixels(0, c0);
        }
    }
}

