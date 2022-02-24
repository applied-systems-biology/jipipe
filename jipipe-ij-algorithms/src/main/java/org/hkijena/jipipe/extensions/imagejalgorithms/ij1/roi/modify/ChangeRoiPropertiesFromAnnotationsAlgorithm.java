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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.modify;

import ij.gui.Roi;
import ij.plugin.RoiScaler;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.ColorUtils;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Change ROI properties from annotations", description = "Sets properties of all Roi to values extracted from annotations.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class ChangeRoiPropertiesFromAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter roiName = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter positionX = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter positionY = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter positionZ = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter positionC = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter positionT = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter fillColor = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter lineColor = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter lineWidth = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter scaleX = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter scaleY = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter centerScale = new OptionalAnnotationNameParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ChangeRoiPropertiesFromAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ROIListData.class)
                .addOutputSlot("Output", "", ROIListData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ChangeRoiPropertiesFromAnnotationsAlgorithm(ChangeRoiPropertiesFromAnnotationsAlgorithm other) {
        super(other);
        this.positionX = new OptionalAnnotationNameParameter(other.positionX);
        this.positionY = new OptionalAnnotationNameParameter(other.positionY);
        this.positionZ = new OptionalAnnotationNameParameter(other.positionZ);
        this.positionC = new OptionalAnnotationNameParameter(other.positionC);
        this.positionT = new OptionalAnnotationNameParameter(other.positionT);
        this.fillColor = new OptionalAnnotationNameParameter(other.fillColor);
        this.lineColor = new OptionalAnnotationNameParameter(other.lineColor);
        this.lineWidth = new OptionalAnnotationNameParameter(other.lineWidth);
        this.roiName = new OptionalAnnotationNameParameter(other.roiName);
        this.scaleX = new OptionalAnnotationNameParameter(other.scaleX);
        this.scaleY = new OptionalAnnotationNameParameter(other.scaleY);
        this.centerScale = new OptionalAnnotationNameParameter(other.centerScale);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo).duplicate(progressInfo);
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
                x = NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(positionX.getContent()).getValue());
            if (positionY.isEnabled())
                y = NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(positionY.getContent()).getValue());
            if (positionZ.isEnabled())
                z = NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(positionZ.getContent()).getValue()).intValue();
            if (positionC.isEnabled())
                c = NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(positionC.getContent()).getValue()).intValue();
            if (positionT.isEnabled())
                t = NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(positionT.getContent()).getValue()).intValue();
            roi.setPosition(c, z, t);
            roi.setLocation(x, y);
            if (fillColor.isEnabled())
                roi.setFillColor(ColorUtils.parseColor(dataBatch.getMergedTextAnnotation(fillColor.getContent()).getValue()));
            if (lineColor.isEnabled())
                roi.setStrokeColor(ColorUtils.parseColor(dataBatch.getMergedTextAnnotation(lineColor.getContent()).getValue()));
            if (lineWidth.isEnabled())
                roi.setStrokeWidth(NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(lineWidth.getContent()).getValue()));
            if (roiName.isEnabled())
                roi.setName(dataBatch.getMergedTextAnnotation(roiName.getContent()).getValue());
            if (this.scaleX.isEnabled())
                scaleX = NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(this.scaleX.getContent()).getValue());
            if (this.scaleY.isEnabled())
                scaleY = NumberUtils.createDouble(dataBatch.getMergedTextAnnotation(this.scaleY.getContent()).getValue());
            if (this.centerScale.isEnabled())
                centerScale = BooleanUtils.toBoolean(dataBatch.getMergedTextAnnotation(this.centerScale.getContent()).getValue());
            if(scaleX != 1.0 || scaleY != 1.0) {
                roi = RoiScaler.scale(roi, scaleX, scaleY, centerScale);
                data.set(i, roi);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Location (X)", description = "The X location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-x")
    public OptionalAnnotationNameParameter getPositionX() {
        return positionX;
    }

    @JIPipeParameter("position-x")
    public void setPositionX(OptionalAnnotationNameParameter positionX) {
        this.positionX = positionX;
    }

    @JIPipeDocumentation(name = "Location (Y)", description = "The Y location. The annotation value is converted to an integer.")
    @JIPipeParameter("position-y")
    public OptionalAnnotationNameParameter getPositionY() {
        return positionY;
    }

    @JIPipeParameter("position-y")
    public void setPositionY(OptionalAnnotationNameParameter positionY) {
        this.positionY = positionY;
    }

    @JIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices. The annotation value is converted to an integer.")
    @JIPipeParameter("position-z")
    public OptionalAnnotationNameParameter getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(OptionalAnnotationNameParameter positionZ) {
        this.positionZ = positionZ;
    }

    @JIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
            "that 'Channel' refers to an image slice and not to a pixel channel. " +
            "The first index is 1. If set to zero, the ROI is located on all channels. The annotation value is converted to an integer.")
    @JIPipeParameter("position-c")
    public OptionalAnnotationNameParameter getPositionC() {
        return positionC;
    }

    @JIPipeParameter("position-c")
    public void setPositionC(OptionalAnnotationNameParameter positionC) {
        this.positionC = positionC;
    }

    @JIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames. The annotation value is converted to an integer.")
    @JIPipeParameter("position-t")
    public OptionalAnnotationNameParameter getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(OptionalAnnotationNameParameter positionT) {
        this.positionT = positionT;
    }

    @JIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    public OptionalAnnotationNameParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalAnnotationNameParameter fillColor) {
        this.fillColor = fillColor;
    }

    @JIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("line-color")
    public OptionalAnnotationNameParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalAnnotationNameParameter lineColor) {
        this.lineColor = lineColor;
    }

    @JIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ. The annotation value is converted to an integer.")
    @JIPipeParameter("line-width")
    public OptionalAnnotationNameParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalAnnotationNameParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @JIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    public OptionalAnnotationNameParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalAnnotationNameParameter roiName) {
        this.roiName = roiName;
    }

    @JIPipeDocumentation(name = "Scale X", description = "Allows to scale the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-x")
    public OptionalAnnotationNameParameter getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(OptionalAnnotationNameParameter scaleX) {
        this.scaleX = scaleX;
    }

    @JIPipeDocumentation(name = "Scale Y", description = "Allows to scale the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-y")
    public OptionalAnnotationNameParameter getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(OptionalAnnotationNameParameter scaleY) {
        this.scaleY = scaleY;
    }

    @JIPipeDocumentation(name = "Center scale", description = "If the annotation is true, each ROI is scaled relative to its center. Defaults to false. Must be true or false")
    @JIPipeParameter("center-scale")
    public OptionalAnnotationNameParameter getCenterScale() {
        return centerScale;
    }

    @JIPipeParameter("center-scale")
    public void setCenterScale(OptionalAnnotationNameParameter centerScale) {
        this.centerScale = centerScale;
    }
}
