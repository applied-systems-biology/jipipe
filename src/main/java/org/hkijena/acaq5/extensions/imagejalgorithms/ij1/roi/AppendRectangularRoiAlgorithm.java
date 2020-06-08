package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.parameters.collections.MarginList;
import org.hkijena.acaq5.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Append rectangular ROI", description = "Appends manually defines rectangular ROI to all input ROI lists. " +
        "Ths algorithm allows to add rectangles as margins. As no reference image is available, the reference area is defined by the " +
        "bounds of the already existing ROI.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class AppendRectangularRoiAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private MarginList rectangles = new MarginList();
    private boolean close = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public AppendRectangularRoiAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                .addInputSlot("Input", ROIListData.class)
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
    public AppendRectangularRoiAlgorithm(AppendRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new MarginList(other.rectangles);
        this.close = other.close;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData currentData = (ROIListData) dataInterface.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();
        Rectangle bounds = currentData.getBounds();
        for (Margin margin : rectangles) {
            currentData.addRectangle(margin.apply(bounds), close);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), currentData);
    }

    @ACAQDocumentation(name = "Rectangles", description = "List of rectangles")
    @ACAQParameter("rectangles")
    public MarginList getRectangles() {
        return rectangles;
    }

    @ACAQParameter("rectangles")
    public void setRectangles(MarginList rectangles) {
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
