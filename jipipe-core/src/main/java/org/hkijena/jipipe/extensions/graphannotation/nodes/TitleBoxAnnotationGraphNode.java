package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeAnnotationGraphNodeUI;

import java.awt.*;

@JIPipeDocumentation(name = "Title box", description = "A box with a title and optional text")
@JIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class TitleBoxAnnotationGraphNode extends JIPipeAnnotationGraphNode {

    private Color fillColor = new Color(255, 255, 204);

    public TitleBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
    }

    public TitleBoxAnnotationGraphNode(TitleBoxAnnotationGraphNode other) {
        super(other);
        this.fillColor = other.fillColor;
    }

    @JIPipeDocumentation(name = "Fill color", description = "The fill color of this node")
    @JIPipeParameter("fill-color")
    public Color getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    @Override
    public void paintNode(Graphics2D g2, JIPipeAnnotationGraphNodeUI nodeUI) {
        g2.setColor(fillColor);
        g2.fillRect(0,0, nodeUI.getWidth(), nodeUI.getHeight());
    }
}
