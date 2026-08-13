package org.hkijena.jipipe.plugins.instrumentation;

import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.instrumentation.InstrumentationApplicationSettings;
import org.hkijena.jipipe.api.instrumentation.operations.*;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.Set;

/**
 * Registers the built-in instrumentation operations.
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class InstrumentationPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public String getName() {
        return "Instrumentation";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides the instrumentation WebSocket API for external automation and diagnostics.");
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:instrumentation";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public Set<org.hkijena.jipipe.JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerApplicationSettingsSheet(new InstrumentationApplicationSettings());

        // Project operations
        registerInstrumentationOperation("list_projects", new ProjectOperations.ListProjects());
        registerInstrumentationOperation("list_operations", new ProjectOperations.ListOperations());

        // Query operations
        registerInstrumentationOperation("query_compartments", new QueryOperations.QueryCompartments());
        registerInstrumentationOperation("query_graph", new QueryOperations.QueryGraph());
        registerInstrumentationOperation("query_node", new QueryOperations.QueryNode());
        registerInstrumentationOperation("query_data", new QueryOperations.QueryData());

        // Execution operations
        registerInstrumentationOperation("run_node", new ExecutionOperations.RunNode());
        registerInstrumentationOperation("run_compartment", new ExecutionOperations.RunCompartment());
        registerInstrumentationOperation("run_pipeline", new ExecutionOperations.RunPipeline());

        // Modification operations
        registerInstrumentationOperation("add_node", new ModificationOperations.AddNode());
        registerInstrumentationOperation("remove_node", new ModificationOperations.RemoveNode());
        registerInstrumentationOperation("add_connection", new ModificationOperations.AddConnection());
        registerInstrumentationOperation("remove_connection", new ModificationOperations.RemoveConnection());
        registerInstrumentationOperation("set_parameter", new ModificationOperations.SetParameter());
        registerInstrumentationOperation("set_project_metadata", new ModificationOperations.SetProjectMetadata());
        registerInstrumentationOperation("add_compartment", new ModificationOperations.AddCompartment());
        registerInstrumentationOperation("rename_compartment", new ModificationOperations.RenameCompartment());

        // Pipeline map operations
        registerInstrumentationOperation("get_pipeline_map", new PipelineMapOperations.GetPipelineMap());
        registerInstrumentationOperation("get_segment_detail", new PipelineMapOperations.GetSegmentDetail());
        registerInstrumentationOperation("search_nodes", new PipelineMapOperations.SearchNodes());
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
