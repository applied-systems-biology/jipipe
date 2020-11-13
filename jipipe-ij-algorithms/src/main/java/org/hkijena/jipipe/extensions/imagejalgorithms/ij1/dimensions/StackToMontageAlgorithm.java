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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalIntegerParameter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

/**
 * Implementation of {@link ij.plugin.MontageMaker}
 */
@JIPipeDocumentation(name = "Stack to montage", description = "Converts an image stack into a montage.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus2DData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class StackToMontageAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalIntegerParameter rows = new OptionalIntegerParameter();
    private OptionalIntegerParameter columns = new OptionalIntegerParameter();
    private boolean labels = false;
    private double scale = 1;
    private int borderWidth = 0;
    private int fontSize = 12;
    private OptionalIntegerParameter firstIndex = new OptionalIntegerParameter();
    private OptionalIntegerParameter lastIndex = new OptionalIntegerParameter();
    private int indexIncrement = 1;
    private Color foregroundColor = Color.WHITE;
    private Color backgroundColor = Color.BLACK;

    public StackToMontageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public StackToMontageAlgorithm(StackToMontageAlgorithm other) {
        super(other);
        this.rows = new OptionalIntegerParameter(other.rows);
        this.columns = new OptionalIntegerParameter(other.columns);
        this.labels = other.labels;
        this.scale = other.scale;
        this.borderWidth = other.borderWidth;
        this.fontSize = other.fontSize;
        this.firstIndex = new OptionalIntegerParameter(other.firstIndex);
        this.lastIndex = new OptionalIntegerParameter(other.lastIndex);
        this.indexIncrement = other.indexIncrement;
        this.foregroundColor = other.foregroundColor;
        this.backgroundColor = other.backgroundColor;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        ImagePlus imp = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class).getImage();
        if (!imp.isStack() || imp.getStackSize() <= 1) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp));
            return;
        }
        imp = imp.duplicate();
        boolean hyperstack = imp.isHyperStack();
        if (hyperstack && imp.getNSlices() > 1 && imp.getNFrames() > 1) {
            IJ.error("5D hyperstacks are not supported");
            return;
        }
        int channels = imp.getNChannels();
        if (!hyperstack && imp.isComposite() && channels > 1) {
            int channel = imp.getChannel();
            CompositeImage ci = (CompositeImage) imp;
            int mode = ci.getMode();
            if (mode == IJ.COMPOSITE)
                ci.setMode(IJ.COLOR);
            ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
            for (int c = 1; c <= channels; c++) {
                imp.setPosition(c, imp.getSlice(), imp.getFrame());
                Image img = imp.getImage();
                stack.addSlice(null, new ColorProcessor(img));
            }
            if (ci.getMode() != mode)
                ci.setMode(mode);
            imp.setPosition(channel, imp.getSlice(), imp.getFrame());
            Calibration cal = imp.getCalibration();
            imp = new ImagePlus(imp.getTitle(), stack);
            imp.setCalibration(cal);
        }
        ImagePlus montage = makeMontage(imp);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(montage));
    }

    private ImagePlus makeMontage(ImagePlus imp) {
        int nSlices = imp.getStackSize();
        boolean hyperstack = imp.isHyperStack();
        if (hyperstack) {
            nSlices = imp.getNSlices();
            if (nSlices == 1)
                nSlices = imp.getNFrames();
        }
        int columns;
        int rows;
        if (!this.columns.isEnabled() && !this.rows.isEnabled()) {
            columns = Math.max(1, (int) Math.sqrt(nSlices));
            rows = (int) Math.ceil(1.0 * nSlices / columns);
        } else if (this.columns.isEnabled() && !this.rows.isEnabled()) {
            columns = this.columns.getContent();
            rows = (int) Math.ceil(1.0 * nSlices / columns);
        } else if (this.rows.isEnabled() && !this.columns.isEnabled()) {
            rows = this.rows.getContent();
            columns = (int) Math.ceil(1.0 * nSlices / rows);
        } else {
            columns = Math.max(1, (int) Math.sqrt(nSlices));
            rows = Math.max(this.rows.getContent(), (int) Math.ceil(1.0 * nSlices / columns));
        }

        if (hyperstack) {
            return makeHyperstackMontage(imp, columns, rows, scale, indexIncrement, borderWidth, labels);
        } else {
            int first = firstIndex.isEnabled() ? firstIndex.getContent() : 1;
            int last = lastIndex.isEnabled() ? lastIndex.getContent() : nSlices;
            return makeMontage2(imp, columns, rows, scale, first, last, indexIncrement, borderWidth, labels);
        }
    }

    /**
     * Creates a montage and returns it as an ImagePlus.
     */
    public ImagePlus makeMontage2(ImagePlus imp, int columns, int rows, double scale, int first, int last, int inc, int borderWidth, boolean labels) {
        int stackWidth = imp.getWidth();
        int stackHeight = imp.getHeight();
        int nSlices = imp.getStackSize();
        int width = (int) (stackWidth * scale);
        int height = (int) (stackHeight * scale);
        int montageWidth = width * columns + borderWidth * (columns - 1);
        int montageHeight = height * rows + borderWidth * (rows - 1);
        ImageProcessor ip = imp.getProcessor();
        ImageProcessor montage = ip.createProcessor(montageWidth, montageHeight);
        ImagePlus imp2 = new ImagePlus("Montage", montage);
        imp2.setCalibration(imp.getCalibration());
        montage = imp2.getProcessor();
        Color fgColor = foregroundColor;
        Color bgColor = backgroundColor;
        montage.setColor(bgColor);
        montage.fill();
        montage.setColor(fgColor);
        montage.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        montage.setAntialiasedText(true);
        ImageStack stack = imp.getStack();
        int x = 0;
        int y = 0;
        ImageProcessor aSlice;
        int slice = first;
        while (slice <= last) {
            aSlice = stack.getProcessor(slice);
            if (scale != 1.0) {
                aSlice.setInterpolationMethod(ImageProcessor.BILINEAR);
                boolean averageWhenDownSizing = width < 200;
                aSlice = aSlice.resize(width, height, averageWhenDownSizing);
            }
            montage.insert(aSlice, x, y);
            String label = stack.getShortSliceLabel(slice);
            if (labels)
                drawLabel(montage, slice, label, x, y, width, height, borderWidth);
            x += width + borderWidth;
            if (x >= montageWidth) {
                x = 0;
                y += height + borderWidth;
                if (y >= montageHeight)
                    break;
            }
            IJ.showProgress((double) (slice - first) / (last - first));
            slice += inc;
        }
        if (borderWidth > 0) {
            for (x = width; x < montageWidth; x += width + borderWidth) {
                montage.setRoi(x, 0, borderWidth, montageHeight);
                montage.fill();
            }
            for (y = height; y < montageHeight; y += height + borderWidth) {
                montage.setRoi(0, y, montageWidth, borderWidth);
                montage.fill();
            }
        }
        IJ.showProgress(1.0);
        Calibration cal = imp2.getCalibration();
        if (cal.scaled()) {
            cal.pixelWidth /= scale;
            cal.pixelHeight /= scale;
        }
        imp2.setProperty("Info", "xMontage=" + columns + "\nyMontage=" + rows + "\n");
        return imp2;
    }

    /**
     * Creates a hyperstack montage and returns it as an ImagePlus.
     */
    private ImagePlus makeHyperstackMontage(ImagePlus imp, int columns, int rows, double scale, int inc, int borderWidth, boolean labels) {
        ImagePlus[] channels = ChannelSplitter.split(imp);
        int n = channels.length;
        ImagePlus[] montages = new ImagePlus[n];
        for (int i = 0; i < n; i++) {
            int last = channels[i].getStackSize();
            montages[i] = makeMontage2(channels[i], columns, rows, scale, 1, last, inc, borderWidth, labels);
        }
        ImagePlus montage = (new RGBStackMerge()).mergeHyperstacks(montages, false);
        montage.setCalibration(montages[0].getCalibration());
        montage.setTitle("Montage");
        return montage;
    }

    private void drawLabel(ImageProcessor montage, int slice, String label, int x, int y, int width, int height, int borderWidth) {
        if (label != null && !label.equals("") && montage.getStringWidth(label) >= width) {
            do {
                label = label.substring(0, label.length() - 1);
            } while (label.length() > 1 && montage.getStringWidth(label) >= width);
        }
        if (label == null || label.equals(""))
            label = "" + slice;
        int swidth = montage.getStringWidth(label);
        x += width / 2 - swidth / 2;
        y -= borderWidth / 2;
        y += height;
        montage.drawString(label, x, y);
    }

    @JIPipeDocumentation(name = "Rows", description = "The number of rows to generate. If disabled, rows are generated automatically.")
    @JIPipeParameter(value = "rows", uiOrder = 10)
    public OptionalIntegerParameter getRows() {
        return rows;
    }

    @JIPipeParameter("rows")
    public void setRows(OptionalIntegerParameter rows) {
        this.rows = rows;
    }

    @JIPipeDocumentation(name = "Columns", description = "The number of columns to generate. If disabled, columns are generated automatically.")
    @JIPipeParameter(value = "columns", uiOrder = 11)
    public OptionalIntegerParameter getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(OptionalIntegerParameter columns) {
        this.columns = columns;
    }

    @JIPipeDocumentation(name = "Draw labels", description = "If enabled, text labels are generated.")
    @JIPipeParameter("draw-labels")
    public boolean isLabels() {
        return labels;
    }

    @JIPipeParameter("draw-labels")
    public void setLabels(boolean labels) {
        this.labels = labels;
    }

    @JIPipeDocumentation(name = "Scale", description = "Scale to apply to each tile")
    @JIPipeParameter("scale")
    public double getScale() {
        return scale;
    }

    @JIPipeParameter("scale")
    public void setScale(double scale) {
        this.scale = scale;
    }

    @JIPipeDocumentation(name = "Border width", description = "Distance between each tile")
    @JIPipeParameter("border-width")
    public int getBorderWidth() {
        return borderWidth;
    }

    @JIPipeParameter("border-width")
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    @JIPipeDocumentation(name = "Font size", description = "The size of labels")
    @JIPipeParameter("font-size")
    public int getFontSize() {
        return fontSize;
    }

    @JIPipeParameter("font-size")
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    @JIPipeDocumentation(name = "First index", description = "Determines the first slice index to put into the montage. The first index is 1. Ignored if the image is a hyperstack.")
    @JIPipeParameter(value = "first-index", uiOrder = 13)
    public OptionalIntegerParameter getFirstIndex() {
        return firstIndex;
    }

    @JIPipeParameter("first-index")
    public void setFirstIndex(OptionalIntegerParameter firstIndex) {
        this.firstIndex = firstIndex;
    }

    @JIPipeDocumentation(name = "Last index", description = "Determines the last slice index to put into the montage. Starts with 1. Ignored if the image is a hyperstack.")
    @JIPipeParameter(value = "last-index", uiOrder = 14)
    public OptionalIntegerParameter getLastIndex() {
        return lastIndex;
    }

    @JIPipeParameter("last-index")
    public void setLastIndex(OptionalIntegerParameter lastIndex) {
        this.lastIndex = lastIndex;
    }

    @JIPipeDocumentation(name = "Index increment", description = "Allows you to skip image slices. Ignored if the image is a hyperstack.")
    @JIPipeParameter(value = "index-increment", uiOrder = 15)
    public int getIndexIncrement() {
        return indexIncrement;
    }

    @JIPipeParameter("index-increment")
    public void setIndexIncrement(int indexIncrement) {
        this.indexIncrement = indexIncrement;
    }

    @JIPipeDocumentation(name = "Foreground color", description = "The color of labels")
    @JIPipeParameter("foreground-color")
    public Color getForegroundColor() {
        return foregroundColor;
    }

    @JIPipeParameter("foreground-color")
    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    @JIPipeDocumentation(name = "Background color", description = "The color of backgrounds")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
