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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.RoiLabel;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.NumberParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Advanced ROI drawing algorithm with better flexibility compared to the methods of {@link ROIListData}
 */
public class RoiDrawer extends AbstractJIPipeParameterCollection {
    private ROIElementDrawingMode drawOutlineMode = ROIElementDrawingMode.Always;
    private ROIElementDrawingMode drawFilledOutlineMode = ROIElementDrawingMode.IfAvailable;
    private RoiLabel drawnLabel = RoiLabel.None;
    private OptionalColorParameter overrideFillColor = new OptionalColorParameter(Color.RED, false);
    private OptionalColorParameter overrideLineColor = new OptionalColorParameter(Color.YELLOW, false);
    private OptionalDoubleParameter overrideLineWidth = new OptionalDoubleParameter(1, false);
    private boolean drawOver = true;
    private Color labelForeground = Color.WHITE;

    private FontFamilyParameter labelFontFamily = new FontFamilyParameter();
    private OptionalColorParameter labelBackground = new OptionalColorParameter(Color.BLACK, false);
    private int labelSize = 9;
    private double opacity = 1.0;
    private boolean ignoreZ = false;
    private boolean ignoreC = false;
    private boolean ignoreT = false;

    public RoiDrawer() {
    }

    public RoiDrawer(RoiDrawer other) {
        copyFrom(other);
    }

    /**
     * Draws a label on graphics
     *
     * @param text           the text
     * @param graphics2D     the graphics
     * @param centroidX      the x location (centroid)
     * @param centroidY      the y location (centroid)
     * @param magnification  the magnification
     * @param foreground     the foreground
     * @param background     the background
     * @param font           the font
     * @param drawBackground if a background should be drawn
     */
    public static void drawLabelOnGraphics(String text, Graphics2D graphics2D, double centroidX, double centroidY, double magnification, Color foreground, Color background, Font font, boolean drawBackground) {
        graphics2D.setFont(font);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int width = fontMetrics.stringWidth(text);
        int height = fontMetrics.getHeight();
        int ascent = fontMetrics.getAscent();
        int x = (int) (centroidX * magnification - width / 2);
        int y = (int) (centroidY * magnification - height / 2);
        if (drawBackground) {
            graphics2D.setColor(background);
            graphics2D.fillRect(x - 1, y - 1, width + 2, height + 2);
        }
        graphics2D.setColor(foreground);
        graphics2D.drawString(text, x, y + ascent);
    }

