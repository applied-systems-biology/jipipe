package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.ai.JIPipeEmbeddingAIModelRunner;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;

public class JIPipeAIServiceComponent extends JIPipeServiceComponent {

    private JIPipeEmbeddingAIModelRunner embeddingModelRunner;

    public JIPipeAIServiceComponent(JIPipeService service) {
        super(service);
    }

    public boolean hasEmbeddingModel() {
        return embeddingModelRunner != null;
    }
}
