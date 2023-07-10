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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.modify;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.Image5DExpressionParameterVariableSource;

import java.awt.geom.Point2D;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Rotate ROI 2D", description = "Rotates all ROI in the ROI list.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class RotateRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private DefaultExpressionParameter angle = new DefaultExpressionParameter("15");
    private DefaultExpressionParameter centerX = new DefaultExpressionParameter("0");
    private DefaultExpressionParameter centerY = new DefaultExpressionParameter("0");

    public RotateRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }


    public RotateRoiAlgorithm(RotateRoiAlgorithm other) {
        super(other);
        this.angle = new DefaultExpressionParameter(other.angle);
        this.centerX = new DefaultExpressionParameter(other.centerX);
        this.centerY = new DefaultExpressionParameter(other.centerY);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ROIListData rois = dataBatch.getInputData("Input", ROIListData.class, progressInfo);
        ImagePlusData imagePlusData = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        if (imagePlusData != null) {
            variables.set("width", imagePlusData.getImage().getWidth());
            variables.set("height", imagePlusData.getImage().getHeight());
            variables.set("num_z", imagePlusData.getImage().getNSlices());
            variables.set("num_c", imagePlusData.getImage().getNChannels());
            variables.set("num_t", imagePlusData.getImage().getNFrames());
            variables.set("num_d", imagePlusData.getImage().getNDimensions());
        }

        double finalAngle = angle.evaluateToDouble(variables);
        double finalCenterX = centerX.evaluateToDouble(variables);
        double finalCenterY = centerY.evaluateToDouble(variables);

        rois = rois.rotate(finalAngle, new Point2D.Double(finalCenterX, finalCenterY));

        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @JIPipeDocumentation(name = "Angle (in °)", description = "The angle of the rotation in degrees")
    @JIPipeParameter("angle")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(DefaultExpressionParameter angle) {
        this.angle = angle;
    }

    @JIPipeDocumentation(name = "Center (X)", description = "The rotation center in X coordinates")
    @JIPipeParameter("center-x")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(DefaultExpressionParameter centerX) {
        this.centerX = centerX;
    }

    @JIPipeDocumentation(name = "Center (Y)", description = "The rotation center in Y coordinates")
    @JIPipeParameter("center-y")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(DefaultExpressionParameter centerY) {
        this.centerY = centerY;
    }
}
