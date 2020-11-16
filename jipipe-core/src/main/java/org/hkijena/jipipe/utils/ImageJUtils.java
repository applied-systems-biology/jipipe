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

package org.hkijena.jipipe.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

public class ImageJUtils {
    private ImageJUtils() {

    }

    /**
     * Convert a RGB image to a grayscale image where the RGB channels are split into individual planes
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
                            rProcessor.set(x,y,r);
                            gProcessor.set(x,y,g);
                            bProcessor.set(x,y,b);
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
        if (image.getNDimensions() == 3 && image.getNChannels() == 3) {
            return new ImagePlus(image.getTitle() + "_RGB", image.getBufferedImage());
        } else if (image.getNChannels() == 3) {
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
        public int compareTo(@NotNull ImageJUtils.GradientStop o) {
            return Float.compare(fraction, o.fraction);
        }
    }
}
