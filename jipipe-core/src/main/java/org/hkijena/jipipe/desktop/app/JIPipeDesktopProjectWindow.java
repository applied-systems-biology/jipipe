/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app;

import net.java.balloontip.BalloonTip;
import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.project.io.JIPipeLegacyProjectWriter;
import org.hkijena.jipipe.api.project.io.JIPipeProjectIOs;
import org.hkijena.jipipe.api.project.io.JIPipeProjectPeek;
import org.hkijena.jipipe.api.project.jipx.JIPipeXProjectWriter;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.api.service.components.JIPipePluginsServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.project.*;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.desktop.commons.components.app.JIPipeDesktopSplashScreen;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.dialogs.Button;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEventEmitter;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEventEmitter;
import org.hkijena.jipipe.plugins.cef.JIPipeCefClientService;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Window that holds an {@link JIPipeDesktopProjectWorkbench} instance
 */
public class JIPipeDesktopProjectWindow extends JFrame {
    public static final WindowOpenedEventEmitter WINDOW_OPENED_EVENT_EMITTER = new WindowOpenedEventEmitter();
    public static final WindowClosedEventEmitter WINDOW_CLOSED_EVENT_EMITTER = new WindowClosedEventEmitter();
    private static final Set<JIPipeDesktopProjectWindow> OPEN_WINDOWS = new HashSet<>();
    private final Context context;
    private final List<BalloonTip> registeredBalloons = new ArrayList<>();
    private final JIPipeCefClientService cefClientService;
    private JIPipeProject project;
    private JIPipeDesktopProjectWorkbench projectWorkbench;
    private Path projectSavePath;
    private UUID sessionId = UUID.randomUUID();

    /**
     * Per-project queue for JIPX saves (feature #753). Lazily created on the
     * first JIPX save; a NEW executor instance per window/project (NOT the
     * global singleton), so a large save in one project never stalls a save
     * in another, while same-project saves (manual vs. autosave) still
     * serialize.
     */
    private JIPipeQueuedRunnableExecutor saveQueue;

    /**
     * Feature #1359: non-null while an in-window loading/closing screen is
     * shown (the content pane replacement for the former modal dialogs).
     */
    private JIPipeDesktopProjectWindowLoadingScreen loadingScreen;

    /**
     * Feature #1359: the content pane shown before the current loading
     * screen. Restored when a load into this window fails or is cancelled.
     * Only captured when a workbench is displayed (a freshly created window
     * has nothing to restore and disposes itself instead).
     */
    private Container contentBeforeLoadingScreen;

    /**
     * Feature #1359: re-entry guard for the asynchronous window teardown.
     */
    private boolean disposing;

    /**
     * Feature #1359: the close callbacks (EDT) per project whose background
     * close is currently in flight. Requests for the same project are
     * coalesced: all callbacks run after the single close run finishes.
     */
    private final Map<JIPipeProject, List<Runnable>> projectCloseCallbacks = new HashMap<>();

    /**
     * Feature #1359: the load run that is currently in flight (cancelled
     * when the window is disposed during a load).
     */
    private JIPipeRunnable activeLoadRun;
    private JIPipeQueuedRunnableExecutor activeLoadQueue;

    /**
     * Feature #1359: set before the user-requested cancellation of the
     * active load, so the completion handler can distinguish a deliberate
     * cancellation from a failure.
     */
    private final AtomicBoolean loadCancelRequested = new AtomicBoolean(false);

    /**
     * @param context          context
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     */
    public JIPipeDesktopProjectWindow(Context context, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        JIPipeDesktopSplashScreen.getInstance().hideSplash();
        this.context = context;
        this.cefClientService = new JIPipeCefClientService(this);
        OPEN_WINDOWS.add(this);
        WINDOW_OPENED_EVENT_EMITTER.emit(new WindowOpenedEvent(this));
        initialize();
        loadProject(project, showIntroduction, isNewProject);
    }

    /**
     * Feature #1359: creates a window without a project. It shows a loading
     * screen until {@link #loadProject(JIPipeProject, boolean, boolean)} is
     * called (used by the project-open flow, where the window is created
     * before the project file is read).
     *
     * @param context context
     */
    private JIPipeDesktopProjectWindow(Context context) {
        JIPipeDesktopSplashScreen.getInstance().hideSplash();
        this.context = context;
        this.cefClientService = new JIPipeCefClientService(this);
        OPEN_WINDOWS.add(this);
        WINDOW_OPENED_EVENT_EMITTER.emit(new WindowOpenedEvent(this));
        initialize();
        showLoadingScreen("Loading project", null, false);
    }

    /**
     * Tries to find the window that belongs to the provided project
     *
     * @param project the project
     * @return the window or null if none is found
     */
    public static JIPipeDesktopProjectWindow getWindowFor(JIPipeProject project) {
        for (JIPipeDesktopProjectWindow window : OPEN_WINDOWS) {
            if (window.project == project) {
                return window;
            }
        }
        return null;
    }

    /**
     * Creates a new project instance based on the current template selection
     *
     * @return the project
     */
    public static JIPipeProject getDefaultTemplateProject() {
        JIPipeProject project = null;
        if (JIPipeProjectDefaultsApplicationSettings.getInstance().getProjectTemplate().getValue() != null) {
            try {
                String id = JIPipeProjectDefaultsApplicationSettings.getInstance().getProjectTemplate().getValue();
                if (StringUtils.isNullOrEmpty(id) || !JIPipe.getInstance().getProjectTemplates().getRegisteredTemplates().containsKey(id)) {
                    id = JIPipeProjectTemplate.getFallbackTemplateId();
                    JIPipeProjectDefaultsApplicationSettings.getInstance().getProjectTemplate().setValue(id);
                    JIPipe.autoSaveSettings();
                }
                JIPipeProjectTemplate template = JIPipe.getInstance().getProjectTemplates().getRegisteredTemplates().get(id);
                JIPipeValidationReport report = new JIPipeValidationReport();
                JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
                project = template.loadAsProject(report, notifications, JIPipeProgressInfo.SILENT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (project == null) {
            project = new JIPipeProject();
        }
        return project;
    }

    /**
     * Creates a new window
     *
     * @param context          context
     * @param project          The project
     * @param showIntroduction show an introduction
     * @param isNewProject     if the project is a new empty project
     * @return The window
     */
    public static JIPipeDesktopProjectWindow newWindow(Context context, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        JIPipeDesktopProjectWindow frame = new JIPipeDesktopProjectWindow(context, project, showIntroduction, isNewProject);
        frame.pack();
        frame.setSize(1920, 900);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        return frame;
    }

    /**
     * Feature #1359: creates a new window that shows a loading screen until
     * a project is loaded into it
     *
     * @param context context
     * @return The window
     */
    public static JIPipeDesktopProjectWindow newWindow(Context context) {
        JIPipeDesktopProjectWindow frame = new JIPipeDesktopProjectWindow(context);
        frame.pack();
        frame.setSize(1920, 900);
        frame.setVisible(true);
        return frame;
    }

    /**
     * @return All open project windows
     */
    public static Set<JIPipeDesktopProjectWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }

    /**
     * The SAVING stage label for the save button, derived from the writer's
     * progress log. Returns {@code null} for log lines that do not indicate a
     * new stage (e.g. the initial progress frame, per-part zips).
     */
    private static String mapSaveStage(String statusMessage) {
        if (statusMessage == null) {
            return null;
        }
        if (statusMessage.startsWith("Materialize")) {
            return "writing";
        }
        if (statusMessage.startsWith("Zipping")) {
            return "compressing";
        }
        if (statusMessage.startsWith("Self-verifying")) {
            return "verifying";
        }
        if (statusMessage.startsWith("Re-check")) {
            return "verifying";
        }
        return null;
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(8, 8));
        updateTitle();
        setIconImage(UIUtils.getJIPipeIcon128());
        UIUtils.setToAskOnClose(this, () -> {
            if (projectWorkbench != null && projectWorkbench.isProjectModified()) {
                return "Do you really want to close JIPipe?\nThere are some unsaved changes.";
            } else {
                return "Do you really want to close JIPipe?";
            }
        }, "Close window");
        if (JIPipeGeneralUIApplicationSettings.getInstance().isMaximizeWindows()) {
            SwingUtilities.invokeLater(() -> setExtendedState(getExtendedState() | MAXIMIZED_BOTH));
        }
    }

    @Override
    public void dispose() {
        if (disposing) {
            return;
        }
        disposing = true;
        // Further X presses during the asynchronous teardown close silently
        for (WindowListener listener : getWindowListeners()) {
            removeWindowListener(listener);
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        OPEN_WINDOWS.remove(this);
        // Cancel a load that is currently in flight. Its completion handler
        // releases what was already read (see onProjectLoadFinished).
        cancelActiveLoad();
        // Release the per-window UI tree FIRST (tab pane → tab contents →
        // graph editors' settings bindings, the settings tab's change-event
        // subscription, ...) while the settings service and the project are
        // still alive. The background close then runs project teardown only
        // - project.close(...) does not touch the workbench (the runnables
        // are driven by the queued executor).
        if (projectWorkbench != null) {
            projectWorkbench.dispose();
            projectWorkbench = null;
        }
        closeAllBalloons();
        // Feature #1359: replace the workbench with an in-window closing
        // screen and release the project in the background - no modal
        // dialog, and no second OS window that a fast close could leave
        // stuck open (e.g. Ubuntu 24.04 with Mutter).
        showLoadingScreen("Closing project", projectSavePath, false);
        requestProjectClose(project, this::finishDispose);
    }

    /**
     * Feature #1359: EDT bookkeeping for the final teardown after the
     * project was released in the background
     */
    private void finishDispose() {
        // Drop the closed project's run-log entries (and their NDJSON data files). The project's
        // temporary base - which contains the log files of its graph runs - is cleaned up by
        // project.close(...); removing the entries keeps the Tools > Logs list consistent.
        if (project != null) {
            JIPipeDesktopRunnableLogsCollection.getInstance().removeProjectEntries(project);
        }
        WINDOW_CLOSED_EVENT_EMITTER.emit(new WindowClosedEvent(this));
        super.dispose();
    }

    /**
     * Loads a project into the window
     *
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     */
    public void loadProject(JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        this.project = project;
        this.projectWorkbench = new JIPipeDesktopProjectWorkbench(this, context, project, showIntroduction, isNewProject);
        this.sessionId = UUID.randomUUID();
        // The workbench replaces the loading screen (feature #1359)
        this.loadingScreen = null;
        this.contentBeforeLoadingScreen = null;
        setContentPane(projectWorkbench);
        revalidate();
        repaint();
    }

    /**
     * Creates a new project.
     * Asks the user if it should replace the currently displayed project
     */
    public void newProject() {
        JIPipeProject project = getDefaultTemplateProject();
        JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true, target -> {
            target.projectSavePath = null;
            target.updateTitle();
            target.getProjectWorkbench().sendStatusBarText("Created new project");
        });
        if (window == null) {
            return;
        }
    }

    /**
     * Creates a new project from template
     */
    public void newProjectFromTemplate() {
        JIPipeDesktopJIPipeTemplateSelectionUI dialog = new JIPipeDesktopJIPipeTemplateSelectionUI(projectWorkbench, this);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.getSelectedTemplate() != null) {
            newProjectFromTemplate(dialog.getSelectedTemplate());
        }
    }

