package org.hkijena.acaq5.extension.algorithms.segmenters;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.utils.Hough_Circle;

public class HoughSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private int minRadius = 7;
    private int maxRadius = 25;
    private int radiusIncrement = 1;
    private int minNumCircles = 0;
    private int maxNumCircles = 700;
    private double threshold = 0.6;
    private int resolution = 113;
    private double ratio = 1.0;
    private int bandwidth = 10;
    private int localRadius = 10;

    public HoughSegmenter() {
        super("Image", ACAQGreyscaleImageDataSlot.class,
                "Mask", ACAQMaskDataSlot.class);
    }

    private boolean outOfBounds(int width, int height, int y, int x) {
        if (x >= width) {
            return true;
        } else if (x <= 0) {
            return true;
        } else if (y >= height) {
            return true;
        } else {
            return y <= 0;
        }
    }

    private ImagePlus drawCircleMask(ImagePlus img, ResultsTable table) {

        int nCircles = (int)table.getValue("nCircles", 0);
        int width = img.getWidth();
        int height = img.getHeight();

        byte[] circleMaskPixels = new byte[width * height];
        for(int l = 0; l < nCircles; ++l) {
            int i = (int)table.getValue("X (" + img.getCalibration().getUnits() + ")", l);
            int j = (int)table.getValue("Y (" + img.getCalibration().getUnits() + ")", l);
            int radius = (int)table.getValue("Radius (" + img.getCalibration().getUnits() + ")", l);
            short ID = (short)(int)table.getValue("ID", l);
            float score = (float)(int)table.getValue("Score", l) / (float)this.resolution;
            int rSquared = radius * radius;

            for(int y = -1 * radius; y <= radius; ++y) {
                for(int x = -1 * radius; x <= radius; ++x) {
                    if (x * x + y * y <= rSquared) {
                        if (!this.outOfBounds(width, height, j + y, i + x)) {
                            circleMaskPixels[(j + y) * width + i + x] = (byte)255;
                        }
                    }
                }
            }
        }

        ImagePlus result = IJ.createImage("HoughCircles", width, height, 1, 8);
        result.getProcessor().setPixels(circleMaskPixels);

        return result;
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        // Apply Hough circle transform
        Hough_Circle hough_circle = new Hough_Circle();
        hough_circle.setParameters(minRadius,
                maxRadius,
                radiusIncrement,
                minNumCircles,
                maxNumCircles,
                threshold,
                resolution,
                ratio,
                bandwidth,
                localRadius,
                true,
                false,
                false,
                false,
                true,
                false,
                true,
                false);
        WindowManager.setTempCurrentImage(img);
        hough_circle.startTransform();
        WindowManager.setTempCurrentImage(null);

        // Draw the circles
        ResultsTable resultsTable = Analyzer.getResultsTable();
        ImagePlus result = drawCircleMask(img, resultsTable);

        getOutputSlot().setData(new ACAQMaskData(result));
    }
}