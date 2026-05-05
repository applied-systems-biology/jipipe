package org.hkijena.jipipe.api.service.components;

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

    public boolean hasEmbeddingModel() {
        return embeddingModelRunner != null;
    }
}
