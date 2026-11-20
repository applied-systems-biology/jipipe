package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AsyncAutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.project.io.JIPipeProjectIOs;
import org.hkijena.jipipe.api.service.components.JIPipePluginsServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.project.JIPipeProjectFlowDecisions;
import org.hkijena.jipipe.desktop.app.project.JIPipeProjectSaveListener;
import org.hkijena.jipipe.desktop.app.project.JIPipeProjectSaveTarget;
import org.hkijena.jipipe.desktop.app.project.ProjectOpenDecisions;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.scijava.Context;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Built-in project management operations.
 * These are handled by the server directly (not via JIPipeAutomationAPI)
 * because they require access to the desktop window system.
 * <p>
 * The project lifecycle operations (create_project, open_project, save_project,
 * get_project_metadata; issue #1379) drive the SAME flows as the GUI menus: the
 * decisions inside the interactive prompts live in shared, parameter-driven helpers
 * ({@link JIPipeProjectFlowDecisions}, {@link JIPipeProjectSaveTarget}) that both the
 * dialogs and these operations feed, so the interactive and the automated behavior
 * cannot fork. Created and opened windows come to the front and focused, like the
 * interactive case.
 */
public class ProjectOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The bounded wait for a background flow completion (the asynchronous project
     * load, the non-modal JIPX save). The bound matches the automation layer's ask
     * bound: a paused UI must not hold the endpoint thread forever.
     */
    private static final long COMPLETION_TIMEOUT_MS = 300_000;

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * Runs the callable on the event dispatch thread (directly when already there,
     * otherwise via invokeAndWait). The project-window flows are EDT-confined; the
     * lifecycle operations dispatch off the EDT and bridge their window phases, so
     * the bounded completion waits never block the UI.
     */
    private static <T> T callOnEdt(Callable<T> callable) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        final Object[] result = new Object[1];
        final Exception[] failure = new Exception[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = callable.call();
                } catch (Exception e) {
                    failure[0] = e;
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the event dispatch thread", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(cause.getMessage(), cause);
        }
        if (failure[0] != null) {
            if (failure[0] instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(failure[0].getMessage(), failure[0]);
        }
        //noinspection unchecked
        return (T) result[0];
    }

    /**
     * Runs the runnable on the event dispatch thread (directly when already there,
     * otherwise via invokeAndWait).
     */
    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the event dispatch thread", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    /**
     * Resolves the project window a lifecycle operation acts on (save_project,
     * get_project_metadata): an explicit "project" parameter (a session id as
     * returned by list_projects) wins, otherwise the window the dispatch context is
     * bound to, then the focused project window, then the first open one. Must run
     * on the EDT.
     */
    private static JIPipeDesktopProjectWindow resolveTargetWindow(AutomationContext ctx, JsonNode params) {
        if (params.has("project") && !params.get("project").asText().isBlank()) {
            return AutomationOperationUtils.findProjectWindowByKey(params.get("project").asText().trim());
        }
        return AutomationOperationUtils.resolveDefaultProjectWindow(ctx);
    }

    /**
     * Waits (bounded) for the project load of the given window to complete: the load
     * finishes with a ProjectLoadedEvent, which also regenerates the window's session
     * id. Runs off the EDT (the load completion hops to the EDT itself).
     *
     * @return the window (read its fresh session id after the load)
     * @throws TimeoutException when the load did not complete within the bound (it
     *                          may have failed, or an interactive dialog may be
     *                          waiting in the JIPipe window)
     */
    private static JIPipeDesktopProjectWindow awaitProjectLoad(JIPipeDesktopProjectWindow targetWindow) throws TimeoutException {
        CompletableFuture<JIPipeDesktopProjectWindow> loaded = new CompletableFuture<>();
        JIPipeDesktopProjectWindow.ProjectLoadedEventListener listener = event -> {
            if (event.getWindow() == targetWindow) {
                loaded.complete(event.getWindow());
            }
        };
        JIPipeDesktopProjectWindow.PROJECT_LOADED_EVENT_EMITTER.subscribe(listener);
        try {
            return loaded.get(COMPLETION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the project load", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("The project load failed", e.getCause());
        } finally {
            JIPipeDesktopProjectWindow.PROJECT_LOADED_EVENT_EMITTER.unsubscribe(listener);
        }
    }

    /**
     * The outcome of one automated save.
     */
    private record SaveOutcome(Throwable failure, String stage, boolean cancelled) {
    }

    /**
     * Runs the save on the EDT and waits (bounded) for the real outcome: the
     * non-modal JIPX save reports through its completion listener when the
     * background run finished or was interrupted; the synchronous legacy save
     * reports before the EDT call returns. Runs off the EDT (the listener hops to
     * the EDT itself, so waiting here never blocks the UI).
     *
     * @throws TimeoutException when the save did not complete within the bound (it
     *                          may still be running)
     */
    private static SaveOutcome awaitSave(JIPipeDesktopProjectWindow window, Path savePath, JIPipeProjectSaveTarget.Format format) throws TimeoutException {
        CompletableFuture<SaveOutcome> future = new CompletableFuture<>();
        JIPipeProjectSaveListener listener = new JIPipeProjectSaveListener() {
            @Override
            public void onSaveFinished(Throwable failure, String stage) {
                future.complete(new SaveOutcome(failure, stage, false));
            }

            @Override
            public void onSaveCancelled() {
                future.complete(new SaveOutcome(null, null, true));
            }
        };
        runOnEdt(() -> window.saveProjectTo(savePath, format, listener));
        try {
            return future.get(COMPLETION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the save", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("The save failed", e.getCause());
        }
    }

    /**
     * The metadata name of a project (or "Untitled").
     */
    private static String projectNameOf(JIPipeProject project) {
        if (project == null || project.getMetadata() == null) {
            return "Untitled";
        }
        String name = project.getMetadata().getName();
        return name == null || name.isBlank() ? "Untitled" : name;
    }

    /**
     * A file-system-safe base name for automated extraction directories (the
     * template metadata name or a fallback).
     */
    private static String templateBaseName(JIPipeProjectTemplate template) {
        String name = template.getMetadata() != null ? template.getMetadata().getName() : null;
        if (name == null || name.isBlank()) {
            name = "template-project";
        }
        return name.replaceAll("[^\\w.\\- ]", "_").trim();
    }

    /**
     * The explicit save format of save_project (the schema enum guards the values).
     */
    private static JIPipeProjectSaveTarget.Format parseSaveFormat(JsonNode params) {
        if (!params.has("format") || params.get("format").asText().isBlank()) {
            return JIPipeProjectSaveTarget.Format.AUTO;
        }
        return switch (params.get("format").asText().trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> JIPipeProjectSaveTarget.Format.AUTO;
            case "jipx" -> JIPipeProjectSaveTarget.Format.MODERN;
            case "legacy" -> JIPipeProjectSaveTarget.Format.LEGACY;
            default -> null;
        };
    }

    /**
     * The explicit template data mode of create_project (the schema enum guards the
     * values).
     */
    private static JIPipeProjectFlowDecisions.TemplateDataMode parseTemplateDataMode(JsonNode params) {
        if (!params.has("template-data") || params.get("template-data").asText().isBlank()) {
            return JIPipeProjectFlowDecisions.TemplateDataMode.WITH_DATA;
        }
        return "pipeline-only".equals(params.get("template-data").asText().trim().toLowerCase(Locale.ROOT))
                ? JIPipeProjectFlowDecisions.TemplateDataMode.PIPELINE_ONLY
                : JIPipeProjectFlowDecisions.TemplateDataMode.WITH_DATA;
    }

    /**
     * Reports the lifecycle state of a project window: the metadata name, the
     * current save path, and the modified flag. Read-only.
     */
    public static class GetProjectMetadata implements AutomationOperation {
        @Override
        public String getId() {
            return "get_project_metadata";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Report the lifecycle state of a project window: the project metadata name, the current save path "
                    + "(null for a project that was never saved), and the modified flag (whether the window has "
                    + "unsaved changes). Read-only";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "project": {"type": "string", "description": "Session id of the project window to report (as returned by list_projects). Default: the window this call was routed to, else the focused project window, else the first open one"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeDesktopProjectWindow window = resolveTargetWindow(ctx, params);
            if (window == null) {
                return error("No matching project window is open");
            }
            JIPipeProject project = window.getProject();
            ObjectNode data = mapper.createObjectNode();
            data.put("id", window.getSessionId().toString());
            data.put("name", projectNameOf(project));
            data.put("save_path", window.getProjectSavePath() != null ? window.getProjectSavePath().toString() : null);
            data.put("modified", window.getProjectWorkbench() != null && window.getProjectWorkbench().isProjectModified());
            return data;
        }
    }

    /**
     * Creates a new project (optionally from a registered template) and opens it in
     * a new, front and focused project window - the non-interactive twin of the GUI
     * new-project flows. The zipped-template data decision runs through the shared
     * helper; an optional immediate first save is awaited.
     */
    public static class CreateProject implements AutomationOperation {

        /**
         * The EDT-resolved creation route: either the finished window (default
         * template, pipeline-only template) or the template extraction task
         * (with-data; the window opens after the extraction).
         */
        private record CreationRoute(JIPipeDesktopProjectWindow window, JIPipeProjectTemplate template,
                                     Path extractTarget, Context context) {
        }

        @Override
        public String getId() {
            return "create_project";
        }

        @Override
        public boolean requiresEdt() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Create a new project (optionally from a registered project template) and open it in a new, front "
                    + "and focused project window. Optionally saves the project immediately (save-to) and waits for "
                    + "the save to complete";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". Template ids are the registered JIPipeProjectTemplate ids (the same list "
                    + "the template selection dialog and the template settings offer); an unknown id is refused "
                    + "with the list of registered ids. Omitting template-id uses the default template configured "
                    + "in the JIPipe settings. Unattended runs without a user to answer the permission prompt: "
                    + "start the application with --mcp-permissions allow=create_project,open_project (the CLI "
                    + "override)";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["name"],
                      "properties": {
                        "name": {"type": "string", "description": "The metadata name of the new project"},
                        "template-id": {"type": "string", "description": "A registered JIPipeProjectTemplate id (the same ids the template selection dialog and the template settings offer; an unknown id is refused with the registered ids). Omit for the default template configured in the JIPipe settings"},
                        "template-data": {"type": "string", "enum": ["with-data", "pipeline-only"], "description": "For templates that carry zipped data: with-data extracts the template archive into an automatically chosen directory under the default projects directory and opens the extracted project (the GUI default), pipeline-only loads the pipeline alone. No effect for templates without zipped data or when template-id is omitted"},
                        "save-to": {"type": "string", "description": "Absolute path (.jipx or .jip) inside an allowed export root to save the new project to immediately after creation (the tracked save path switches to this file); omit for an unsaved project"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String name = params.get("name").asText().trim();
            if (name.isBlank()) {
                return error("name must not be blank");
            }
            JIPipeProjectFlowDecisions.TemplateDataMode requestedMode = parseTemplateDataMode(params);

            // The template registry is the non-interactive source of the template
            // ids (the same registry the template selection dialog shows)
            Map<String, JIPipeProjectTemplate> registered = JIPipe.getInstance().getProjectTemplates().getRegisteredTemplates();
            JIPipeProjectTemplate template = null;
            if (params.has("template-id") && !params.get("template-id").asText().isBlank()) {
                String templateId = params.get("template-id").asText().trim();
                template = registered.get(templateId);
                if (template == null) {
                    return error("Unknown template-id '" + templateId + "'. Registered template ids: "
                            + String.join(", ", registered.keySet()));
                }
            }

            // The save target is validated before anything is created (fail fast;
            // the roots for a not-yet-existing project are the export root)
            Path saveTo = null;
            if (params.has("save-to") && !params.get("save-to").asText().isBlank()) {
                AutomationOperationUtils.TargetPath resolved = AutomationOperationUtils.resolveWriteTarget(
                        null, "save-to", params.get("save-to").asText());
                if (resolved.path() == null) {
                    return error(resolved.errorMessage());
                }
                saveTo = resolved.path();
            }

            // EDT: resolve the creation route (the same decisions and flows as the
            // interactive new-project menus)
            final JIPipeProjectTemplate finalTemplate = template;
            final String finalName = name;
            final JIPipeProjectFlowDecisions.TemplateDataMode finalRequestedMode = requestedMode;
            CreationRoute route;
            try {
                route = callOnEdt(() -> {
                    // The context for the new window: an open project window's
                    // context, else the application context
                    Context context = null;
                    for (JIPipeDesktopProjectWindow open : JIPipeDesktopProjectWindow.getOpenWindows()) {
                        context = open.getContext();
                        break;
                    }
                    if (context == null) {
                        context = JIPipe.getInstance().getContext();
                    }
                    if (finalTemplate == null) {
                        // The default template (the GUI "New project" behavior)
                        JIPipeProject project = JIPipeDesktopProjectWindow.getDefaultTemplateProject();
                        project.getMetadata().setName(finalName);
                        return new CreationRoute(JIPipeDesktopProjectWindow.openCreatedProjectWindow(context, project, null, null),
                                null, null, context);
                    }
                    boolean hasZipData = finalTemplate.getZipFile() != null && Files.isRegularFile(finalTemplate.getZipFile());
                    JIPipeProjectFlowDecisions.TemplateDataMode mode = JIPipeProjectFlowDecisions.decideTemplateDataMode(hasZipData, finalRequestedMode);
                    if (mode == JIPipeProjectFlowDecisions.TemplateDataMode.PIPELINE_ONLY) {
                        JIPipeValidationReport report = new JIPipeValidationReport();
                        JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
                        JIPipeProject project = finalTemplate.loadAsProject(report, notifications, JIPipeProgressInfo.STDOUT);
                        project.getMetadata().setName(finalName);
                        return new CreationRoute(JIPipeDesktopProjectWindow.openCreatedProjectWindow(context, project, notifications, report),
                                null, null, context);
                    }
                    // WITH_DATA: the automated extract target (the same
                    // unique-directory convention as the automated crate import,
                    // under the default projects directory)
                    Path baseDir = JIPipeProjectDefaultsApplicationSettings.getInstance().getAbsoluteDefaultProjectsDirectory();
                    PathUtils.createDirectories(baseDir);
                    Path extractTarget = JIPipeProjectFlowDecisions.uniqueSubDirectory(baseDir, templateBaseName(finalTemplate));
                    return new CreationRoute(null, finalTemplate, extractTarget, context);
                });
            } catch (RuntimeException e) {
                return error("Creating the project failed: " + e.getMessage());
            }

            Path extractTarget = null;
            JIPipeDesktopProjectWindow window = route.window();
            if (window == null) {
                // WITH_DATA: extract off the EDT (the same archive call the template
                // extraction run performs), then open the extracted project through
                // the standard open flow with explicit decisions
                extractTarget = route.extractTarget();
                try {
                    PathUtils.createDirectories(extractTarget);
                    ArchiveUtils.decompressZipFile(route.template().getZipFile(), extractTarget, JIPipeProgressInfo.SILENT);
                } catch (Exception e) {
                    return error("Extracting the template data failed: " + e.getMessage());
                }
                Path projectFile = PathUtils.findFileByExtensionRecursivelyIn(extractTarget, ".jip");
                if (projectFile == null) {
                    return error("The template archive did not contain a project file (extracted to " + extractTarget + ")");
                }
                JIPipeDesktopProjectWindow extractedWindow;
                try {
                    extractedWindow = openProjectFileAndAwait(null, route.context(), projectFile,
                            JIPipeProjectFlowDecisions.OpenFormat.AUTO, false);
                } catch (TimeoutException e) {
                    return error("The extracted project did not finish loading within " + (COMPLETION_TIMEOUT_MS / 1000)
                            + " s (an interactive dialog may be waiting in the JIPipe window)");
                }
                window = extractedWindow;
                // The extracted project carries the template's name: apply the
                // requested one (a metadata change does not dirty the project)
                runOnEdt(() -> extractedWindow.getProject().getMetadata().setName(finalName));
            }

            // Created and opened windows come to the front and focused (the user
            // waits for the AI and wants to see what it does)
            runOnEdt(window::bringWindowToFront);

            ObjectNode data = mapper.createObjectNode();
            data.put("window_id", window.getSessionId().toString());
            data.put("project_name", projectNameOf(window.getProject()));
            if (extractTarget != null) {
                data.put("extracted_to", extractTarget.toString());
            }
            if (saveTo != null) {
                putImmediateSave(data, window, saveTo);
            }
            return data;
        }

        /**
         * Runs the immediate first save (save-to) and adds the outcome to the
         * result: the created window stays open even when the save failed.
         */
        private void putImmediateSave(ObjectNode data, JIPipeDesktopProjectWindow window, Path saveTo) {
            JIPipeProjectSaveTarget.ResolvedTarget target = callOnEdt(() -> {
                Path normalized = PathUtils.isJipPath(saveTo) || PathUtils.isJipxPath(saveTo)
                        ? saveTo : PathUtils.ensureExtension(saveTo, ".jipx");
                return JIPipeProjectSaveTarget.resolveSaveTarget(window.getProject(), normalized, JIPipeProjectSaveTarget.Format.AUTO);
            });
            try {
                SaveOutcome outcome = awaitSave(window, target.path(), JIPipeProjectSaveTarget.Format.AUTO);
                if (outcome.cancelled()) {
                    data.put("save_error", "The initial save was cancelled before completion");
                } else if (outcome.failure() != null) {
                    data.put("save_error", "The initial save failed: " + outcome.failure());
                } else {
                    data.put("saved_path", target.path().toString());
                }
            } catch (TimeoutException e) {
                data.put("save_error", "The initial save did not complete within " + (COMPLETION_TIMEOUT_MS / 1000)
                        + " s; it may still be running - check the project window");
            }
        }
    }

    /**
     * Opens a project (.jip, .jipx, RO-Crate container, or JIPipe output folder)
     * through the standard open flow with explicit decisions and waits for the load
     * to complete. The non-interactive twin of the GUI open flow.
     * <p>
     * The path is not restricted to the allowed roots (user ruling 2026-11-17):
     * opening mirrors the GUI, where a user may open any file, and is confirmed by
     * the user through the permission prompt. The write targets of the other
     * lifecycle operations (save-to) stay rooted.
     */
    public static class OpenProject implements AutomationOperation {
        @Override
        public String getId() {
            return "open_project";
        }

        @Override
        public boolean requiresEdt() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Open a project (.jip, .jipx project file, .crate.zip RO-Crate container, or JIPipe output folder) "
                    + "in a new, front and focused project window (or replace the project of the routed window with "
                    + "force-current-window) and wait for the load to complete";
        }

        @Override
        public String getAgentDocumentation() {
            return getDescription() + ". The format parameter replaces the interactive prompts with explicit "
                    + "decisions; with auto the GUI defaults apply without prompting (a .crate.zip imports through "
                    + "the RO-Crate flow, a legacy .jip with a .jipx sibling opens the sibling, an output folder "
                    + "loads its results into a new tab). A project that is already open is brought to the front "
                    + "instead of being loaded twice. Unattended runs without a user to answer the permission "
                    + "prompt: start the application with --mcp-permissions allow=create_project,open_project (the "
                    + "CLI override)";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["path"],
                      "properties": {
                        "path": {"type": "string", "description": "Absolute path to a .jip/.jipx project file, a .crate.zip RO-Crate container, or a JIPipe output folder; any location is allowed (opening is confirmed by the user through the operation's permission prompt)"},
                        "format": {"type": "string", "enum": ["auto", "jipx", "legacy", "crate"], "description": "auto applies the GUI defaults without prompting (a .crate.zip imports through the RO-Crate flow, a legacy .jip with a .jipx sibling opens the sibling, an output folder loads its results into a new tab), jipx and legacy force the sibling choice, crate forces the RO-Crate import (requires a .crate.zip file)"},
                        "force-current-window": {"type": "boolean", "description": "Replace the project of the routed window instead of opening a new window (default: false)"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            // The path is NOT restricted to the allowed roots (user ruling
            // 2026-11-17): opening mirrors the GUI, where a user may open any file,
            // and the Ask permission is the consent gate. The write targets
            // (save-to) stay rooted. Normalized like the previous rooted resolution.
            Path path = Path.of(params.get("path").asText()).toAbsolutePath().normalize();

            JIPipeProjectFlowDecisions.OpenFormat format = JIPipeProjectFlowDecisions.OpenFormat.parse(
                    params.has("format") ? params.get("format").asText() : null);
            if (format == null) {
                return error("format must be one of auto, jipx, legacy, crate");
            }
            boolean forceCurrentWindow = params.has("force-current-window") && params.get("force-current-window").asBoolean();

            // The open flow runs on a project window (the flow initiator for the
            // placement and the dialogs)
            JIPipeDesktopProjectWindow initiator = callOnEdt(() -> AutomationOperationUtils.resolveDefaultProjectWindow(ctx));
            if (initiator == null) {
                return error("No project window is open: open_project uses an open project window as its flow "
                        + "initiator. Create a project first (create_project)");
            }

            if (JIPipeProjectFlowDecisions.routesToCrateImport(path, format)) {
                return executeCrateImport(initiator, path, forceCurrentWindow);
            }
            if (format == JIPipeProjectFlowDecisions.OpenFormat.CRATE) {
                return error("format 'crate' requires a .crate.zip RO-Crate container, but '" + path.getFileName() + "' is not one");
            }

            // Resolve the concrete project file and the format that will be used
            // (the same sibling decision the open flow runs through the shared helper)
            Path projectFile;
            String formatUsed;
            if (Files.isDirectory(path)) {
                if (format != JIPipeProjectFlowDecisions.OpenFormat.AUTO) {
                    return error("format '" + format.name().toLowerCase(Locale.ROOT)
                            + "' only applies to project files; use auto to open an output folder");
                }
                projectFile = JIPipeProjectFlowDecisions.resolveOutputFolderProjectFile(path);
                formatUsed = "output-folder";
            } else if (Files.isRegularFile(path)) {
                Path sibling = PathUtils.findJipxSibling(path);
                JIPipeProjectFlowDecisions.SiblingChoice siblingChoice = JIPipeProjectFlowDecisions.siblingChoiceFor(format);
                if (sibling != null && siblingChoice == JIPipeProjectFlowDecisions.SiblingChoice.OPEN_CRATE) {
                    projectFile = sibling;
                    formatUsed = "jipx";
                } else {
                    projectFile = path;
                    formatUsed = PathUtils.isJipxPath(path) ? "jipx" : "legacy";
                }
            } else {
                return error("The path does not point to an existing file or directory: " + path);
            }

            // Dependency pre-check: refuse with the missing list instead of showing
            // the interactive dependency dialog
            try {
                Set<JIPipeDependency> missing = JIPipePluginsServiceComponent.findUnsatisfiedDependencies(
                        JIPipeProjectIOs.peek(projectFile).getDependencies());
                if (!missing.isEmpty()) {
                    return error("The project requires missing dependencies: "
                            + missing.stream().map(JIPipeDependency::getDependencyId).sorted()
                            .reduce((a, b) -> a + ", " + b).orElse("")
                            + ". Open it in the JIPipe window to resolve them interactively");
                }
            } catch (IOException e) {
                return error("The project could not be read: " + e.getMessage());
            }

            // Already-open pre-check (the same lookup the open flow uses): bring
            // the window to the front and report it instead of loading a second
            // instance (the prompt's default)
            JIPipeDesktopProjectWindow existing = callOnEdt(() -> JIPipeDesktopProjectWindow.findOpenWindowForProjectFile(projectFile));
            if (existing != null) {
                runOnEdt(existing::bringWindowToFront);
                return openResult("already_open", existing.getSessionId().toString(),
                        callOnEdt(() -> projectNameOf(existing.getProject())), projectFile, formatUsed,
                        "already-open", null);
            }

            JIPipeDesktopProjectWindow targetWindow;
            try {
                targetWindow = openProjectFileAndAwait(initiator, null, projectFile, format, forceCurrentWindow);
            } catch (TimeoutException e) {
                return error("The project load did not complete within " + (COMPLETION_TIMEOUT_MS / 1000)
                        + " s (it may have failed, or an interactive dialog may be waiting in the JIPipe window)");
            }
            runOnEdt(targetWindow::bringWindowToFront);
            return openResult("opened", targetWindow.getSessionId().toString(),
                    callOnEdt(() -> projectNameOf(targetWindow.getProject())), projectFile, formatUsed,
                    forceCurrentWindow ? "this-window" : "new-window", null);
        }

        /**
         * The automated RO-Crate import: the extract-target decision is the same
         * helper the GUI import uses for its non-interactive branch (a unique
         * directory in the default projects directory); the extraction itself runs
         * inline (no progress dialog) and the extracted project goes through the
         * standard open flow.
         */
        private JsonNode executeCrateImport(JIPipeDesktopProjectWindow initiator, Path path, boolean forceCurrentWindow) {
            Path baseDir = JIPipeProjectDefaultsApplicationSettings.getInstance().getAbsoluteDefaultProjectsDirectory();
            PathUtils.createDirectories(baseDir);
            Path extractTarget = JIPipeProjectFlowDecisions.crateExtractTarget(path, baseDir);
            try {
                PathUtils.createDirectories(extractTarget);
                ArchiveUtils.decompressZipFile(path, extractTarget, JIPipeProgressInfo.SILENT);
            } catch (Exception e) {
                return error("The RO-Crate import failed: " + e.getMessage());
            }
            Path projectFile = extractTarget.resolve("project.jip");
            if (!Files.isRegularFile(projectFile)) {
                return error("The RO-Crate import did not contain a project.jip file (extracted to " + extractTarget + ")");
            }
            try {
                Set<JIPipeDependency> missing = JIPipePluginsServiceComponent.findUnsatisfiedDependencies(
                        JIPipeProjectIOs.peek(projectFile).getDependencies());
                if (!missing.isEmpty()) {
                    return error("The extracted project requires missing dependencies: "
                            + missing.stream().map(JIPipeDependency::getDependencyId).sorted()
                            .reduce((a, b) -> a + ", " + b).orElse("")
                            + ". Open it in the JIPipe window to resolve them interactively");
                }
            } catch (IOException e) {
                return error("The extracted project could not be read: " + e.getMessage());
            }
            JIPipeDesktopProjectWindow existing = callOnEdt(() -> JIPipeDesktopProjectWindow.findOpenWindowForProjectFile(projectFile));
            if (existing != null) {
                runOnEdt(existing::bringWindowToFront);
                return openResult("already_open", existing.getSessionId().toString(),
                        callOnEdt(() -> projectNameOf(existing.getProject())), projectFile, "crate",
                        "already-open", extractTarget);
            }
            JIPipeDesktopProjectWindow targetWindow;
            try {
                targetWindow = openProjectFileAndAwait(initiator, null, projectFile,
                        JIPipeProjectFlowDecisions.OpenFormat.AUTO, forceCurrentWindow);
            } catch (TimeoutException e) {
                return error("The extracted project did not finish loading within " + (COMPLETION_TIMEOUT_MS / 1000)
                        + " s (an interactive dialog may be waiting in the JIPipe window)");
            }
            runOnEdt(targetWindow::bringWindowToFront);
            return openResult("opened", targetWindow.getSessionId().toString(),
                    callOnEdt(() -> projectNameOf(targetWindow.getProject())), projectFile, "crate",
                    forceCurrentWindow ? "this-window" : "new-window", extractTarget);
        }

        /**
         * The result body of one open.
         */
        private ObjectNode openResult(String status, String windowId, String projectName, Path projectFile,
                                      String format, String openedIn, Path extractedTo) {
            ObjectNode data = mapper.createObjectNode();
            data.put("status", status);
            data.put("window_id", windowId);
            data.put("project_name", projectName);
            data.put("resolved_path", projectFile.toString());
            data.put("format", format);
            data.put("opened_in", openedIn);
            if (extractedTo != null) {
                data.put("extracted_to", extractedTo.toString());
            }
            return data;
        }
    }

    /**
     * Opens a project file through the standard open flow with explicit decisions
     * and waits (bounded) for the load to complete.
     * <p>
     * When no project window is open, a fresh window is created first and the load
     * targets it (the load mechanics of the open flow are unchanged). The caller is
     * responsible for the dependency pre-check (the ops refuse unsatisfied
     * dependencies before reaching here).
     *
     * @param initiator          the flow initiator window (may be null: then the
     *                           first open window is used, or a fresh window is
     *                           created when none is open)
     * @param context            the context for a freshly created initiator window
     *                           (may be null: the application context is used)
     * @param projectFile        the project file to open
     * @param format             the open format (drives the sibling decision inside
     *                           the flow)
     * @param forceCurrentWindow load into the initiator window instead of a new
     *                           window
     * @return the window that holds the project (with its fresh session id)
     * @throws TimeoutException when the load did not complete within the bound
     */
    private static JIPipeDesktopProjectWindow openProjectFileAndAwait(JIPipeDesktopProjectWindow initiator, Context context, Path projectFile,
                                                                      JIPipeProjectFlowDecisions.OpenFormat format, boolean forceCurrentWindow) throws TimeoutException {
        boolean freshInitiator = false;
        if (initiator == null) {
            final JIPipeDesktopProjectWindow[] holder = new JIPipeDesktopProjectWindow[1];
            runOnEdt(() -> {
                for (JIPipeDesktopProjectWindow open : JIPipeDesktopProjectWindow.getOpenWindows()) {
                    holder[0] = open;
                    break;
                }
            });
            initiator = holder[0];
            if (initiator == null) {
                Context contextToUse = context != null ? context : JIPipe.getInstance().getContext();
                Context finalContext = contextToUse;
                initiator = callOnEdt(() -> JIPipeDesktopProjectWindow.newWindow(finalContext));
                freshInitiator = true;
            }
        }
        // A freshly created initiator only exists to receive the load; otherwise
        // the placement follows forceCurrentWindow (false: a new window, the
        // placement prompt's default)
        boolean loadIntoInitiator = freshInitiator || forceCurrentWindow;
        final JIPipeDesktopProjectWindow initiatorWindow = initiator;
        final boolean loadIntoInitiatorFinal = loadIntoInitiator;
        JIPipeDesktopProjectWindow targetWindow = callOnEdt(() -> initiatorWindow.openProject(projectFile,
                loadIntoInitiatorFinal,
                ProjectOpenDecisions.forAutomation(format, loadIntoInitiatorFinal)));
        if (targetWindow == null) {
            throw new RuntimeException("The project open was aborted (the flow refused to open " + projectFile + ")");
        }
        // The flow may have brought an already-open window to the front (a window
        // that opened between the pre-check and the flow): its workbench is up and
        // the save path already matches
        boolean alreadyOpen = callOnEdt(() -> targetWindow.getProjectWorkbench() != null
                && targetWindow.getProjectSavePath() != null
                && targetWindow.getProjectSavePath().equals(projectFile));
        if (alreadyOpen) {
            return targetWindow;
        }
        return awaitProjectLoad(targetWindow);
    }

    /**
     * Saves a project to its current file (the Ctrl+S behavior) or to a new path
     * (save-as) and waits for the save to actually complete: the non-modal project
     * crate (.jipx) save is awaited and the response reports the real outcome, not
     * a scheduled state.
     */
    public static class SaveProject implements AutomationOperation {
        @Override
        public String getId() {
            return "save_project";
        }

        @Override
        public boolean requiresEdt() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Save a project to its current save path (the Ctrl+S behavior) or to a new path (save-as with "
                    + "save-to), and wait for the save to complete. The response reports the real outcome "
                    + "(success, the saved path, and the format), not a scheduled state";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "project": {"type": "string", "description": "Session id of the project window to save (as returned by list_projects). Default: the window this call was routed to, else the focused project window, else the first open one"},
                        "save-to": {"type": "string", "description": "Absolute path (.jipx or .jip) inside an allowed export root to save the project to (save-as: the tracked save path switches to this file). Omit to save to the current save path; a project that was never saved must be given save-to"},
                        "format": {"type": "string", "enum": ["auto", "jipx", "legacy"], "description": "auto honors the target extension (a .jip target stays legacy, anything else is saved as .jipx) and always saves as .jipx when the project cannot be expressed in the legacy format; jipx and legacy force the format (the matching extension is appended to a target without one)"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeDesktopProjectWindow window = callOnEdt(() -> resolveTargetWindow(ctx, params));
            if (window == null) {
                return error("No matching project window is open");
            }
            JIPipeProjectSaveTarget.Format format = parseSaveFormat(params);
            if (format == null) {
                return error("format must be one of auto, jipx, legacy");
            }

            Path savePath;
            if (params.has("save-to") && !params.get("save-to").asText().isBlank()) {
                // The allowed-roots check (the export_data mechanism): the project
                // work directory extends the roots
                AutomationOperationUtils.TargetPath resolved = callOnEdt(() -> AutomationOperationUtils.resolveWriteTarget(
                        window.getProject(), "save-to", params.get("save-to").asText()));
                if (resolved.path() == null) {
                    return error(resolved.errorMessage());
                }
                savePath = resolved.path();
            } else {
                Path current = callOnEdt(window::getProjectSavePath);
                if (current == null) {
                    return error("The project was never saved; provide save-to");
                }
                savePath = current;
            }

            // The shared format resolution (the same decision the interactive save
            // flow runs through). Only AUTO defaults an unrecognized extension to
            // .jipx (the save chooser's first filter); the explicit formats reach
            // the helper verbatim, so legacy appends .jip and jipx appends .jipx.
            JIPipeProjectSaveTarget.ResolvedTarget target = callOnEdt(() -> {
                Path normalized = format == JIPipeProjectSaveTarget.Format.AUTO
                        && !PathUtils.isJipPath(savePath) && !PathUtils.isJipxPath(savePath)
                        ? PathUtils.ensureExtension(savePath, ".jipx") : savePath;
                return JIPipeProjectSaveTarget.resolveSaveTarget(window.getProject(), normalized, format);
            });

            SaveOutcome outcome;
            try {
                outcome = awaitSave(window, target.path(), format);
            } catch (TimeoutException e) {
                return error("The save did not complete within " + (COMPLETION_TIMEOUT_MS / 1000)
                        + " s; it may still be running - check the project window and the project's run logs");
            }

            ObjectNode data = mapper.createObjectNode();
            boolean success = !outcome.cancelled() && outcome.failure() == null;
            data.put("success", success);
            data.put("saved_path", target.path().toString());
            data.put("format", target.modern() ? "jipx" : "legacy");
            if (outcome.cancelled()) {
                data.put("error", "The save was cancelled before completion");
            } else if (outcome.failure() != null) {
                if ("recheck".equals(outcome.stage())) {
                    data.put("error", "The project crate was written to '" + target.path()
                            + "' (the existing file WAS replaced), but loading the saved file failed: " + outcome.failure());
                } else {
                    data.put("error", "Writing the project failed (the existing file was not changed): " + outcome.failure());
                }
            }
            return data;
        }
    }

    public static class ListProjects implements AutomationOperation {
        @Override
        public String getId() {
            return "list_projects";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "List all open projects";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.emptyObjectSchema();
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL_AGGREGATED;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode projects = data.putArray("projects");
            for (var window : JIPipeDesktopProjectWindow.getOpenWindows()) {
                ObjectNode p = projects.addObject();
                p.put("id", window.getSessionId().toString());
                // Same null guards as ProjectSource.openWindows(): a window can exist without a
                // project or without metadata (e.g. while the window is still initializing)
                p.put("name", window.getProject() != null && window.getProject().getMetadata() != null
                        ? window.getProject().getMetadata().getName() : "Untitled");
                p.put("path", window.getProject() != null && window.getProject().getWorkDirectory() != null
                        ? window.getProject().getWorkDirectory().toString() : null);
                p.put("isOpen", true);
            }
            return data;
        }
    }

    public static class ListOperations implements AutomationOperation {
        @Override
        public String getId() {
            return "list_operations";
        }

        @Override
        public String getDescription() {
            return "List all available operations";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.emptyObjectSchema();
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Meta;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.NOT_A_TOOL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode data = mapper.createObjectNode();
            ArrayNode ops = data.putArray("operations");
            var registry = JIPipe.getInstance().getAutomation().getOperationRegistry();
            for (var entry : registry.getAll().entrySet()) {
                ObjectNode op = ops.addObject();
                op.put("id", entry.getKey());
                op.put("description", entry.getValue().getDescription());
                op.put("async", entry.getValue() instanceof AsyncAutomationOperation);
            }
            return data;
        }
    }

    /**
     * Health and version probe for automation agents: reports the JIPipe version,
     * the number of open project windows, the number of registered operations, and
     * the server time. Callable without a project; the context and parameters are
     * ignored. The open-window read is best-effort: a health probe never throws,
     * so an unavailable window system (e.g. a headless environment) reports zero
     * open projects instead of failing.
     */
    public static class GetServerInfo implements AutomationOperation {
        @Override
        public String getId() {
            return "get_server_info";
        }

        @Override
        public boolean requiresEdt() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Health and version probe: JIPipe version, open project count, registered operation count, and server time. Callable without a project";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.emptyObjectSchema();
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public AutomationToolScope getToolScope() {
            return AutomationToolScope.INSTANCE_GLOBAL;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            int openProjects = 0;
            try {
                openProjects = JIPipeDesktopProjectWindow.getOpenWindows().size();
            } catch (Exception e) {
                // Best-effort: a health probe never throws. An unavailable window
                // system reports zero open projects instead of failing the probe.
            }
            ObjectNode data = mapper.createObjectNode();
            data.put("jipipeVersion", JIPipe.getJIPipeVersion());
            data.put("projects", openProjects);
            data.put("operations", JIPipe.getInstance().getAutomation().getOperationRegistry().getIds().size());
            data.put("serverTime", System.currentTimeMillis());
            return data;
        }
    }
}
