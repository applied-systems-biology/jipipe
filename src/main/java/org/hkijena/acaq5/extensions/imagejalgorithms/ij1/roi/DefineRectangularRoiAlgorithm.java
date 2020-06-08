package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.parameters.collections.RectangleList;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Define rectangular ROI", description = "Manually defines rectangular ROI")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class DefineRectangularRoiAlgorithm extends ACAQAlgorithm {

    private RectangleList rectangles = new RectangleList();
    private boolean split = false;
    private boolean close = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public DefineRectangularRoiAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
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
    public DefineRectangularRoiAlgorithm(DefineRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new RectangleList(other.rectangles);
        this.close = other.close;
        this.split = other.split;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData currentData = new ROIListData();
        for (Rectangle rectangle : rectangles) {
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
    public RectangleList getRectangles() {
        return rectangles;
    }

    @ACAQParameter("rectangles")
    public void setRectangles(RectangleList rectangles) {
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
