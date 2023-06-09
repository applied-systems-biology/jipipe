package org.hkijena.jipipe.extensions.graphannotation.tools;

import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.extensions.graphannotation.nodes.ImageBoxAnnotationGraphNode;

public class ImageBoxAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<ImageBoxAnnotationGraphNode> {
    public ImageBoxAnnotationGraphNodeTool() {
        super(ImageBoxAnnotationGraphNode.class);
    }
}
