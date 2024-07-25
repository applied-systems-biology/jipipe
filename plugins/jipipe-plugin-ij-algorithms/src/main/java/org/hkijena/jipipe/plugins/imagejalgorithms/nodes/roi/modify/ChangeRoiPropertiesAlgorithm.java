/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.modify;

import ij.gui.Roi;
import ij.plugin.RoiScaler;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Set 2D ROI properties", description = "Sets properties of all Roi to a user-defined value.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class ChangeRoiPropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter roiName = new OptionalStringParameter();
    private OptionalDoubleParameter positionX = new OptionalDoubleParameter();
    private OptionalDoubleParameter positionY = new OptionalDoubleParameter();
    private OptionalIntegerParameter positionZ = new OptionalIntegerParameter();
    private OptionalIntegerParameter positionC = new OptionalIntegerParameter();
    private OptionalIntegerParameter positionT = new OptionalIntegerParameter();
    private OptionalColorParameter fillColor = new OptionalColorParameter();
    private OptionalColorParameter lineColor = new OptionalColorParameter();
    private OptionalDoubleParameter lineWidth = new OptionalDoubleParameter();
    private OptionalDoubleParameter scaleX = new OptionalDoubleParameter(1.0, false);
    private OptionalDoubleParameter scaleY = new OptionalDoubleParameter(1.0, false);
    private boolean centerScale = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeRoiPropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        lineWidth.setContent(1.0);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ChangeRoiPropertiesAlgorithm(ChangeRoiPropertiesAlgorithm other) {
        super(other);
        this.positionX = new OptionalDoubleParameter(other.positionX);
        this.positionY = new OptionalDoubleParameter(other.positionY);
        this.positionZ = new OptionalIntegerParameter(other.positionZ);
        this.positionC = new OptionalIntegerParameter(other.positionC);
        this.positionT = new OptionalIntegerParameter(other.positionT);
        this.fillColor = new OptionalColorParameter(other.fillColor);
        this.lineColor = new OptionalColorParameter(other.lineColor);
        this.lineWidth = new OptionalDoubleParameter(other.lineWidth);
        this.roiName = new OptionalStringParameter(other.roiName);
        this.scaleX = new OptionalDoubleParameter(other.scaleX);
        this.scaleY = new OptionalDoubleParameter(other.scaleY);
        this.centerScale = other.centerScale;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData data = (ROI2DListData) iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo).duplicate(progressInfo);
        for (int i = 0; i < data.size(); i++) {
            Roi roi = data.get(i);
            double x;
            double y;
            double scaleX = this.scaleX.isEnabled() ? this.scaleX.getContent() : 1.0;
            double scaleY = this.scaleY.isEnabled() ? this.scaleY.getContent() : 1.0;
            int z;
            int c;
            int t;
            x = roi.getXBase();
            y = roi.getYBase();
            z = roi.getZPosition();
            c = roi.getCPosition();
            t = roi.getTPosition();
            if (positionX.isEnabled())
                x = positionX.getContent();
            if (positionY.isEnabled())
                y = positionY.getContent();
            if (positionZ.isEnabled())
                z = positionZ.getContent();
            if (positionC.isEnabled())
                c = positionC.getContent();
            if (positionT.isEnabled())
                t = positionT.getContent();
            roi.setPosition(c, z, t);
            roi.setLocation(x, y);

            if (fillColor.isEnabled())
                roi.setFillColor(fillColor.getContent());
            if (lineColor.isEnabled())
                roi.setStrokeColor(lineColor.getContent());
            if (lineWidth.isEnabled())
                roi.setStrokeWidth(lineWidth.getContent());
            if (roiName.isEnabled())
                roi.setName(roiName.getContent());
            if (scaleX != 1.0 || scaleY != 1.0) {
                roi = RoiScaler.scale(roi, scaleX, scaleY, centerScale);
                data.set(i, roi);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Location (X)", description = "The X location")
    @JIPipeParameter("position-x")
    public OptionalDoubleParameter getPositionX() {
        return positionX;
    }

    @JIPipeParameter("position-x")
    public void setPositionX(OptionalDoubleParameter positionX) {
        this.positionX = positionX;
    }

    @SetJIPipeDocumentation(name = "Location (Y)", description = "The Y location")
    @JIPipeParameter("position-y")
    public OptionalDoubleParameter getPositionY() {
        return positionY;
    }

    @JIPipeParameter("position-y")
    public void setPositionY(OptionalDoubleParameter positionY) {
        this.positionY = positionY;
    }

    @SetJIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices.")
    @JIPipeParameter("position-z")
    public OptionalIntegerParameter getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(OptionalIntegerParameter positionZ) {
        this.positionZ = positionZ;
    }

    @SetJIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
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

    @SetJIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames.")
    @JIPipeParameter("position-t")
    public OptionalIntegerParameter getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(OptionalIntegerParameter positionT) {
        this.positionT = positionT;
    }

    @SetJIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ")
    @JIPipeParameter("fill-color")
    public OptionalColorParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalColorParameter fillColor) {
        this.fillColor = fillColor;
    }

    @SetJIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ")
    @JIPipeParameter("line-color")
    public OptionalColorParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalColorParameter lineColor) {
        this.lineColor = lineColor;
    }

    @SetJIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ")
    @JIPipeParameter("line-width")
    public OptionalDoubleParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalDoubleParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    public OptionalStringParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalStringParameter roiName) {
        this.roiName = roiName;
    }

    @SetJIPipeDocumentation(name = "Scale X", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-x")
    public OptionalDoubleParameter getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(OptionalDoubleParameter scaleX) {
        this.scaleX = scaleX;
    }

    @SetJIPipeDocumentation(name = "Scale Y", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-y")
    public OptionalDoubleParameter getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(OptionalDoubleParameter scaleY) {
        this.scaleY = scaleY;
    }

    @SetJIPipeDocumentation(name = "Center scale", description = "If true, each ROI is scaled relative to its center.")
    @JIPipeParameter("center-scale")
    public boolean getCenterScale() {
        return centerScale;
    }

    @JIPipeParameter("center-scale")
    public void setCenterScale(boolean centerScale) {
        this.centerScale = centerScale;
    }
}
