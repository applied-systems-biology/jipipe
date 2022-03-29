package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

/**
 * Copy of {@link ij.plugin.Slicer} that has a proper API
 */
public class Reslicer {
    private Direction startAt = Direction.Top;
    private boolean rotate;
    private boolean flip;
    private int sliceCount = 1;
    private boolean noInterpolate = Prefs.avoidResliceInterpolation;
    private double inputZSpacing = 1.0;
    private double outputZSpacing = 1.0;
    private int outputSlices = 1;
    private boolean noRoi;
    private boolean rgb, notFloat;
    private ImagePlus imp;
    private double gx1, gy1, gx2, gy2, gLength;

    // Variables used by getIrregularProfile and doIrregularSetup
    private int n;
    private double[] x;
    private double[] y;
    private int xbase;
    private int ybase;
    private double length;
    private double[] segmentLengths;
    private double[] dx;
    private double[] dy;

    public ImagePlus reslice(ImagePlus imp) {
        rgb = imp.getType() == ImagePlus.COLOR_RGB;
        notFloat = !rgb && imp.getType() != ImagePlus.GRAY32;
        ImagePlus imp2;
        if (imp.isHyperStack())
            imp2 = resliceHyperstack(imp);
        else
            imp2 = resliceStack(imp);
        ImageProcessor ip = imp.getProcessor();
        double min = ip.getMin();
        double max = ip.getMax();
        if (!rgb) imp2.getProcessor().setMinAndMax(min, max);
        if (noRoi)
            imp.deleteRoi();
        else
            imp.draw();
        return imp2;
    }

    private ImagePlus resliceStack(ImagePlus imp) {
        ImagePlus imp2;
        Roi roi = imp.getRoi();
        int roiType = roi != null ? roi.getType() : 0;
        Calibration origCal = imp.getCalibration();
        boolean globalCalibration = false;
        if (noInterpolate) {// temporarily clear spatial calibration
            globalCalibration = imp.getGlobalCalibration() != null;
            imp.setGlobalCalibration(null);
            Calibration tmpCal = origCal.copy();
            tmpCal.pixelWidth = 1.0;
            tmpCal.pixelHeight = 1.0;
            tmpCal.pixelDepth = 1.0;
            imp.setCalibration(tmpCal);
            inputZSpacing = 1.0;
            if (roiType != Roi.LINE)
                outputZSpacing = 1.0;
        }
        double zSpacing = inputZSpacing / imp.getCalibration().pixelWidth;
        if (roi == null || roiType == Roi.RECTANGLE || roiType == Roi.LINE) {
            imp2 = resliceRectOrLine(imp);
        } else {// we assert roiType==Roi.POLYLINE || roiType==Roi.FREELINE
            ImageProcessor ip2 = getSlice(imp, 0.0, 0.0, 0.0, 0.0);
            imp2 = new ImagePlus("Reslice of " + imp.getShortTitle(), ip2);
        }
        if (noInterpolate) { // restore calibration
            if (globalCalibration)
                imp.setGlobalCalibration(origCal);
            imp.setCalibration(origCal);
        }
        // create Calibration for new stack
        // start from previous cal and swap appropriate fields
        boolean horizontal = false;
        boolean vertical = false;
        if (roi == null || roiType == Roi.RECTANGLE) {
            if (startAt == Direction.Top || startAt == Direction.Bottom)
                horizontal = true;
            else
                vertical = true;
        }
        if (roi != null && roiType == Roi.LINE) {
            Line l = (Line) roi;
            horizontal = (l.y2 - l.y1) == 0;
            vertical = (l.x2 - l.x1) == 0;
        }
        if (imp2 == null) return null;
        imp2.setCalibration(imp.getCalibration());
        Calibration cal = imp2.getCalibration();
        if (horizontal) {
            cal.pixelWidth = origCal.pixelWidth;
            cal.pixelHeight = origCal.pixelDepth / zSpacing;
            cal.pixelDepth = origCal.pixelHeight * outputZSpacing;
        } else if (vertical) {
            cal.pixelWidth = origCal.pixelHeight;
            cal.pixelHeight = origCal.pixelDepth / zSpacing;
            //cal.pixelWidth = origCal.pixelDepth/zSpacing;
            //cal.pixelHeight = origCal.pixelHeight;
            cal.pixelDepth = origCal.pixelWidth * outputZSpacing;
            ;
        } else { // oblique line, polyLine or freeline
            if (origCal.pixelHeight == origCal.pixelWidth) {
                cal.pixelWidth = origCal.pixelWidth;
                cal.pixelHeight = origCal.pixelDepth / zSpacing;
                cal.pixelDepth = origCal.pixelWidth * outputZSpacing;
            } else {
                cal.pixelWidth = cal.pixelHeight = cal.pixelDepth = 1.0;
                cal.setUnit("pixel");
            }
        }
        double tmp;
        if (rotate) {// if rotated flip X and Y
            tmp = cal.pixelWidth;
            cal.pixelWidth = cal.pixelHeight;
            cal.pixelHeight = tmp;
        }
        return imp2;
    }

