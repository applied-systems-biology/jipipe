package org.hkijena.jipipe.api.ai;

public class JIPipeAPIEmbeddingAIModelRunner implements JIPipeEmbeddingAIModelRunner {
    private final String apiBase;
    private final String apiModel;
    private final String apiKey;

    public JIPipeAPIEmbeddingAIModelRunner(String apiBase, String apiModel, String apiKey) {
        this.apiBase = apiBase;
        this.apiModel = apiModel;
        this.apiKey = apiKey;
    }

    @Override
    public float[] embed(String text) {
        return new float[0]; // TODO
    }

    @Override
    public JIPipeAIModelRunnerStatus getStatus() {
        return null; // TODO
    }

    @Override
    public void start() {
        // Nothing to do
    }

    @Override
    public void shutdown() {
        // Nothing to do
    }
}
