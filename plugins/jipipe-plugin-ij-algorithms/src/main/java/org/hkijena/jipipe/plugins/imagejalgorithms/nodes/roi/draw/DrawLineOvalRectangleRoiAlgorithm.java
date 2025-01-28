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
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;

import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

@SetJIPipeDocumentation(name = "Draw 2D rectangle/oval/line ROI", description = "Draws one or multiple ROI that have two definition points, including lines, rectangles, and ovals.")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "Reference image for the positioning. If not set, all image-related variables will be set to 0", optional = true, create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "ROI", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw")
public class DrawLineOvalRectangleRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final VisualLocationROIProperties roiProperties;

    private ParameterCollectionList entries;

    public DrawLineOvalRectangleRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualLocationROIProperties();
        this.entries = ParameterCollectionList.containingCollection(Entry.class);
        entries.addNewInstance();
    }

    public DrawLineOvalRectangleRoiAlgorithm(DrawLineOvalRectangleRoiAlgorithm other) {
        super(other);
        this.roiProperties = new VisualLocationROIProperties(other.roiProperties);
        this.entries = new ParameterCollectionList(other.entries);
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
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            double x1 = entry.getX1().evaluateToDouble(variables);
            double y1 = entry.getY1().evaluateToDouble(variables);
            double x2 = entry.getX2().evaluateToDouble(variables);
            double y2 = entry.getY2().evaluateToDouble(variables);
            switch (entry.getType()) {
                case Line: {
                    Line roi = new Line(x1, y1, x2, y2);
                    roiProperties.applyTo(roi, variables);
                    target.add(roi);
                }
                break;
                case Oval: {
                    Rectangle2D.Double rectangle = ImageJUtils.pointsToRectangle(x1, y1, x2, y2);
                    OvalRoi roi = new OvalRoi(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                    roiProperties.applyTo(roi, variables);
                    target.add(roi);
                }
                break;
                case Rectangle: {
                    double arcWidth = entry.getArcWidth().evaluateToDouble(variables);
                    double arcHeight = entry.getArcHeight().evaluateToDouble(variables);
                    Rectangle2D.Double rectangle = ImageJUtils.pointsToRectangle(x1, y1, x2, y2);
                    if (arcWidth <= 0 && arcHeight <= 0) {
                        ShapeRoi roi = new ShapeRoi(rectangle);
                        roiProperties.applyTo(roi, variables);
                        target.add(roi);
                    } else {
                        RoundRectangle2D rectangle2D = new RoundRectangle2D.Double(rectangle.x, rectangle.y, rectangle.width, rectangle.height, arcWidth, arcHeight);
                        ShapeRoi roi = new ShapeRoi(rectangle2D);
                        roiProperties.applyTo(roi, variables);
                        target.add(roi);
                    }
                }
                break;
                default:
                    throw new RuntimeException("Unknown entry type: " + entry.getType());
            }

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

    @SetJIPipeDocumentation(name = "ROI", description = "List of ROI to be created")
    @JIPipeParameter("lines")
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("lines")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @SetJIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public VisualLocationROIProperties getRoiProperties() {
        return roiProperties;
    }

    public enum RoiType {
        Line,
        Rectangle,
        Oval
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter x1 = new JIPipeExpressionParameter("0");
        private JIPipeExpressionParameter y1 = new JIPipeExpressionParameter("0");
        private JIPipeExpressionParameter x2 = new JIPipeExpressionParameter("0");
        private JIPipeExpressionParameter y2 = new JIPipeExpressionParameter("0");
        private RoiType type = RoiType.Line;
        private JIPipeExpressionParameter arcWidth = new JIPipeExpressionParameter("0");
        private JIPipeExpressionParameter arcHeight = new JIPipeExpressionParameter("0");

        public Entry() {
        }

        public Entry(Entry other) {
            this.x1 = new JIPipeExpressionParameter(other.x1);
            this.y1 = new JIPipeExpressionParameter(other.y1);
            this.x2 = new JIPipeExpressionParameter(other.x2);
            this.y2 = new JIPipeExpressionParameter(other.y2);
            this.type = other.type;
            this.arcWidth = new JIPipeExpressionParameter(other.arcWidth);
            this.arcHeight = new JIPipeExpressionParameter(other.arcHeight);
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

        @SetJIPipeDocumentation(name="ROI type")
        @JIPipeParameter("type")
        public RoiType getType() {
            return type;
        }

        @JIPipeParameter("type")
        public void setType(RoiType type) {
            this.type = type;
        }

        @SetJIPipeDocumentation(name = "Rectangle arc width", description = "Only applicable if the type is set to 'Rectangle'")
        @JIPipeParameter("arc-width")
        public JIPipeExpressionParameter getArcWidth() {
            return arcWidth;
        }

        @JIPipeParameter("arc-width")
        public void setArcWidth(JIPipeExpressionParameter arcWidth) {
            this.arcWidth = arcWidth;
        }

        @SetJIPipeDocumentation(name = "Rectangle arc height", description = "Only applicable if the type is set to 'Rectangle'")
        @JIPipeParameter("arc-height")
        public JIPipeExpressionParameter getArcHeight() {
            return arcHeight;
        }

        @JIPipeParameter("arc-height")
        public void setArcHeight(JIPipeExpressionParameter arcHeight) {
            this.arcHeight = arcHeight;
        }
    }
}
