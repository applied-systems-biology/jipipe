package org.hkijena.jipipe.extensions.graphannotation.tools;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.extensions.graphannotation.nodes.AbstractTextBoxAnnotationGraphNode;
import org.hkijena.jipipe.extensions.graphannotation.nodes.TextBoxAnnotationGraphNode;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;

@JIPipeDocumentation(name = "Text box", description = "A box with a title and optional text")
@JIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class TextBoxAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<TextBoxAnnotationGraphNode> {
    public TextBoxAnnotationGraphNodeTool() {
        super(TextBoxAnnotationGraphNode.class);
    }

    @Override
    protected TextBoxAnnotationGraphNode createAndConfigureNode(Point firstPoint, Point secondPoint) {
        TextBoxAnnotationGraphNode node = super.createAndConfigureNode(firstPoint, secondPoint);
        String title = JOptionPane.showInputDialog(getWorkbench().getWindow(), "Please input the title:", "Create text box", JOptionPane.PLAIN_MESSAGE);
        if(!StringUtils.isNullOrEmpty(title)) {
            node.setTextTitle(title);
        }
        return node;
    }
}
