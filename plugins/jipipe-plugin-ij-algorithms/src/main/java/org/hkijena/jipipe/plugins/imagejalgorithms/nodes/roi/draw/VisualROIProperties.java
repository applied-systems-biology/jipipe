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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.draw;

import ij.gui.Roi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ROIEditor;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;

import java.awt.*;

/**
 * Limited version of {@link ROIEditor} that only contains properties for creating ROI
 */
public class VisualROIProperties extends AbstractJIPipeParameterCollection {

    private JIPipeExpressionParameter roiName = new JIPipeExpressionParameter("\"unnamed\"");
    private OptionalColorParameter fillColor = new OptionalColorParameter(Color.RED, false);
    private OptionalColorParameter lineColor = new OptionalColorParameter(Color.YELLOW, true);
    private double lineWidth = 1;

    public VisualROIProperties() {

    }

    public VisualROIProperties(VisualROIProperties other) {
        this.roiName = new JIPipeExpressionParameter(other.roiName);
        this.fillColor = new OptionalColorParameter(other.fillColor);
        this.lineColor = new OptionalColorParameter(other.lineColor);
        this.lineWidth = other.lineWidth;
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
    public double getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(JIPipeExpressionParameter roiName) {
        this.roiName = roiName;
    }

    /**
     * Note that this is without cloning the ROI
     *
     * @param target target
     */
    public void applyToRoiReference(Roi target, JIPipeExpressionVariablesMap variables) {
        if (fillColor.isEnabled())
            target.setFillColor(fillColor.getContent());
        if (lineColor.isEnabled())
            target.setStrokeColor(lineColor.getContent());
        target.setStrokeWidth(lineWidth);
        target.setName(roiName.evaluateToString(variables));
    }

    public void applyTo(Roi roi, JIPipeExpressionVariablesMap variables) {
        roi.setName(getRoiName().evaluateToString(variables));
        roi.setStrokeWidth(getLineWidth());
        if (getFillColor().isEnabled()) {
            roi.setFillColor(getFillColor().getContent());
        }
        if (getLineColor().isEnabled()) {
            roi.setStrokeColor(getLineColor().getContent());
        }
    }
}
