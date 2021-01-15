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

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.PolygonRoi;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.parameters.roi.Anchor;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility functions for ImageJ
 */
public class ImageJUtils {
    private ImageJUtils() {

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
            return img.duplicate();
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
                ImagePlus imagePlus = img.duplicate();
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
        if (img.isStack()) {
            for (int i = 0; i < img.getStack().size(); ++i) {
                ImageProcessor ip = img.getStack().getProcessor(i + 1);
                progressInfo.resolveAndLog("Slice", i, img.getStackSize());
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
     * @param nChannels number of channels
     * @param nSlices   number of slices
     * @param nFrames   number of frames
     * @return one-based stack index
     */
    public static int getStackIndex(int channel, int slice, int frame, int nChannels, int nSlices, int nFrames) {
        if (channel < 1) channel = 1;
        if (channel > nChannels) channel = nChannels;
        if (slice < 1) slice = 1;
        if (slice > nSlices) slice = nSlices;
        if (frame < 1) frame = 1;
        if (frame > nFrames) frame = nFrames;
        return (frame - 1) * nChannels * nSlices + (slice - 1) * nChannels + channel;
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
            int index = getStackIndex(c, z, t, distinctC.size(), distinctZ.size(), distinctT.size()) - 1;
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
        if (img.isStack()) {
            for (int i = 0; i < img.getStack().size(); ++i) {
                ImageProcessor ip = img.getStack().getProcessor(i + 1);
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
     * @param function     the function. The indices are one-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZCTSlice(ImagePlus img, BiConsumer<ImageProcessor, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        if (img.isStack()) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {
                        int index = img.getStackIndex(c + 1, z + 1, t + 1);
                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        ImageProcessor processor = img.getImageStack().getProcessor(index);
                        function.accept(processor, new ImageSliceIndex(z, c, t));
                    }
                }
            }
        } else {
            function.accept(img.getProcessor(), new ImageSliceIndex(0, 0, 0));
        }
    }

    /**
     * Runs the function for each Z and T slice.
     * The function consumes a map from channel index to the channel slice.
     * The slice index channel is always set to -1
     *
     * @param img          the image
     * @param function     the function
     * @param progressInfo the progress
     */
    public static void forEachIndexedZTSlice(ImagePlus img, BiConsumer<Map<Integer, ImageProcessor>, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        int iterationIndex = 0;
        for (int t = 0; t < img.getNFrames(); t++) {
            for (int z = 0; z < img.getNSlices(); z++) {
                Map<Integer, ImageProcessor> channels = new HashMap<>();
                for (int c = 0; c < img.getNChannels(); c++) {
                    progressInfo.resolveAndLog("Slice", iterationIndex++, img.getStackSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                    channels.put(c, img.getStack().getProcessor(img.getStackIndex(c + 1, z + 1, t + 1)));
                }
                function.accept(channels, new ImageSliceIndex(z, -1, t));
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
        ImagePlus copy = img.duplicate();
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
        ImagePlus copy = img.duplicate();
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
            return new ImagePlus(image.getTitle() + "_RGB", stack);
        }
        return image;
    }

    public static LUT createLUTFromGradient(List<GradientStop> stops) {
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
        if (stops.get(0).fraction > 0)
            stops.add(0, new GradientStop(stops.get(0).getColor(), 0));
        if (stops.get(stops.size() - 1).fraction < 1)
            stops.add(new GradientStop(stops.get(stops.size() - 1).getColor(), 1));
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];
        int currentFirstStop = 0;
        int currentLastStop = 1;
        int startIndex = 0;
        int endIndex = (int) (255 * stops.get(currentLastStop).fraction);
        for (int i = 0; i < 256; i++) {
            if (i != 255 && i >= endIndex) {
                startIndex = i;
                ++currentFirstStop;
                ++currentLastStop;
                endIndex = (int) (255 * stops.get(currentLastStop).fraction);
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

    /**
     * Calibrates the image. Ported from {@link ij.plugin.frame.ContrastAdjuster}
     *
     * @param imp             the image
     * @param calibrationMode the calibration mode
     * @param customMin       custom min value (only used if calibrationMode is Custom)
     * @param customMax       custom max value (only used if calibrationMode is Custom)
     */
    public static void calibrate(ImagePlus imp, ImageJCalibrationMode calibrationMode, double customMin, double customMax) {
        double min = calibrationMode.getMin();
        double max = calibrationMode.getMax();
        if (calibrationMode == ImageJCalibrationMode.Custom) {
            min = customMin;
            max = customMax;
        } else if (calibrationMode == ImageJCalibrationMode.AutomaticImageJ) {
            ImageStatistics stats = imp.getRawStatistics();
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
                    imp.resetDisplayRange();
                    min = imp.getDisplayRangeMin();
                    max = imp.getDisplayRangeMax();
                }
            }
        } else if (calibrationMode == ImageJCalibrationMode.MinMax) {
            ImageStatistics stats = imp.getRawStatistics();
            min = stats.min;
            max = stats.max;
        }

        boolean rgb = imp.getType() == ImagePlus.COLOR_RGB;
        int channels = imp.getNChannels();
        if (channels != 7 && rgb)
            imp.setDisplayRange(min, max, channels);
        else
            imp.setDisplayRange(min, max);
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
        if (imp.isStack()) {
            return new ImagePlus(imp.getTitle() + "_Expanded", expandImageStackCanvas(imp.getImageStack(), backgroundColor, newWidth, newHeight, xOff, yOff));
        } else {
            return new ImagePlus(imp.getTitle() + "_Expanded", expandImageProcessorCanvas(imp.getProcessor(), backgroundColor, newWidth, newHeight, xOff, yOff));
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
            image = image.duplicate();
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

    public static class GradientStop implements Comparable<GradientStop> {
        private final Color color;
        private final float fraction;

        public GradientStop(Color color, float fraction) {
            this.color = color;
            this.fraction = fraction;
        }

        public Color getColor() {
            return color;
        }

        public float getFraction() {
            return fraction;
        }

        @Override
        public int compareTo(@NotNull GradientStop o) {
            return Float.compare(fraction, o.fraction);
        }
    }
}
