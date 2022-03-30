package org.hkijena.jipipe.extensions.clij2;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class CLIJCommandNode extends JIPipeIteratingAlgorithm {

    private final JIPipeDynamicParameterCollection clijParameters;

    public CLIJCommandNode(JIPipeNodeInfo info) {
        super(info);
        this.clijParameters = new JIPipeDynamicParameterCollection (((CLIJCommandNodeInfo)info).getNodeParameters());
    }

    public CLIJCommandNode(CLIJCommandNode other) {
        super(other);
        this.clijParameters = new JIPipeDynamicParameterCollection(other.clijParameters);
    }

    @JIPipeDocumentation(name = "CLIJ parameters", description = "Following parameters were extracted from the CLIJ2 operation:")
    @JIPipeParameter("clij-parameters")
    public JIPipeDynamicParameterCollection getClijParameters() {
        return clijParameters;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }
}
