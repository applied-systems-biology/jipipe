package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI;

import java.awt.*;

@JIPipeDocumentation(name = "Text box", description = "An annotation that contains text")
@JIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class TextBoxAnnotationGraphNode extends AbstractTextBoxAnnotationGraphNode {
    private final ShapeParameters shapeParameters;

    public TextBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
        this.shapeParameters = new ShapeParameters();
        this.setTextTitle("Text box");
        registerSubParameter(shapeParameters);
    }

    public TextBoxAnnotationGraphNode(TextBoxAnnotationGraphNode other) {
        super(other);
        this.shapeParameters = new ShapeParameters(other.shapeParameters);
        registerSubParameter(shapeParameters);
    }

    @JIPipeDocumentation(name = "Shape", description = "The following settings allow to modify the text box shape")
    @JIPipeParameter(value = "shape-parameters", uiOrder = -10)
    public ShapeParameters getShapeParameters() {
        return shapeParameters;
    }

    @Override
    protected void paintShape(Graphics2D g2, JIPipeAnnotationGraphNodeUI nodeUI, double zoom) {
        int nodeWidth = nodeUI.getWidth();
        int nodeHeight = nodeUI.getHeight();
        if (shapeParameters.fillColor.isEnabled()) {
            g2.setColor(shapeParameters.fillColor.getContent());
            g2.fillRect(0, 0, nodeWidth, nodeHeight);
        }
        int finalBorderThickness = (int) Math.max(1, shapeParameters.borderThickness * zoom);
        g2.setColor(shapeParameters.borderColor);
        g2.setStroke(new BasicStroke(finalBorderThickness));
        g2.drawRect(finalBorderThickness / 2, finalBorderThickness / 2, nodeWidth - finalBorderThickness, nodeHeight - finalBorderThickness);
    }

    @Override
    protected int getBorderThickness() {
        return shapeParameters.borderThickness;
    }

    public static class ShapeParameters extends AbstractJIPipeParameterCollection {
        private OptionalColorParameter fillColor = new OptionalColorParameter(new Color(255, 255, 204), true);
        private Color borderColor = new Color(255, 255, 204).darker();
        private int borderThickness = 1;

        public ShapeParameters() {

        }

        public ShapeParameters(ShapeParameters other) {
            this.fillColor = new OptionalColorParameter(other.fillColor);
            this.borderColor = other.borderColor;
            this.borderThickness = other.borderThickness;
        }

        @JIPipeDocumentation(name = "Fill color", description = "The fill color of this node")
        @JIPipeParameter("fill-color")
        public OptionalColorParameter getFillColor() {
            return fillColor;
        }

        @JIPipeParameter("fill-color")
        public void setFillColor(OptionalColorParameter fillColor) {
            this.fillColor = fillColor;
        }

        @JIPipeDocumentation(name = "Border color", description = "The border color of this node")
        @JIPipeParameter("border-color")
        public Color getBorderColor() {
            return borderColor;
        }

        @JIPipeParameter("border-color")
        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        @JIPipeDocumentation(name = "Border thickness", description = "The thickness of the border")
        @JIPipeParameter("border-thickness")
        public int getBorderThickness() {
            return borderThickness;
        }

        @JIPipeParameter("border-thickness")
        public void setBorderThickness(int borderThickness) {
            this.borderThickness = borderThickness;
        }
    }
}
