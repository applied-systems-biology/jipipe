package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI;

import java.awt.*;

@JIPipeDocumentation(name = "Text box", description = "A box with a title and optional text")
@JIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class TextBoxAnnotationGraphNode extends JIPipeAnnotationGraphNode {

    private OptionalColorParameter fillColor = new OptionalColorParameter(new Color(255, 255, 204), false);
    private Color borderColor = new Color(255, 255, 204).darker();

    private int borderThickness = 1;

    public TextBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
    }

    public TextBoxAnnotationGraphNode(TextBoxAnnotationGraphNode other) {
        super(other);
        this.fillColor = other.fillColor;
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

    @Override
    public boolean isPaintNodeShadow() {
        return false;
    }

    @Override
    public void paintNode(Graphics2D g2, JIPipeAnnotationGraphNodeUI nodeUI) {
        if(fillColor.isEnabled()) {
            g2.setColor(fillColor.getContent());
            g2.fillRect(0, 0, nodeUI.getWidth(), nodeUI.getHeight());
        }
        int finalBorderThickness = Math.max(1, borderThickness);
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(finalBorderThickness));
        g2.drawRect(finalBorderThickness / 2,finalBorderThickness / 2, nodeUI.getWidth() - finalBorderThickness, nodeUI.getHeight() - finalBorderThickness);
    }
}
