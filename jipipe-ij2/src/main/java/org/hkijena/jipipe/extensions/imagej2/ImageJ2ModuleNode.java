package org.hkijena.jipipe.extensions.imagej2;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;

public class ImageJ2ModuleNode extends JIPipeIteratingAlgorithm {
    public ImageJ2ModuleNode(JIPipeNodeInfo info) {
        super(info);
    }

    public ImageJ2ModuleNode(ImageJ2ModuleNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }
}