    /**
     * Creates a new project from template
     */
    public void newProjectFromTemplate(JIPipeProjectTemplate template) {
        Path loadZipTarget = null;
        if (template.getZipFile() != null && Files.isRegularFile(template.getZipFile())) {
            try {
                double sizeMB = Files.size(template.getZipFile()) / 1024.0 / 1024.0;
                Button loadWithData = Button.custom("Load with data").asDefault();
                Button loadOnlyPipeline = Button.custom("Load only pipeline");
                Optional<Button> selected = JIPipeDesktopDialogs.choose(this,
                        "Load template",
                        "The template contains " + Precision.round(sizeMB, 2) + " MB of data.\nYou can choose to either load only the " +
                                "pipeline or extract the template project and related data into a directory.",
                        loadWithData, loadOnlyPipeline);
                if (selected.isEmpty()) {
                    return;
                } else if (selected.get() == loadWithData) {
                    loadZipTarget = JIPipeDesktop.saveDirectory(this,
                            getProjectWorkbench(),
                            JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
                            "Load template: Choose an empty directory", HTMLText.EMPTY);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (loadZipTarget != null) {
            JIPipeExtractTemplateZipFileRun run = new JIPipeExtractTemplateZipFileRun(template, loadZipTarget);
            Path finalLoadZipTarget = loadZipTarget;

            JIPipeQueuedRunnableExecutor.getInstance().getFinishedEventEmitter().subscribeLambda((emitter, event) -> {
                if (event.getRun() == run) {
                    SwingUtilities.invokeLater(() -> {
                        Path projectFile = PathUtils.findFileByExtensionRecursivelyIn(finalLoadZipTarget, ".jip");
                        if (projectFile == null) {
                            JIPipeDesktopDialogs.message(JIPipeDesktopProjectWindow.this, "Load template", "No project file in " + finalLoadZipTarget);
                        }
                        openProject(projectFile, false);
                    });
                }
            });
            JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run);
        } else {
            try {
                JIPipeValidationReport report = new JIPipeValidationReport();
                JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
                JIPipeProject project = template.loadAsProject(report, notifications, JIPipeProgressInfo.STDOUT);
                JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true, target -> {
                    target.projectSavePath = null;
                    target.updateTitle();
                    target.getProjectWorkbench().sendStatusBarText("Created new project");
                    if (!notifications.isEmpty()) {
                        UIUtils.openNotificationsDialog(target.getProjectWorkbench(), this, notifications, "Potential issues found", "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.", true);
                    }
                    if (!report.isValid()) {
                        UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(), this, report, "Errors while loading the project", "It seems that not all parameters/nodes/connections could be restored from the project file. The cause might be that you are using a version of JIPipe that changed the affected features. " +
                                "Please review the entries and apply the necessary changes (e.g., reconnecting nodes).", false);
                    }
                });
                if (window == null) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Updates the title based on the current state
     */
    public void updateTitle() {
        if (projectWorkbench == null) {
            setTitle("JIPipe");
            return;
        }
        if (projectSavePath == null) {
            setTitle("JIPipe - New project" + (projectWorkbench.isProjectModified() ? "*" : ""));
        } else {
            setTitle("JIPipe - " + projectSavePath + (projectWorkbench.isProjectModified() ? "*" : ""));
        }
    }

    /**
     * Opens a project from a file or folder
     * Asks the user if it should replace the currently displayed project
     *
     * @param path               JSON project file or result folder
     * @param forceCurrentWindow if the project will be forced to be opened in the current window
     */
    public void openProject(Path path, boolean forceCurrentWindow) {
        if (Files.isRegularFile(path)) {
            // .crate.zip files are imported through the RO-Crate flow
            // BEFORE the project peek (peek refuses non-JIPX ZIPs; the
            // open chooser offers crate.zip for imports)
            if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".crate.zip")) {
                importROCrate(path, true, true, forceCurrentWindow);
                return;
            }
            // A legacy .jip with an existing .jipx sibling: offer the
            // project crate (existence-based - mtimes are unreliable
            // across NAS/SFTP/USB copies, the user decides which is current).
            Path jipxSibling = PathUtils.findJipxSibling(path);
            if (jipxSibling != null) {
                Button openProjectCrate = Button.custom("Open project crate").asDefault();
                Button openLegacyProject = Button.custom("Open legacy project");
                Optional<Button> selected = JIPipeDesktopDialogs.choose(this,
                        "Project crate available",
                        "A project crate of this project (" + jipxSibling.getFileName()
                                + ") exists next to the legacy project. Open that one instead?",
                        openProjectCrate, openLegacyProject);
                if (selected.orElse(null) == openProjectCrate) {
                    openProject(jipxSibling, forceCurrentWindow);
                    return;
                }
                // Anything that is not an explicit "open the legacy project"
                // aborts the open: the implicit Cancel button and a dismissal
                // (the window's X / Escape) - a press of X must not be
                // treated as a choice.
                if (selected.isEmpty()) {
                    return;
                }
                // "Open legacy project": continue with the legacy .jip
            }
            JIPipeValidationReport report = new JIPipeValidationReport();
            JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
            notifications.connectDismissTo(JIPipeNotificationInbox.getInstance());
            try {
                // Feature #1359: if this project file is already open in a
                // window, offer to bring that window to the front instead of
                // loading a second instance
                JIPipeDesktopProjectWindow existingWindow = findOpenWindowForProjectFile(path);
                if (existingWindow != null) {
                    Button bringToFront = Button.custom("Bring window to front").asDefault();
                    Button openAnyway = Button.custom("Open anyway");
                    Optional<Button> alreadyOpen = JIPipeDesktopDialogs.choose(this,
                            "Project already open",
                            "The project " + path + " is already open in a project window.\n" +
                                    "You can bring that window to the front instead of loading the project again.",
                            bringToFront, openAnyway);
                    if (alreadyOpen.isEmpty()) {
                        return;
                    }
                    if (alreadyOpen.get() == bringToFront) {
                        existingWindow.bringWindowToFront();
                        return;
                    }
                    // "Open anyway": continue with a second instance
                }

                JIPipeProjectPeek peek = JIPipeProjectIOs.peek(path);
                Set<JIPipeDependency> dependencySet = peek.getDependencies();

                Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites = new HashSet<>();
//                if (JIPipe.getInstance().getImageJPlugins() != null) {
//                    // Populate
//                    for (JIPipeDependency dependency : dependencySet) {
//                        missingUpdateSites.addAll(dependency.getImageJUpdateSiteDependencies());
//                    }
//                    missingUpdateSites.addAll(metadata.getUpdateSiteDependencies());
//                    // Remove existing
//                    for (UpdateSite updateSite : JIPipe.getInstance().getImageJPlugins().getUpdateSites(true)) {
//                        if (updateSite.isActive()) {
//                            missingUpdateSites.removeIf(site -> Objects.equals(site.getName(), updateSite.getName()));
//                        }
//                    }
//                }
                Set<JIPipeDependency> missingDependencies = JIPipePluginsServiceComponent.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty() || !missingUpdateSites.isEmpty()) {
                    if (!JIPipeDesktopInvalidProjectDependenciesInfoDialog.showDialog(getProjectWorkbench(), path, missingDependencies)) {
                        return;
                    }
                }

                // Feature #1359: the This/New placement is decided BEFORE the
                // file is read (it does not depend on the read result), so the
                // loading screen can be shown in the window that receives the
                // project. The former modal "Load project" dialog is gone.
                final boolean loadIntoThisWindow;
                if (forceCurrentWindow) {
                    loadIntoThisWindow = true;
                } else {
                    Button openInThisWindow = Button.custom("This window");
                    Button openInNewWindow = Button.custom("New window").asDefault();
                    Optional<Button> placement = JIPipeDesktopDialogs.choose(this, "Open project",
                            "Projects can either be opened in a new window or replace the project in an existing window.",
                            openInThisWindow, openInNewWindow);
                    if (placement.isEmpty()) {
                        return;
                    }
                    loadIntoThisWindow = placement.get() == openInThisWindow;
                }

                JIPipeFileChooserApplicationSettings.getInstance().setLastDirectoryBy(JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, path.getParent());

                final JIPipeDesktopProjectWindow targetWindow = loadIntoThisWindow ? this : newWindow(context);
                targetWindow.beginProjectLoad(path, report, notifications, this, loadIntoThisWindow);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (Files.isDirectory(path)) {
            JIPipeValidationReport report = new JIPipeValidationReport();
            JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
            notifications.connectDismissTo(JIPipeNotificationInbox.getInstance());
            try {
                // The output-folder convention carries project.jip; the JIPX
                // convention writes project.jipx - try the legacy name first
                Path projectPath = path.resolve("project.jip");
                if (!Files.isRegularFile(projectPath)) {
                    Path legacyProjectPath = projectPath;
                    projectPath = path.resolve("project.jipx");
                    if (!Files.isRegularFile(projectPath)) {
                        projectPath = legacyProjectPath;
                    }
                }
                JIPipeProjectPeek peek = JIPipeProjectIOs.peek(projectPath);
                Set<JIPipeDependency> dependencySet = peek.getDependencies();
                Set<JIPipeDependency> missingDependencies = JIPipePluginsServiceComponent.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty()) {
                    if (!JIPipeDesktopInvalidProjectDependenciesInfoDialog.showDialog(getProjectWorkbench(), path, missingDependencies)) {
                        return;
                    }
                }

                JIPipeProject newProject = JIPipeProject.loadProject(projectPath,
                        new UnspecifiedValidationReportContext(),
                        report,
                        notifications,
                        JIPipeProgressInfo.STDOUT);
                final Path finalProjectPath = projectPath;
                JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("Open JIPipe output", newProject, false, false, target -> {
                    JIPipeFileChooserApplicationSettings.getInstance().setLastDirectoryBy(JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, path);

                    target.projectSavePath = finalProjectPath;
                    target.getProjectWorkbench().sendStatusBarText("Opened project from " + target.projectSavePath);
                    target.updateTitle();

                    JIPipe.getInstance().getRecentProjects().add(path);

                    // Give user the option to either open in tab or cache
                    Button loadInNewTab = Button.custom("Load in new tab").asDefault();
                    Button loadIntoCache = Button.custom("Load into cache");
                    Optional<Button> selected = JIPipeDesktopDialogs.choose(target,
                            "Open JIPipe output",
                            "You can either open the output in a tab or load them into the cache. " +
                                    "Please note that caching the results might require large amounts of memory.",
                            loadInNewTab, loadIntoCache);

                    if (selected.orElse(null) == loadInNewTab) {
                        // Create a new tab
                        target.getProjectWorkbench().getDocumentTabPane().addTab("Results",
                                JIPipe.RESOURCES.getIcon16("actions/document-open-folder.png"),
                                new JIPipeDesktopResultUI(target.getProjectWorkbench(), newProject, path),
                                JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton,
                                true);
                        target.getProjectWorkbench().getDocumentTabPane().switchToLastTab();
                    } else if (selected.orElse(null) == loadIntoCache) {
                        // Load into cache with a run
                        JIPipeDesktopRunExecuteUI.runInDialog(target.getProjectWorkbench(), target, new JIPipeDesktopLoadResultDirectoryIntoCacheRun(target.getProjectWorkbench(), newProject, path, true));
                    }
                    if (!notifications.isEmpty()) {
                        UIUtils.openNotificationsDialog(target.getProjectWorkbench(), this, notifications, "Potential issues found", "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.", true);
                    }
                });
                if (window == null) {
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!report.isValid()) {
                UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(),
                        this,
                        report,
                        "Errors while loading the project",
                        "It seems that not all parameters/nodes/connections could be restored from the project file. The cause might be that you are using a version of JIPipe that changed the affected features. " +
                                "Please review the entries and apply the necessary changes (e.g., reconnecting nodes).",
                        false);
            }

        }
    }

    /**
     * Opens a file chooser where the user can select a project file
     */
    public void openProject() {
        Path file = JIPipeDesktop.openFile(this,
                getProjectWorkbench(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
                "Open JIPipe project",
                HTMLText.EMPTY,
                PathUtils.EXTENSION_FILTER_JIP_JIPX,
                PathUtils.EXTENSION_FILTER_JIP,
                PathUtils.EXTENSION_FILTER_JIPX,
                PathUtils.EXTENSION_FILTER_WORKFLOW_RO_CRATE);
        if (file != null) {
            openProject(file, false);
        }
    }

    /**
     * Opens a file chooser where the user can select a result folder
     */
    public void openProjectAndOutput() {
        Path file = JIPipeDesktop.openDirectory(this,
                getProjectWorkbench(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
                "Open JIPipe output folder", HTMLText.EMPTY);
        if (file != null) {
            openProject(file, false);
        }
    }

    /**
     * Saves the project
     *
     * @param avoidDialog    If true, the project is stored in the last known valid output location if possible
     * @param updateSavePath if true, update the save path of the window
     */
    public void saveProjectAs(boolean avoidDialog, boolean updateSavePath) {
        Path savePath = null;
        if (avoidDialog && projectSavePath != null) {
            savePath = projectSavePath;
        }
        if (savePath == null) {
            savePath = JIPipeDesktop.saveFile(this,
                    getProjectWorkbench(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
                    "Save JIPipe project",
                    HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JIPX, PathUtils.EXTENSION_FILTER_JIP);
            if (savePath == null) {
                return;
            }
        }

        // JIPX migration UX (spec §7): a save target that is still a legacy
        // .jip file gets a per-save format prompt - even for
        // Ctrl+S (avoidDialog) and for files chosen in the save dialog
        // that end in .jip. Autosaves (avoidDialog without updateSavePath)
        // never prompt. The compatibility gate overrides the prompt: a
        // project that cannot be expressed in the legacy format (see
        // JIPipeProjectSaveTarget.isLegacyCompatible) is always saved as
        // .jipx (forward hook for future jipx-exclusive features).
        if (!JIPipeProjectSaveTarget.isLegacyCompatible(getProject())) {
            savePath = PathUtils.replaceExtension(savePath, ".jip", ".jipx");
        } else if (JIPipeProjectSaveTarget.shouldPrompt(savePath, avoidDialog, updateSavePath)) {
            Button modernFormat = Button.custom("Modern format (Recommended)").asDefault();
            Button legacyFormat = Button.custom("Legacy format");
            Optional<Button> selected = JIPipeDesktopDialogs.choose(this,
                    "Save project format",
                    "This project uses the legacy file format.\n" +
                            "Saving in the modern format writes a new file next to the legacy one, which is kept.",
                    modernFormat, legacyFormat);
            if (selected.isEmpty()) {
                // Dismissal (Escape / closing the dialog window): abort the
                // save (spec §7).
                return;
            }
            if (selected.get() == modernFormat) {
                // A sibling .jipx in the same directory: replace the
                // extension, preserving the base name's original casing -
                // the original .jip is never touched on disk.
                savePath = PathUtils.replaceExtension(savePath, ".jip", ".jipx");
            }
        }

        if (PathUtils.isJipxPath(savePath)) {
            saveProjectAsJipx(savePath, updateSavePath);
        } else {
            saveProjectAsLegacy(savePath, updateSavePath);
        }
    }

    /**
     * The JIPX (project crate) save flow (spec §5.6) - NON-MODAL (feature #753):
     * the workspace materialization, the crate build (incl. JSON-schema
     * validation), the zip, and the full re-load of the saved container all
     * run in a Swing worker on a PER-PROJECT queue - no modal log dialog, no
     * EDT hang. The save button shows a "Saving…" state and a transient
     * result flash; the user can keep working on other projects while a
     * large save runs. The legacy save stays synchronous (frozen behavior).
     * <p>
     * EDT pre-steps: setting the work directory and injecting the GUI tab
     * metadata (reads the workbench UI state - done BEFORE enqueue). The
     * run body (write → self-verify → re-check) and the writer's
     * {@code synchronized(workspace)} lock are unchanged.
     * <p>
     * Finish handling (worker thread → hop to EDT): on success, the title /
     * modified flag / status bar / {@code ProjectSavedEvent} / recent
     * projects + a "Saved" flash - NO compliance dialog (replaced by the
     * publish button's static FAIR badge, see the workbench); on failure, an
     * accurate error dialog (write failed → the existing file was not
     * changed; write succeeded but the re-check load failed → the file WAS
     * replaced) + a "Save failed" flash; on cancellation, a status-bar
     * message. The modified flag is kept on failure/cancellation. The save
     * is pushed to the run-logs marked read (the run is log-silent).
     *
     * @param savePath       the JIPX target file
     * @param updateSavePath if the internally tracked project file path should be updated to the new save path
     */
    private void saveProjectAsJipx(Path savePath, boolean updateSavePath) {
        JIPipeProject project = getProject();
        // EDT pre-steps: the work directory (it must be set before the
        // first workspace access - temp-base memoization) and the GUI
        // tab metadata (workbench UI state) - done BEFORE enqueue.
        project.setWorkDirectory(savePath.getParent());
        project.getAdditionalMetadata().put(JIPipeDesktopJIPipeProjectTabMetadata.METADATA_KEY,
                new JIPipeDesktopJIPipeProjectTabMetadata(getProjectWorkbench()));

        final JIPipeDesktopProjectWorkbench workbench = getProjectWorkbench();
        final List<String> violations = new ArrayList<>();
        final Throwable[] failure = new Throwable[1];
        // "write" = the container write itself; "recheck" = the container
        // was written (the atomic replace succeeded) but the re-check
        // load of the saved file failed.
        final String[] failureStage = new String[]{"write"};
        final JIPipeQueuedRunnableExecutor queue = getOrCreateSaveQueue();

        DefaultJIPipeRunnable run = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Save project crate " + savePath;
            }

            @Override
            public boolean isLogSilent() {
                // Feature #753: the save is pushed to the run-logs, but is
                // auto-marked read (no notification badge) because a normal
                // save is not an error the user must react to.
                return true;
            }

            @Override
            public void run() {
                try {
                    if (getProgressInfo().isCancelled()) {
                        return;
                    }
                    // (1) Write: the writer materializes the workspace,
                    // builds the crate and container, zips it,
                    // self-verifies, and atomically replaces the target
                    // (all synchronized on the project workspace).
                    new JIPipeXProjectWriter().writeWithViolations(project, savePath, getProgressInfo(), violations);
                    if (getProgressInfo().isCancelled()) {
                        return;
                    }
                    // (2) Re-check: the file we just wrote must actually
                    // load (the format-agnostic "the file we just wrote
                    // loads" check - the writer already self-verified the
                    // container structurally).
                    failureStage[0] = "recheck";
                    // The re-check load is a THROWAWAY model: its
                    // temporary base (and workspace) must be released -
                    // without close(), every save leaks one JIPipe.tmp.dir
                    // tree (feature #753 safety audit).
                    JIPipeProject recheck = JIPipeProject.loadProject(savePath,
                            new UnspecifiedValidationReportContext(),
                            new JIPipeValidationReport(),
                            new JIPipeNotificationInbox(),
                            getProgressInfo().resolve("Re-check saved project"));
                    recheck.close(getProgressInfo().resolve("Close re-check project"));
                } catch (Exception e) {
                    failure[0] = e;
                }
            }
        };

        // Live SAVING stage label: the writer logs "Materialize JIPX
        // workspace", "Zipping JIPX workspace", "Self-verifying JIPX
        // container"; the re-check resolves to "Re-check saved project".
        // The button shows "Saving… <stage>" (indeterminate - no % bar).
        // One update per coalesced batch of the run's progress store: the
        // batch's records are mapped on the writer thread, the (at most
        // one) stage update is hopped to the EDT. The subscription follows
        // store adoption (the run receives a file-backed store from the
        // worker before execution).
        final JIPipeProgressInfo.BatchEventSubscription stageSubscription =
                run.getProgressInfo().subscribeToBatchEvents(event -> {
                    if (run.getProgressInfo().isCancelled()) {
                        return;
                    }
                    if (event.fromLine() > event.toLine()) {
                        return;
                    }
                    JIPipeProgressLog store = (JIPipeProgressLog) event.getSource();
                    List<JIPipeProgressRecord> records = store.readRecords(event.fromLine(), event.toLine() - event.fromLine() + 1);
                    String stage = null;
                    for (JIPipeProgressRecord record : records) {
                        if (record instanceof JIPipeProgressRecord.Log logRecord) {
                            String mapped = mapSaveStage(logRecord.m());
                            if (mapped != null) {
                                stage = mapped;
                            }
                        }
                    }
                    if (stage == null) {
                        return;
                    }
                    final String stageToSend = stage;
                    SwingUtilities.invokeLater(() -> workbench.getProjectSavingEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavingEvent(workbench, true, stageToSend)));
                });

        // The SAVING state is shown as soon as the run actually starts (not
        // merely when it is enqueued - it may wait behind another save on
        // this project's queue), and reverted when it finishes.
        final JIPipeRunnable.StartedEventListener startListener = event -> {
            if (event.getRun() == run) {
                workbench.getProjectSavingEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavingEvent(workbench, true, ""));
            }
        };
        queue.getStartedEventEmitter().subscribe(startListener);

        // Finish bookkeeping. A per-run, self-unsubscribing listener on the
        // LONG-LIVED per-project queue (NOT subscribeLambdaOnce: on a queue
        // reused across saves, a once-subscription is consumed by whichever
        // run finishes first - the `event.getRun() == run` guard prevents a
        // wrong run from triggering bookkeeping, but it does NOT prevent the
        // once-flag from being consumed, which would drop a sibling save's
        // bookkeeping). Self-unsubscribe keeps the long-lived queue's
        // subscriber list clean (leak-free). The two completion listeners
        // reference each other to unsubscribe, so they are held in arrays to
        // break the forward-reference (each is subscribed before it is
        // invoked, so the slot is always populated in practice).
        final JIPipeRunnable.FinishedEventListener[] finishHolder = new JIPipeRunnable.FinishedEventListener[1];
        final JIPipeRunnable.InterruptedEventListener[] interruptHolder = new JIPipeRunnable.InterruptedEventListener[1];
        JIPipeRunnable.FinishedEventListener finishListener = event -> {
            if (event.getRun() != run) {
                return;
            }
            queue.getFinishedEventEmitter().unsubscribe(finishHolder[0]);
            queue.getInterruptedEventEmitter().unsubscribe(interruptHolder[0]);
            queue.getStartedEventEmitter().unsubscribe(startListener);
            stageSubscription.close();
            SwingUtilities.invokeLater(() -> onSaveFinished(run, failure, failureStage, savePath, updateSavePath));
        };
        JIPipeRunnable.InterruptedEventListener interruptListener = event -> {
            if (event.getRun() != run) {
                return;
            }
            queue.getFinishedEventEmitter().unsubscribe(finishHolder[0]);
            queue.getInterruptedEventEmitter().unsubscribe(interruptHolder[0]);
            queue.getStartedEventEmitter().unsubscribe(startListener);
            stageSubscription.close();
            SwingUtilities.invokeLater(() -> onSaveCancelled(run, savePath));
        };
        finishHolder[0] = finishListener;
        interruptHolder[0] = interruptListener;
        queue.getFinishedEventEmitter().subscribe(finishListener);
        queue.getInterruptedEventEmitter().subscribe(interruptListener);

        queue.enqueue(run);
    }

