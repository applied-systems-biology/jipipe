package org.hkijena.jipipe.extensions.imagej2;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;

import java.util.Map;

public class ImageJ2OpAlgorithm extends JIPipeIteratingAlgorithm implements JIPipeCustomParameterCollection {
    private JIPipeDynamicParameterCollection ij2Parameters = new JIPipeDynamicParameterCollection();

    public ImageJ2OpAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImageJ2OpAlgorithm(ImageJ2OpAlgorithm other) {
        super(other);
        this.ij2Parameters = new JIPipeDynamicParameterCollection(other.ij2Parameters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        return ij2Parameters.getParameters();
    }

    @Override
    public boolean getIncludeReflectionParameters() {
        return true;
    }
}
