package org.hkijena.jipipe.plugins.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.capabilities.JIPipeArtifactCapabilityProvider;
import org.hkijena.jipipe.api.capabilities.JIPipeInstallHint;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelListEnvironment;
import org.hkijena.jipipe.plugins.ai.environments.OptionalEmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.ai.tools.BuildAIEmbeddingIndexTool;
import org.hkijena.jipipe.plugins.ai.tools.ExportGlobalEmbeddingDatabaseTool;
import org.hkijena.jipipe.plugins.ai.tools.ReviewEmbeddingInputTool;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class AIPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    /**
     * The coordinate prefix (group and artifact id, without version and
     * classifier) of the default embedding model artifact. Both the artifact
     * environment query and the capability provider derive from this one
     * constant.
     */
    public static final String DEFAULT_EMBEDDING_ARTIFACT_PREFIX = "cn.ac.baai.bge_small_en";

    /**
     * The version-wildcard glob query of the default embedding model artifact,
     * derived from {@link #DEFAULT_EMBEDDING_ARTIFACT_PREFIX}.
     */
    public static final String DEFAULT_EMBEDDING_ARTIFACT = DEFAULT_EMBEDDING_ARTIFACT_PREFIX + ":*";

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
        registerEnumParameterType("ai-embedding-model-type",
                EmbeddingModelType.class,
                "AI embedding model type",
                "An embedding model type");
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
        registerMenuExtension(BuildAIEmbeddingIndexTool.class);
        registerMenuExtension(ExportGlobalEmbeddingDatabaseTool.class);
        registerMenuExtension(ReviewEmbeddingInputTool.class);

        // Capability provider: the embedding model is satisfied when the default
        // embedding artifact is installed in the local artifact repository. The
        // capability definitions themselves (ai:embedding, ai:mcp, ai:acp-client)
        // are registered by StandardSettingsPlugin (Task 4); only the providers
        // are registered here. The artifact system computes sizes at install
        // time, so the size hint is intentionally left as UNKNOWN_SIZE. The
        // provider carries the coordinate prefix (not the glob); the probe and
        // the installer derive the coordinate boundary from it.
        registerCapabilityProvider(new JIPipeArtifactCapabilityProvider(DEFAULT_EMBEDDING_ARTIFACT_PREFIX)
                .provides("ai:embedding")
                .installHint(new JIPipeInstallHint(JIPipeInstallHint.METHOD_ARTIFACT, JIPipeInstallHint.UNKNOWN_SIZE, null)));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:ai";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
