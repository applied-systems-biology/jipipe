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

import com.fasterxml.jackson.databind.JsonNode;
import net.java.balloontip.BalloonTip;
import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectMetadata;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.components.JIPipePluginsServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.project.*;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.app.JIPipeDesktopSplashScreen;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
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
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

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
     * @return All open project windows
     */
    public static Set<JIPipeDesktopProjectWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
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
        closeCurrentProject();
        OPEN_WINDOWS.remove(this);
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
        setContentPane(projectWorkbench);
    }

    /**
     * Creates a new project.
     * Asks the user if it should replace the currently displayed project
     */
    public void newProject() {
        JIPipeProject project = getDefaultTemplateProject();
        JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true);
        if (window == null) {
            return;
        }
        window.projectSavePath = null;
        window.updateTitle();
        window.getProjectWorkbench().sendStatusBarText("Created new project");
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
                int result = JOptionPane.showOptionDialog(this,
                        "The template contains " + Precision.round(sizeMB, 2) + " MB of data.\nYou can choose to either load only the " +
                                "pipeline or extract the template project and related data into a directory.",
                        "Load template",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[]{"Load with data", "Load only pipeline", "Cancel"},
                        "Load with data");
                switch (result) {
                    case JOptionPane.CANCEL_OPTION:
                        return;
                    case JOptionPane.NO_OPTION:
                        break;
                    case JOptionPane.YES_OPTION:
                        loadZipTarget = JIPipeDesktop.saveDirectory(this,
                                getProjectWorkbench(),
                                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
                                "Load template: Choose an empty directory", HTMLText.EMPTY);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (loadZipTarget != null) {
            JIPipeExtractTemplateZipFileRun run = new JIPipeExtractTemplateZipFileRun(template, loadZipTarget);
            Path finalLoadZipTarget = loadZipTarget;

            JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeLambda((emitter, event) -> {
                if (event.getRun() == run) {
                    SwingUtilities.invokeLater(() -> {
                        Path projectFile = PathUtils.findFileByExtensionRecursivelyIn(finalLoadZipTarget, ".jip");
                        if (projectFile == null) {
                            JOptionPane.showMessageDialog(JIPipeDesktopProjectWindow.this,
                                    "No project file in " + finalLoadZipTarget,
                                    "Load template",
                                    JOptionPane.ERROR_MESSAGE);
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
                JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true);
                if (window == null) {
                    return;
                }
                window.projectSavePath = null;
                window.updateTitle();
                window.getProjectWorkbench().sendStatusBarText("Created new project");
                if (!notifications.isEmpty()) {
                    UIUtils.openNotificationsDialog(window.getProjectWorkbench(), this, notifications, "Potential issues found", "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.", true);
                }
                if (!report.isValid()) {
                    UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(), this, report, "Errors while loading the project", "It seems that not all parameters/nodes/connections could be restored from the project file. The cause might be that you are using a version of JIPipe that changed the affected features. " +
                            "Please review the entries and apply the necessary changes (e.g., reconnecting nodes).", false);
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
            JIPipeValidationReport report = new JIPipeValidationReport();
            JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
            notifications.connectDismissTo(JIPipeNotificationInbox.getInstance());
            try {
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
                Set<JIPipeDependency> dependencySet = JIPipeProject.loadDependenciesFromJson(jsonData);

                JIPipeProjectMetadata metadata = JIPipeProject.loadMetadataFromJson(jsonData);

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

                JIPipeRunnableQueue localQueue = new JIPipeRunnableQueue("Project loading");
                JIPipeDesktopProjectWindow currentWindow = this;
                JIPipeRunnable run = new DefaultJIPipeRunnable() {
                    @Override
                    public String getTaskLabel() {
                        return "Load project " + path;
                    }

                    @Override
                    public void run() {
                        try {
                            JIPipeProject project = new JIPipeProject();
                            getProgressInfo().log("Loading project " + path);
                            if (Files.size(path) > 3 * 1024 * 1024) {
                                getProgressInfo().log("INFO: File size is " + (Files.size(path) / 1024 / 1024) + " MB. Loading this project may take long.");
                                getProgressInfo().log("INFO: Consider down-sizing your projects if you experience performance issues.");
                            }
                            project.fromJson(jsonData, new UnspecifiedValidationReportContext(), report, notifications, getProgressInfo());
                            project.setWorkDirectory(path.getParent());
                            project.validateUserPaths(notifications);
                            project.setProjectFile(path);
                            project.fixBrokenEnvironments(getProgressInfo().resolve("Fix environments"));

                            if (getProgressInfo().isCancelled()) {
                                return;
                            }

                            SwingUtilities.invokeLater(() -> {
                                JIPipeDesktopProjectWindow window;
                                if (forceCurrentWindow) {
                                    window = currentWindow;
                                    loadProject(project, false, false);
                                } else {
                                    window = openProjectInThisOrNewWindow("Open project", project, false, false);
                                }
                                if (window == null) {
                                    return;
                                }
                                window.projectSavePath = path;
                                window.getProjectWorkbench().sendStatusBarText("Opened project from " + window.projectSavePath);
                                window.updateTitle();
                                JIPipe.getInstance().getRecentProjects().add(path);
                                if (!notifications.isEmpty()) {
                                    UIUtils.openNotificationsDialog(window.getProjectWorkbench(),
                                            currentWindow,
                                            notifications,
                                            "Potential issues found",
                                            "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.",
                                            true);
                                }
                            });

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run, localQueue);

                JIPipeFileChooserApplicationSettings.getInstance().setLastDirectoryBy(JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, path.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!report.isValid()) {
                UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(), this, report, "Errors while loading the project", "It seems that not all parameters/nodes/connections could be restored from the project file. The cause might be that you are using a version of JIPipe that changed the affected features. " +
                        "Please review the entries and apply the necessary changes (e.g., reconnecting nodes).", false);
            }
        } else if (Files.isDirectory(path)) {
            JIPipeValidationReport report = new JIPipeValidationReport();
            JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
            notifications.connectDismissTo(JIPipeNotificationInbox.getInstance());
            try {
                Path projectPath = path.resolve("project.jip");
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(projectPath.toFile(), JsonNode.class);
                Set<JIPipeDependency> dependencySet = JIPipeProject.loadDependenciesFromJson(jsonData);
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
                JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("Open JIPipe output", newProject, false, false);
                if (window == null) {
                    return;
                }

                JIPipeFileChooserApplicationSettings.getInstance().setLastDirectoryBy(JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, path);

                window.projectSavePath = path.resolve("project.jip");
                window.getProjectWorkbench().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.updateTitle();

                JIPipe.getInstance().getRecentProjects().add(path);

                // Give user the option to either open in tab or cache
                int selectedOption = JOptionPane.showOptionDialog(window,
                        "You can either open the output in a tab or load them into the cache. " +
                                "Please note that caching the results might require large amounts of memory.",
                        "Open JIPipe output",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[]{"Load in new tab", "Load into cache", "Cancel"},
                        "Load in new tab");

                if (selectedOption == JOptionPane.YES_OPTION) {
                    // Create a new tab
                    window.getProjectWorkbench().getDocumentTabPane().addTab("Results",
                            JIPipe.RESOURCES.getIcon16("actions/document-open-folder.png"),
                            new JIPipeDesktopResultUI(window.projectWorkbench, project, path),
                            JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton,
                            true);
                    window.getProjectWorkbench().getDocumentTabPane().switchToLastTab();
                } else if (selectedOption == JOptionPane.NO_OPTION) {
                    // Load into cache with a run
                    JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, new JIPipeDesktopLoadResultDirectoryIntoCacheRun(projectWorkbench, project, path, true));
                }
                if (!notifications.isEmpty()) {
                    UIUtils.openNotificationsDialog(window.getProjectWorkbench(), this, notifications, "Potential issues found", "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.", true);
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
                "Open JIPipe project (*.jip)",
                HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JIP);
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
                    "Save JIPipe project (*.jip)",
                    HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JIP);
            if (savePath == null) {
                return;
            }
        }

        try {
            Path tempFile = PathUtils.createSubTempFilePath(savePath.getParent(), savePath.getFileName().toString(), ".part");
            getProject().setWorkDirectory(savePath.getParent());
            getProject().getAdditionalMetadata().put(JIPipeDesktopJIPipeProjectTabMetadata.METADATA_KEY, new JIPipeDesktopJIPipeProjectTabMetadata(getProjectWorkbench()));
            getProject().saveProject(tempFile, updateSavePath);

            // Check if the saved project can be loaded
            JIPipeProject.loadProject(tempFile,
                    new UnspecifiedValidationReportContext(),
                    new JIPipeValidationReport(),
                    new JIPipeNotificationInbox(),
                    JIPipeProgressInfo.STDOUT);

            // Overwrite the target file
            if (Files.exists(savePath)) {
                Files.delete(savePath);
            }
            Files.copy(tempFile, savePath);

            // Everything OK, now set the title
            if (updateSavePath) {
                projectSavePath = savePath;
            }
            updateTitle();
            projectWorkbench.setProjectModified(false);
            projectWorkbench.sendStatusBarText("Saved project to " + savePath);
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
     * @return The window that holds the project
     */
    private JIPipeDesktopProjectWindow openProjectInThisOrNewWindow(String messageTitle, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        switch (UIUtils.askOpenInCurrentWindow(this, messageTitle)) {
            case JOptionPane.YES_OPTION:
                closeAllBalloons();
                closeCurrentProject();
                loadProject(project, false, isNewProject);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(context, project, showIntroduction, isNewProject);
        }
        return null;
    }

    private void closeCurrentProject() {
        if(project != null && projectWorkbench != null) {
            JIPipeRunnable runnable = new DefaultJIPipeRunnable() {
                @Override
                public String getTaskLabel() {
                    return "Close project";
                }

                @Override
                public void run() {
                    project.close(getProgressInfo());
                }
            };
            JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, runnable, new JIPipeRunnableQueue("Project cleanup"));
        }
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
                if (JOptionPane.showConfirmDialog(this, "The selected directory " + directory + " is not empty. The contents will be deleted before writing the outputs. " +
                        "Continue anyway?", "Save project and cache", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            if (Files.exists(directory) && Files.list(directory).count() > 0 && !Files.exists(directory.resolve("project.jip"))) {
                if (JOptionPane.showConfirmDialog(this, "The selected directory " + directory + " does not look like an old output. Please note that the directory will be deleted!" +
                        "Continue anyway?", "Save project and cache", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
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

        int option;

        if (nonInteractive) {
            option = JOptionPane.YES_OPTION;
        } else {
            option = JOptionPane.showOptionDialog(this,
                    "The RO-Crate will be extracted to:\n\n" + extractPath + "\n\nDo you want to continue or customize the path?",
                    "Import RO-Crate",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Continue", "Select custom path", "Cancel"},
                    "Continue");
        }

        if (option == JOptionPane.CANCEL_OPTION) {
            return;
        }

        // Ask for a non-existing extract path
        if (option == JOptionPane.NO_OPTION) {
            while (true) {
                extractPath = JIPipeDesktop.saveDirectory(this, getProjectWorkbench(), projectPath.getParent().resolve(fileNameNoExt), "Import JIPipe Workflow RO-Crate - Target directory",
                        new HTMLText("Please confirm where the RO-Crate contents will be extracted."));
                if (extractPath != null) {
                    if (!PathUtils.isEmptyOrNonExistingDirectory(extractPath)) {
                        JOptionPane.showMessageDialog(window,
                                "The directory " + extractPath + " already exists or is non-empty. Please choose a different directory.",
                                "Import JIPipe Workflow RO-Crate",
                                JOptionPane.ERROR_MESSAGE);
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
            JIPipeRunnableQueue localQueue = new JIPipeRunnableQueue("Project loading");
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
                            JOptionPane.showMessageDialog(window,
                                    "Unable to find project.jip file inside the extracted files!",
                                    "Import JIPipe Workflow RO-Crate",
                                    JOptionPane.ERROR_MESSAGE);
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

            JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run, localQueue);
        }
    }

    public JIPipeCefClientService getCefClientService() {
        return cefClientService;
    }

    public void importURL(String url, boolean forceCurrentWindow) {
        // as default explicity no file name to leave it to ZIP detection
        String fileName = WebUtils.extractFileName(url, "project");
        JIPipeRunnableQueue localQueue = new JIPipeRunnableQueue("Project loading");
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
                if (fileName.endsWith(".jip")) {
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

                    // Copy into unique file within the fallback dir
                    String fileNameNoExt;
                    if (fileName.endsWith(".jip")) {
                        fileNameNoExt = fileName.substring(0, fileName.length() - 4);
                    } else {
                        fileNameNoExt = fileName;
                    }

                    String uniqueFileNameNoExt = StringUtils.makeUniqueString(fileNameNoExt, "-", str -> Files.exists(basePath.resolve(str)));
                    Path finalProjectFile = basePath.resolve(uniqueFileNameNoExt + ".jip");
                    PathUtils.copyFile(outputFile, finalProjectFile);

                    SwingUtilities.invokeLater(() -> { openProject(finalProjectFile, forceCurrentWindow); });
                }
            }
        };

        JIPipeDesktopRunExecuteUI.runInDialog(projectWorkbench, this, run, localQueue);
    }
}
