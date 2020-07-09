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

import ij.ImagePlus;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.pipelinej.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.api.algorithm.ACAQIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Append rectangular ROI (referenced)", description = "Appends manually defines rectangular ROI to all input ROI lists. " +
        "Ths algorithm allows to add rectangles as margins to the reference image bounds." + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Reference")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class ReferencedAppendRectangularRoiAlgorithm extends ACAQIteratingAlgorithm {

    private Margin.List rectangles = new Margin.List();
    private boolean close = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ReferencedAppendRectangularRoiAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder()
                .addInputSlot("ROI", ROIListData.class)
                .addInputSlot("Reference", ImagePlusData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
        rectangles.addNewInstance();
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public ReferencedAppendRectangularRoiAlgorithm(ReferencedAppendRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new Margin.List(other.rectangles);
        this.close = other.close;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData currentData = (ROIListData) dataInterface.getInputData("ROI", ROIListData.class).duplicate();
        ImagePlus reference = dataInterface.getInputData("Reference", ImagePlusData.class).getImage();
        Rectangle bounds = new Rectangle(0, 0, reference.getWidth(), reference.getHeight());
        for (Margin margin : rectangles) {
            currentData.addRectangle(margin.apply(bounds), close);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), currentData);
    }

    @ACAQDocumentation(name = "Rectangles", description = "List of rectangles")
    @ACAQParameter("rectangles")
    public Margin.List getRectangles() {
        return rectangles;
    }

    @ACAQParameter("rectangles")
    public void setRectangles(Margin.List rectangles) {
        this.rectangles = rectangles;
    }

    @ACAQDocumentation(name = "Close polygon", description = "If true, the polygon shape is closed")
    @ACAQParameter("close")
    public boolean isClose() {
        return close;
    }

    @ACAQParameter("close")
    public void setClose(boolean close) {
        this.close = close;
    }
}
