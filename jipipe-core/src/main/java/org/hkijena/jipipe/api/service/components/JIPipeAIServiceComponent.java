package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.ai.JIPipeAIModelRunnerStatus;
import org.hkijena.jipipe.api.ai.JIPipeEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;

public class JIPipeAIServiceComponent extends JIPipeServiceComponent {

    private JIPipeEmbeddingAIModelRunner embeddingModelRunner;

    public JIPipeAIServiceComponent(JIPipeService service) {
        super(service);
    }

    public boolean hasConfiguredAndReadyEmbeddingModel() {
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        EmbeddingModelEnvironment environment = settings.getEmbeddingModelEnvironment();
        if(environment.isLoadFromArtifact()) {
            // Resolve the artifact and check if it's downloaded
            JIPipeArtifact artifact = getService().getArtifacts()
                    .queryPreferredCachedArtifact(environment.getArtifactQuery().getQuery());
            return artifact instanceof JIPipeLocalArtifact;
        }
        else {
            // Do a local configuration check
            return environment.isValid();
        }
    }

    public JIPipeAIModelRunnerStatus getEmbeddingModelStatus() {
        if(embeddingModelRunner != null) {
            return embeddingModelRunner.getStatus();
        }
        return JIPipeAIModelRunnerStatus.Unloaded;
    }

    public boolean hasEmbeddingModel() {
        return embeddingModelRunner != null;
    }

    public void startEmbeddingModelNow(JIPipeProgressInfo progressInfo) {
        // TODO: blocking start
    }

    public void stopEmbeddingModelNow(JIPipeProgressInfo progressInfo) {
        // TODO: blocking shutdown
    }

    public void tryStartEmbeddingModel() {
        // TODO: enqueue start
    }

    public void tryStopEmbeddingModel() {
        // TODO: enqueue stop
    }
}
