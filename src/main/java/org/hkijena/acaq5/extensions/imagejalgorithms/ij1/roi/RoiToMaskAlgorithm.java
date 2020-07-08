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
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Convert ROI to mask", description = "Converts ROI lists to masks. " +
        "This algorithm needs a reference image that provides the output sizes. If you do not have a reference image, you can use the unreferenced variant."
        + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class RoiToMaskAlgorithm extends ACAQIteratingAlgorithm {

    private boolean drawOutline = false;
    private boolean drawFilledOutline = true;
    private int lineThickness = 1;
    private boolean drawOver = false;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RoiToMaskAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("ROI", ROIListData.class)
                .addInputSlot("Image", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public RoiToMaskAlgorithm(RoiToMaskAlgorithm other) {
        super(other);
        this.drawOutline = other.drawOutline;
        this.drawFilledOutline = other.drawFilledOutline;
        this.lineThickness = other.lineThickness;
        this.drawOver = other.drawOver;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = (ROIListData) dataInterface.getInputData("ROI", ROIListData.class).duplicate();
        ImagePlus reference = dataInterface.getInputData("Image", ImagePlusData.class).getImage();

        // Find the bounds and future stack position
        int sx = reference.getWidth();
        int sy = reference.getHeight();
        int sz = reference.getNSlices();
        int sc = reference.getNChannels();
        int st = reference.getNFrames();

        ImagePlus result;
        if (drawOver) {
            result = ImagePlusGreyscaleMaskData.convertIfNeeded(reference.duplicate());
            result.setTitle("Reference+ROIs");
        } else {
            result = IJ.createImage("ROIs", "8-bit", sx, sy, sc, sz, st);
        }
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

    @ACAQDocumentation(name = "Draw over reference", description = "If enabled, draw the ROI over the reference image.")
    @ACAQParameter("draw-over")
    public boolean isDrawOver() {
        return drawOver;
    }

    @ACAQParameter("draw-over")
    public void setDrawOver(boolean drawOver) {
        this.drawOver = drawOver;
    }
}