    public boolean isNoInterpolate() {
        return noInterpolate;
    }

    public void setNoInterpolate(boolean noInterpolate) {
        this.noInterpolate = noInterpolate;
    }

    public boolean isFlip() {
        return flip;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public boolean isRotate() {
        return rotate;
    }

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
    }

    ImagePlus resliceHyperstack(ImagePlus imp) {
        int channels = imp.getNChannels();
        int slices = imp.getNSlices();
        int frames = imp.getNFrames();
        if (slices == 1)
            return resliceTimeLapseHyperstack(imp);
        int c1 = imp.getChannel();
        int z1 = imp.getSlice();
        int t1 = imp.getFrame();
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImagePlus imp2 = null;
        ImageStack stack2 = null;
        Roi roi = imp.getRoi();
        for (int t = 1; t <= frames; t++) {
            for (int c = 1; c <= channels; c++) {
                ImageStack tmp1Stack = new ImageStack(width, height);
                for (int z = 1; z <= slices; z++) {
                    imp.setPositionWithoutUpdate(c, z, t);
                    tmp1Stack.addSlice(null, imp.getProcessor());
                }
                ImagePlus tmp1 = new ImagePlus("tmp", tmp1Stack);
                tmp1.setCalibration(imp.getCalibration());
                tmp1.setRoi(roi);
                ImagePlus tmp2 = resliceStack(tmp1);
                int slices2 = tmp2.getStackSize();
                if (imp2 == null) {
                    imp2 = tmp2.createHyperStack("Reslice of " + imp.getTitle(), channels, slices2, frames, tmp2.getBitDepth());
                    stack2 = imp2.getStack();
                }
                ImageStack tmp2Stack = tmp2.getStack();
                for (int z = 1; z <= slices2; z++) {
                    imp.setPositionWithoutUpdate(c, z, t);
                    int n2 = imp2.getStackIndex(c, z, t);
                    stack2.setPixels(tmp2Stack.getPixels(z), n2);
                }
            }
        }
        imp.setPosition(c1, z1, t1);
        if (channels > 1 && imp.isComposite()) {
            imp2 = new CompositeImage(imp2, ((CompositeImage) imp).getMode());
            ((CompositeImage) imp2).copyLuts(imp);
        }
        return imp2;
    }

