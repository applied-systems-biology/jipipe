package org.hkijena.jipipe.plugins.embeddingserver;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.ai.environments.EmbeddingModelEnvironment;
import org.hkijena.jipipe.plugins.embeddingserver.tools.OpenServerMonitorTool;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.*;

/**
 * Plugin that registers the embedding server instance factory.
 *
 * <p>Registers the {@link EmbeddingServerInstance} factory with the
 * {@link JIPipeServerServiceComponent}, allowing embedding model environments to be backed by a
 * spawned ONNX process that exposes an OpenAI-compatible API.</p>
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class EmbeddingServerPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public String getName() {
        return "Embedding Server";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides the embedding server for AI functionality (spawned ONNX process with OpenAI-compatible API)");
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        JIPipeServerServiceComponent serverService = service.getServerService();
        serverService.<EmbeddingModelEnvironment, EmbeddingServerInstance>registerServerInstanceFactory(
                EmbeddingServerInstance.FACTORY_ID,
                EmbeddingModelEnvironment.class,
                EmbeddingServerInstance.class,
                (env, port) -> new EmbeddingServerInstance(env, port),
                JIPipe.RESOURCES.getIcon16("actions/ai.png"));

        registerMenuExtension(OpenServerMonitorTool.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:embedding-server";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
