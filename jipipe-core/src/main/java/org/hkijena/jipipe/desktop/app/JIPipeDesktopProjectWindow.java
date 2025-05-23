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
import net.imagej.updater.UpdateSite;
import net.java.balloontip.BalloonTip;
import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectMetadata;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.registries.JIPipePluginRegistry;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.project.*;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopSplashScreen;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEventEmitter;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEventEmitter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
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
    private JIPipeProject project;
    private JIPipeDesktopProjectWorkbench projectUI;
    private Path projectSavePath;
    private UUID sessionId = UUID.randomUUID();
    private List<BalloonTip> registeredBalloons = new ArrayList<>();

    /**
     * @param context          context
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     */
    public JIPipeDesktopProjectWindow(Context context, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        JIPipeDesktopSplashScreen.getInstance().hideSplash();
        this.context = context;
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
            if (window.project == project)
                return window;
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
                if (StringUtils.isNullOrEmpty(id) || !JIPipe.getInstance().getProjectTemplateRegistry().getRegisteredTemplates().containsKey(id)) {
                    id = JIPipeProjectTemplate.getFallbackTemplateId();
                    JIPipeProjectDefaultsApplicationSettings.getInstance().getProjectTemplate().setValue(id);
                    if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                        JIPipe.getInstance().getApplicationSettingsRegistry().save();
                    }
                }
                JIPipeProjectTemplate template = JIPipe.getInstance().getProjectTemplateRegistry().getRegisteredTemplates().get(id);
                JIPipeValidationReport report = new JIPipeValidationReport();
                JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
                project = template.loadAsProject(report, notifications);
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
        frame.setSize(1280, 800);
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
            if (projectUI != null && projectUI.isProjectModified()) {
                return "Do you really want to close JIPipe?\nThere are some unsaved changes.";
            } else {
                return "Do you really want to close JIPipe?";
            }
        }, "Close window");
        if (JIPipeGeneralUIApplicationSettings.getInstance().isMaximizeWindows()) {
            SwingUtilities.invokeLater(() -> setExtendedState(getExtendedState() | MAXIMIZED_BOTH));
        }
    }

    private void unloadProject() {
        projectUI.unload();
    }

    @Override
    public void dispose() {
        unloadProject();
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
        this.projectUI = new JIPipeDesktopProjectWorkbench(this, context, project, showIntroduction, isNewProject);
        this.sessionId = UUID.randomUUID();
        setContentPane(projectUI);
    }

    /**
     * Creates a new project.
     * Asks the user if it should replace the currently displayed project
     */
    public void newProject() {
        JIPipeProject project = getDefaultTemplateProject();
        JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true);
        if (window == null)
            return;
        window.projectSavePath = null;
        window.updateTitle();
        window.getProjectUI().sendStatusBarText("Created new project");
    }

    /**
     * Creates a new project from template
     */
    public void newProjectFromTemplate() {
        JIPipeDesktopJIPipeTemplateSelectionUI dialog = new JIPipeDesktopJIPipeTemplateSelectionUI(projectUI, this);
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
                                getProjectUI(),
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
            JIPipeDesktopRunExecuteUI.runInDialog(projectUI, this, run);
        } else {
            try {
                JIPipeValidationReport report = new JIPipeValidationReport();
                JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
                JIPipeProject project = template.loadAsProject(report, notifications);
                JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true);
                if (window == null)
                    return;
                window.projectSavePath = null;
                window.updateTitle();
                window.getProjectUI().sendStatusBarText("Created new project");
                if (!notifications.isEmpty()) {
                    UIUtils.openNotificationsDialog(window.getProjectUI(), this, notifications, "Potential issues found", "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.", true);
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
        if (projectUI == null) {
            setTitle("JIPipe");
            return;
        }
        if (projectSavePath == null) {
            setTitle("JIPipe - New project" + (projectUI.isProjectModified() ? "*" : ""));
        } else {
            setTitle("JIPipe - " + projectSavePath + (projectUI.isProjectModified() ? "*" : ""));
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
                if (JIPipe.getInstance().getImageJPlugins() != null) {
                    // Populate
                    for (JIPipeDependency dependency : dependencySet) {
                        missingUpdateSites.addAll(dependency.getImageJUpdateSiteDependencies());
                    }
                    missingUpdateSites.addAll(metadata.getUpdateSiteDependencies());
                    // Remove existing
                    for (UpdateSite updateSite : JIPipe.getInstance().getImageJPlugins().getUpdateSites(true)) {
                        if (updateSite.isActive()) {
                            missingUpdateSites.removeIf(site -> Objects.equals(site.getName(), updateSite.getName()));
                        }
                    }
                }
                Set<JIPipeDependency> missingDependencies = JIPipePluginRegistry.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty() || !missingUpdateSites.isEmpty()) {
                    if (!JIPipeDesktopInvalidProjectDependenciesInfoDialog.showDialog(getProjectUI(), path, missingDependencies))
                        return;
                }

                JIPipeRunnableQueue localQueue = new JIPipeRunnableQueue("Project loading");
                JIPipeDesktopProjectWindow currentWindow = this;
                JIPipeRunnable run = new AbstractJIPipeRunnable() {
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
                            project.fromJson(jsonData, new UnspecifiedValidationReportContext(), report, notifications);
                            project.setWorkDirectory(path.getParent());
                            project.validateUserDirectories(notifications);

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
                                if (window == null)
                                    return;
                                window.projectSavePath = path;
                                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                                window.updateTitle();
                                JIPipe.getInstance().getRecentProjectsRegistry().add(path);
                                if (!notifications.isEmpty()) {
                                    UIUtils.openNotificationsDialog(window.getProjectUI(),
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
                JIPipeDesktopRunExecuteUI.runInDialog(projectUI, this, run, localQueue);

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
                Set<JIPipeDependency> missingDependencies = JIPipePluginRegistry.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty()) {
                    if (!JIPipeDesktopInvalidProjectDependenciesInfoDialog.showDialog(getProjectUI(), path, missingDependencies))
                        return;
                }

                JIPipeProject newProject = JIPipeProject.loadProject(projectPath, new UnspecifiedValidationReportContext(), report, notifications);
                JIPipeDesktopProjectWindow window = openProjectInThisOrNewWindow("Open JIPipe output", newProject, false, false);
                if (window == null)
                    return;

                JIPipeFileChooserApplicationSettings.getInstance().setLastDirectoryBy(JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, path);

                window.projectSavePath = path.resolve("project.jip");
                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.updateTitle();

                JIPipe.getInstance().getRecentProjectsRegistry().add(path);

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
                    window.getProjectUI().getDocumentTabPane().addTab("Results",
                            UIUtils.getIconFromResources("actions/document-open-folder.png"),
                            new JIPipeDesktopResultUI(window.projectUI, project, path),
                            JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton,
                            true);
                    window.getProjectUI().getDocumentTabPane().switchToLastTab();
                } else if (selectedOption == JOptionPane.NO_OPTION) {
                    // Load into cache with a run
                    JIPipeDesktopRunExecuteUI.runInDialog(projectUI, this, new JIPipeDesktopLoadResultDirectoryIntoCacheRun(projectUI, project, path, true));
                }
                if (!notifications.isEmpty()) {
                    UIUtils.openNotificationsDialog(window.getProjectUI(), this, notifications, "Potential issues found", "There seem to be potential issues that might prevent the successful execution of the pipeline. Please review the following entries and resolve the issues if possible.", true);
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
                getProjectUI(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
                "Open JIPipe project (*.jip)",
                HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_JIP);
        if (file != null) {
            openProject(file, false);
        }
    }

    /**
     * Opens a file chooser where the user can select a result folder
     */
    public void openProjectAndOutput() {
        Path file = JIPipeDesktop.openDirectory(this,
                getProjectUI(),
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
        if (avoidDialog && projectSavePath != null)
            savePath = projectSavePath;
        if (savePath == null) {
            savePath = JIPipeDesktop.saveFile(this,
                    getProjectUI(),
                    JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
                    "Save JIPipe project (*.jip)",
                    HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_JIP);
            if (savePath == null)
                return;
        }

        try {
            Path tempFile = PathUtils.createSubTempFilePath(savePath.getParent(), savePath.getFileName().toString(), ".part");
            getProject().setWorkDirectory(savePath.getParent());
            getProject().getAdditionalMetadata().put(JIPipeDesktopJIPipeProjectTabMetadata.METADATA_KEY, new JIPipeDesktopJIPipeProjectTabMetadata(getProjectUI()));
            getProject().saveProject(tempFile);

            // Check if the saved project can be loaded
            JIPipeProject.loadProject(tempFile, new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());

            // Overwrite the target file
            if (Files.exists(savePath))
                Files.delete(savePath);
            Files.copy(tempFile, savePath);

            // Everything OK, now set the title
            if (updateSavePath) {
                projectSavePath = savePath;
            }
            updateTitle();
            projectUI.setProjectModified(false);
            projectUI.sendStatusBarText("Saved project to " + savePath);
            JIPipe.getInstance().getRecentProjectsRegistry().add(savePath);

            // Remove tmp file
            Files.delete(tempFile);
        } catch (IOException e) {
            UIUtils.showErrorDialog(getProjectUI(), this, new JIPipeValidationRuntimeException(e,
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
                loadProject(project, false, isNewProject);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(context, project, showIntroduction, isNewProject);
        }
        return null;
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
    public JIPipeDesktopProjectWorkbench getProjectUI() {
        return projectUI;
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
        Path directory = JIPipeDesktop.saveDirectory(this, getProjectUI(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, title, HTMLText.EMPTY);
        if (directory == null)
            return;
        try {
            if (Files.exists(directory) && Files.list(directory).count() > 0) {
                if (JOptionPane.showConfirmDialog(this, "The selected directory " + directory + " is not empty. The contents will be deleted before writing the outputs. " +
                        "Continue anyway?", "Save project and cache", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                    return;
            }
            if (Files.exists(directory) && Files.list(directory).count() > 0 && !Files.exists(directory.resolve("project.jip"))) {
                if (JOptionPane.showConfirmDialog(this, "The selected directory " + directory + " does not look like an old output. Please note that the directory will be deleted!" +
                        "Continue anyway?", "Save project and cache", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                    return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        JIPipeDesktopSaveProjectAndCacheToDirectoryRun run = new JIPipeDesktopSaveProjectAndCacheToDirectoryRun(projectUI, project, directory, addAsRecentProject);
        JIPipeDesktopRunExecuteUI.runInDialog(projectUI, this, run);
    }

    /**
     * Saves the project and cache
     */
    public void saveProjectAndCacheToZIP(String title) {
        Path file = JIPipeDesktop.saveFile(this, getProjectUI(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, title, HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_ZIP);
        if (file == null)
            return;
        if (Files.exists(file)) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        JIPipeDesktopSaveProjectAndCacheToZipRun run = new JIPipeDesktopSaveProjectAndCacheToZipRun(projectUI, project, file);
        JIPipeDesktopRunExecuteUI.runInDialog(projectUI, this, run);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void registerBalloon(BalloonTip balloonTip) {
        this.registeredBalloons.add(balloonTip);
    }
}
