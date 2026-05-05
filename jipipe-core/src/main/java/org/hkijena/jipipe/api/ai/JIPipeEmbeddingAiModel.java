package org.hkijena.jipipe.api.ai;

public interface JIPipeEmbeddingAiModel extends JIPipeAiModel {
    float[] embed(String text);
}
