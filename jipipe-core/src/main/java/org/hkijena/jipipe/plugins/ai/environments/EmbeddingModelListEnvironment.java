package org.hkijena.jipipe.plugins.ai.environments;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class EmbeddingModelListEnvironment extends JIPipeListParameter<EmbeddingModelEnvironment> {
    public EmbeddingModelListEnvironment() {
        super(EmbeddingModelEnvironment.class);
    }

    public EmbeddingModelListEnvironment(EmbeddingModelListEnvironment other) {
        super(other);
    }
}
