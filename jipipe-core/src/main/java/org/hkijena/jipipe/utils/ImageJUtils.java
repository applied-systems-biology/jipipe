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
import ij.process.ImageStatistics;
import ij.process.LUT;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

public class ImageJUtils {
    private ImageJUtils() {

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
        if(stops.get(0).fraction > 0)
            stops.add(0, new GradientStop(stops.get(0).getColor(), 0));
        if(stops.get(stops.size() - 1).fraction < 1)
            stops.add(new GradientStop(stops.get(stops.size() - 1).getColor(), 1));
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];
        int currentFirstStop = 0;
        int currentLastStop = 1;
        int startIndex = 0;
        int endIndex = (int)(255 * stops.get(currentLastStop).fraction);
        for (int i = 0; i < 256; i++) {
            if(i != 255 && i >= endIndex) {
                startIndex = i;
                ++currentFirstStop;
                ++currentLastStop;
                endIndex = (int)(255 * stops.get(currentLastStop).fraction);
            }
            Color currentStart = stops.get(currentFirstStop).getColor();
            Color currentEnd = stops.get(currentLastStop).getColor();
            int r0 = currentStart.getRed();
            int g0 = currentStart.getGreen();
            int b0 = currentStart.getBlue();
            int r1 = currentEnd.getRed();
            int g1 = currentEnd.getGreen();
            int b1 = currentEnd.getBlue();
            int r = (int)(r0 + (r1 - r0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int g = (int)(g0 + (g1 - g0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int b = (int)(b0 + (b1 - b0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            reds[i] = (byte)r;
            greens[i] = (byte)g;
            blues[i] = (byte)b;
        }
        return new LUT(reds, greens, blues);
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
}
