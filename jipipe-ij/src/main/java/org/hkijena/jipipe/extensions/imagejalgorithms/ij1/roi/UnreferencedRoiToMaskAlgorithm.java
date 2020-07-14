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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Convert only ROI to mask", description = "Converts ROI lists to masks. " +
        "This algorithm does not need a reference image that determines the output size.")
@JIPipeOrganization(menuPath = "ROI", algorithmCategory = JIPipeNodeCategory.Converter)
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class UnreferencedRoiToMaskAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Margin imageArea = new Margin();
    private boolean drawOutline = false;
    private boolean drawFilledOutline = true;
    private int lineThickness = 1;

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public UnreferencedRoiToMaskAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData inputData = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();

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

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Image area", description = "Allows modification of the output image width and height.")
    @JIPipeParameter("image-area")
    public Margin getImageArea() {
        return imageArea;
    }

    @JIPipeParameter("image-area")
    public void setImageArea(Margin imageArea) {
        this.imageArea = imageArea;
    }

    @JIPipeDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @JIPipeParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @JIPipeParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @JIPipeDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @JIPipeParameter("fill-outline")
    public boolean isDrawFilledOutline() {
        return drawFilledOutline;
    }

    @JIPipeParameter("fill-outline")
    public void setDrawFilledOutline(boolean drawFilledOutline) {
        this.drawFilledOutline = drawFilledOutline;
    }

    @JIPipeDocumentation(name = "Line thickness", description = "Only relevant if 'Draw outline' is enabled. Sets the outline thickness.")
    @JIPipeParameter("line-thickness")
    public int getLineThickness() {
        return lineThickness;
    }

    @JIPipeParameter("line-thickness")
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }
}
