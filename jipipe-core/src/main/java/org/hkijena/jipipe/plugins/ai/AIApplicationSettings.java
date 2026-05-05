package org.hkijena.jipipe.plugins.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.ai.environments.OptionalEmbeddingModelEnvironment;

import javax.swing.*;

public class AIApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:ai";
    private boolean enableAI = true;
    private OptionalEmbeddingModelEnvironment embeddingModelEnvironment = new OptionalEmbeddingModelEnvironment();

    public AIApplicationSettings() {
    }

    @SetJIPipeDocumentation(name = "Enable AI functionality", description = "Use this setting to disable all AI-related features")
    @JIPipeParameter("enable-ai")
    public boolean isEnableAI() {
        return enableAI;
    }

    @JIPipeParameter("enable-ai")
    public void setEnableAI(boolean enableAI) {
        this.enableAI = enableAI;
    }

    @SetJIPipeDocumentation(name = "Override embedding model", description = "Allows to overwrite the embedding model (semantic search etc.)")
    @JIPipeParameter("embedding-model")
    public OptionalEmbeddingModelEnvironment getEmbeddingModelEnvironment() {
        return embeddingModelEnvironment;
    }

    @JIPipeParameter("embedding-model")
    public void setEmbeddingModelEnvironment(OptionalEmbeddingModelEnvironment embeddingModelEnvironment) {
        this.embeddingModelEnvironment = embeddingModelEnvironment;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/ai.png");
    }

    @Override
    public String getName() {
        return "AI";
    }

    @Override
    public String getDescription() {
        return "Settings related to AI helpers";
    }


}
