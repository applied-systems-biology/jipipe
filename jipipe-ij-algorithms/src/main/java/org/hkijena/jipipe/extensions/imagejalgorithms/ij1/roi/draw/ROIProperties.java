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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.draw;

import ij.gui.Roi;
import ij.gui.TextRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ROIEditor;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;

import java.awt.*;

/**
 * Limited version of {@link ROIEditor} that only contains properties for creating ROI
 */
public class ROIProperties extends AbstractJIPipeParameterCollection {

    private DefaultExpressionParameter roiName = new DefaultExpressionParameter("\"unnamed\"");
    private int positionZ = 0;
    private int positionC = 0;
    private int positionT = 0;
    private OptionalColorParameter fillColor = new OptionalColorParameter(Color.RED, false);
    private OptionalColorParameter lineColor = new OptionalColorParameter(Color.YELLOW, true);
    private double lineWidth = 1;

    public ROIProperties() {

    }

    public ROIProperties(ROIProperties other) {
        this.roiName = new DefaultExpressionParameter(other.roiName);
        this.positionZ = other.positionZ;
        this.positionC = other.positionC;
        this.positionT = other.positionT;
        this.fillColor = new OptionalColorParameter(other.fillColor);
        this.lineColor = new OptionalColorParameter(other.lineColor);
        this.lineWidth = other.lineWidth;
    }

    @JIPipeDocumentation(name = "Slice position (Z)", description = "Allows to relocate the ROI to a different Z-position. " +
            "The first index is 1. If set to zero, the ROI is located on all slices.")
    @JIPipeParameter("position-z")
    public int getPositionZ() {
        return positionZ;
    }

    @JIPipeParameter("position-z")
    public void setPositionZ(int positionZ) {
        this.positionZ = positionZ;
    }

    @JIPipeDocumentation(name = "Slice position (Channel)", description = "Allows to relocate the ROI to a different channel-position. Please note " +
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

    @JIPipeDocumentation(name = "Slice position (Frame)", description = "Allows to relocate the ROI to a different frame/time-position. " +
            "The first index is 1. If set to zero, the ROI is located on all frames.")
    @JIPipeParameter("position-t")
    public int getPositionT() {
        return positionT;
    }

    @JIPipeParameter("position-t")
    public void setPositionT(int positionT) {
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
    public double getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    @JIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(DefaultExpressionParameter roiName) {
        this.roiName = roiName;
    }

    /**
     * Note that this is without cloning the ROI
     *
     * @param target target
     */
    public void applyToRoiReference(Roi target, ExpressionVariables variables) {
        target.setPosition(positionC, positionZ, positionT);
        if (fillColor.isEnabled())
            target.setFillColor(fillColor.getContent());
        if (lineColor.isEnabled())
            target.setStrokeColor(lineColor.getContent());
        target.setStrokeWidth(lineWidth);
        target.setName(roiName.evaluateToString(variables));
    }

    public void applyTo(Roi roi, ExpressionVariables variables) {
        roi.setName(getRoiName().evaluateToString(variables));
        roi.setStrokeWidth(getLineWidth());
        roi.setPosition(getPositionC(), getPositionZ(), getPositionT());
        if(getFillColor().isEnabled()) {
            roi.setFillColor(getFillColor().getContent());
        }
        if(getLineColor().isEnabled()) {
            roi.setStrokeColor(getLineColor().getContent());
        }
    }
}
