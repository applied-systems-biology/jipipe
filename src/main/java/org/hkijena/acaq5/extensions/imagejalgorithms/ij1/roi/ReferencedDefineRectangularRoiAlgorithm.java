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

import static org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Define rectangular ROI (referenced)", description = "Manually defines a rectangular ROI. This algorithm requires a reference " +
        "image, but also allows more flexibility in defining the rectangles." + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Reference")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class ReferencedDefineRectangularRoiAlgorithm extends ACAQIteratingAlgorithm {

    private MarginList rectangles = new MarginList();
    private boolean split = false;
    private boolean close = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ReferencedDefineRectangularRoiAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
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
    public ReferencedDefineRectangularRoiAlgorithm(ReferencedDefineRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new MarginList(other.rectangles);
        this.close = other.close;
        this.split = other.split;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus reference = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class).getImage();
        Rectangle boundaries = new Rectangle(0, 0, reference.getWidth(), reference.getHeight());

        ROIListData currentData = new ROIListData();
        for (Margin margin : rectangles) {
            Rectangle rectangle = margin.apply(boundaries);
            currentData.addRectangle(rectangle, close);
            if (split) {
                getFirstOutputSlot().addData(currentData);
                currentData = new ROIListData();
            }
        }
        if (!currentData.isEmpty()) {
            getFirstOutputSlot().addData(currentData);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
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

    @ACAQDocumentation(name = "Split output", description = "If enabled, each rectangle is put into its own ROI list")
    @ACAQParameter("split")
    public boolean isSplit() {
        return split;
    }

    @ACAQParameter("split")
    public void setSplit(boolean split) {
        this.split = split;
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