    ImagePlus resliceTimeLapseHyperstack(ImagePlus imp) {
        int channels = imp.getNChannels();
        int frames = imp.getNFrames();
        int c1 = imp.getChannel();
        int t1 = imp.getFrame();
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImagePlus imp2 = null;
        ImageStack stack2 = null;
        Roi roi = imp.getRoi();
        int z = 1;
        for (int c = 1; c <= channels; c++) {
            ImageStack tmp1Stack = new ImageStack(width, height);
            for (int t = 1; t <= frames; t++) {
                imp.setPositionWithoutUpdate(c, z, t);
                tmp1Stack.addSlice(null, imp.getProcessor());
            }
            ImagePlus tmp1 = new ImagePlus("tmp", tmp1Stack);
            tmp1.setCalibration(imp.getCalibration());
            tmp1.setRoi(roi);
            ImagePlus tmp2 = resliceStack(tmp1);
            int frames2 = tmp2.getStackSize();
            if (imp2 == null) {
                imp2 = tmp2.createHyperStack("Reslice of " + imp.getTitle(), channels, 1, frames2, tmp2.getBitDepth());
                stack2 = imp2.getStack();
            }
            ImageStack tmp2Stack = tmp2.getStack();
            for (int t = 1; t <= frames2; t++) {
                imp.setPositionWithoutUpdate(c, z, t);
                int n2 = imp2.getStackIndex(c, z, t);
                stack2.setPixels(tmp2Stack.getPixels(z), n2);
            }
        }
        imp.setPosition(c1, 1, t1);
        if (channels > 1 && imp.isComposite()) {
            imp2 = new CompositeImage(imp2, ((CompositeImage) imp).getMode());
            ((CompositeImage) imp2).copyLuts(imp);
        }
        return imp2;
    }

    public Direction getStartAt() {
        return startAt;
    }

    public void setStartAt(Direction startAt) {
        this.startAt = startAt;
    }

    void saveLineInfo(Roi roi) {
        Line line = (Line) roi;
        gx1 = line.x1;
        gy1 = line.y1;
        gx2 = line.x2;
        gy2 = line.y2;
        gLength = line.getRawLength();
    }

    ImagePlus resliceRectOrLine(ImagePlus imp) {
        double x1 = 0.0;
        double y1 = 0.0;
        double x2 = 0.0;
        double y2 = 0.0;
        double xInc = 0.0;
        double yInc = 0.0;
        noRoi = false;

        Roi roi = imp.getRoi();
        if (roi == null) {
            noRoi = true;
            imp.setRoi(0, 0, imp.getWidth(), imp.getHeight());
            roi = imp.getRoi();
        }
        if (roi.getType() == Roi.RECTANGLE) {
            Rectangle r = roi.getBounds();
            if (startAt == Direction.Top) { // top
                x1 = r.x;
                y1 = r.y;
                x2 = r.x + r.width;
                y2 = r.y;
                xInc = 0.0;
                yInc = outputZSpacing;
                outputSlices = (int) (r.height / outputZSpacing);
            } else if (startAt == Direction.Left) { // left
                x1 = r.x;
                y1 = r.y;
                x2 = r.x;
                y2 = r.y + r.height;
                xInc = outputZSpacing;
                yInc = 0.0;
                outputSlices = (int) (r.width / outputZSpacing);
            } else if (startAt == Direction.Bottom) { // bottom
                x1 = r.x;
                y1 = r.y + r.height - 1;
                x2 = r.x + r.width;
                y2 = r.y + r.height - 1;
                xInc = 0.0;
                yInc = -outputZSpacing;
                outputSlices = (int) (r.height / outputZSpacing);
            } else if (startAt == Direction.Right) { // right
                x1 = r.x + r.width - 1;
                y1 = r.y;
                x2 = r.x + r.width - 1;
                y2 = r.y + r.height;
                xInc = -outputZSpacing;
                yInc = 0.0;
                outputSlices = (int) (r.width / outputZSpacing);
            }
        } else if (roi.getType() == Roi.LINE) {
            Line line = (Line) roi;
            x1 = line.x1;
            y1 = line.y1;
            x2 = line.x2;
            y2 = line.y2;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double nrm = Math.sqrt(dx * dx + dy * dy) / outputZSpacing;
            xInc = -(dy / nrm);
            yInc = (dx / nrm);
        } else
            return null;

        if (outputSlices == 0) {
            IJ.error("Reslicer", "Output Z spacing (" + IJ.d2s(outputZSpacing, 0) + " pixels) is too large.\n"
                    + "Is the voxel size in Image>Properties correct?.");
            return null;
        }
        boolean virtualStack = imp.getStack().isVirtual();
        String status = null;
        ImagePlus imp2 = null;
        ImageStack stack2 = null;
        boolean isStack = imp.getStackSize() > 1;
        IJ.resetEscape();
        for (int i = 0; i < outputSlices; i++) {
            if (virtualStack)
                status = outputSlices > 1 ? (i + 1) + "/" + outputSlices + ", " : "";
            ImageProcessor ip = getSlice(imp, x1, y1, x2, y2);
            //IJ.log(i+" "+x1+" "+y1+" "+x2+" "+y2+"   "+ip);
            if (isStack) drawLine(x1, y1, x2, y2, imp);
            if (stack2 == null) {
                stack2 = createOutputStack(imp, ip);
                if (stack2 == null || stack2.getSize() < outputSlices) return null; // out of memory
            }
            stack2.setPixels(ip.getPixels(), i + 1);
            x1 += xInc;
            x2 += xInc;
            y1 += yInc;
            y2 += yInc;
            if (IJ.escapePressed()) {
                IJ.beep();
                imp.draw();
                return null;
            }
        }
        return new ImagePlus("Reslice of " + imp.getShortTitle(), stack2);
    }

