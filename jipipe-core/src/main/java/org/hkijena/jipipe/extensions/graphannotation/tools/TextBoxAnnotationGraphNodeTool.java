package org.hkijena.jipipe.extensions.graphannotation.tools;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.extensions.graphannotation.nodes.AbstractTextBoxAnnotationGraphNode;
import org.hkijena.jipipe.extensions.graphannotation.nodes.TextBoxAnnotationGraphNode;

@JIPipeDocumentation(name = "Text box", description = "A box with a title and optional text")
@JIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class TextBoxAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<TextBoxAnnotationGraphNode> {


    public TextBoxAnnotationGraphNodeTool() {
        super(TextBoxAnnotationGraphNode.class);
    }
}
