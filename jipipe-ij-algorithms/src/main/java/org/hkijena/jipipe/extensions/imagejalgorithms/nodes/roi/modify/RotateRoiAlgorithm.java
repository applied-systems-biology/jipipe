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
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.Image5DExpressionParameterVariablesInfo;

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

        ROIListData rois = iterationStep.getInputData("Input", ROIListData.class, progressInfo);
        ImagePlusData imagePlusData = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
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

    @JIPipeDocumentation(name = "Angle (in °)", description = "The angle of the rotation in degrees")
    @JIPipeParameter("angle")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(JIPipeExpressionParameter angle) {
        this.angle = angle;
    }

    @JIPipeDocumentation(name = "Center (X)", description = "The rotation center in X coordinates")
    @JIPipeParameter("center-x")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(JIPipeExpressionParameter centerX) {
        this.centerX = centerX;
    }

    @JIPipeDocumentation(name = "Center (Y)", description = "The rotation center in Y coordinates")
    @JIPipeParameter("center-y")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(JIPipeExpressionParameter centerY) {
        this.centerY = centerY;
    }
}