    ImageStack createOutputStack(ImagePlus imp, ImageProcessor ip) {
        int bitDepth = imp.getBitDepth();
        int w2 = ip.getWidth(), h2 = ip.getHeight(), d2 = outputSlices;
        int flags = NewImage.FILL_BLACK + NewImage.CHECK_AVAILABLE_MEMORY;
        ImagePlus imp2 = NewImage.createImage("temp", w2, h2, d2, bitDepth, flags);
        if (imp2 == null)
            return null;
        else {
            ImageStack stack2 = imp2.getStack();
            stack2.setColorModel(ip.getColorModel());
            return stack2;
        }
    }

    ImageProcessor getSlice(ImagePlus imp, double x1, double y1, double x2, double y2) {
        Roi roi = imp.getRoi();
        int roiType = roi != null ? roi.getType() : 0;
        ImageStack stack = imp.getStack();
        int stackSize = stack.size();
        ImageProcessor ip, ip2 = null;
        float[] line = null;
        boolean ortho = (int) x1 == x1 && (int) y1 == y1 && x1 == x2 || y1 == y2;
        //boolean vertical = x1==x2 && (roi==null||roiType==Roi.RECTANGLE);
        //if (rotate) vertical = !vertical;
        for (int i = 0; i < stackSize; i++) {
            ip = stack.getProcessor(flip ? stackSize - i : i + 1);
            if (roiType == Roi.POLYLINE || roiType == Roi.FREELINE)
                line = getIrregularProfile(roi, ip);
            else if (ortho)
                line = getOrthoLine(ip, (int) x1, (int) y1, (int) x2, (int) y2, line);
            else
                line = getLine(ip, x1, y1, x2, y2, line);
            if (rotate) {
                if (i == 0) ip2 = ip.createProcessor(stackSize, line.length);
                putColumn(ip2, i, 0, line, line.length);
            } else {
                if (i == 0) ip2 = ip.createProcessor(line.length, stackSize);
                putRow(ip2, 0, i, line, line.length);
            }
        }
        Calibration cal = imp.getCalibration();
        double zSpacing = inputZSpacing / cal.pixelWidth;
        if (zSpacing != 1.0) {
            ip2.setInterpolate(true);
            if (rotate)
                ip2 = ip2.resize((int) (stackSize * zSpacing), line.length);
            else
                ip2 = ip2.resize(line.length, (int) (stackSize * zSpacing));
        }
        return ip2;
    }

