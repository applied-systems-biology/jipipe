package org.hkijena.jipipe.plugins.ai.environments;

import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;

public class OptionalEmbeddingModelEnvironment extends JIPipeOptionalParameter<EmbeddingModelEnvironment> {
    public OptionalEmbeddingModelEnvironment() {
        super(EmbeddingModelEnvironment.class);
    }

    public OptionalEmbeddingModelEnvironment(OptionalEmbeddingModelEnvironment other) {
        super(other);
    }

    public OptionalEmbeddingModelEnvironment(EmbeddingModelEnvironment embeddingModelEnvironment, boolean enabled) {
        super(EmbeddingModelEnvironment.class);
        setContent(embeddingModelEnvironment);
        setEnabled(enabled);
    }
}
