package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.parameters.collections.MarginList;
import org.hkijena.acaq5.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Append rectangular ROI (referenced)", description = "Appends manually defines rectangular ROI to all input ROI lists. " +
        "Ths algorithm allows to add rectangles as margins to the reference image bounds.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "ROI")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Reference")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class ReferencedAppendRectangularRoiAlgorithm extends ACAQIteratingAlgorithm {

    private MarginList rectangles = new MarginList();
    private boolean close = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ReferencedAppendRectangularRoiAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
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
        this.rectangles = new MarginList(other.rectangles);
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
