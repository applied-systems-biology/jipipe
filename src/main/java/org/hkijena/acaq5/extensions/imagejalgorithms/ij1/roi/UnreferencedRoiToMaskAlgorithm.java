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
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "ROI to mask (unreferenced)", description = "Converts ROI lists to masks. " +
        "This algorithm does not need a reference image that determines the output size.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class UnreferencedRoiToMaskAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private Margin imageArea = new Margin();
    private boolean drawOutline = false;
    private boolean drawFilledOutline = true;
    private int lineThickness = 1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public UnreferencedRoiToMaskAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, null)
                .seal()
                .build());
        imageArea.getWidth().setUseExactValue(false);
        imageArea.getHeight().setUseExactValue(false);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public UnreferencedRoiToMaskAlgorithm(UnreferencedRoiToMaskAlgorithm other) {
        super(other);
        this.imageArea = new Margin(other.imageArea);
        this.drawOutline = other.drawOutline;
        this.drawFilledOutline = other.drawFilledOutline;
        this.lineThickness = other.lineThickness;
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

        ImagePlus result = IJ.createImage("ROIs", "8-bit", sx, sy, sc, sz, st);
        Map<Integer, List<Roi>> groupedByStackIndex =
                inputData.stream().collect(Collectors.groupingBy(roi -> result.getStackIndex(roi.getCPosition(), roi.getZPosition(), roi.getTPosition())));
        for (Map.Entry<Integer, List<Roi>> entry : groupedByStackIndex.entrySet()) {
            ImageProcessor processor = result.getStack().getProcessor(entry.getKey());
            processor.setLineWidth(lineThickness);
            processor.setColor(255);
            for (Roi roi : entry.getValue()) {
                if (drawFilledOutline)
                    processor.fill(roi);
                if (drawOutline)
                    roi.drawPixels(processor);
            }
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Image area", description = "Allows modification of the output image width and height.")
    @ACAQParameter("image-area")
    public Margin getImageArea() {
        return imageArea;
    }

    @ACAQParameter("image-area")
    public void setImageArea(Margin imageArea) {
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

    @ACAQDocumentation(name = "Line thickness", description = "Only relevant if 'Draw outline' is enabled. Sets the outline thickness.")
    @ACAQParameter("line-thickness")
    public int getLineThickness() {
        return lineThickness;
    }

    @ACAQParameter("line-thickness")
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }
}
