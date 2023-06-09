package org.hkijena.jipipe.extensions.graphannotation.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;

@JIPipeDocumentation(name = "Group box", description = "A text box pre-configured to contain a title at the top left")
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