    /**
     * EDT bookkeeping for a JIPX save that just finished (feature #753).
     */
    private void onSaveFinished(JIPipeRunnable run, Throwable[] failure, String[] failureStage,
                                Path savePath, boolean updateSavePath) {
        JIPipeProject project = getProject();
        // The window may have been closed (the project disposed) while the save
        // was still running on its per-project queue. A finished run is still
        // worth logging (pushToLog below), but skip all UI bookkeeping - the
        // workbench / title / status bar / recents / dialog would otherwise
        // touch a dead workbench parented to a disposed frame.
        if (!project.isDisposed()) {
            JIPipeDesktopProjectWorkbench workbench = getProjectWorkbench();
            if (failure[0] == null) {
                // Everything succeeded: the EDT bookkeeping. NO compliance
                // dialog - replaced by the publish button's static FAIR badge.
                if (updateSavePath) {
                    project.setProjectFile(savePath);
                    projectSavePath = savePath;
                }
                updateTitle();
                workbench.setProjectModified(false);
                workbench.sendStatusBarText("Saved project to " + savePath);
                workbench.getProjectSavedEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavedEvent(workbench));
                JIPipe.getInstance().getRecentProjects().add(savePath);
                workbench.getProjectSavingEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavingEvent(workbench, false, "Saved ✓"));
            } else if ("recheck".equals(failureStage[0])) {
                // The write succeeded (the target WAS replaced), but loading
                // the saved file failed - say exactly that.
                UIUtils.showErrorDialog(workbench, this, new JIPipeValidationRuntimeException(failure[0],
                        "Error after saving!",
                        "The project crate file was written to '" + savePath + "' (the existing file WAS replaced), " +
                                "but loading the saved file failed: " + failure[0],
                        "The container may be corrupt or not fully loadable by this JIPipe version. " +
                                "Please check the file and re-save the project if necessary."));
                workbench.getProjectSavingEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavingEvent(workbench, false, "Save failed ✗"));
            } else {
                // The write itself failed - the atomic replace never ran, so
                // the existing file is untouched.
                UIUtils.showErrorDialog(workbench, this, new JIPipeValidationRuntimeException(failure[0],
                        "Error during saving!",
                        "While saving the project into '" + savePath + "'. Any existing file was not changed or overwritten." + " The issue cannot be determined. Please contact the JIPipe authors.",
                        "Please check if you have write access to the temporary directory and the target directory. " +
                                "If this is the case, please contact the JIPipe authors."));
                workbench.getProjectSavingEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavingEvent(workbench, false, "Save failed ✗"));
            }
        }
        // Push the save to the run-logs, marked read (feature #753). The
        // per-project save queue is NOT the global singleton that the log
        // collection auto-subscribes to, so a manual push is required and
        // cannot double-add (see the log-collection wiring note in the
        // report). isLogSilent()=true makes it auto-mark as read.
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(run, failure[0] == null);
    }

    /**
     * EDT bookkeeping for a cancelled JIPX save (feature #753).
     */
    private void onSaveCancelled(JIPipeRunnable run, Path savePath) {
        // Same disposed-guard as onSaveFinished: skip UI bookkeeping if the
        // window was closed mid-save, but still log the cancellation.
        if (!getProject().isDisposed()) {
            JIPipeDesktopProjectWorkbench workbench = getProjectWorkbench();
            // The save was cancelled mid-run: the atomic replace may not have
            // run - treat it as an aborted save and keep the modified flag.
            workbench.sendStatusBarText("Saving to " + savePath + " was cancelled");
            workbench.getProjectSavingEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavingEvent(workbench, false, "Save cancelled"));
        }
        // Push the cancellation to the run-logs, marked read.
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(run, false);
    }

    /**
     * Lazily creates the per-project save queue. A NEW executor instance per
     * window/project (NOT the global singleton) - feature #753.
     */
    private JIPipeQueuedRunnableExecutor getOrCreateSaveQueue() {
        if (saveQueue == null) {
            String projectName = project == null || project.getMetadata() == null
                    ? "project"
                    : StringUtils.nullToEmpty(project.getMetadata().getName()).trim();
            if (projectName.isEmpty()) {
                projectName = "project";
            }
            saveQueue = new JIPipeQueuedRunnableExecutor("Save " + projectName);
        }
        return saveQueue;
    }

    /**
     * The legacy .jip save flow, frozen: write to a {@code .part} temp
     * file, self-verify by reloading it, then copy over the target.
     * <p>
     * The temp write goes directly through the legacy writer (not the
     * extension-driven dispatch): the temp file's randomized
     * {@code .part} name carries no format information, and the target
     * of a save dialog may end in any of the chooser's extensions.
     *
     * @param savePath       the legacy target file
     * @param updateSavePath if the internally tracked project file path should be updated to the new save path
     */
    private void saveProjectAsLegacy(Path savePath, boolean updateSavePath) {
        try {
            Path tempFile = PathUtils.createSubTempFilePath(savePath.getParent(), savePath.getFileName().toString(), ".part");
            getProject().setWorkDirectory(savePath.getParent());
            getProject().getAdditionalMetadata().put(JIPipeDesktopJIPipeProjectTabMetadata.METADATA_KEY, new JIPipeDesktopJIPipeProjectTabMetadata(getProjectWorkbench()));
            new JIPipeLegacyProjectWriter().write(getProject(), tempFile, JIPipeProgressInfo.SILENT);
            if (updateSavePath) {
                getProject().setProjectFile(tempFile);
            }

            // Check if the saved project can be loaded (a throwaway model
            // - closed immediately so its temporary base is cleaned up).
            JIPipeProject recheck = JIPipeProject.loadProject(tempFile,
                    new UnspecifiedValidationReportContext(),
                    new JIPipeValidationReport(),
                    new JIPipeNotificationInbox(),
                    JIPipeProgressInfo.STDOUT);
            recheck.close(JIPipeProgressInfo.STDOUT);

            // Overwrite the target file
            if (Files.exists(savePath)) {
                Files.delete(savePath);
            }
            Files.copy(tempFile, savePath);

            // Everything OK, now set the title
            if (updateSavePath) {
                getProject().setProjectFile(savePath);
                projectSavePath = savePath;
            }
            updateTitle();
            projectWorkbench.setProjectModified(false);
            projectWorkbench.sendStatusBarText("Saved project to " + savePath);
            projectWorkbench.getProjectSavedEventEmitter().emit(new JIPipeDesktopProjectWorkbench.ProjectSavedEvent(projectWorkbench));
            JIPipe.getInstance().getRecentProjects().add(savePath);

            // Remove tmp file
            Files.delete(tempFile);
        } catch (IOException e) {
            UIUtils.showErrorDialog(getProjectWorkbench(), this, new JIPipeValidationRuntimeException(e,
                    "Error during saving!",
                    "While saving the project into '" + savePath + "'. Any existing file was not changed or overwritten." + " The issue cannot be determined. Please contact the JIPipe authors.",
                    "Please check if you have write access to the temporary directory and the target directory. " +
                            "If this is the case, please contact the JIPipe authors."));
        }
    }

    /**
     * @param messageTitle     Description of the project source
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     * @param postLoadAction   Executed on the EDT when the project is visible in its window (may be null; receives the window)
     * @return The window that holds the project, or null if the user dismissed the chooser
     */
    private JIPipeDesktopProjectWindow openProjectInThisOrNewWindow(String messageTitle, JIPipeProject project, boolean showIntroduction, boolean isNewProject, Consumer<JIPipeDesktopProjectWindow> postLoadAction) {
        Button thisWindow = Button.custom("This window");
        Button newWindow = Button.custom("New window").asDefault();
        Optional<Button> selected = JIPipeDesktopDialogs.choose(this, messageTitle,
                "Projects can either be opened in a new window or replace the project in an existing window.",
                thisWindow, newWindow);
        if (selected.isEmpty()) {
            return null;
        } else if (selected.get() == thisWindow) {
            closeAllBalloons();
            // Feature #1359: replace the workbench with an in-window loading
            // screen and release the previous project in the background (the
            // former modal "Close project" dialog could get stuck open on
            // some window managers). The post-load action runs once the new
            // project is visible.
            showLoadingScreen("Opening project", null, false);
            if (projectWorkbench != null) {
                projectWorkbench.dispose();
                projectWorkbench = null;
            }
            final JIPipeProject previousProject = this.project;
            requestProjectClose(previousProject, () -> {
                if (disposing) {
                    return;
                }
                loadProject(project, false, isNewProject);
                if (postLoadAction != null) {
                    postLoadAction.accept(this);
                }
            });
            return this;
        } else {
            JIPipeDesktopProjectWindow window = newWindow(context, project, showIntroduction, isNewProject);
            if (postLoadAction != null) {
                postLoadAction.accept(window);
            }
            return window;
        }
    }

    /**
     * Feature #1359: closes a project in the background (the replacement for
     * the modal "Close project" dialog) and runs the callback on the EDT
     * when the close finished. Requests for the same project are coalesced:
     * all callbacks run after the single close run finished. The close is
     * also wired to the status line of the loading screen (if one is shown).
     *
     * @param target   the project to close (may be null - then the callback runs immediately)
     * @param onClosed EDT callback after the close (may be null)
     */
    private void requestProjectClose(JIPipeProject target, Runnable onClosed) {
        if (target == null || target.isDisposed()) {
            if (onClosed != null) {
                SwingUtilities.invokeLater(onClosed);
            }
            return;
        }
        List<Runnable> callbacks = projectCloseCallbacks.get(target);
        if (callbacks == null) {
            callbacks = new ArrayList<>();
            projectCloseCallbacks.put(target, callbacks);
            startProjectCloseRun(target, callbacks);
        }
        if (onClosed != null) {
            callbacks.add(onClosed);
        }
    }

    /**
     * Feature #1359: runs the actual background close of a project on a
     * fresh per-close queue (the same pattern as the former per-operation
     * "Project cleanup" queue of the modal dialog)
     */
    private void startProjectCloseRun(JIPipeProject target, List<Runnable> callbacks) {
        JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Project cleanup");
        DefaultJIPipeRunnable run = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Close project";
            }

            @Override
            public void run() {
                target.close(getProgressInfo());
            }
        };
        final JIPipeProgressInfo.BatchEventSubscription statusSubscription = wireStatusToLoadingScreen(run);
        final AtomicBoolean handled = new AtomicBoolean(false);
        final AtomicBoolean pushInterruptedToLogs = new AtomicBoolean(false);
        Runnable dispatch = () -> {
            if (!handled.compareAndSet(false, true)) {
                return;
            }
            statusSubscription.close();
            if (pushInterruptedToLogs.get()) {
                // Push the failure to the run logs (the former dialog used
                // GlobalLogMode.OnlyFailures)
                JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(run, false);
            }
            // The event may arrive off the EDT - hop before touching the
            // callback list (it is EDT-confined)
            SwingUtilities.invokeLater(() -> {
                List<Runnable> pending = projectCloseCallbacks.remove(target);
                if (pending != null) {
                    for (Runnable callback : pending) {
                        callback.run();
                    }
                }
            });
        };
        // Await the completion on the worker's own emitters as well: for a
        // run cancelled while running, the executor never forwards the late
        // done() event to the queue emitter (see beginProjectLoad). The
        // handled flag deduplicates the two event sources.
        JIPipeRunnableWorker worker = queue.enqueue(run);
        worker.getFinishedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == run) {
                dispatch.run();
            }
        });
        worker.getInterruptedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == run) {
                pushInterruptedToLogs.set(true);
                dispatch.run();
            }
        });
        queue.getFinishedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == run) {
                dispatch.run();
            }
        });
        queue.getInterruptedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == run) {
                pushInterruptedToLogs.set(true);
                dispatch.run();
            }
        });
    }

    /**
     * Feature #1359: feeds the latest progress log line of a run into the
     * status line of the loading screen (so the user can follow what is
     * happening for larger projects)
     */
    private JIPipeProgressInfo.BatchEventSubscription wireStatusToLoadingScreen(JIPipeRunnable run) {
        return run.getProgressInfo().subscribeToBatchEvents(event -> {
            if (event.fromLine() > event.toLine()) {
                return;
            }
            JIPipeProgressLog store = (JIPipeProgressLog) event.getSource();
            List<JIPipeProgressRecord> records = store.readRecords(event.fromLine(), event.toLine() - event.fromLine() + 1);
            String last = null;
            for (JIPipeProgressRecord record : records) {
                if (record instanceof JIPipeProgressRecord.Log logRecord) {
                    last = logRecord.m();
                }
            }
            if (last == null) {
                return;
            }
            final String status = last;
            SwingUtilities.invokeLater(() -> {
                if (loadingScreen != null) {
                    loadingScreen.setStatus(status);
                }
            });
        });
    }

    /**
     * Feature #1359: shows the in-window loading screen (the content pane
     * replacement for the former modal dialogs)
     *
     * @param task       the task label (e.g. "Loading project")
     * @param detail     optional detail line (e.g. the project path)
     * @param cancelable whether a cancel button is shown
     */
    private void showLoadingScreen(String task, Path detail, boolean cancelable) {
        if (loadingScreen == null) {
            // Only a displayed workbench can be restored - a freshly created
            // window has nothing to restore and disposes itself instead
            contentBeforeLoadingScreen = projectWorkbench != null ? getContentPane() : null;
        }
        loadingScreen = new JIPipeDesktopProjectWindowLoadingScreen(task, detail);
        loadingScreen.setCancelVisible(cancelable);
        loadingScreen.setCancelAction(this::cancelActiveLoad);
        setContentPane(loadingScreen);
        revalidate();
        repaint();
    }

    /**
     * Feature #1359: cancels the load that is currently in flight (the
     * loading screen's cancel button)
     */
    private void cancelActiveLoad() {
        loadCancelRequested.set(true);
        if (activeLoadRun != null && activeLoadQueue != null) {
            activeLoadQueue.cancel(activeLoadRun);
        }
    }

    /**
     * Feature #1359: restores the content that was shown before the loading
     * screen (a failed or cancelled load into this window)
     */
    private void restoreAfterCancelledLoad() {
        loadingScreen = null;
        if (contentBeforeLoadingScreen != null) {
            setContentPane(contentBeforeLoadingScreen);
            contentBeforeLoadingScreen = null;
            revalidate();
            repaint();
        }
    }

    /**
     * Feature #1359: after an aborted load, either restore the previous
     * content (load into this window) or dispose the freshly created window
     * (load into a new window)
     */
    private void restoreOrDisposeAfterAbortedLoad(JIPipeDesktopProjectWorkbench previousWorkbench) {
        if (previousWorkbench != null || contentBeforeLoadingScreen != null) {
            restoreAfterCancelledLoad();
        } else {
            dispose();
        }
    }

    /**
     * Feature #1359: loads a project file into this window with an
     * in-window loading screen (the replacement for the modal "Load
     * project" dialog).
     *
     * @param path                  the project file
     * @param report                validation report filled by the reader
     * @param notifications         notification inbox filled by the reader
     * @param initiatorWindow       the window the open was initiated from (dialog parenting)
     * @param replaceCurrentProject if true, the currently displayed project is released once the read succeeded
     */
    private void beginProjectLoad(Path path, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeDesktopProjectWindow initiatorWindow, boolean replaceCurrentProject) {
        final JIPipeDesktopProjectWindow thisWindow = this;
        final JIPipeProject previousProject = replaceCurrentProject ? project : null;
        final JIPipeDesktopProjectWorkbench previousWorkbench = replaceCurrentProject ? projectWorkbench : null;
        final JIPipeProject[] loadedProjectHolder = new JIPipeProject[1];
        final Throwable[] failureHolder = new Throwable[1];
        loadCancelRequested.set(false);

        showLoadingScreen("Loading project", path, true);

        final JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Project loading");
        final DefaultJIPipeRunnable loadRun = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Load project " + path;
            }

            @Override
            public void run() {
                try {
                    getProgressInfo().log("Loading project " + path);
                    if (Files.size(path) > 3 * 1024 * 1024) {
                        getProgressInfo().log("INFO: File size is " + (Files.size(path) / 1024 / 1024) + " MB. Loading this project may take long.");
                        getProgressInfo().log("INFO: Consider down-sizing your projects if you experience performance issues.");
                    }
                    // The dispatcher picks the reader by content
                    // (ZIP -> JIPX, else legacy JSON) and the reader
                    // performs the full load contract (work directory,
                    // user path validation, environment fix gate,
                    // project file)
                    JIPipeProject loaded = JIPipeProjectIOs.readerFromPath(path)
                            .read(path, new UnspecifiedValidationReportContext(), report, notifications, getProgressInfo());
                    if (getProgressInfo().isCancelled()) {
                        // The read completed but was cancelled: release the
                        // throwaway project (its temporary base must not leak)
                        if (loaded != null) {
                            loaded.close(getProgressInfo().resolve("Close cancelled load"));
                        }
                        return;
                    }
                    loadedProjectHolder[0] = loaded;
                } catch (Exception e) {
                    failureHolder[0] = e;
                }
            }
        };
        activeLoadRun = loadRun;
        activeLoadQueue = queue;
        final JIPipeProgressInfo.BatchEventSubscription statusSubscription = wireStatusToLoadingScreen(loadRun);
        final AtomicBoolean handled = new AtomicBoolean(false);
        final AtomicBoolean pushInterruptedToLogs = new AtomicBoolean(false);
        Runnable dispatch = () -> {
            if (!handled.compareAndSet(false, true)) {
                return;
            }
            statusSubscription.close();
            if (pushInterruptedToLogs.get()) {
                // Push genuine failures to the run logs (the former dialog
                // used GlobalLogMode.OnlyFailures)
                JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(loadRun, false);
            }
            SwingUtilities.invokeLater(() -> thisWindow.onProjectLoadFinished(path, report, notifications, initiatorWindow,
                    loadedProjectHolder[0], failureHolder[0], previousProject, previousWorkbench));
        };
        // The queue-level events are NOT delivered for a run that is
        // cancelled while running: the executor unregisters itself from the
        // worker before the worker's done() emits, so nothing forwards the
        // event to the queue emitter (that dead-end stuck the loading
        // screen on cancel). The worker's done() always emits exactly one
        // completion event on its own emitters - await it there directly.
        // The handled flag deduplicates the two event sources.
        JIPipeRunnableWorker worker = queue.enqueue(loadRun);
        worker.getFinishedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == loadRun) {
                dispatch.run();
            }
        });
        worker.getInterruptedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == loadRun) {
                pushInterruptedToLogs.set(!loadCancelRequested.get());
                dispatch.run();
            }
        });
        queue.getFinishedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == loadRun) {
                dispatch.run();
            }
        });
        queue.getInterruptedEventEmitter().subscribeLambda((emitter, event) -> {
            if (event.getRun() == loadRun) {
                pushInterruptedToLogs.set(!loadCancelRequested.get());
                dispatch.run();
            }
        });
    }

    /**
     * Feature #1359: EDT completion of a project load
     */
    private void onProjectLoadFinished(Path path, JIPipeValidationReport report, JIPipeNotificationInbox notifications,
                                       JIPipeDesktopProjectWindow initiatorWindow, JIPipeProject loadedProject, Throwable failure,
                                       JIPipeProject previousProject, JIPipeDesktopProjectWorkbench previousWorkbench) {
        activeLoadRun = null;
        activeLoadQueue = null;
        if (disposing) {
            // The window was closed while loading. Silently release what was
            // already read (its temporary base must not leak) - dispose()
            // already requested the close of the previous project.
            requestProjectClose(loadedProject, null);
            return;
        }
        if (failure != null && !loadCancelRequested.get()) {
            showLoadFailureDialog(path, failure);
            restoreOrDisposeAfterAbortedLoad(previousWorkbench);
            return;
        }
        if (loadedProject == null) {
            // The load was cancelled (or the reader produced no project)
            restoreOrDisposeAfterAbortedLoad(previousWorkbench);
            return;
        }
        if (loadingScreen != null) {
            loadingScreen.setCancelVisible(false);
        }
        if (previousProject != null) {
            // "This window": release the old workbench UI first (the loading
            // screen already replaced it as content pane), then release the
            // old project in the background, then instantiate the workbench
            // of the loaded project
            if (previousWorkbench != null) {
                previousWorkbench.dispose();
            }
            projectWorkbench = null;
            if (loadingScreen != null) {
                loadingScreen.setTask("Closing project");
            }
            requestProjectClose(previousProject, () -> {
                if (disposing) {
                    return;
                }
                finishProjectLoad(path, loadedProject, report, notifications, initiatorWindow);
            });
        } else {
            finishProjectLoad(path, loadedProject, report, notifications, initiatorWindow);
        }
    }

    /**
     * Feature #1359: EDT bookkeeping after a project file was loaded
     * successfully (the former EDT callback of the modal dialog flow)
     */
    private void finishProjectLoad(Path path, JIPipeProject loadedProject, JIPipeValidationReport report,
                                   JIPipeNotificationInbox notifications, JIPipeDesktopProjectWindow initiatorWindow) {
        loadingScreen = null;
        contentBeforeLoadingScreen = null;
        loadProject(loadedProject, false, false);
        projectSavePath = path;
        if (projectWorkbench != null) {
            projectWorkbench.sendStatusBarText("Opened project from " + projectSavePath);
        }
        updateTitle();
        JIPipe.getInstance().getRecentProjects().add(path);
        if (!notifications.isEmpty()) {
            UIUtils.openNotificationsDialog(projectWorkbench,
                    initiatorWindow != null ? initiatorWindow : this,
                    notifications,
                    "Potential issues found",
                    "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.",
                    true);
        }
        if (!report.isValid()) {
            UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(),
                    initiatorWindow != null ? initiatorWindow : this,
                    report,
                    "Errors while loading the project",
                    "It seems that not all parameters/nodes/connections could be restored from the project file. The cause might be that you are using a version of JIPipe that changed the affected features. " +
                            "Please review the entries and apply the necessary changes (e.g., reconnecting nodes).", false);
        }
    }

    /**
     * Feature #1359: error dialog for a failed project load (the former
     * modal dialog showed the error in its log panel)
     */
    private void showLoadFailureDialog(Path path, Throwable failure) {
        UIUtils.showErrorDialog(new JIPipeDesktopDummyWorkbench(), this, new JIPipeValidationRuntimeException(failure,
                "Error while loading the project",
                "The project file " + path + " could not be loaded: " + failure,
                "Please check the project file and the Tools > Logs entries for details."));
    }

    /**
     * Feature #1359: finds an open window that displays a project loaded
     * from the given file (the "project already open" route)
     */
    public static JIPipeDesktopProjectWindow findOpenWindowForProjectFile(Path path) {
        for (JIPipeDesktopProjectWindow window : OPEN_WINDOWS) {
            if (window.disposing || window.projectSavePath == null) {
                continue;
            }
            if (isSameProjectFile(window.projectSavePath, path)) {
                return window;
            }
        }
        return null;
    }

    private static boolean isSameProjectFile(Path a, Path b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        try {
            return Files.isSameFile(a, b);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Feature #1359: brings this window to the front and un-iconifies it
     * (the "project already open" route)
     */
    public void bringWindowToFront() {
        if ((getExtendedState() & Frame.ICONIFIED) != 0) {
            setExtendedState(getExtendedState() & ~Frame.ICONIFIED);
        }
        toFront();
        requestFocus();
    }

    private void closeAllBalloons() {
        for (BalloonTip registeredBalloon : registeredBalloons) {
            registeredBalloon.closeBalloon();
        }
        registeredBalloons.clear();
    }

    /**
     * @return GUI command
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return The current project
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * @return The current project UI
     */
    public JIPipeDesktopProjectWorkbench getProjectWorkbench() {
        return projectWorkbench;
    }

    /**
     * @return Last known project save path
     */
    public Path getProjectSavePath() {
        return projectSavePath;
    }

    /**
     * Saves the project and cache
     */
    public void saveProjectAndCacheToDirectory(String title, boolean addAsRecentProject) {
        Path directory = JIPipeDesktop.saveDirectory(this, getProjectWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, title, HTMLText.EMPTY);
        if (directory == null) {
            return;
        }
        try {
            if (Files.exists(directory) && Files.list(directory).count() > 0) {
                if (!JIPipeDesktopDialogs.confirm(this, "Save project and cache", "The selected directory " + directory + " is not empty. The contents will be deleted before writing the outputs. " +
                        "Continue anyway?")) {
                    return;
                }
            }
            if (Files.exists(directory) && Files.list(directory).count() > 0 && !Files.exists(directory.resolve("project.jip"))) {
                if (!JIPipeDesktopDialogs.confirm(this, "Save project and cache", "The selected directory " + directory + " does not look like an old output. Please note that the directory will be deleted!" +
                        "Continue anyway?")) {
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        JIPipeDesktopSaveProjectAndCacheToDirectoryRun run = new JIPipeDesktopSaveProjectAndCacheToDirectoryRun(projectWorkbench, project, directory, addAsRecentProject);
        JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run);
    }

    /**
     * Saves the project and cache
     */
    public void saveProjectAndCacheToZIP(String title) {
        Path file = JIPipeDesktop.saveFile(this, getProjectWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, title, HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_ZIP);
        if (file == null) {
            return;
        }
        if (Files.exists(file)) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        JIPipeDesktopSaveProjectAndCacheToZipRun run = new JIPipeDesktopSaveProjectAndCacheToZipRun(projectWorkbench, project, file);
        JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void registerBalloon(BalloonTip balloonTip) {
        this.registeredBalloons.add(balloonTip);
    }

    public JIPipeDesktopProjectWindow newProjectWindow() {
        JIPipeProject project = getDefaultTemplateProject();
        JIPipeDesktopProjectWindow window = newWindow(context, project, false, true);
        window.projectSavePath = null;
        window.updateTitle();
        window.getProjectWorkbench().sendStatusBarText("Created new project");
        return window;
    }

    public void importROCrate() {
        Path projectPath = JIPipeDesktop.openFile(this, getProjectWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Import JIPipe Workflow RO-Crate",
                new HTMLText("Please select a *.crate.zip file that was generated using JIPipe."),
                PathUtils.EXTENSION_FILTER_WORKFLOW_RO_CRATE);
        if (projectPath != null) {
            importROCrate(projectPath, false, false, false);
        }
    }

    public void importROCrate(Path projectPath, boolean forceFallbackProjectDir, boolean nonInteractive, boolean forceCurrentWindow) {
        var window = this;
        String fileName = projectPath.getFileName().toString();
        String fileNameNoExt = fileName.substring(0, fileName.length() - 4);
        Path extractPath;

        // Determine an automated directory
        Path baseExtractPath;
        if (forceFallbackProjectDir) {
            Path defaultProjectsDirectory = JIPipeProjectDefaultsApplicationSettings.getInstance().getAbsoluteDefaultProjectsDirectory();
            PathUtils.createDirectories(defaultProjectsDirectory);
            baseExtractPath = defaultProjectsDirectory;
        } else {
            baseExtractPath = projectPath.getParent();
        }

        String uniqueFileNameNoExt = StringUtils.makeUniqueString(fileNameNoExt, "-", str -> Files.exists(baseExtractPath.resolve(str)));
        extractPath = baseExtractPath.resolve(uniqueFileNameNoExt);

        Button continueButton = Button.custom("Continue").asDefault();
        Button customPathButton = Button.custom("Select custom path");

        Optional<Button> option;

        if (nonInteractive) {
            option = Optional.of(continueButton);
        } else {
            option = JIPipeDesktopDialogs.choose(this,
                    "Import RO-Crate",
                    "The RO-Crate will be extracted to:\n\n" + extractPath + "\n\nDo you want to continue or customize the path?",
                    continueButton, customPathButton);
        }

        // Any result that is not an explicit choice (a dismissal via the
        // window's X / Escape) cancels the import.
        if (option.isEmpty()) {
            return;
        }

        // Ask for a non-existing extract path
        if (option.get() == customPathButton) {
            while (true) {
                extractPath = JIPipeDesktop.saveDirectory(this, getProjectWorkbench(), projectPath.getParent().resolve(fileNameNoExt), "Import JIPipe Workflow RO-Crate - Target directory",
                        new HTMLText("Please confirm where the RO-Crate contents will be extracted."));
                if (extractPath != null) {
                    if (!PathUtils.isEmptyOrNonExistingDirectory(extractPath)) {
                        JIPipeDesktopDialogs.message(window, "Import JIPipe Workflow RO-Crate", "The directory " + extractPath + " already exists or is non-empty. Please choose a different directory.");
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        if (extractPath != null) {
            Path finalExtractPath = extractPath;
            JIPipeQueuedRunnableExecutor localQueue = new JIPipeQueuedRunnableExecutor("Project loading");
            var run = new DefaultJIPipeRunnable() {
                @Override
                public void run() {
                    getProgressInfo().log("Extracting to " + finalExtractPath);
                    PathUtils.createDirectories(finalExtractPath);
                    try {
                        ArchiveUtils.decompressZipFile(projectPath, finalExtractPath, getProgressInfo().resolve("Extract archive"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Path projectFile = finalExtractPath.resolve("project.jip");
                    if (!Files.isRegularFile(projectFile)) {
                        SwingUtilities.invokeLater(() -> {
                            JIPipeDesktopDialogs.message(window, "Import JIPipe Workflow RO-Crate", "Unable to find project.jip file inside the extracted files!");
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            openProject(projectFile, forceCurrentWindow);
                        });
                    }
                }

                @Override
                public String getTaskLabel() {
                    return "Extract RO-Crate";
                }
            };

            JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run, localQueue, JIPipeDesktopRunExecuteUI.GlobalLogMode.OnlyFailures);
        }
    }

    public JIPipeCefClientService getCefClientService() {
        return cefClientService;
    }

    public void importURL(String url, boolean forceCurrentWindow) {
        // as default explicity no file name to leave it to ZIP detection
        String fileName = WebUtils.extractFileName(url, "project");
        JIPipeQueuedRunnableExecutor localQueue = new JIPipeQueuedRunnableExecutor("Project loading");
        var run = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Import project from URL";
            }

            @Override
            public void run() {
                getProgressInfo().aggressiveWarn("You are about to download and open the following project:",
                        url,
                        "",
                        "Cancel the operation if you do not trust this source.");
                for (int i = 5; i >= 0; i--) {
                    if (getProgressInfo().isCancelled()) {
                        return;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    getProgressInfo().warn("About to download URL in " + i + "s ...");
                }
                if (getProgressInfo().isCancelled()) {
                    return;
                }

                Path tmpDir = JIPipe.getTemporaryDirectory("import-project-from-url");
                Path outputFile = tmpDir.resolve(fileName);
                WebUtils.download(WebUtils.toURL(url), outputFile, "Download project", getProgressInfo().resolve("Download"));

                if (getProgressInfo().isCancelled()) {
                    return;
                }

                // Analyze the output file
                boolean isRoCrate;
                if (fileName.endsWith(".jip") || PathUtils.isJipxPath(outputFile)) {
                    // A JIPX container is opened directly as a project -
                    // this check precedes the ZIP-magic check, because the
                    // container IS a ZIP
                    isRoCrate = false;
                } else if (fileName.endsWith(".crate.zip")) {
                    isRoCrate = true;
                } else if (ArchiveUtils.isZipFile(outputFile)) {
                    isRoCrate = true;
                } else {
                    isRoCrate = false;
                }

                // Depending on the status of the project, it will be copied into the standard folder or be imported directy
                if (isRoCrate) {
                    SwingUtilities.invokeLater(() -> {
                        importROCrate(outputFile, true, true, forceCurrentWindow);
                    });
                } else {
                    Path basePath = JIPipeProjectDefaultsApplicationSettings.getInstance().getAbsoluteDefaultProjectsDirectory();

                    // Copy into unique file within the fallback dir, preserving
                    // the project's format (.jip/.jipx); truly unknown
                    // extensions fall back to .jip
                    String fileNameNoExt;
                    String extension;
                    if (fileName.endsWith(".jip")) {
                        fileNameNoExt = fileName.substring(0, fileName.length() - 4);
                        extension = ".jip";
                    } else if (PathUtils.isJipxPath(outputFile)) {
                        fileNameNoExt = fileName.substring(0, fileName.length() - 5);
                        extension = ".jipx";
                    } else {
                        fileNameNoExt = fileName;
                        extension = ".jip";
                    }

                    String uniqueFileNameNoExt = StringUtils.makeUniqueString(fileNameNoExt, "-", str -> Files.exists(basePath.resolve(str)));
                    Path finalProjectFile = basePath.resolve(uniqueFileNameNoExt + extension);
                    PathUtils.copyFile(outputFile, finalProjectFile);

                    SwingUtilities.invokeLater(() -> {
                        openProject(finalProjectFile, forceCurrentWindow);
                    });
                }
            }
        };

        JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run, localQueue, JIPipeDesktopRunExecuteUI.GlobalLogMode.OnlyFailures);
    }
}
