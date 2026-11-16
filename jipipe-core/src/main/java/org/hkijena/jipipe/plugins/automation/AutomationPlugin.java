package org.hkijena.jipipe.plugins.automation;

import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.instrumentation.InstrumentationApplicationSettings;
import org.hkijena.jipipe.api.automation.operations.*;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.Set;

/**
 * Registers the built-in automation operations.
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class AutomationPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public String getName() {
        return "Instrumentation";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides the automation operation API for the automation layer used by external automation and diagnostics tools.");
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
        registerApplicationSettingsSheet(new MCPApplicationSettings());

        // Project operations
        registerAutomationOperation("list_projects", new ProjectOperations.ListProjects());
        registerAutomationOperation("list_operations", new ProjectOperations.ListOperations());

        // Query operations
        registerAutomationOperation("query_compartments", new QueryOperations.QueryCompartments());
        registerAutomationOperation("query_graph", new QueryOperations.QueryGraph());
        registerAutomationOperation("query_node", new QueryOperations.QueryNode());
        registerAutomationOperation("query_data", new QueryOperations.QueryData());

        // Execution operations
        registerAutomationOperation("run_node", new ExecutionOperations.RunNode());
        registerAutomationOperation("run_compartment", new ExecutionOperations.RunCompartment());
        registerAutomationOperation("run_pipeline", new ExecutionOperations.RunPipeline());

        // Modification operations
        registerAutomationOperation("add_node", new ModificationOperations.AddNode());
        registerAutomationOperation("remove_node", new ModificationOperations.RemoveNode());
        registerAutomationOperation("add_connection", new ModificationOperations.AddConnection());
        registerAutomationOperation("remove_connection", new ModificationOperations.RemoveConnection());
        registerAutomationOperation("set_parameter", new ModificationOperations.SetParameter());
        registerAutomationOperation("set_project_metadata", new ModificationOperations.SetProjectMetadata());
        registerAutomationOperation("add_compartment", new ModificationOperations.AddCompartment());
        registerAutomationOperation("rename_compartment", new ModificationOperations.RenameCompartment());

        // Pipeline map operations
        registerAutomationOperation("get_pipeline_map", new PipelineMapOperations.GetPipelineMap());
        registerAutomationOperation("get_segment_detail", new PipelineMapOperations.GetSegmentDetail());
        registerAutomationOperation("search_nodes", new PipelineMapOperations.SearchNodes());

        // Info operations
        registerAutomationOperation("get_project_info", new InfoOperations.GetProjectInfo());
        registerAutomationOperation("list_algorithms", new InfoOperations.ListAlgorithms());
        registerAutomationOperation("get_node_docs", new InfoOperations.GetNodeDocs());

        // RO-Crate operations
        registerAutomationOperation("create_ro_crate", new ROCrateOperations.CreateROCrate());
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
