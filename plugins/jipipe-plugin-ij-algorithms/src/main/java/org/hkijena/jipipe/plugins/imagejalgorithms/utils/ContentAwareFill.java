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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import gnu.trove.list.array.TDoubleArrayList;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

public class ContentAwareFill {

    public static void fillMean(ImageProcessor imageProcessor, ImageProcessor maskProcessor) {
        if (imageProcessor instanceof ColorProcessor) {
            double sumR = 0, sumG = 0, sumB = 0;
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) == 0) {
                    int rgb = imageProcessor.get(i);
                    int r = (rgb & 0xff0000) >> 16;
                    int g = (rgb & 0xff00) >> 8;
                    int b = rgb & 0xff;
                    sumR += r;
                    sumG += g;
                    sumB += b;
                }
            }
            int newR = (int) (sumR / imageProcessor.getPixelCount());
            int newG = (int) (sumG / imageProcessor.getPixelCount());
            int newB = (int) (sumB / imageProcessor.getPixelCount());
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) > 0) {
                    int rgb = newB + (newG << 8) + (newR << 16);
                    imageProcessor.set(i, rgb);
                }
            }
        } else {
            double sum = 0;
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) == 0) {
                    sum += imageProcessor.getf(i);
                }
            }
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) > 0) {
                    imageProcessor.setf(i, (float) (sum / imageProcessor.getPixelCount()));
                }
            }
        }
    }

    public static void fillMedian(ImageProcessor imageProcessor, ImageProcessor maskProcessor) {
        if (imageProcessor instanceof ColorProcessor) {
            TDoubleArrayList reds = new TDoubleArrayList(imageProcessor.getPixelCount());
            TDoubleArrayList greens = new TDoubleArrayList(imageProcessor.getPixelCount());
            TDoubleArrayList blues = new TDoubleArrayList(imageProcessor.getPixelCount());
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) == 0) {
                    int rgb = imageProcessor.get(i);
                    int r = (rgb & 0xff0000) >> 16;
                    int g = (rgb & 0xff00) >> 8;
                    int b = rgb & 0xff;
                    reds.add(r);
                    greens.add(g);
                    blues.add(b);
                }
            }
            reds.sort();
            greens.sort();
            blues.sort();
            int newR = (int) reds.get(reds.size() / 2);
            int newG = (int) greens.get(greens.size() / 2);
            int newB = (int) blues.get(blues.size() / 2);
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) > 0) {
                    int rgb = newB + (newG << 8) + (newR << 16);
                    imageProcessor.set(i, rgb);
                }
            }
        } else {
            TDoubleArrayList values = new TDoubleArrayList(imageProcessor.getPixelCount());
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) == 0) {
                    values.add(imageProcessor.getf(i));
                }
            }
            values.sort();
            float newValue = (float) values.get(values.size() / 2);
            for (int i = 0; i < imageProcessor.getPixelCount(); i++) {
                if (maskProcessor.get(i) > 0) {
                    imageProcessor.setf(i, newValue);
                }
            }
        }
    }

    /**
     * Simple content-aware fill that just takes the closest pixel
     *
     * @param imageProcessor the processor i/o
     * @param maskProcessor  the mask
     */
    public static void fillClosestPixel(ImageProcessor imageProcessor, ImageProcessor maskProcessor) {
        int width = imageProcessor.getWidth();
        int height = imageProcessor.getHeight();

        // Initialize the output image
        float[][] output = new float[width][height];
        boolean[][] visited = new boolean[width][height];

        Queue<Point> queue = new LinkedList<>();
        // Identify the boundary pixels of the mask
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (maskProcessor.get(x, y) != 0) { // Inside the mask
                    boolean isBoundary = false;

                    // Check neighbors
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = x + dx, ny = y + dy;
                            if (maskProcessor.get(nx, ny) == 0) { // Outside the mask
                                isBoundary = true;
                                break;
                            }
                        }
                        if (isBoundary) break;
                    }

                    if (isBoundary) {
                        queue.add(new Point(x, y));
                        visited[x][y] = true;
                        output[x][y] = imageProcessor.getf(x, y);
                    }
                }
            }
        }

        // Propagate the pixel values inside the mask
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            Point point = queue.poll();
            int px = point.x, py = point.y;
            float value = output[px][py];

            for (int i = 0; i < 4; i++) {
                int nx = px + dx[i], ny = py + dy[i];
                if (nx >= 1 && nx < width - 1 && ny >= 1 && ny < height - 1) {
                    if (!visited[nx][ny] && maskProcessor.get(nx, ny) != 0) {
                        output[nx][ny] = value;
                        visited[nx][ny] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }

        // Write the output back to the original ImageProcessor
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (maskProcessor.get(x, y) != 0) {
                    imageProcessor.setf(x, y, output[x][y]);
                }
            }
        }
    }
}
