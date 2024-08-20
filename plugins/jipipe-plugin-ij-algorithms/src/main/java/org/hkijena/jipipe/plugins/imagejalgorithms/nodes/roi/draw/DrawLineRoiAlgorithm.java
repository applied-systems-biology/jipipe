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

import ij.gui.Line;
import ij.gui.Roi;
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
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.Image5DExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;

@SetJIPipeDocumentation(name = "Draw 2D line ROI", description = "Draws one or multiple line ROI.")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "Reference image for the positioning. If not set, all image-related variables will be set to 0", optional = true, create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "ROI", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw")
public class DrawLineRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final VisualLocationROIProperties roiProperties;

    private ParameterCollectionList lines;

    public DrawLineRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualLocationROIProperties();
        this.lines = ParameterCollectionList.containingCollection(LineParameter.class);
        lines.addNewInstance();
    }

    public DrawLineRoiAlgorithm(DrawLineRoiAlgorithm other) {
        super(other);
        this.roiProperties = new VisualLocationROIProperties(other.roiProperties);
        this.lines = new ParameterCollectionList(other.lines);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Generate variables
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        // Collect target and reference
        ROI2DListData target = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
        if (target == null) {
            target = new ROI2DListData();
        } else {
            target = new ROI2DListData(target);
        }

        // Extract reference variables
        ImagePlusData referenceImage = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);
        if (referenceImage != null) {
            Image5DExpressionParameterVariablesInfo.writeToVariables(referenceImage.getImage(), variables);
        } else {
            variables.put("width", 0);
            variables.put("height", 0);
            variables.put("num_c", 0);
            variables.put("num_z", 0);
            variables.put("num_t", 0);
            variables.put("num_d", 0);
        }

        // Create lines
        for (LineParameter lineParameter : lines.mapToCollection(LineParameter.class)) {
            double x1 = lineParameter.getX1().evaluateToDouble(variables);
            double y1 = lineParameter.getY1().evaluateToDouble(variables);
            double x2 = lineParameter.getX2().evaluateToDouble(variables);
            double y2 = lineParameter.getY2().evaluateToDouble(variables);
            Line roi = new Line(x1, y1, x2, y2);
            roiProperties.applyTo(roi, variables);
            target.add(roi);
        }

        // Apply properties
        for (Roi roi : target) {
            roi.setName(roiProperties.getRoiName().evaluateToString(variables));
            roi.setStrokeWidth(roiProperties.getLineWidth());
            roi.setPosition(roiProperties.getPositionC(), roiProperties.getPositionZ(), roiProperties.getPositionT());
            if (roiProperties.getFillColor().isEnabled()) {
                roi.setFillColor(roiProperties.getFillColor().getContent());
            }
            if (roiProperties.getLineColor().isEnabled()) {
                roi.setStrokeColor(roiProperties.getLineColor().getContent());
            }
        }

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Lines", description = "List of lines to be created")
    @JIPipeParameter("lines")
    public ParameterCollectionList getLines() {
        return lines;
    }

    @JIPipeParameter("lines")
    public void setLines(ParameterCollectionList lines) {
        this.lines = lines;
    }

    @SetJIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public VisualLocationROIProperties getRoiProperties() {
        return roiProperties;
    }

    public static class LineParameter extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter x1 = new JIPipeExpressionParameter("0");
        private JIPipeExpressionParameter y1 = new JIPipeExpressionParameter("0");
        private JIPipeExpressionParameter x2 = new JIPipeExpressionParameter("0");
        private JIPipeExpressionParameter y2 = new JIPipeExpressionParameter("0");

        public LineParameter() {
        }

        public LineParameter(LineParameter other) {
            this.x1 = new JIPipeExpressionParameter(other.x1);
            this.y1 = new JIPipeExpressionParameter(other.y1);
            this.x2 = new JIPipeExpressionParameter(other.x2);
            this.y2 = new JIPipeExpressionParameter(other.y2);
        }

        @SetJIPipeDocumentation(name = "X1")
        @JIPipeParameter("x1")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getX1() {
            return x1;
        }

        @JIPipeParameter("x1")
        public void setX1(JIPipeExpressionParameter x1) {
            this.x1 = x1;
        }

        @SetJIPipeDocumentation(name = "Y1")
        @JIPipeParameter("y1")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getY1() {
            return y1;
        }

        @JIPipeParameter("y1")
        public void setY1(JIPipeExpressionParameter y1) {
            this.y1 = y1;
        }

        @SetJIPipeDocumentation(name = "X2")
        @JIPipeParameter("x2")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getX2() {
            return x2;
        }

        @JIPipeParameter("x2")
        public void setX2(JIPipeExpressionParameter x2) {
            this.x2 = x2;
        }

        @SetJIPipeDocumentation(name = "Y2")
        @JIPipeParameter("y2")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = Image5DExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getY2() {
            return y2;
        }

        @JIPipeParameter("y2")
        public void setY2(JIPipeExpressionParameter y2) {
            this.y2 = y2;
        }
    }
}
