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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.roi;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.pipelinej.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Convert only ROI to mask", description = "Converts ROI lists to masks. " +
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
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
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
        for (int z = 0; z < sz; z++) {
            for (int c = 0; c < sc; c++) {
                for (int t = 0; t < st; t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);
                    processor.setLineWidth(lineThickness);
                    processor.setColor(255);

                    for (Roi roi : inputData) {
                        if (drawFilledOutline)
                            processor.fill(roi);
                        if (drawOutline)
                            roi.drawPixels(processor);
                    }
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
