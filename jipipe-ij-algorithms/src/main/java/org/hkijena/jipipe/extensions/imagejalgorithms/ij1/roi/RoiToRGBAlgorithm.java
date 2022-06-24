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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ROIElementDrawingMode;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.NumberParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Convert ROI to RGB", description = "Converts ROI lists to masks. The line and fill color is stored within the ROI themselves. " +
        "This algorithm needs a reference image that provides the output sizes. If you do not have a reference image, you can use the unreferenced variant.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class RoiToRGBAlgorithm extends JIPipeIteratingAlgorithm {

    private ROIElementDrawingMode drawOutlineMode = ROIElementDrawingMode.Always;
    private ROIElementDrawingMode drawFilledOutlineMode = ROIElementDrawingMode.IfAvailable;
    private RoiLabel drawnLabel = RoiLabel.None;
    private OptionalColorParameter overrideFillColor = new OptionalColorParameter();
    private OptionalColorParameter overrideLineColor = new OptionalColorParameter();
    private OptionalDoubleParameter overrideLineWidth = new OptionalDoubleParameter();
    private boolean drawOver = true;
    private Color labelForeground = Color.WHITE;
    private OptionalColorParameter labelBackground = new OptionalColorParameter(Color.BLACK, false);
    private int labelSize = 9;
    private double opacity = 1.0;
    private boolean ignoreZ = false;
    private boolean ignoreC = false;
    private boolean ignoreT = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("ROI", "The ROI", ROIListData.class)
                .addInputSlot("Image", "The image where ROI are drawn on", ImagePlusData.class)
                .addOutputSlot("Output", "The ROI visualization (RGB image)", ImagePlusColorRGBData.class, null)
                .seal()
                .build());
        overrideLineWidth.setContent(1.0);
        overrideFillColor.setContent(Color.RED);
        overrideLineColor.setContent(Color.YELLOW);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RoiToRGBAlgorithm(RoiToRGBAlgorithm other) {
        super(other);
        this.drawOutlineMode = other.drawOutlineMode;
        this.drawFilledOutlineMode = other.drawFilledOutlineMode;
        this.drawnLabel = other.drawnLabel;
        this.overrideFillColor = new OptionalColorParameter(other.overrideFillColor);
        this.overrideLineColor = new OptionalColorParameter(other.overrideLineColor);
        this.overrideLineWidth = new OptionalDoubleParameter(other.overrideLineWidth);
        this.drawOver = other.drawOver;
        this.labelForeground = other.labelForeground;
        this.labelBackground = other.labelBackground;
        this.labelSize = other.labelSize;
        this.opacity = other.opacity;
        this.ignoreC = other.ignoreC;
        this.ignoreZ = other.ignoreZ;
        this.ignoreT = other.ignoreT;
    }

    @Override
    protected boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runPassThrough(JIPipeProgressInfo progressInfo) {
        getFirstOutputSlot().addData(getInputSlot("Image"), progressInfo);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = (ROIListData) dataBatch.getInputData("ROI", ROIListData.class, progressInfo).duplicate(progressInfo);
        ImagePlus reference = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getImage();

        // Find the bounds and future stack position
        int sx = reference.getWidth();
        int sy = reference.getHeight();
        int sz = reference.getNSlices();
        int sc = reference.getNChannels();
        int st = reference.getNFrames();

        // ROI statistics needed for labels
        Map<Roi, Point> roiCentroids = new HashMap<>();
        Map<Roi, Integer> roiIndices = new HashMap<>();
        if (drawnLabel != RoiLabel.None) {
            for (int i = 0; i < inputData.size(); i++) {
                Roi roi = inputData.get(i);
                roiIndices.put(roi, i);
                roiCentroids.put(roi, ROIListData.getCentroid(roi));
            }
        }

        ImagePlus result;
        if (drawOver) {
            result = ImageJUtils.convertToColorRGBIfNeeded(ImageJUtils.duplicate(reference));
            result.setTitle("Reference+ROIs");
        } else {
            result = IJ.createImage("ROIs", "RGB", sx, sy, sc, sz, st);
        }

        Font labelFont = new Font(Font.DIALOG, Font.PLAIN, labelSize);

        // Draw ROI
        for (int z = 0; z < sz; z++) {
            for (int c = 0; c < sc; c++) {
                for (int t = 0; t < st; t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor originalProcessor = opacity != 1.0 ? result.getStack().getProcessor(stackIndex).duplicate() : null;
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);
                    for (Roi roi : inputData) {

                        if (progressInfo.isCancelled())
                            return;

                        int rz = ignoreZ ? 0 : roi.getZPosition();
                        int rc = ignoreC ? 0 : roi.getCPosition();
                        int rt = ignoreT ? 0 : roi.getTPosition();
                        if (rz != 0 && rz != (z + 1))
                            continue;
                        if (rc != 0 && rc != (c + 1))
                            continue;
                        if (rt != 0 && rt != (t + 1))
                            continue;
                        if (drawFilledOutlineMode.shouldDraw(roi.getFillColor(), overrideFillColor.getContent())) {
                            Color color = (overrideFillColor.isEnabled() || roi.getFillColor() == null) ? overrideFillColor.getContent() : roi.getFillColor();
                            processor.setColor(color);
                            processor.fill(roi);
                        }
                        if (drawOutlineMode.shouldDraw(roi.getStrokeColor(), overrideLineColor.getContent())) {
                            Color color = (overrideLineColor.isEnabled() || roi.getStrokeColor() == null) ? overrideLineColor.getContent() : roi.getStrokeColor();
                            int width = (overrideLineWidth.isEnabled() || roi.getStrokeWidth() <= 0) ? (int) (double) (overrideLineWidth.getContent()) : (int) roi.getStrokeWidth();
                            processor.setLineWidth(width);
                            processor.setColor(color);
                            roi.drawPixels(processor);
                        }
                        if (drawnLabel != RoiLabel.None) {
                            Point centroid = roiCentroids.get(roi);
                            drawnLabel.draw(result,
                                    processor,
                                    roi,
                                    roiIndices.get(roi),
                                    new Rectangle(centroid.x, centroid.y, 0, 0),
                                    labelForeground,
                                    labelBackground.getContent(),
                                    labelFont,
                                    labelBackground.isEnabled());
                        }
                    }

                    // Apply opacity
                    if (originalProcessor != null) {
                        int[] originalBytes = (int[]) originalProcessor.getPixels();
                        int[] bytes = (int[]) processor.getPixels();
                        for (int i = 0; i < bytes.length; i++) {
                            int rs = (originalBytes[i] & 0xff0000) >> 16;
                            int gs = (originalBytes[i] & 0xff00) >> 8;
                            int bs = originalBytes[i] & 0xff;
                            int rt = (bytes[i] & 0xff0000) >> 16;
                            int gt = (bytes[i] & 0xff00) >> 8;
                            int bt = bytes[i] & 0xff;
                            int r = Math.min(255, Math.max((int) ((1 - opacity) * rs + opacity * rt), 0));
                            int g = Math.min(255, Math.max((int) ((1 - opacity) * gs + opacity * gt), 0));
                            int b = Math.min(255, Math.max((int) ((1 - opacity) * bs + opacity * bt), 0));
                            int rgb = b + (g << 8) + (r << 16);
                            bytes[i] = rgb;
                        }
                    }
                }
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Opacity", description = "Opacity of the added ROI and labels. If zero, they are not visible. If set to one, they are fully visible.")
    @JIPipeParameter("opacity")
    @NumberParameterSettings(step = 0.1)
    public double getOpacity() {
        return opacity;
    }

    @JIPipeParameter("opacity")
    public boolean setOpacity(double opacity) {
        if (opacity < 0 || opacity > 1)
            return false;
        this.opacity = opacity;
        return true;
    }

    @JIPipeDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @JIPipeParameter("draw-outline-mode")
    public ROIElementDrawingMode getDrawOutlineMode() {
        return drawOutlineMode;
    }

    @JIPipeParameter("draw-outline-mode")
    public void setDrawOutlineMode(ROIElementDrawingMode drawOutlineMode) {
        this.drawOutlineMode = drawOutlineMode;
    }

    @JIPipeDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @JIPipeParameter("fill-outline-mode")
    public ROIElementDrawingMode getDrawFilledOutlineMode() {
        return drawFilledOutlineMode;
    }

    @JIPipeParameter("fill-outline-mode")
    public void setDrawFilledOutlineMode(ROIElementDrawingMode drawFilledOutlineMode) {
        this.drawFilledOutlineMode = drawFilledOutlineMode;
    }

    @JIPipeDocumentation(name = "Draw labels", description = "Allows to draw labels on top of ROI.")
    @JIPipeParameter("drawn-label")
    public RoiLabel getDrawnLabel() {
        return drawnLabel;
    }

    @JIPipeParameter("drawn-label")
    public void setDrawnLabel(RoiLabel drawnLabel) {
        this.drawnLabel = drawnLabel;
    }

    @JIPipeDocumentation(name = "Label foreground", description = "The text color of the label (if enabled)")
    @JIPipeParameter("label-foreground")
    public Color getLabelForeground() {
        return labelForeground;
    }

    @JIPipeParameter("label-foreground")
    public void setLabelForeground(Color labelForeground) {
        this.labelForeground = labelForeground;
    }

    @JIPipeDocumentation(name = "Label background", description = "The background color of the label (if enabled)")
    @JIPipeParameter("label-background")
    public OptionalColorParameter getLabelBackground() {
        return labelBackground;
    }

    @JIPipeParameter("label-background")
    public void setLabelBackground(OptionalColorParameter labelBackground) {
        this.labelBackground = labelBackground;
    }

    @JIPipeDocumentation(name = "Label size", description = "Font size of drawn labels")
    @JIPipeParameter("label-size")
    public int getLabelSize() {
        return labelSize;
    }

    @JIPipeParameter("label-size")
    public boolean setLabelSize(int labelSize) {
        if (labelSize < 1)
            return false;
        this.labelSize = labelSize;
        return true;
    }

    @JIPipeDocumentation(name = "Override fill color", description = "If enabled, the fill color will be overridden by this value. " +
            "If a ROI has no fill color, it will always fall back to this color.")
    @JIPipeParameter("override-fill-color")
    public OptionalColorParameter getOverrideFillColor() {
        return overrideFillColor;
    }

    @JIPipeParameter("override-fill-color")
    public void setOverrideFillColor(OptionalColorParameter overrideFillColor) {
        this.overrideFillColor = overrideFillColor;
    }

    @JIPipeDocumentation(name = "Override line color", description = "If enabled, the line color will be overridden by this value. " +
            "If a ROI has no line color, it will always fall back to this color.")
    @JIPipeParameter("override-line-color")
    public OptionalColorParameter getOverrideLineColor() {
        return overrideLineColor;
    }

    @JIPipeParameter("override-line-color")
    public void setOverrideLineColor(OptionalColorParameter overrideLineColor) {
        this.overrideLineColor = overrideLineColor;
    }

    @JIPipeDocumentation(name = "Override line width", description = "If enabled, the line width will be overridden by this value. " +
            "If a ROI has a line width equal or less than zero, it will fall back to this value.")
    @JIPipeParameter("override-line-width")
    public OptionalDoubleParameter getOverrideLineWidth() {
        return overrideLineWidth;
    }

    @JIPipeParameter("override-line-width")
    public void setOverrideLineWidth(OptionalDoubleParameter overrideLineWidth) {
        this.overrideLineWidth = overrideLineWidth;
    }

    @JIPipeDocumentation(name = "Draw over reference", description = "If enabled, draw the ROI over the reference image.")
    @JIPipeParameter("draw-over")
    public boolean isDrawOver() {
        return drawOver;
    }

    @JIPipeParameter("draw-over")
    public void setDrawOver(boolean drawOver) {
        this.drawOver = drawOver;
    }

    @JIPipeDocumentation(name = "Ignore Z", description = "If enabled, ROI will show outside their Z layer")
    @JIPipeParameter("ignore-z")
    public boolean isIgnoreZ() {
        return ignoreZ;
    }

    @JIPipeParameter("ignore-z")
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    @JIPipeDocumentation(name = "Ignore channel", description = "If enabled, ROI will show outside their channel (C) layer")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @JIPipeDocumentation(name = "Ignore frame", description = "If enabled, ROI will show outside their frame (T) layer")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }
}
