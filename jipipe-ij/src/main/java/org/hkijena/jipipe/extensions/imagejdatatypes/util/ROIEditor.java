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
 *
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import ij.gui.Roi;
import ij.plugin.RoiScaler;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.awt.*;

/**
 * Parameters to edit ROI and transform {@link Roi} via the settings (no-destructive)
 */
public class ROIEditor extends AbstractJIPipeParameterCollection {
    private String roiName;
    private double positionX;
    private double positionY;
    private int positionZ;
    private int positionC;
    private int positionT;
    private Color fillColor;
    private Color lineColor;
    private double lineWidth;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private boolean centerScale;

    public ROIEditor() {

    }

    public ROIEditor(Roi roi) {
        this.roiName = roi.getName();
        this.positionX = roi.getXBase();
        this.positionY = roi.getYBase();
        this.positionC = roi.getCPosition();
        this.positionZ = roi.getZPosition();
        this.positionT = roi.getTPosition();
        this.fillColor = roi.getFillColor() == null ? Color.RED : roi.getFillColor();
        this.lineColor = roi.getStrokeColor() == null ? Color.YELLOW : roi.getStrokeColor();
        this.lineWidth = roi.getStrokeWidth();
    }

    @SetJIPipeDocumentation(name = "Location (X)", description = "The X location")
    @JIPipeParameter("position-x")
    public double getPositionX() {
        return positionX;
    }

    @JIPipeParameter("position-x")
    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    @SetJIPipeDocumentation(name = "Location (Y)", description = "The Y location")
    @JIPipeParameter("position-y")
    public double getPositionY() {
        return positionY;
    }

    @JIPipeParameter("position-y")
    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    @SetJIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices.")
    @JIPipeParameter("position-z")
    public int getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(int positionZ) {
        this.positionZ = positionZ;
    }

    @SetJIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
            "that 'Channel' refers to an image slice and not to a pixel channel. " +
            "The first index is 1. If set to zero, the ROI is located on all channels.")
    @JIPipeParameter("position-c")
    public int getPositionC() {
        return positionC;
    }

    @JIPipeParameter("position-c")
    public void setPositionC(int positionC) {
        this.positionC = positionC;
    }

    @SetJIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames.")
    @JIPipeParameter("position-t")
    public int getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(int positionT) {
        this.positionT = positionT;
    }

    @SetJIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ")
    @JIPipeParameter("fill-color")
    public Color getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    @SetJIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ")
    @JIPipeParameter("line-color")
    public Color getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

    @SetJIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ")
    @JIPipeParameter("line-width")
    public double getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    public String getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(String roiName) {
        this.roiName = roiName;
    }

    @SetJIPipeDocumentation(name = "Scale X", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-x")
    public double getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    @SetJIPipeDocumentation(name = "Scale Y", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI. Must be a number.")
    @JIPipeParameter("scale-y")
    public double getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(double scaleY) {
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

    public Roi applyToRoi(Roi target) {
        Roi roi = (Roi) target.clone();
        roi.setPosition(positionC, positionZ, positionT);
        roi.setLocation(positionX, positionY);
        roi.setFillColor(fillColor);
        roi.setStrokeColor(lineColor);
        roi.setStrokeWidth(lineWidth);
        roi.setName(roiName);
        if (scaleX != 1.0 || scaleY != 1.0) {
            roi = RoiScaler.scale(roi, scaleX, scaleY, centerScale);
        }
        return roi;
    }
}