    public void putRow(ImageProcessor ip, int x, int y, float[] data, int length) {
        if (rgb) {
            for (int i = 0; i < length; i++)
                ip.putPixel(x++, y, Float.floatToIntBits(data[i]));
        } else {
            for (int i = 0; i < length; i++)
                ip.putPixelValue(x++, y, data[i]);
        }
    }

    public void putColumn(ImageProcessor ip, int x, int y, float[] data, int length) {
        if (rgb) {
            for (int i = 0; i < length; i++)
                ip.putPixel(x, y++, Float.floatToIntBits(data[i]));
        } else {
            for (int i = 0; i < length; i++)
                ip.putPixelValue(x, y++, data[i]);
        }
    }

    float[] getIrregularProfile(Roi roi, ImageProcessor ip) {
        if (x == null)
            doIrregularSetup(roi);
        float[] values = new float[(int) length];
        double leftOver = 1.0;
        double distance = 0.0;
        int index;
        double oldx = xbase, oldy = ybase;
        for (int i = 0; i < n; i++) {
            double len = segmentLengths[i];
            if (len == 0.0)
                continue;
            double xinc = dx[i] / len;
            double yinc = dy[i] / len;
            double start = 1.0 - leftOver;
            double rx = xbase + x[i] + start * xinc;
            double ry = ybase + y[i] + start * yinc;
            double len2 = len - start;
            int n2 = (int) len2;
            for (int j = 0; j <= n2; j++) {
                index = (int) distance + j;
                if (index < values.length) {
                    if (notFloat)
                        values[index] = (float) ip.getInterpolatedPixel(rx, ry);
                    else if (rgb) {
                        int rgbPixel = ((ColorProcessor) ip).getInterpolatedRGBPixel(rx, ry);
                        values[index] = Float.intBitsToFloat(rgbPixel & 0xffffff);
                    } else
                        values[index] = (float) ip.getInterpolatedValue(rx, ry);
                }
                rx += xinc;
                ry += yinc;
            }
            distance += len;
            leftOver = len2 - n2;
        }

        return values;

    }

