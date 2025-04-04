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

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions.Image5DExpressionParameterVariablesInfo;

import java.awt.geom.Point2D;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Rotate 2D ROI (old)", description = "Rotates all ROI in the ROI list. A newer algorithm that allows to do scaling/rotating/translation in one is available.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class RotateRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private JIPipeExpressionParameter angle = new JIPipeExpressionParameter("15");
    private JIPipeExpressionParameter centerX = new JIPipeExpressionParameter("0");
    private JIPipeExpressionParameter centerY = new JIPipeExpressionParameter("0");

    public RotateRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }


    public RotateRoiAlgorithm(RotateRoiAlgorithm other) {
        super(other);
        this.angle = new JIPipeExpressionParameter(other.angle);
        this.centerX = new JIPipeExpressionParameter(other.centerX);
        this.centerY = new JIPipeExpressionParameter(other.centerY);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ROI2DListData rois = iterationStep.getInputData("Input", ROI2DListData.class, progressInfo);
        ImagePlusData imagePlusData = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

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

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Angle (in °)", description = "The angle of the rotation in degrees")
    @JIPipeParameter("angle")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(JIPipeExpressionParameter angle) {
        this.angle = angle;
    }

    @SetJIPipeDocumentation(name = "Origin (X)", description = "The rotation center in X coordinates")
    @JIPipeParameter("center-x")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(JIPipeExpressionParameter centerX) {
        this.centerX = centerX;
    }

    @SetJIPipeDocumentation(name = "Origin (Y)", description = "The rotation center in Y coordinates")
    @JIPipeParameter("center-y")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(JIPipeExpressionParameter centerY) {
        this.centerY = centerY;
    }
}
