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

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.nodes.JIPipeColumMatching;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform.CanvasEqualizer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalIntegerParameter;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import java.awt.Color;
import java.awt.Font;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Montage of input images", description = "Creates a montage of all 2D input images. The montage labels can be generated by available annotations.")
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus2DData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
public class InputImagesToMontage extends JIPipeMergingAlgorithm {

    private CanvasEqualizer canvasEqualizer = new CanvasEqualizer();
    private JIPipeDataByMetadataExporter labelGenerator = new JIPipeDataByMetadataExporter();
    private OptionalIntegerParameter rows = new OptionalIntegerParameter();
    private OptionalIntegerParameter columns = new OptionalIntegerParameter();
    private boolean drawLabels = true;
    private double scale = 1;
    private int borderWidth = 0;
    private int fontSize = 12;
    private Color foregroundColor = Color.WHITE;
    private Color backgroundColor = Color.BLACK;

    public InputImagesToMontage(JIPipeNodeInfo info) {
        super(info);
        this.getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.MergeAll);
        registerSubParameter(canvasEqualizer);
        registerSubParameter(labelGenerator);
    }

    public InputImagesToMontage(InputImagesToMontage other) {
        super(other);
        this.canvasEqualizer = new CanvasEqualizer(other.canvasEqualizer);
        this.labelGenerator = new JIPipeDataByMetadataExporter(other.labelGenerator);
        this.rows = new OptionalIntegerParameter(other.rows);
        this.columns = new OptionalIntegerParameter(other.columns);
        this.drawLabels = other.drawLabels;
        this.scale = other.scale;
        this.borderWidth = other.borderWidth;
        this.fontSize = other.fontSize;
        this.foregroundColor = other.foregroundColor;
        this.backgroundColor = other.backgroundColor;
        registerSubParameter(canvasEqualizer);
        registerSubParameter(labelGenerator);
    }


    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<ImagePlus, String> labelledImages = new HashMap<>();
        for (int row : dataBatch.getInputRows(getFirstInputSlot())) {
            labelledImages.put(getFirstInputSlot().getData(row, ImagePlus2DData.class, progressInfo).getImage(),
                    labelGenerator.generateMetadataString(getFirstInputSlot(), row, new HashSet<>()));
        }
        if (labelledImages.isEmpty())
            return;
        List<ImagePlus> input = labelledImages.keySet().stream()
                .sorted(Comparator.comparing(labelledImages::get, NaturalOrderComparator.INSTANCE)).collect(Collectors.toList());
        List<String> labels = input.stream().map(labelledImages::get).collect(Collectors.toList());

        // Equalize canvas
        List<ImagePlus> equalized = canvasEqualizer.equalize(input);

        // Determine number of rows & columns
        int columns;
        int rows;
        if (!this.columns.isEnabled() && !this.rows.isEnabled()) {
            columns = Math.max(1, (int) Math.sqrt(input.size()));
            rows = (int) Math.ceil(1.0 * input.size() / columns);
        } else if (this.columns.isEnabled() && !this.rows.isEnabled()) {
            columns = this.columns.getContent();
            rows = (int) Math.ceil(1.0 * input.size() / columns);
        } else if (this.rows.isEnabled() && !this.columns.isEnabled()) {
            rows = this.rows.getContent();
            columns = (int) Math.ceil(1.0 * input.size() / rows);
        } else {
            columns = Math.max(1, (int) Math.sqrt(input.size()));
            rows = Math.max(this.rows.getContent(), (int) Math.ceil(1.0 * input.size() / columns));
        }

        ImagePlus result = makeMontage2(equalized, labels, columns, rows, scale, borderWidth);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    /**
     * Creates a montage and returns it as an ImagePlus.
     */
    public ImagePlus makeMontage2(List<ImagePlus> input, List<String> labels, int columns, int rows, double scale, int borderWidth) {
        int stackWidth = input.get(0).getWidth();
        int stackHeight = input.get(0).getHeight();
        int width = (int) (stackWidth * scale);
        int height = (int) (stackHeight * scale);
        int montageWidth = width * columns + borderWidth * (columns - 1);
        int montageHeight = height * rows + borderWidth * (rows - 1);
        ImageProcessor ip = input.get(0).getProcessor();
        ImageProcessor montage = ip.createProcessor(montageWidth, montageHeight);
        ImagePlus imp2 = new ImagePlus("Montage", montage);
        imp2.setCalibration(input.get(0).getCalibration());
        montage = imp2.getProcessor();
        Color fgColor = foregroundColor;
        Color bgColor = backgroundColor;
        montage.setColor(bgColor);
        montage.fill();
        montage.setColor(fgColor);
        montage.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        montage.setAntialiasedText(true);
        int x = 0;
        int y = 0;
        for (int i = 0; i < input.size(); ++i) {
            ImageProcessor slice = input.get(i).getProcessor();
            if (scale != 1.0) {
                slice.setInterpolationMethod(ImageProcessor.BILINEAR);
                boolean averageWhenDownSizing = width < 200;
                slice = slice.resize(width, height, averageWhenDownSizing);
            }
            montage.insert(slice, x, y);
            String label = labels.get(i);
            if (this.drawLabels)
                drawLabel(montage, label, x, y, width, height, borderWidth);
            x += width + borderWidth;
            if (x >= montageWidth) {
                x = 0;
                y += height + borderWidth;
                if (y >= montageHeight)
                    break;
            }
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
        Calibration cal = imp2.getCalibration();
        if (cal.scaled()) {
            cal.pixelWidth /= scale;
            cal.pixelHeight /= scale;
        }
        imp2.setProperty("Info", "xMontage=" + columns + "\nyMontage=" + rows + "\n");
        return imp2;
    }

    private void drawLabel(ImageProcessor montage, String label, int x, int y, int width, int height, int borderWidth) {
        if (label != null && !label.equals("") && montage.getStringWidth(label) >= width) {
            do {
                label = label.substring(0, label.length() - 1);
            } while (label.length() > 1 && montage.getStringWidth(label) >= width);
        }
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
    public boolean isDrawLabels() {
        return drawLabels;
    }

    @JIPipeParameter("draw-labels")
    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
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

    @JIPipeDocumentation(name = "Canvas expansion", description = "To generate a montage, all images need to have the same size. Following settings allow you to control how this is applied.")
    @JIPipeParameter("canvas-equalizer")
    public CanvasEqualizer getCanvasEqualizer() {
        return canvasEqualizer;
    }

    @JIPipeDocumentation(name = "Label generation", description = "If you want labels inside your montage, you can control how the labels are generated here.")
    @JIPipeParameter("label-generator")
    public JIPipeDataByMetadataExporter getLabelGenerator() {
        return labelGenerator;
    }
}
