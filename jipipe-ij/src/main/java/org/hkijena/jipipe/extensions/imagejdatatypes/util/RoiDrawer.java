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
 *
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import com.google.common.eventbus.EventBus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.RoiLabel;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.NumberParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Advanced ROI drawing algorithm with better flexibility compared to the methods of {@link ROIListData}
 */
public class RoiDrawer implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
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

    public RoiDrawer() {
    }

    public RoiDrawer(RoiDrawer other) {
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
    public EventBus getEventBus() {
        return eventBus;
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

    public ImagePlus draw(ImagePlus reference, ROIListData roisToDraw, JIPipeProgressInfo progressInfo) {

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
            for (int i = 0; i < roisToDraw.size(); i++) {
                Roi roi = roisToDraw.get(i);
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
                    for (Roi roi : roisToDraw) {

                        if (progressInfo.isCancelled())
                            return null;

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

                    applyOpacity(originalProcessor, processor);
                }
            }
        }
        return result;
    }

    private void applyOpacity(ImageProcessor originalProcessor, ImageProcessor processor) {
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

    /**
     * Draws rois on a processor
     * @param index the index of the slice (zero-based)
     * @param processor the target processor
     * @param roisToDraw the ROIs to draw
     * @param roisToHighlight ROIs to highlight (will be drawn again, while other ROIs are toned down)
     */
    public void drawOnProcessor(ImageSliceIndex index, ColorProcessor processor, ROIListData roisToDraw, Set<Roi> roisToHighlight) {
        // ROI statistics needed for labels
        Map<Roi, Point> roiCentroids = new HashMap<>();
        Map<Roi, Integer> roiIndices = new HashMap<>();
        if (drawnLabel != RoiLabel.None) {
            for (int i = 0; i < roisToDraw.size(); i++) {
                Roi roi = roisToDraw.get(i);
                roiIndices.put(roi, i);
                roiCentroids.put(roi, ROIListData.getCentroid(roi));
            }
        }

        // Determine if we need highlighting
        final boolean withHighlight = roisToHighlight != null && roisToHighlight.size() > 0;

        // Create processor
        ColorProcessor originalProcessor = opacity != 1.0 ? processor : null;
        if(opacity != 1.0) {
            processor = (ColorProcessor) originalProcessor.duplicate();
        }

        final int z = index.getZ();
        final int c = index.getC();
        final int t = index.getT();
        final Font labelFont = new Font(Font.DIALOG, Font.PLAIN, labelSize);

        // Draw the ROIs
        if(withHighlight) {
            for (Roi roi : roisToDraw) {
                if(!roisToHighlight.contains(roi)) {
                    drawRoi(processor, roiCentroids, roiIndices, z, c, t, labelFont, roi, true);
                }
            }
            for (Roi roi : roisToHighlight) {
                drawRoi(processor, roiCentroids, roiIndices, z, c, t, labelFont, roi, false);
            }
        }
        else {
            for (Roi roi : roisToDraw) {
                drawRoi(processor, roiCentroids, roiIndices, z, c, t, labelFont, roi, false);
            }
        }

        // Apply opacity
        applyOpacity(originalProcessor, processor);
    }

    private void drawRoi(ColorProcessor processor, Map<Roi, Point> roiCentroids, Map<Roi, Integer> roiIndices, int z, int c, int t, Font labelFont, Roi roi, boolean drawMuted) {
        int rz = ignoreZ ? 0 : roi.getZPosition();
        int rc = ignoreC ? 0 : roi.getCPosition();
        int rt = ignoreT ? 0 : roi.getTPosition();
        if (rz != 0 && rz != (z + 1))
            return;
        if (rc != 0 && rc != (c + 1))
            return;
        if (rt != 0 && rt != (t + 1))
            return;
        if (drawFilledOutlineMode.shouldDraw(roi.getFillColor(), overrideFillColor.getContent())) {
            Color color = (overrideFillColor.isEnabled() || roi.getFillColor() == null) ? overrideFillColor.getContent() : roi.getFillColor();
            if(drawMuted) {
                color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
            }
            processor.setColor(color);
            processor.fill(roi);
        }
        if (drawOutlineMode.shouldDraw(roi.getStrokeColor(), overrideLineColor.getContent())) {
            Color color = (overrideLineColor.isEnabled() || roi.getStrokeColor() == null) ? overrideLineColor.getContent() : roi.getStrokeColor();
            int width = (overrideLineWidth.isEnabled() || roi.getStrokeWidth() <= 0) ? (int) (double) (overrideLineWidth.getContent()) : (int) roi.getStrokeWidth();
            if(drawMuted) {
                color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
            }
            processor.setLineWidth(width);
            processor.setColor(color);
            roi.drawPixels(processor);
        }
        if (drawnLabel != RoiLabel.None) {
            Point centroid = roiCentroids.get(roi);
            drawnLabel.draw(new ImagePlus("image", processor),
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
}
