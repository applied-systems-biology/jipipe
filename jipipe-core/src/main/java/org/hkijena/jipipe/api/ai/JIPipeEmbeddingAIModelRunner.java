package org.hkijena.jipipe.api.ai;

public interface JIPipeEmbeddingAIModelRunner extends JIPipeAIModelRunner {
    float[] embed(String text);
}
