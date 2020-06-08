package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.parameters.OptionalColorParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalIntegerParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalStringParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Change ROI properties", description = "Changes properties of all Roi to a user-defined value.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class ChangeRoiPropertiesAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private OptionalStringParameter roiName = new OptionalStringParameter();
    private OptionalIntegerParameter positionZ = new OptionalIntegerParameter();
    private OptionalIntegerParameter positionC = new OptionalIntegerParameter();
    private OptionalIntegerParameter positionT = new OptionalIntegerParameter();
    private OptionalColorParameter fillColor = new OptionalColorParameter();
    private OptionalColorParameter lineColor = new OptionalColorParameter();
    private OptionalDoubleParameter lineWidth = new OptionalDoubleParameter();

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ChangeRoiPropertiesAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
        lineWidth.setContent(1.0);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public ChangeRoiPropertiesAlgorithm(ChangeRoiPropertiesAlgorithm other) {
        super(other);
        this.positionZ = new OptionalIntegerParameter(other.positionZ);
        this.positionC = new OptionalIntegerParameter(other.positionC);
        this.positionT = new OptionalIntegerParameter(other.positionT);
        this.fillColor = new OptionalColorParameter(other.fillColor);
        this.lineColor = new OptionalColorParameter(other.lineColor);
        this.lineWidth = new OptionalDoubleParameter(other.lineWidth);
        this.roiName = new OptionalStringParameter(other.roiName);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = (ROIListData) dataInterface.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();
        for (Roi roi : data) {
            int z, c, t;
            z = roi.getZPosition();
            c = roi.getCPosition();
            t = roi.getTPosition();
            if (positionZ.isEnabled())
                z = positionZ.getContent();
            if (positionC.isEnabled())
                c = positionC.getContent();
            if (positionT.isEnabled())
                t = positionT.getContent();
            roi.setPosition(c, z, t);

            if (fillColor.isEnabled())
                roi.setFillColor(fillColor.getContent());
            if (lineColor.isEnabled())
                roi.setStrokeColor(lineColor.getContent());
            if (lineWidth.isEnabled())
                roi.setStrokeWidth(lineWidth.getContent());
            if (roiName.isEnabled())
                roi.setName(roiName.getContent());
        }

        dataInterface.addOutputData(getFirstOutputSlot(), data);
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position")
    @ACAQParameter("position-z")
    public OptionalIntegerParameter getPositionZ() {
        return positionZ;
    }

    @ACAQParameter("position-z")
    public void setPositionZ(OptionalIntegerParameter positionZ) {
        this.positionZ = positionZ;
    }

    @ACAQDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
            "that 'Channel' refers to an image slice and not to a pixel channel.")
    @ACAQParameter("position-c")
    public OptionalIntegerParameter getPositionC() {
        return positionC;
    }

    @ACAQParameter("position-c")
    public void setPositionC(OptionalIntegerParameter positionC) {
        this.positionC = positionC;
    }

    @ACAQDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position")
    @ACAQParameter("position-t")
    public OptionalIntegerParameter getPositionT() {
        return positionT;
    }

    @ACAQParameter("position-t")
    public void setPositionT(OptionalIntegerParameter positionT) {
        this.positionT = positionT;
    }

    @ACAQDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ")
    @ACAQParameter("fill-color")
    public OptionalColorParameter getFillColor() {
        return fillColor;
    }

    @ACAQParameter("fill-color")
    public void setFillColor(OptionalColorParameter fillColor) {
        this.fillColor = fillColor;
    }

    @ACAQDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ")
    @ACAQParameter("line-color")
    public OptionalColorParameter getLineColor() {
        return lineColor;
    }

    @ACAQParameter("line-color")
    public void setLineColor(OptionalColorParameter lineColor) {
        this.lineColor = lineColor;
    }

    @ACAQDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ")
    @ACAQParameter("line-width")
    public OptionalDoubleParameter getLineWidth() {
        return lineWidth;
    }

    @ACAQParameter("line-width")
    public void setLineWidth(OptionalDoubleParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @ACAQDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @ACAQParameter("roi-name")
    public OptionalStringParameter getRoiName() {
        return roiName;
    }

    @ACAQParameter("roi-name")
    public void setRoiName(OptionalStringParameter roiName) {
        this.roiName = roiName;
    }
}
