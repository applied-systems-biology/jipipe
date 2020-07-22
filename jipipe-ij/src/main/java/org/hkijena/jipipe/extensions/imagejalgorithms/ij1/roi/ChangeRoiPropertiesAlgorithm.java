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

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDoubleParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalIntegerParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Change ROI properties", description = "Changes properties of all Roi to a user-defined value.")
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class ChangeRoiPropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter roiName = new OptionalStringParameter();
    private OptionalIntegerParameter positionZ = new OptionalIntegerParameter();
    private OptionalIntegerParameter positionC = new OptionalIntegerParameter();
    private OptionalIntegerParameter positionT = new OptionalIntegerParameter();
    private OptionalColorParameter fillColor = new OptionalColorParameter();
    private OptionalColorParameter lineColor = new OptionalColorParameter();
    private OptionalDoubleParameter lineWidth = new OptionalDoubleParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeRoiPropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
        lineWidth.setContent(1.0);
    }

    /**
     * Instantiates a new node type.
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();
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

        dataBatch.addOutputData(getFirstOutputSlot(), data);
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices.")
    @JIPipeParameter("position-z")
    public OptionalIntegerParameter getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(OptionalIntegerParameter positionZ) {
        this.positionZ = positionZ;
    }

    @JIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
            "that 'Channel' refers to an image slice and not to a pixel channel. " +
            "The first index is 1. If set to zero, the ROI is located on all channels.")
    @JIPipeParameter("position-c")
    public OptionalIntegerParameter getPositionC() {
        return positionC;
    }

    @JIPipeParameter("position-c")
    public void setPositionC(OptionalIntegerParameter positionC) {
        this.positionC = positionC;
    }

    @JIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames.")
    @JIPipeParameter("position-t")
    public OptionalIntegerParameter getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(OptionalIntegerParameter positionT) {
        this.positionT = positionT;
    }

    @JIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ")
    @JIPipeParameter("fill-color")
    public OptionalColorParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalColorParameter fillColor) {
        this.fillColor = fillColor;
    }

    @JIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ")
    @JIPipeParameter("line-color")
    public OptionalColorParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalColorParameter lineColor) {
        this.lineColor = lineColor;
    }

    @JIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ")
    @JIPipeParameter("line-width")
    public OptionalDoubleParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalDoubleParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @JIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    public OptionalStringParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalStringParameter roiName) {
        this.roiName = roiName;
    }
}
