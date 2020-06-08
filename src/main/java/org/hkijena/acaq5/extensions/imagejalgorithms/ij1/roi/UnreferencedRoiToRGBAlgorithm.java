package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.acaq5.extensions.parameters.OptionalColorParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.acaq5.extensions.parameters.roi.RectangleROIDefinitionParameter;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "ROI to RGB (unreferenced)", description = "Converts ROI lists to color images. The line and fill color is stored within the ROI themselves. " +
        "This algorithm does not need a reference image that determines the output size.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class UnreferencedRoiToRGBAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private RectangleROIDefinitionParameter imageArea = new RectangleROIDefinitionParameter();
    private boolean drawOutline = false;
    private boolean drawFilledOutline = true;
    private OptionalColorParameter overrideFillColor = new OptionalColorParameter();
    private OptionalColorParameter overrideLineColor = new OptionalColorParameter();
    private OptionalDoubleParameter overrideLineWidth = new OptionalDoubleParameter();

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public UnreferencedRoiToRGBAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ImagePlusColorRGBData.class, null)
                .seal()
                .build());
        imageArea.getWidth().setUseExactValue(false);
        imageArea.getHeight().setUseExactValue(false);
        overrideLineWidth.setContent(1.0);
        overrideFillColor.setContent(Color.RED);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public UnreferencedRoiToRGBAlgorithm(UnreferencedRoiToRGBAlgorithm other) {
        super(other);
        this.imageArea = new RectangleROIDefinitionParameter(other.imageArea);
        this.drawOutline = other.drawOutline;
        this.drawFilledOutline = other.drawFilledOutline;
        this.overrideFillColor = new OptionalColorParameter(other.overrideFillColor);
        this.overrideLineColor = new OptionalColorParameter(other.overrideLineColor);
        this.overrideLineWidth = new OptionalDoubleParameter(other.overrideLineWidth);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = (ROIListData) dataInterface.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();

        // Find the bounds and future stack position
        Rectangle bounds = imageArea.apply(inputData.getBounds());
        int sx = bounds.width + bounds.x;
        int sy = bounds.height + bounds.y;
        int sz = 1;
        int sc = 1;
        int st = 1;
        for (Roi roi : inputData) {
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            sz = Math.max(sz, z);
            sc = Math.max(sc, c);
            st = Math.max(st, t);
        }

        ImagePlus result = IJ.createImage("ROIs", "RGB", sx, sy, sc, sz, st);
        Map<Integer, List<Roi>> groupedByStackIndex =
                inputData.stream().collect(Collectors.groupingBy(roi -> result.getStackIndex(roi.getCPosition(), roi.getZPosition(), roi.getTPosition())));
        for (Map.Entry<Integer, List<Roi>> entry : groupedByStackIndex.entrySet()) {
            ImageProcessor processor = result.getStack().getProcessor(entry.getKey());
            for (Roi roi : entry.getValue()) {
                if (drawFilledOutline) {
                    Color color = (overrideFillColor.isEnabled() || roi.getFillColor() == null) ? overrideFillColor.getContent() : roi.getFillColor();
                    processor.setColor(color);
                    processor.fill(roi);
                }
                if (drawOutline) {
                    Color color = (overrideLineColor.isEnabled() || roi.getStrokeColor() == null) ? overrideLineColor.getContent() : roi.getStrokeColor();
                    int width = (overrideLineWidth.isEnabled() || roi.getStrokeWidth() <= 0) ? (int)(double)(overrideLineWidth.getContent()) : (int)roi.getStrokeWidth();
                    processor.setLineWidth(width);
                    processor.setColor(color);
                    roi.drawPixels(processor);
                }
            }
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Image area", description = "Allows modification of the output image width and height.")
    @ACAQParameter("image-area")
    public RectangleROIDefinitionParameter getImageArea() {
        return imageArea;
    }

    @ACAQParameter("image-area")
    public void setImageArea(RectangleROIDefinitionParameter imageArea) {
        this.imageArea = imageArea;
    }

    @ACAQDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @ACAQParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @ACAQParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @ACAQDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @ACAQParameter("fill-outline")
    public boolean isDrawFilledOutline() {
        return drawFilledOutline;
    }

    @ACAQParameter("fill-outline")
    public void setDrawFilledOutline(boolean drawFilledOutline) {
        this.drawFilledOutline = drawFilledOutline;
    }

    @ACAQDocumentation(name = "Override fill color", description = "If enabled, the fill color will be overridden by this value. " +
            "If a ROI has no fill color, it will always fall back to this color.")
    @ACAQParameter("override-fill-color")
    public OptionalColorParameter getOverrideFillColor() {
        return overrideFillColor;
    }

    @ACAQParameter("override-fill-color")
    public void setOverrideFillColor(OptionalColorParameter overrideFillColor) {
        this.overrideFillColor = overrideFillColor;
    }

    @ACAQDocumentation(name = "Override line color", description = "If enabled, the line color will be overridden by this value. " +
            "If a ROI has no line color, it will always fall back to this color.")
    @ACAQParameter("override-line-color")
    public OptionalColorParameter getOverrideLineColor() {
        return overrideLineColor;
    }

    @ACAQParameter("override-line-color")
    public void setOverrideLineColor(OptionalColorParameter overrideLineColor) {
        this.overrideLineColor = overrideLineColor;
    }

    @ACAQDocumentation(name = "Override line width", description = "If enabled, the line width will be overridden by this value. " +
            "If a ROI has a line width equal or less than zero, it will fall back to this value.")
    @ACAQParameter("override-line-width")
    public OptionalDoubleParameter getOverrideLineWidth() {
        return overrideLineWidth;
    }

    @ACAQParameter("override-line-width")
    public void setOverrideLineWidth(OptionalDoubleParameter overrideLineWidth) {
        this.overrideLineWidth = overrideLineWidth;
    }
}
