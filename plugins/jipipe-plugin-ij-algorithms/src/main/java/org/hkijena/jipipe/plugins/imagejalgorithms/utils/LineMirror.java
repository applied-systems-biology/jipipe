package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import ij.process.*;

public class LineMirror {

    public static void mirrorImage(ImageProcessor ip, int x1, int y1, int x2, int y2, MirrorOperationMode mode) {
        if (y1 == y2) {
            mirrorHorizontal(ip, y1, mode);
        } else if (x1 == x2) {
            mirrorVertical(ip, x1, mode);
        } else {
            mirrorAny(ip, x1, y1, x2, y2, mode);
        }
    }

    private static void mirrorHorizontal(ImageProcessor ip, int mirrorY, MirrorOperationMode mode) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        for (int y = 0; y < height; y++) {
            int mirrorYPos = 2 * mirrorY - y;
            if (mirrorYPos >= 0 && mirrorYPos < height) {
                for (int x = 0; x < width; x++) {
                    handleMirror(ip, x, y, x, mirrorYPos, mode);
                }
            }
        }
    }

    private static void mirrorVertical(ImageProcessor ip, int mirrorX, MirrorOperationMode mode) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mirrorXPos = 2 * mirrorX - x;
                if (mirrorXPos >= 0 && mirrorXPos < width) {
                    handleMirror(ip, x, y, mirrorXPos, y, mode);
                }
            }
        }
    }

    private static void mirrorAny(ImageProcessor ip, int x1, int y1, int x2, int y2, MirrorOperationMode mode) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        // Calculate line coefficients: ax + by + c = 0
        double a = y2 - y1;
        double b = x1 - x2;
        double c = x2 * y1 - x1 * y2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Reflect (x, y) across the line
                double scale = (a * x + b * y + c) / (a * a + b * b);
                int rx = (int) Math.round(x - 2 * a * scale);
                int ry = (int) Math.round(y - 2 * b * scale);

                if (rx >= 0 && rx < width && ry >= 0 && ry < height) {
                    handleMirror(ip, x, y, rx, ry, mode);
                }
            }
        }
    }

    private static void handleMirror(ImageProcessor ip, int x1, int y1, int x2, int y2, MirrorOperationMode mode) {
        if (ip instanceof ByteProcessor || ip instanceof ShortProcessor || ip instanceof FloatProcessor) {
            handleGrayscaleMirror(ip, x1, y1, x2, y2, mode);
        } else if (ip instanceof ColorProcessor) {
            handleColorMirror((ColorProcessor) ip, x1, y1, x2, y2, mode);
        } else {
            throw new IllegalArgumentException("Unsupported ImageProcessor type: " + ip.getClass().getName());
        }
    }

    private static void handleGrayscaleMirror(ImageProcessor ip, int x1, int y1, int x2, int y2, MirrorOperationMode mode) {
        int value1 = ip.get(x1, y1);
        int value2 = ip.get(x2, y2);

        switch (mode) {
            case AboveOrLeft:
                ip.set(x2, y2, value1);
                break;
            case BelowOrRight:
                ip.set(x1, y1, value2);
                break;
            case Max:
                int maxValue = Math.max(value1, value2);
                ip.set(x1, y1, maxValue);
                ip.set(x2, y2, maxValue);
                break;
            case Min:
                int minValue = Math.min(value1, value2);
                ip.set(x1, y1, minValue);
                ip.set(x2, y2, minValue);
                break;
        }
    }

    private static void handleColorMirror(ColorProcessor ip, int x1, int y1, int x2, int y2, MirrorOperationMode mode) {
        int rgb1 = ip.get(x1, y1);
        int rgb2 = ip.get(x2, y2);

        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        switch (mode) {
            case AboveOrLeft:
                ip.set(x1, y1, rgb2);
                break;
            case BelowOrRight:
                ip.set(x2, y2, rgb1);
                break;
            case Max:
                int maxR = Math.max(r1, r2);
                int maxG = Math.max(g1, g2);
                int maxB = Math.max(b1, b2);
                int maxRGB = (maxR << 16) | (maxG << 8) | maxB;
                ip.set(x1, y1, maxRGB);
                ip.set(x2, y2, maxRGB);
                break;
            case Min:
                int minR = Math.min(r1, r2);
                int minG = Math.min(g1, g2);
                int minB = Math.min(b1, b2);
                int minRGB = (minR << 16) | (minG << 8) | minB;
                ip.set(x1, y1, minRGB);
                ip.set(x2, y2, minRGB);
                break;
        }
    }


    public enum MirrorOperationMode {
        Max, Min, AboveOrLeft, BelowOrRight
    }
}

