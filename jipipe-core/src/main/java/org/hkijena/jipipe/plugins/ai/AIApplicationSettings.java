package org.hkijena.jipipe.plugins.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.ai.environments.OptionalEmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

import javax.swing.*;

public class AIApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:ai";
    private boolean enableAI = true;
    private OptionalEmbeddingModelEnvironment overrideEmbeddingModelEnvironment = new OptionalEmbeddingModelEnvironment();

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
    public OptionalEmbeddingModelEnvironment getOverrideEmbeddingModelEnvironment() {
        return overrideEmbeddingModelEnvironment;
    }

    @JIPipeParameter("embedding-model")
    public void setOverrideEmbeddingModelEnvironment(OptionalEmbeddingModelEnvironment overrideEmbeddingModelEnvironment) {
        this.overrideEmbeddingModelEnvironment = overrideEmbeddingModelEnvironment;
    }

    public EmbeddingModelEnvironment getEmbeddingModelEnvironment() {
        if(overrideEmbeddingModelEnvironment.isEnabled() && overrideEmbeddingModelEnvironment.getContent().isValid()) {
            return new EmbeddingModelEnvironment(overrideEmbeddingModelEnvironment.getContent());
        }
        else {
            EmbeddingModelEnvironment environment = new EmbeddingModelEnvironment();
            environment.setLoadFromArtifact(true);
            environment.setArtifactQuery(new JIPipeArtifactQueryParameter(AIPlugin.DEFAULT_EMBEDDING_ARTIFACT));
            return environment;
        }
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

    public static AIApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, AIApplicationSettings.class);
    }
}
