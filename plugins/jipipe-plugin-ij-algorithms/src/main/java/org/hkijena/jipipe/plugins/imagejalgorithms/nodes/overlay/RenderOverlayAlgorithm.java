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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.overlay;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.NumberParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;

/**
 * Wrapper around {@link RoiDrawer}
 */
@SetJIPipeDocumentation(name = "Render overlay", description = "Renders the overlay ROI of an image to RGB")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, name = "Output", create = true)
public class RenderOverlayAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ROIElementDrawingMode drawOutlineMode = ROIElementDrawingMode.Always;
    private ROIElementDrawingMode drawFilledOutlineMode = ROIElementDrawingMode.IfAvailable;
    private RoiLabel drawnLabel = RoiLabel.None;
    private OptionalColorParameter overrideFillColor = new OptionalColorParameter();
    private OptionalColorParameter overrideLineColor = new OptionalColorParameter();
    private OptionalDoubleParameter overrideLineWidth = new OptionalDoubleParameter();
    private Color labelForeground = Color.WHITE;
    private OptionalColorParameter labelBackground = new OptionalColorParameter(Color.BLACK, false);
    private int labelSize = 9;
    private double opacity = 1.0;
    private boolean ignoreZ = false;
    private boolean ignoreC = false;
    private boolean ignoreT = false;

    private double magnification = 1.0;

    private boolean preferRenderViaOverlay = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RenderOverlayAlgorithm(JIPipeNodeInfo info) {
        super(info);
        overrideLineWidth.setContent(1.0);
        overrideFillColor.setContent(Color.RED);
        overrideLineColor.setContent(Color.YELLOW);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RenderOverlayAlgorithm(RenderOverlayAlgorithm other) {
        super(other);
        this.drawOutlineMode = other.drawOutlineMode;
        this.drawFilledOutlineMode = other.drawFilledOutlineMode;
        this.drawnLabel = other.drawnLabel;
        this.overrideFillColor = new OptionalColorParameter(other.overrideFillColor);
        this.overrideLineColor = new OptionalColorParameter(other.overrideLineColor);
        this.overrideLineWidth = new OptionalDoubleParameter(other.overrideLineWidth);
        this.labelForeground = other.labelForeground;
        this.labelBackground = other.labelBackground;
        this.labelSize = other.labelSize;
        this.opacity = other.opacity;
        this.ignoreC = other.ignoreC;
        this.ignoreZ = other.ignoreZ;
        this.ignoreT = other.ignoreT;
        this.magnification = other.magnification;
        this.preferRenderViaOverlay = other.preferRenderViaOverlay;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus reference = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ROI2DListData rois = new ROI2DListData();
        if (reference.getOverlay() != null) {
            rois.addAll(Arrays.asList(reference.getOverlay().toArray()));
        }

        RoiDrawer drawer = new RoiDrawer();
        drawer.setDrawOutlineMode(drawOutlineMode);
        drawer.setDrawFilledOutlineMode(drawFilledOutlineMode);
        drawer.setDrawnLabel(drawnLabel);
        drawer.setOverrideFillColor(overrideFillColor);
        drawer.setOverrideLineColor(overrideLineColor);
        drawer.setOverrideLineWidth(overrideLineWidth);
        drawer.setDrawOver(true);
        drawer.setLabelForeground(labelForeground);
        drawer.setLabelBackground(labelBackground);
        drawer.setLabelSize(labelSize);
        drawer.setOpacity(opacity);
        drawer.setIgnoreC(ignoreC);
        drawer.setIgnoreZ(ignoreZ);
        drawer.setIgnoreT(ignoreT);

        if (magnification == 1.0 && !preferRenderViaOverlay) {
            ImagePlus result = drawer.draw(reference, rois, progressInfo);
            result.setOverlay(null);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        } else {
            rois = new ROI2DListData(rois);
            ImageCanvas canvas = ImageJUtils.createZoomedDummyCanvas(reference, magnification);
            for (Roi roi : rois) {
                ImageJROIUtils.setRoiCanvas(roi, reference, canvas);
            }
            ROI2DListData finalRois = rois;
            final int targetWidth = (int) (magnification * reference.getWidth());
            final int targetHeight = (int) (magnification * reference.getHeight());
            ImageStack targetStack = new ImageStack(targetWidth, targetHeight, reference.getStackSize());
            ImageJIterationUtils.forEachIndexedZCTSlice(reference, (sourceIp, index) -> {
                drawScaledRoi(reference, drawer, finalRois, targetStack, sourceIp, index);
            }, progressInfo);

            // Generate final output
            ImagePlus result = new ImagePlus("ROI", targetStack);
            ImageJUtils.copyHyperstackDimensions(reference, result);
            result.copyScale(reference);
            result.setOverlay(null);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }

    }

    private void drawScaledRoi(ImagePlus reference, RoiDrawer drawer, ROI2DListData finalRois, ImageStack targetStack, ImageProcessor sourceIp, ImageSliceIndex index) {
        ImageProcessor scaledSourceIp = magnification != 1.0 ? sourceIp.resize((int) (magnification * sourceIp.getWidth()), (int) (magnification * sourceIp.getHeight()), false) : sourceIp;
        ImagePlus sliceImage = new ImagePlus("slice", scaledSourceIp);
        sliceImage.copyScale(reference);
        sliceImage.setCalibration(reference.getCalibration());
        sliceImage = ImageJUtils.convertToColorRGBIfNeeded(sliceImage);
//                sliceImage = drawer.draw(sliceImage, finalRois, progressInfo);
        BufferedImage bufferedImage = BufferedImageUtils.copyBufferedImageToARGB(sliceImage.getBufferedImage());
        Graphics2D graphics2D = bufferedImage.createGraphics();
        drawer.drawOverlayOnGraphics(finalRois, graphics2D, new Rectangle(0, 0, scaledSourceIp.getWidth(), scaledSourceIp.getHeight()), index, Collections.emptySet(), magnification);
        graphics2D.dispose();
        ColorProcessor render = new ColorProcessor(bufferedImage);
        targetStack.setProcessor(render, index.zeroSliceIndexToOneStackIndex(reference));
    }

    @SetJIPipeDocumentation(name = "Opacity", description = "Opacity of the added ROI and labels. If zero, they are not visible. If set to one, they are fully visible.")
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

    @SetJIPipeDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @JIPipeParameter("draw-outline-mode")
    public ROIElementDrawingMode getDrawOutlineMode() {
        return drawOutlineMode;
    }

    @JIPipeParameter("draw-outline-mode")
    public void setDrawOutlineMode(ROIElementDrawingMode drawOutlineMode) {
        this.drawOutlineMode = drawOutlineMode;
    }

    @SetJIPipeDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @JIPipeParameter("fill-outline-mode")
    public ROIElementDrawingMode getDrawFilledOutlineMode() {
        return drawFilledOutlineMode;
    }

    @JIPipeParameter("fill-outline-mode")
    public void setDrawFilledOutlineMode(ROIElementDrawingMode drawFilledOutlineMode) {
        this.drawFilledOutlineMode = drawFilledOutlineMode;
    }

    @SetJIPipeDocumentation(name = "Draw labels", description = "Allows to draw labels on top of ROI.")
    @JIPipeParameter("drawn-label")
    public RoiLabel getDrawnLabel() {
        return drawnLabel;
    }

    @JIPipeParameter("drawn-label")
    public void setDrawnLabel(RoiLabel drawnLabel) {
        this.drawnLabel = drawnLabel;
    }

    @SetJIPipeDocumentation(name = "Label foreground", description = "The text color of the label (if enabled)")
    @JIPipeParameter("label-foreground")
    public Color getLabelForeground() {
        return labelForeground;
    }

    @JIPipeParameter("label-foreground")
    public void setLabelForeground(Color labelForeground) {
        this.labelForeground = labelForeground;
    }

    @SetJIPipeDocumentation(name = "Label background", description = "The background color of the label (if enabled)")
    @JIPipeParameter("label-background")
    public OptionalColorParameter getLabelBackground() {
        return labelBackground;
    }

    @JIPipeParameter("label-background")
    public void setLabelBackground(OptionalColorParameter labelBackground) {
        this.labelBackground = labelBackground;
    }

    @SetJIPipeDocumentation(name = "Label size", description = "Font size of drawn labels")
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

    @SetJIPipeDocumentation(name = "Override fill color", description = "If enabled, the fill color will be overridden by this value. " +
            "If a ROI has no fill color, it will always fall back to this color.")
    @JIPipeParameter("override-fill-color")
    public OptionalColorParameter getOverrideFillColor() {
        return overrideFillColor;
    }

    @JIPipeParameter("override-fill-color")
    public void setOverrideFillColor(OptionalColorParameter overrideFillColor) {
        this.overrideFillColor = overrideFillColor;
    }

    @SetJIPipeDocumentation(name = "Override line color", description = "If enabled, the line color will be overridden by this value. " +
            "If a ROI has no line color, it will always fall back to this color.")
    @JIPipeParameter("override-line-color")
    public OptionalColorParameter getOverrideLineColor() {
        return overrideLineColor;
    }

    @JIPipeParameter("override-line-color")
    public void setOverrideLineColor(OptionalColorParameter overrideLineColor) {
        this.overrideLineColor = overrideLineColor;
    }

    @SetJIPipeDocumentation(name = "Override line width", description = "If enabled, the line width will be overridden by this value. " +
            "If a ROI has a line width equal or less than zero, it will fall back to this value.")
    @JIPipeParameter("override-line-width")
    public OptionalDoubleParameter getOverrideLineWidth() {
        return overrideLineWidth;
    }

    @JIPipeParameter("override-line-width")
    public void setOverrideLineWidth(OptionalDoubleParameter overrideLineWidth) {
        this.overrideLineWidth = overrideLineWidth;
    }

    @SetJIPipeDocumentation(name = "Ignore Z", description = "If enabled, ROI will show outside their Z layer")
    @JIPipeParameter("ignore-z")
    public boolean isIgnoreZ() {
        return ignoreZ;
    }

    @JIPipeParameter("ignore-z")
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    @SetJIPipeDocumentation(name = "Ignore channel", description = "If enabled, ROI will show outside their channel (C) layer")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @SetJIPipeDocumentation(name = "Ignore frame", description = "If enabled, ROI will show outside their frame (T) layer")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }

    @SetJIPipeDocumentation(name = "Magnification", description = "Magnification applied during the rendering")
    @JIPipeParameter("magnification")
    public double getMagnification() {
        return magnification;
    }

    @JIPipeParameter("magnification")
    public void setMagnification(double magnification) {
        this.magnification = magnification;
    }

    @SetJIPipeDocumentation(name = "Prefer render via overlay", description = "If enabled, the rendering via an ImageJ overlay is preferred even if the magnification is 1.0")
    @JIPipeParameter("prefer-render-via-overlay")
    public boolean isPreferRenderViaOverlay() {
        return preferRenderViaOverlay;
    }

    @JIPipeParameter("prefer-render-via-overlay")
    public void setPreferRenderViaOverlay(boolean preferRenderViaOverlay) {
        this.preferRenderViaOverlay = preferRenderViaOverlay;
    }
}
