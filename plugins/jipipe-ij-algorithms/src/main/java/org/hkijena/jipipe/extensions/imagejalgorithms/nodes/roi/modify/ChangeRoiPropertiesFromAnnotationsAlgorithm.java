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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.modify;

import ij.gui.Roi;
import ij.plugin.RoiScaler;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.ColorUtils;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Change ROI properties from annotations", description = "Sets properties of all Roi to values extracted from annotations.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class ChangeRoiPropertiesFromAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter roiName = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter positionX = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter positionY = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter positionZ = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter positionC = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter positionT = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter fillColor = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter lineColor = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter lineWidth = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter scaleX = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter scaleY = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter centerScale = new OptionalTextAnnotationNameParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeRoiPropertiesFromAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ChangeRoiPropertiesFromAnnotationsAlgorithm(ChangeRoiPropertiesFromAnnotationsAlgorithm other) {
        super(other);
        this.positionX = new OptionalTextAnnotationNameParameter(other.positionX);
        this.positionY = new OptionalTextAnnotationNameParameter(other.positionY);
        this.positionZ = new OptionalTextAnnotationNameParameter(other.positionZ);
        this.positionC = new OptionalTextAnnotationNameParameter(other.positionC);
        this.positionT = new OptionalTextAnnotationNameParameter(other.positionT);
        this.fillColor = new OptionalTextAnnotationNameParameter(other.fillColor);
        this.lineColor = new OptionalTextAnnotationNameParameter(other.lineColor);
        this.lineWidth = new OptionalTextAnnotationNameParameter(other.lineWidth);
        this.roiName = new OptionalTextAnnotationNameParameter(other.roiName);
        this.scaleX = new OptionalTextAnnotationNameParameter(other.scaleX);
        this.scaleY = new OptionalTextAnnotationNameParameter(other.scaleY);
        this.centerScale = new OptionalTextAnnotationNameParameter(other.centerScale);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo).duplicate(progressInfo);
        for (int i = 0; i < data.size(); i++) {
            Roi roi = data.get(i);
            double x;
            double y;
            double scaleX = 1.0;
            double scaleY = 1.0;
            boolean centerScale = false;
            int z;
            int c;
            int t;
            x = roi.getXBase();
            y = roi.getYBase();
            z = roi.getZPosition();
            c = roi.getCPosition();
            t = roi.getTPosition();
            if (positionX.isEnabled())
                x = NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(positionX.getContent()).getValue());
            if (positionY.isEnabled())
                y = NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(positionY.getContent()).getValue());
            if (positionZ.isEnabled())
                z = NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(positionZ.getContent()).getValue()).intValue();
            if (positionC.isEnabled())
                c = NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(positionC.getContent()).getValue()).intValue();
            if (positionT.isEnabled())
                t = NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(positionT.getContent()).getValue()).intValue();
            roi.setPosition(c, z, t);
            roi.setLocation(x, y);
            if (fillColor.isEnabled())
                roi.setFillColor(ColorUtils.parseColor(iterationStep.getMergedTextAnnotation(fillColor.getContent()).getValue()));
            if (lineColor.isEnabled())
                roi.setStrokeColor(ColorUtils.parseColor(iterationStep.getMergedTextAnnotation(lineColor.getContent()).getValue()));
            if (lineWidth.isEnabled())
                roi.setStrokeWidth(NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(lineWidth.getContent()).getValue()));
            if (roiName.isEnabled())
                roi.setName(iterationStep.getMergedTextAnnotation(roiName.getContent()).getValue());
            if (this.scaleX.isEnabled())
                scaleX = NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(this.scaleX.getContent()).getValue());
            if (this.scaleY.isEnabled())
                scaleY = NumberUtils.createDouble(iterationStep.getMergedTextAnnotation(this.scaleY.getContent()).getValue());
            if (this.centerScale.isEnabled())
                centerScale = BooleanUtils.toBoolean(iterationStep.getMergedTextAnnotation(this.centerScale.getContent()).getValue());
            if (scaleX != 1.0 || scaleY != 1.0) {
                roi = RoiScaler.scale(roi, scaleX, scaleY, centerScale);
                data.set(i, roi);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Location (X)", description = "The X location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-x")
    public OptionalTextAnnotationNameParameter getPositionX() {
        return positionX;
    }

    @JIPipeParameter("position-x")
    public void setPositionX(OptionalTextAnnotationNameParameter positionX) {
        this.positionX = positionX;
    }

    @SetJIPipeDocumentation(name = "Location (Y)", description = "The Y location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-y")
    public OptionalTextAnnotationNameParameter getPositionY() {
        return positionY;
    }

    @JIPipeParameter("position-y")
    public void setPositionY(OptionalTextAnnotationNameParameter positionY) {
        this.positionY = positionY;
    }

    @SetJIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices. The annotation value is converted to an integer.")
    @JIPipeParameter("position-z")
    public OptionalTextAnnotationNameParameter getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(OptionalTextAnnotationNameParameter positionZ) {
        this.positionZ = positionZ;
    }

    @SetJIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
            "that 'Channel' refers to an image slice and not to a pixel channel. " +
            "The first index is 1. If set to zero, the ROI is located on all channels. The annotation value is converted to an integer.")
    @JIPipeParameter("position-c")
    public OptionalTextAnnotationNameParameter getPositionC() {
        return positionC;
    }

    @JIPipeParameter("position-c")
    public void setPositionC(OptionalTextAnnotationNameParameter positionC) {
        this.positionC = positionC;
    }

    @SetJIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames. The annotation value is converted to an integer.")
    @JIPipeParameter("position-t")
    public OptionalTextAnnotationNameParameter getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(OptionalTextAnnotationNameParameter positionT) {
        this.positionT = positionT;
    }

    @SetJIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    public OptionalTextAnnotationNameParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalTextAnnotationNameParameter fillColor) {
        this.fillColor = fillColor;
    }

    @SetJIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("line-color")
    public OptionalTextAnnotationNameParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalTextAnnotationNameParameter lineColor) {
        this.lineColor = lineColor;
    }

    @SetJIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ. The annotation value is converted to an integer.")
    @JIPipeParameter("line-width")
    public OptionalTextAnnotationNameParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalTextAnnotationNameParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    public OptionalTextAnnotationNameParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalTextAnnotationNameParameter roiName) {
        this.roiName = roiName;
    }

    @SetJIPipeDocumentation(name = "Scale X", description = "Allows to scale the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-x")
    public OptionalTextAnnotationNameParameter getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(OptionalTextAnnotationNameParameter scaleX) {
        this.scaleX = scaleX;
    }

    @SetJIPipeDocumentation(name = "Scale Y", description = "Allows to scale the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-y")
    public OptionalTextAnnotationNameParameter getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(OptionalTextAnnotationNameParameter scaleY) {
        this.scaleY = scaleY;
    }

    @SetJIPipeDocumentation(name = "Center scale", description = "If the annotation is true, each ROI is scaled relative to its center. Defaults to false. Must be true or false")
    @JIPipeParameter("center-scale")
    public OptionalTextAnnotationNameParameter getCenterScale() {
        return centerScale;
    }

    @JIPipeParameter("center-scale")
    public void setCenterScale(OptionalTextAnnotationNameParameter centerScale) {
        this.centerScale = centerScale;
    }
}