    public void copyFrom(RoiDrawer other) {
        this.drawOutlineMode = other.drawOutlineMode;
        this.drawFilledOutlineMode = other.drawFilledOutlineMode;
        this.drawnLabel = other.drawnLabel;
        this.labelFontFamily = new FontFamilyParameter(other.labelFontFamily);
        this.overrideFillColor = new OptionalColorParameter(other.overrideFillColor);
        this.overrideLineColor = new OptionalColorParameter(other.overrideLineColor);
        this.overrideLineWidth = new OptionalDoubleParameter(other.overrideLineWidth);
        this.drawOver = other.drawOver;
        this.labelForeground = other.labelForeground;
        this.labelBackground = new OptionalColorParameter(other.labelBackground);
        this.labelSize = other.labelSize;
        this.opacity = other.opacity;
        this.ignoreC = other.ignoreC;
        this.ignoreZ = other.ignoreZ;
        this.ignoreT = other.ignoreT;
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

    @JIPipeDocumentation(name = "Label font family", description = "The font family of the label")
    @JIPipeParameter("label-font-family")
    public FontFamilyParameter getLabelFontFamily() {
        return labelFontFamily;
    }

    @JIPipeParameter("label-font-family")
    public void setLabelFontFamily(FontFamilyParameter labelFontFamily) {
        this.labelFontFamily = labelFontFamily;
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
            if (reference.getType() == ImagePlus.COLOR_RGB) {
                result = ImageJUtils.duplicate(reference);
            } else {
                result = ImageJUtils.renderToRGBWithLUTIfNeeded(reference, progressInfo.resolve("Render RGB"));
            }
            result.setTitle("Reference+ROIs");
        } else {
            result = IJ.createImage("ROIs", "RGB", sx, sy, sc, sz, st);
        }

        Font labelFont = labelFontFamily.toFont(Font.PLAIN, labelSize);

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
                if (bytes[i] > 0) {
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

    /**
     * Draws rois on a processor
     *
     * @param roisToDraw      the ROIs to draw
     * @param processor       the target processor
     * @param index           the index of the slice (zero-based)
     * @param roisToHighlight ROIs to highlight (will be drawn again, while other ROIs are toned down)
     */
    public void drawOnProcessor(ROIListData roisToDraw, ColorProcessor processor, ImageSliceIndex index, Set<Roi> roisToHighlight) {
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
        if (opacity != 1.0) {
            processor = (ColorProcessor) originalProcessor.duplicate();
        }

        final int z = index.getZ();
        final int c = index.getC();
        final int t = index.getT();
        final Font labelFont = labelFontFamily.toFont(Font.PLAIN, labelSize);

        // Draw the ROIs
        if (withHighlight) {
            for (Roi roi : roisToDraw) {
                if (!roisToHighlight.contains(roi)) {
                    drawRoiOnProcessor(roi, processor, roiCentroids, roiIndices, z, c, t, labelFont, true);
                }
            }
            for (Roi roi : roisToHighlight) {
                drawRoiOnProcessor(roi, processor, roiCentroids, roiIndices, z, c, t, labelFont, false);
            }
        } else {
            for (Roi roi : roisToDraw) {
                drawRoiOnProcessor(roi, processor, roiCentroids, roiIndices, z, c, t, labelFont, false);
            }
        }

        // Apply opacity
        applyOpacity(originalProcessor, processor);
    }

    private void drawRoiOnProcessor(Roi roi, ColorProcessor processor, Map<Roi, Point> roiCentroids, Map<Roi, Integer> roiIndices, int z, int c, int t, Font labelFont, boolean drawMuted) {
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
            if (drawMuted) {
                color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
            }
            processor.setColor(color);
            processor.fill(roi);
        }
        if (drawOutlineMode.shouldDraw(roi.getStrokeColor(), overrideLineColor.getContent())) {
            Color color = (overrideLineColor.isEnabled() || roi.getStrokeColor() == null) ? overrideLineColor.getContent() : roi.getStrokeColor();
            int width = (overrideLineWidth.isEnabled() || roi.getStrokeWidth() <= 0) ? (int) (double) (overrideLineWidth.getContent()) : (int) roi.getStrokeWidth();
            if (drawMuted) {
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

    /**
     * Uses the current settings to filter ROI based on visibility within the current image index
     *
     * @param rois  the ROI
     * @param index the current image index
     * @return the filtered ROI
     */
    public ROIListData filterVisibleROI(ROIListData rois, ImageSliceIndex index) {
        final int z = index.getZ();
        final int c = index.getC();
        final int t = index.getT();

        ROIListData result = new ROIListData();
        for (Roi roi : rois) {
            int rz = ignoreZ ? 0 : roi.getZPosition();
            int rc = ignoreC ? 0 : roi.getCPosition();
            int rt = ignoreT ? 0 : roi.getTPosition();
            if (rz != 0 && rz != (z + 1))
                continue;
            if (rc != 0 && rc != (c + 1))
                continue;
            if (rt != 0 && rt != (t + 1))
                continue;
            result.add(roi);
        }

        return result;
    }

    /**
     * Draws ROI as overlay. Assumes that the ROI's {@link ImageCanvas} (ic) field is set
     *
     * @param roisToDraw      the ROI to draw
     * @param graphics2D      the graphics
     * @param renderArea      area where the ROI are rendered
     * @param index           position of the ROI (zero-based)
     * @param roisToHighlight highlighted ROI
     * @param magnification   the magnification
     */
    public void drawOverlayOnGraphics(ROIListData roisToDraw, Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex index, Set<Roi> roisToHighlight, double magnification) {
        // ROI statistics needed for labels
        Map<Roi, Point2D> roiCentroids = new HashMap<>();
        Map<Roi, Integer> roiIndices = new HashMap<>();
        if (drawnLabel != RoiLabel.None) {
            for (int i = 0; i < roisToDraw.size(); i++) {
                Roi roi = roisToDraw.get(i);
                roiIndices.put(roi, i);
                double[] contourCentroid = roi.getContourCentroid();
                roiCentroids.put(roi, new Point2D.Double(contourCentroid[0], contourCentroid[1]));
            }
        }

        // Determine if we need highlighting
        final boolean withHighlight = roisToHighlight != null && roisToHighlight.size() > 0;

        final int z = index.getZ();
        final int c = index.getC();
        final int t = index.getT();
        final Font labelFont = labelFontFamily.toFont(Font.PLAIN, labelSize);

        // Draw the ROIs
        if (withHighlight) {
            for (int i = 0; i < roisToDraw.size(); i++) {
                Roi roi = roisToDraw.get(i);
                if (!roisToHighlight.contains(roi)) {
                    drawRoiOnGraphics(roi, graphics2D, renderArea, z, c, t, roiCentroids, roiIndices, labelFont, true, false, magnification);
                }
            }
            for (Roi roi : roisToHighlight) {
                drawRoiOnGraphics(roi, graphics2D, renderArea, z, c, t, roiCentroids, roiIndices, labelFont, false, true, magnification);
            }
        } else {
            for (int i = 0; i < roisToDraw.size(); i++) {
                Roi roi = roisToDraw.get(i);
                drawRoiOnGraphics(roi, graphics2D, renderArea, z, c, t, roiCentroids, roiIndices, labelFont, false, false, magnification);
            }
        }
    }

    /**
     * Draws a ROI on graphics
     * Assumes that the ROI has an appropriate image canvas (ic) for its magnification
     *
     * @param roi           the roi
     * @param graphics2D    the graphics
     * @param renderArea    the target area
     * @param z             location slice
     * @param c             location channel
     * @param t             location frame
     * @param roiCentroids  centroids
     * @param roiIndices    indices
     * @param labelFont     label font
     * @param drawMuted     draw muted
     * @param highlighted   if highlighted
     * @param magnification the magnification
     */
    private void drawRoiOnGraphics(Roi roi, Graphics2D graphics2D, Rectangle renderArea, int z, int c, int t, Map<Roi, Point2D> roiCentroids, Map<Roi, Integer> roiIndices, Font labelFont, boolean drawMuted, boolean highlighted, double magnification) {
        int rz = ignoreZ ? 0 : roi.getZPosition();
        int rc = ignoreC ? 0 : roi.getCPosition();
        int rt = ignoreT ? 0 : roi.getTPosition();
        if (rz != 0 && rz != (z + 1))
            return;
        if (rc != 0 && rc != (c + 1))
            return;
        if (rt != 0 && rt != (t + 1))
            return;

        Color oldFillColor = roi.getFillColor();
        Color oldStrokeColor = roi.getStrokeColor();
        float oldLineWidth = roi.getStrokeWidth();
//        roi.setImage(calibrationImage);

        if (drawFilledOutlineMode.shouldDraw(roi.getFillColor(), overrideFillColor.getContent())) {
            Color color = (overrideFillColor.isEnabled() || roi.getFillColor() == null) ? overrideFillColor.getContent() : roi.getFillColor();
            if (drawMuted) {
                color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
            }
            if (opacity < 1) {
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
            }
            roi.setFillColor(color);
        } else {
            roi.setFillColor(null);
        }
        if (drawOutlineMode.shouldDraw(roi.getStrokeColor(), overrideLineColor.getContent())) {
            Color color = (overrideLineColor.isEnabled() || roi.getStrokeColor() == null) ? overrideLineColor.getContent() : roi.getStrokeColor();
            float width = (overrideLineWidth.isEnabled() || roi.getStrokeWidth() <= 0) ? overrideLineWidth.getContent().floatValue() : roi.getStrokeWidth();
            if (drawMuted) {
                color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
            }
            if (opacity < 1) {
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
            }
            roi.setStrokeWidth(width);
            roi.setStrokeColor(color);
        } else {
            roi.setStrokeColor(null);
        }

        graphics2D.translate(renderArea.x, renderArea.y);

        // Render ROI
        roi.drawOverlay(graphics2D);

        // Render label
        if (drawnLabel != RoiLabel.None) {
            Point2D centroid = roiCentroids.get(roi);
            // Apply modifications
            drawnLabel.draw(graphics2D,
                    roi,
                    roiIndices.get(roi),
                    centroid,
                    magnification,
                    labelForeground,
                    labelBackground.getContent(),
                    labelFont,
                    labelBackground.isEnabled());
        }

        graphics2D.translate(-renderArea.x, -renderArea.y);

        if (highlighted) {
            Rectangle bounds = roi.getBounds();
            int x = renderArea.x + (int) (bounds.x * magnification);
            int y = renderArea.y + (int) (bounds.y * magnification);
            int w = (int) (bounds.width * magnification);
            int h = (int) (bounds.height * magnification);
            graphics2D.setStroke(new BasicStroke(1));
            graphics2D.setColor(Color.CYAN);
            graphics2D.drawRect(x, y, w, h);
        }

        // Restore old values
        roi.setFillColor(oldFillColor);
        roi.setStrokeColor(oldStrokeColor);
        roi.setStrokeWidth(oldLineWidth);
    }
}