    void doIrregularSetup(Roi roi) {
        n = ((PolygonRoi) roi).getNCoordinates();
        int[] ix = ((PolygonRoi) roi).getXCoordinates();
        int[] iy = ((PolygonRoi) roi).getYCoordinates();
        x = new double[n];
        y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = ix[i];
            y[i] = iy[i];
        }
        if (roi.getType() == Roi.FREELINE) {
            // smooth line
            for (int i = 1; i < n - 1; i++) {
                x[i] = (x[i - 1] + x[i] + x[i + 1]) / 3.0 + 0.5;
                y[i] = (y[i - 1] + y[i] + y[i + 1]) / 3.0 + 0.5;
            }
        }
        Rectangle r = roi.getBounds();
        xbase = r.x;
        ybase = r.y;
        length = 0.0;
        double segmentLength;
        double xdelta, ydelta;
        segmentLengths = new double[n];
        dx = new double[n];
        dy = new double[n];
        for (int i = 0; i < (n - 1); i++) {
            xdelta = x[i + 1] - x[i];
            ydelta = y[i + 1] - y[i];
            segmentLength = Math.sqrt(xdelta * xdelta + ydelta * ydelta);
            length += segmentLength;
            segmentLengths[i] = segmentLength;
            dx[i] = xdelta;
            dy[i] = ydelta;
        }
    }

    private float[] getLine(ImageProcessor ip, double x1, double y1, double x2, double y2, float[] data) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        int n = (int) Math.round(Math.sqrt(dx * dx + dy * dy));
        if (data == null)
            data = new float[n];
        double xinc = dx / n;
        double yinc = dy / n;
        double rx = x1;
        double ry = y1;
        for (int i = 0; i < n; i++) {
            if (notFloat)
                data[i] = (float) ip.getInterpolatedPixel(rx, ry);
            else if (rgb) {
                int rgbPixel = ((ColorProcessor) ip).getInterpolatedRGBPixel(rx, ry);
                data[i] = Float.intBitsToFloat(rgbPixel & 0xffffff);
            } else
                data[i] = (float) ip.getInterpolatedValue(rx, ry);
            rx += xinc;
            ry += yinc;
        }
        return data;
    }

    private float[] getOrthoLine(ImageProcessor ip, int x1, int y1, int x2, int y2, float[] data) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int n = Math.max(Math.abs(dx), Math.abs(dy));
        if (data == null) data = new float[n];
        int xinc = dx / n;
        int yinc = dy / n;
        int rx = x1;
        int ry = y1;
        for (int i = 0; i < n; i++) {
            if (notFloat)
                data[i] = (float) ip.getPixel(rx, ry);
            else if (rgb) {
                int rgbPixel = ((ColorProcessor) ip).getPixel(rx, ry);
                data[i] = Float.intBitsToFloat(rgbPixel & 0xffffff);
            } else
                data[i] = (float) ip.getPixelValue(rx, ry);
            rx += xinc;
            ry += yinc;
        }
        return data;
    }

    void drawLine(double x1, double y1, double x2, double y2, ImagePlus imp) {
        ImageCanvas ic = imp.getCanvas();
        if (ic == null) return;
        Graphics g = ic.getGraphics();
        g.setColor(new Color(1f, 1f, 0f, 0.4f));
        g.drawLine(ic.screenX((int) (x1 + 0.5)), ic.screenY((int) (y1 + 0.5)), ic.screenX((int) (x2 + 0.5)), ic.screenY((int) (y2 + 0.5)));
    }

    void makePolygon(int count, double outSpacing) {
        int[] x = new int[4];
        int[] y = new int[4];
        Calibration cal = imp.getCalibration();
        double cx = cal.pixelWidth;    //corrects preview for x calibration
        double cy = cal.pixelHeight;    //corrects preview for y calibration
        x[0] = (int) gx1;
        y[0] = (int) gy1;
        x[1] = (int) gx2;
        y[1] = (int) gy2;
        double dx = gx2 - gx1;
        double dy = gy2 - gy1;
        double nrm = Math.sqrt(dx * dx + dy * dy) / outSpacing;
        double xInc = -(dy / (cx * nrm));    //cx scales the x increment
        double yInc = (dx / (cy * nrm));    //cy scales the y increment
        x[2] = x[1] + (int) (xInc * count);
        y[2] = y[1] + (int) (yInc * count);
        x[3] = x[0] + (int) (xInc * count);
        y[3] = y[0] + (int) (yInc * count);
        imp.setRoi(new PolygonRoi(x, y, 4, PolygonRoi.FREEROI));
    }

    int getOutputStackSize(double inSpacing, double outSpacing, int count) {
        Roi roi = imp.getRoi();
        int width = imp.getWidth();
        int height = imp.getHeight();
        if (roi != null) {
            Rectangle r = roi.getBounds();
            width = r.width;
            width = r.height;
        }
        int type = roi != null ? roi.getType() : 0;
        int stackSize = imp.getStackSize();
        double size = 0.0;
        if (type == Roi.RECTANGLE) {
            size = width * height * stackSize;
            if (outSpacing > 0 && !noInterpolate) size *= inSpacing / outSpacing;
        } else
            size = gLength * count * stackSize;
        int bits = imp.getBitDepth();
        switch (bits) {
            case 16:
                size *= 2;
                break;
            case 24:
            case 32:
                size *= 4;
                break;
        }
        return (int) Math.round(size / 1048576.0);
    }

    int getAvailableMemory() {
        long max = IJ.maxMemory();
        if (max == 0) return -1;
        long inUse = IJ.currentMemory();
        long available = max - inUse;
        return (int) ((available + 524288L) / 1048576L);
    }

    public enum Direction {
        Top,
        Left,
        Bottom,
        Right
    }
}
