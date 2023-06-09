package org.hkijena.jipipe.extensions.graphannotation.tools;

import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.extensions.graphannotation.nodes.GroupBoxAnnotationGraphNode;

public class GroupBoxAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<GroupBoxAnnotationGraphNode> {
    public GroupBoxAnnotationGraphNodeTool() {
        super(GroupBoxAnnotationGraphNode.class);
    }
}
