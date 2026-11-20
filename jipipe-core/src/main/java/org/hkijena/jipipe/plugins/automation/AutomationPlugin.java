package org.hkijena.jipipe.plugins.automation;

import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.operations.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPPermissionsEditorUI;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.plugins.automation.settings.MCPPermissionStates;
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
        return "Automation";
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
        registerApplicationSettingsSheet(new MCPApplicationSettings());
        registerParameterType("org.hkijena.jipipe:mcp-permission-states", MCPPermissionStates.class,
                JIPipeParameterArchetype.Value, "MCP permissions",
                "User-chosen permission state per automation operation (operation id to state name)",
                JIPipeDesktopMCPPermissionsEditorUI.class);

        // Project operations
        registerAutomationOperation("list_projects", new ProjectOperations.ListProjects());
        registerAutomationOperation("list_operations", new ProjectOperations.ListOperations());
        registerAutomationOperation("create_project", new ProjectOperations.CreateProject());
        registerAutomationOperation("open_project", new ProjectOperations.OpenProject());
        registerAutomationOperation("save_project", new ProjectOperations.SaveProject());
        registerAutomationOperation("get_project_metadata", new ProjectOperations.GetProjectMetadata());

        // Query operations
        registerAutomationOperation("query_compartments", new QueryOperations.QueryCompartments());
        registerAutomationOperation("query_graph", new QueryOperations.QueryGraph());
        registerAutomationOperation("query_annotations", new QueryOperations.QueryAnnotations());
        registerAutomationOperation("query_node", new QueryOperations.QueryNode());
        registerAutomationOperation("get_parameter_diff", new QueryOperations.GetParameterDiff());
        registerAutomationOperation("query_data", new QueryOperations.QueryData());
        registerAutomationOperation("find_nodes", new QueryOperations.FindNodes());

        // Execution operations
        registerAutomationOperation("run_node", new ExecutionOperations.RunNode());
        registerAutomationOperation("run_compartment", new ExecutionOperations.RunCompartment());
        registerAutomationOperation("run_pipeline", new ExecutionOperations.RunPipeline());
        registerAutomationOperation("update_cache", new CacheOperations.UpdateCache());
        registerAutomationOperation("get_job_status", new ExecutionOperations.GetJobStatus());
        registerAutomationOperation("cancel_job", new ExecutionOperations.CancelJob());
        registerAutomationOperation("arrange_nodes", new ArrangeOperations.ArrangeNodes());

        // Modification operations
        registerAutomationOperation("add_node", new ModificationOperations.AddNode());
        registerAutomationOperation("remove_node", new ModificationOperations.RemoveNode());
        registerAutomationOperation("add_connection", new ModificationOperations.AddConnection());
        registerAutomationOperation("remove_connection", new ModificationOperations.RemoveConnection());
        registerAutomationOperation("set_parameter", new ModificationOperations.SetParameter());
        registerAutomationOperation("set_project_metadata", new ModificationOperations.SetProjectMetadata());
        registerAutomationOperation("add_compartment", new ModificationOperations.AddCompartment());
        registerAutomationOperation("rename_compartment", new ModificationOperations.RenameCompartment());

        // Slot operations
        registerAutomationOperation("add_slot", new SlotOperations.AddSlot());
        registerAutomationOperation("remove_slot", new SlotOperations.RemoveSlot());

        // User path operations
        registerAutomationOperation("list_user_paths", new UserPathOperations.ListUserPaths());
        registerAutomationOperation("set_user_path", new UserPathOperations.SetUserPath());
        registerAutomationOperation("remove_user_path", new UserPathOperations.RemoveUserPath());

        // Pipeline map operations
        registerAutomationOperation("get_pipeline_map", new PipelineMapOperations.GetPipelineMap());
        registerAutomationOperation("get_segment_detail", new PipelineMapOperations.GetSegmentDetail());
        registerAutomationOperation("search_nodes", new PipelineMapOperations.SearchNodes());

        // Info operations
        registerAutomationOperation("get_project_info", new InfoOperations.GetProjectInfo());
        registerAutomationOperation("get_project_report", new InfoOperations.GetProjectReport());
        registerAutomationOperation("check_fair", new InfoOperations.CheckFair());
        registerAutomationOperation("list_algorithms", new InfoOperations.ListAlgorithms());
        registerAutomationOperation("get_node_docs", new InfoOperations.GetNodeDocs());

        // Data operations
        registerAutomationOperation("describe_data", new DataOperations.DescribeData());
        registerAutomationOperation("export_data", new DataOperations.ExportData());
        registerAutomationOperation("get_thumbnail", new DataOperations.GetThumbnail());
        registerAutomationOperation("open_data_viewer", new DataOperations.OpenDataViewer());

        // UI state operations
        registerAutomationOperation("get_ui_state", new UiStateOperations.GetUiState());
        registerAutomationOperation("navigate_to_node", new UiStateOperations.NavigateToNode());
        registerAutomationOperation("fit_to_nodes", new UiStateOperations.FitToNodes());
        registerAutomationOperation("clear_selection", new UiStateOperations.ClearSelection());

        // UI interaction operations (structured actions/docks and synthetic input)
        registerAutomationOperation("fire_action", new UiInteractionOperations.FireAction());
        registerAutomationOperation("list_ui_actions", new UiInteractionOperations.ListUiActions());
        registerAutomationOperation("set_dock_visible", new UiInteractionOperations.SetDockVisible());
        registerAutomationOperation("list_docks", new UiInteractionOperations.ListDocks());
        registerAutomationOperation("ui_probe", new UiInteractionOperations.UiProbe());
        registerAutomationOperation("ui_click", new UiInteractionOperations.UiClick());
        registerAutomationOperation("ui_drag", new UiInteractionOperations.UiDrag());
        registerAutomationOperation("ui_scroll", new UiInteractionOperations.UiScroll());
        registerAutomationOperation("ui_type_text", new UiInteractionOperations.UiTypeText());

        // Data viewer operations
        registerAutomationOperation("get_viewer_state", new ViewerOperations.GetViewerState());
        registerAutomationOperation("goto_viewer_row", new ViewerOperations.GotoViewerRow());
        registerAutomationOperation("close_viewer", new ViewerOperations.CloseViewer());

        // Diagnostics operations
        registerAutomationOperation("get_errors", new DiagnosticsOperations.GetErrors());
        registerAutomationOperation("get_log", new DiagnosticsOperations.GetLog());

        // Screenshot operations
        registerAutomationOperation("screenshot_canvas", new ScreenshotOperations.ScreenshotCanvas());
        registerAutomationOperation("screenshot_minimap", new ScreenshotOperations.ScreenshotMinimap());
        registerAutomationOperation("screenshot_ui", new ScreenshotOperations.ScreenshotUi());
        registerAutomationOperation("screenshot_window", new ScreenshotOperations.ScreenshotWindow());
        registerAutomationOperation("export_graph_image", new ScreenshotOperations.ExportGraphImage());

        // RO-Crate operations
        registerAutomationOperation("create_ro_crate", new ROCrateOperations.CreateROCrate());
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
