package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.GraphAnnotationsNodeTypeCategory;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;

@SetJIPipeDocumentation(name = "Group box", description = "A text box pre-configured to contain a title at the top left")
@ConfigureJIPipeNode(nodeTypeCategory = GraphAnnotationsNodeTypeCategory.class)
public class GroupBoxAnnotationGraphNode extends TextBoxAnnotationGraphNode {
    public GroupBoxAnnotationGraphNode(JIPipeNodeInfo info) {
        super(info);
        setTextTitle("Group");
        getShapeParameters().getFillColor().setEnabled(false);
        getTextLocation().setAnchor(Anchor.TopLeft);
        getTextLocation().setMarginLeft(8);
        getTextLocation().setMarginTop(8);
        getTextLocation().setMarginRight(8);
        getTextLocation().setMarginBottom(8);
    }

    public GroupBoxAnnotationGraphNode(TextBoxAnnotationGraphNode other) {
        super(other);
    }
}
