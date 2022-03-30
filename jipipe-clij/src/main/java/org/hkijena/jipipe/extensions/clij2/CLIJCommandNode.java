package org.hkijena.jipipe.extensions.clij2;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;

public class CLIJCommandNode extends JIPipeIteratingAlgorithm {
    public CLIJCommandNode(JIPipeNodeInfo info) {
        super(info);
    }

    public CLIJCommandNode(CLIJCommandNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }
}
