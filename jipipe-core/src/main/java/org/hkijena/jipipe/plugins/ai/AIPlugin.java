package org.hkijena.jipipe.plugins.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelListEnvironment;
import org.hkijena.jipipe.plugins.ai.environments.OptionalEmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class AIPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    public static final String DEFAULT_EMBEDDING_ARTIFACT = "cn.ac.baai.bge_small_en:*";

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "AI Integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides support for local AI models");
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerArtifactEnvironment("ai-embedding-model",
                DEFAULT_EMBEDDING_ARTIFACT,
                JIPipeEnvironmentArchetype.Base,
                EmbeddingModelEnvironment.class,
                OptionalEmbeddingModelEnvironment.class,
                EmbeddingModelListEnvironment.class,
                "Embedding model (AI)",
                "An embedding model for AI functionality",
                JIPipe.RESOURCES.getIcon16("actions/ai.png"));
        registerApplicationSettingsSheet(new AIApplicationSettings());
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ai";
    }

    @Override
    public StringList getDependencyProvides() {
        return new  StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
