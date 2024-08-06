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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailGenerationQueue;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeArchiveProjectToDirectoryRun;
import org.hkijena.jipipe.api.project.JIPipeArchiveProjectToZIPRun;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.backups.JIPipeDesktopBackupManagerPanel;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCacheBrowserUI;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCacheManagerUI;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDataTypeCompendiumUI;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopAlgorithmCompendiumUI;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopWelcomePanel;
import org.hkijena.jipipe.desktop.app.grapheditor.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.app.plugins.JIPipeDesktopPluginValidityCheckerPanel;
import org.hkijena.jipipe.desktop.app.plugins.pluginsmanager.JIPipeDesktopManagePluginsButton;
import org.hkijena.jipipe.desktop.app.project.JIPipeDesktopJIPipeProjectTabMetadata;
import org.hkijena.jipipe.desktop.app.project.JIPipeDesktopLoadResultDirectoryIntoCacheRun;
import org.hkijena.jipipe.desktop.app.project.JIPipeDesktopLoadResultZipIntoCacheRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.desktop.app.running.*;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopApplicationSettingsUI;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopProjectOverviewUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMemoryOptionsControl;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMemoryStatusUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopRecentProjectsMenu;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopReloadableValidityChecker;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopNotificationButton;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopWorkbenchNotificationInboxUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;
import org.scijava.Context;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UI around an {@link JIPipeProject}
 */
public class JIPipeDesktopProjectWorkbench extends JPanel implements JIPipeDesktopWorkbench, JIPipeProject.CompartmentRemovedEventListener, JIPipeService.PluginRegisteredEventListener {

    public static final String TAB_INTRODUCTION = "INTRODUCTION";
    public static final String TAB_LICENSE = "LICENSE";
    public static final String TAB_COMPARTMENT_EDITOR = "COMPARTMENT_EDITOR";
    public static final String TAB_VALIDITY_CHECK = "VALIDITY_CHECK";
    public static final String TAB_PLUGIN_VALIDITY_CHECK = "PLUGIN_VALIDITY_CHECK";
    public static final String TAB_NOTIFICATIONS = "NOTIFICATIONS";
    public static final String TAB_PROJECT_OVERVIEW = "PROJECT_OVERVIEW";
    public static final String TAB_LOG = "LOG";
    private final JIPipeDesktopProjectWindow window;
    private final JIPipeProject project;
    private final Context context;
    private final JIPipeDesktopMemoryOptionsControl memoryOptionsControl;
    private final JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();
    private final JIPipeDesktopNotificationButton notificationButton = new JIPipeDesktopNotificationButton(this);
    private final Map<JIPipeGraphNode, Timer> algorithmUpdateTimers = new WeakHashMap<>();
    private final JIPipeNodeDatabase nodeDatabase;
    public JIPipeDesktopTabPane documentTabPane;
    private JLabel statusText;
    private JIPipeDesktopReloadableValidityChecker validityCheckerPanel;
    private JIPipeDesktopPluginValidityCheckerPanel pluginValidityCheckerPanel;
    private boolean projectModified;

    /**
     * @param window           Parent window
     * @param context          SciJava context
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     */
    public JIPipeDesktopProjectWorkbench(JIPipeDesktopProjectWindow window, Context context, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        this.window = window;
        this.project = project;
        this.context = context;
        this.memoryOptionsControl = new JIPipeDesktopMemoryOptionsControl(this);
        this.nodeDatabase = new JIPipeNodeDatabase(project);
        initialize(showIntroduction, isNewProject);
        project.getCompartmentRemovedEventEmitter().subscribe(this);
        JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeWeak(this);

        validatePlugins(true);

        restoreStandardTabs(showIntroduction, isNewProject);
        if (JIPipeProjectDefaultsApplicationSettings.getInstance().isRestoreTabs())
            restoreTabs();
        if (JIPipeGeneralUIApplicationSettings.getInstance().isShowIntroduction() && showIntroduction)
            documentTabPane.selectSingletonTab(TAB_INTRODUCTION);

        // Register modification state watchers
        project.getGraph().getGraphChangedEventEmitter().subscribeLambda((emitter, event) -> setProjectModified(true));
        project.getCompartmentGraph().getGraphChangedEventEmitter().subscribeLambda((emitter, event) -> setProjectModified(true));

        // Install the run notifier
        JIPipeDesktopRunnableQueueNotifier.install();
    }

    /**
     * Attempts to find a workbench
     *
     * @param graph  the graph
     * @param orElse if no workbench could be found
     * @return the workbench
     */
    public static JIPipeDesktopProjectWorkbench tryFindProjectWorkbench(JIPipeGraph graph, JIPipeWorkbench orElse) {
        JIPipeProject project = graph.getAttachment(JIPipeProject.class);
        if (project != null) {
            JIPipeDesktopProjectWindow window = JIPipeDesktopProjectWindow.getWindowFor(project);
            if (window != null) {
                return window.getProjectUI();
            }
        }
        if (orElse instanceof JIPipeDesktopProjectWorkbench) {
            return (JIPipeDesktopProjectWorkbench) orElse;
        } else {
            return null;
        }
    }

    public static boolean canAddOrDeleteNodes(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeProject project = ((JIPipeDesktopProjectWorkbench) workbench).getProject();
            if (project.getMetadata().getPermissions().isPreventAddingDeletingNodes()) {
                workbench.showMessageDialog("Deleting nodes & compartments is disabled for this project. " +
                                "\n\nIf this is not intentional, change this setting in Project > Project settings > Prevent adding/deleting nodes",
                        "Permissions denied");
                return false;
            }
        }
        return true;
    }

    public static boolean canModifySlots(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeProject project = ((JIPipeDesktopProjectWorkbench) workbench).getProject();
            if (project.getMetadata().getPermissions().isPreventModifyingSlots()) {
                workbench.showMessageDialog("Modifying slots is disabled for this project. " +
                                "\n\nIf this is not intentional, change this setting in Project > Project settings > Prevent modifying slots",
                        "Permissions denied");
                return false;
            }
        }
        return true;
    }

    public JIPipeNodeDatabase getNodeDatabase() {
        return nodeDatabase;
    }

    public void restoreStandardTabs(boolean showIntroduction, boolean isNewProject) {
        if (JIPipeGeneralUIApplicationSettings.getInstance().isShowIntroduction() && showIntroduction)
            documentTabPane.selectSingletonTab(TAB_INTRODUCTION);
        else {
            if (JIPipeGeneralUIApplicationSettings.getInstance().isShowProjectInfo() && !isNewProject) {
                documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW);
            } else {
                documentTabPane.selectSingletonTab(TAB_COMPARTMENT_EDITOR);
            }
        }
    }

    private void restoreTabs() {
        if (project.getMetadata().isRestoreTabs()) {
            try {
                Object metadata = project.getAdditionalMetadata().getOrDefault(JIPipeDesktopJIPipeProjectTabMetadata.METADATA_KEY, null);
                if (metadata instanceof JIPipeDesktopJIPipeProjectTabMetadata) {
                    ((JIPipeDesktopJIPipeProjectTabMetadata) metadata).restore(this);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            documentTabPane.selectSingletonTab(TAB_COMPARTMENT_EDITOR);
            documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW);
        }
    }

    private void initialize(boolean showIntroduction, boolean isNewProject) {
        setLayout(new BorderLayout());

        // Initialize JIPipe logger
        JIPipeDesktopRunnableLogsCollection.getInstance();

        documentTabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Top);
        documentTabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        documentTabPane.registerSingletonTab(TAB_INTRODUCTION,
                "Getting started",
                UIUtils.getIconFromResources("actions/help-info.png"),
                () -> new JIPipeDesktopWelcomePanel(this),
                (JIPipeGeneralUIApplicationSettings.getInstance().isShowIntroduction() && showIntroduction) ? JIPipeDesktopTabPane.SingletonTabMode.Present : JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_PROJECT_OVERVIEW,
                "Project",
                UIUtils.getIconFromResources("actions/configure.png"),
                () -> new JIPipeDesktopProjectOverviewUI(this),
                (JIPipeGeneralUIApplicationSettings.getInstance().isShowProjectInfo() && !isNewProject) ? JIPipeDesktopTabPane.SingletonTabMode.Present : JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_LICENSE,
                "License",
                UIUtils.getIconFromResources("actions/license.png"),
                () -> new JIPipeDesktopMarkdownReader(true, MarkdownText.fromPluginResource("documentation/license.md", new HashMap<>())),
                JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_COMPARTMENT_EDITOR,
                "Compartments",
                UIUtils.getIconFromResources("actions/graph-compartments.png"),
                () -> new JIPipeDesktopCompartmentsGraphEditorUI(this),
                JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        validityCheckerPanel = new JIPipeDesktopReloadableValidityChecker(this, project);
        documentTabPane.registerSingletonTab(TAB_VALIDITY_CHECK,
                "Project validation",
                UIUtils.getIconFromResources("actions/checkmark.png"),
                () -> validityCheckerPanel,
                JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        pluginValidityCheckerPanel = new JIPipeDesktopPluginValidityCheckerPanel(this);
        documentTabPane.registerSingletonTab(TAB_PLUGIN_VALIDITY_CHECK,
                "Plugin validation",
                UIUtils.getIconFromResources("actions/plugins.png"),
                () -> pluginValidityCheckerPanel,
                JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_LOG,
                "Log viewer",
                UIUtils.getIconFromResources("actions/show_log.png"),
                () -> new JIPipeDesktopLogViewer(this),
                JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_NOTIFICATIONS,
                "Notifications",
                UIUtils.getIconFromResources("emblems/warning.png"),
                () -> new JIPipeDesktopWorkbenchNotificationInboxUI(this),
                JIPipeDesktopTabPane.SingletonTabMode.Hidden);
        add(documentTabPane, BorderLayout.CENTER);

        initializeMenu();
        initializeStatusBar();
        sendStatusBarText("Welcome to JIPipe");
    }

    public void runUpdateCacheLater(JIPipeGraphNode algorithm) {
        Timer timer = algorithmUpdateTimers.getOrDefault(algorithm, null);
        if (timer == null) {
            timer = new Timer(250, e -> runUpdateCache(algorithm));
            timer.setRepeats(false);
            algorithmUpdateTimers.put(algorithm, timer);
        }
        timer.restart();
    }

    public void runCacheIntermediateResults(JIPipeGraphNode algorithm) {
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(getProject());
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(true);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        JIPipeDesktopQuickRun testBench = new JIPipeDesktopQuickRun(getProject(), algorithm, settings);
        JIPipeRunnableQueue.getInstance().enqueue(testBench);
    }

    public void runUpdateCache(JIPipeGraphNode algorithm) {
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(getProject());
        settings.setSilent(true);
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(false);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        JIPipeDesktopQuickRun testBench = new JIPipeDesktopQuickRun(getProject(), algorithm, settings);
        JIPipeRunnableQueue.getInstance().enqueue(testBench);
    }

    /**
     * Finds open {@link JIPipeDesktopPipelineGraphEditorUI} tabs
     *
     * @param compartment Targeted compartment
     * @return List of UIs
     */
    public List<JIPipeDesktopPipelineGraphEditorUI> findOpenPipelineEditorTabs(JIPipeProjectCompartment compartment) {
        List<JIPipeDesktopPipelineGraphEditorUI> result = new ArrayList<>();
        for (JIPipeDesktopTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if (tab.getContent() instanceof JIPipeDesktopPipelineGraphEditorUI) {
                if (Objects.equals(((JIPipeDesktopPipelineGraphEditorUI) tab.getContent()).getCompartment(), compartment.getProjectCompartmentUUID()))
                    result.add((JIPipeDesktopPipelineGraphEditorUI) tab.getContent());
            }
        }
        return result;
    }

    /**
     * Finds open {@link JIPipeDesktopPipelineGraphEditorUI} tabs
     *
     * @param compartmentUUID Targeted compartment
     * @return List of UIs
     */
    public List<JIPipeDesktopPipelineGraphEditorUI> findOpenPipelineEditorTabs(UUID compartmentUUID) {
        List<JIPipeDesktopPipelineGraphEditorUI> result = new ArrayList<>();
        for (JIPipeDesktopTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if (tab.getContent() instanceof JIPipeDesktopPipelineGraphEditorUI) {
                if (Objects.equals(((JIPipeDesktopPipelineGraphEditorUI) tab.getContent()).getCompartment(), compartmentUUID))
                    result.add((JIPipeDesktopPipelineGraphEditorUI) tab.getContent());
            }
        }
        return result;
    }


    /**
     * Opens the graph editor for specified compartment
     *
     * @param compartment The compartment
     * @param switchToTab If true, switch to the tab
     * @return the tab. its content is a {@link JIPipeDesktopPipelineGraphEditorUI}
     */
    public JIPipeDesktopTabPane.DocumentTab getOrOpenPipelineEditorTab(JIPipeProjectCompartment compartment, boolean switchToTab) {
        List<JIPipeDesktopPipelineGraphEditorUI> compartmentUIs = findOpenPipelineEditorTabs(compartment);
        if (compartmentUIs.isEmpty()) {
            JIPipeDesktopPipelineGraphEditorUI compartmentUI = new JIPipeDesktopPipelineGraphEditorUI(this, compartment.getRuntimeProject().getGraph(), compartment.getProjectCompartmentUUID());
            JIPipeDesktopTabPane.DocumentTab documentTab = documentTabPane.addTab(compartment.getName(),
                    UIUtils.getIconFromResources("actions/graph-compartment.png"),
                    compartmentUI,
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                    false);
            compartment.getParameterChangedEventEmitter().subscribeLambda((emitter, event) -> {
                if (event.getKey().equals("jipipe:node:name")) {
                    documentTab.setTitle(compartment.getName());
                    documentTab.emitParameterChangedEvent("title");
                }
            });
            project.getCompartmentRemovedEventEmitter().subscribeLambda((emitter, event) -> {
                if (event.getCompartment() == compartment) {
                    documentTabPane.closeTab(documentTab);
                }
            });
            if (switchToTab)
                documentTabPane.switchToLastTab();
            return documentTab;
        } else if (switchToTab) {
            JIPipeDesktopTabPane.DocumentTab tab = documentTabPane.getTabContainingContent(compartmentUIs.get(0));
            documentTabPane.switchToContent(compartmentUIs.get(0));
            return tab;
        }
        return null;
    }

    private void initializeStatusBar() {
        JXStatusBar statusBar = new JXStatusBar();
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        statusText = new JLabel("Ready ...");
        statusBar.add(statusText);
        statusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));

        // Thumbnail generation control
        JIPipeDesktopRunnableQueueButton thumbnailQueueButton = new JIPipeDesktopRunnableQueueButton(this, JIPipeThumbnailGenerationQueue.getInstance().getRunnerQueue());
        thumbnailQueueButton.makeFlat();
        thumbnailQueueButton.setReadyLabel("Thumbnails");
        thumbnailQueueButton.setTasksFinishedLabel("Thumbnails");
        thumbnailQueueButton.setTaskSingleRunningLabel("Generating thumbnail");
        thumbnailQueueButton.setTaskSingleEnqueuedRunningLabel("Generating thumbnails (%d)");
        statusBar.add(thumbnailQueueButton);

        // Memory control
        JButton optionsButton = memoryOptionsControl.createOptionsButton();
        UIUtils.makeButtonFlat(optionsButton);
        statusBar.add(optionsButton);
        statusBar.add(Box.createHorizontalStrut(4));
        statusBar.add(new JIPipeDesktopMemoryStatusUI());

        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Sends a text to the status bar
     *
     * @param text The text
     */
    public void sendStatusBarText(String text) {
        LocalDateTime localDateTime = LocalDateTime.now();
        statusText.setText(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + text);
    }

    private void initializeMenu() {
        JMenuBar menu = new JMenuBar();
        menu.setBorderPainted(false);

        JMenu projectMenu = new JMenu("Project");

        // Add "New project" toolbar entry
        JMenuItem newProjectButton = new JMenuItem("New", UIUtils.getIconFromResources("actions/document-new.png"));
        newProjectButton.setToolTipText("Creates a new project");
        newProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newProjectButton.addActionListener(e -> window.newProject());
        projectMenu.add(newProjectButton);

        JMenuItem newProjectFromTemplateButton = new JMenuItem("New from template", UIUtils.getIconFromResources("actions/dialog-templates.png"));
        newProjectFromTemplateButton.setToolTipText("Creates a new project from a template");
        newProjectFromTemplateButton.addActionListener(e -> window.newProjectFromTemplate());
        projectMenu.add(newProjectFromTemplateButton);

        UIUtils.installMenuExtension(this, projectMenu, JIPipeMenuExtensionTarget.ProjectMainMenu, true);
        projectMenu.addSeparator();

        // "Open project" entry
        JMenuItem openProjectButton = new JMenuItem("Open ...", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openProjectButton.setToolTipText("Opens a project from a parameter file");
        openProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openProjectButton.addActionListener(e -> window.openProject());
        projectMenu.add(openProjectButton);

        // "Open output" entry
        JMenuItem openProjectOutputButton = new JMenuItem("Open analysis output ...", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openProjectOutputButton.setToolTipText("<html>Opens a project and its analysis output from an output folder.<br/>" +
                "<i>Note: The output folder must contain a project.jip file</i></html>");
        openProjectOutputButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        openProjectOutputButton.addActionListener(e -> window.openProjectAndOutput());
        projectMenu.add(openProjectOutputButton);

        // Recent projects entry
        projectMenu.add(new JIPipeDesktopRecentProjectsMenu("Recent projects", UIUtils.getIconFromResources("actions/clock.png"), (JIPipeDesktopProjectWindow) getWindow()));

        JMenuItem restoreMenuItem = new JMenuItem("Restore/manage backups ...", UIUtils.getIconFromResources("actions/reload.png"));
        restoreMenuItem.setToolTipText("Restores an automatically created backup and manages the collection of backups");
        restoreMenuItem.addActionListener(e -> JIPipeDesktopBackupManagerPanel.openNewWindow(this));
        projectMenu.add(restoreMenuItem);

        projectMenu.addSeparator();

        // "Save project" entry
        JMenuItem saveProjectButton = new JMenuItem("Save ...", UIUtils.getIconFromResources("actions/filesave.png"));
        saveProjectButton.setToolTipText("Saves the project. If the project was opened from a file or previously saved, the file will be overwritten.");
        saveProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveProjectButton.addActionListener(e -> {
            window.saveProjectAs(true, true);
            if (JIPipeGeneralUIApplicationSettings.getInstance().isValidateOnSave()) {
                validateProject(true);
            }
        });
        projectMenu.add(saveProjectButton);

        // "Save project" entry
        JMenuItem saveProjectAsButton = new JMenuItem("Save as ...", UIUtils.getIconFromResources("actions/filesave.png"));
        saveProjectAsButton.setToolTipText("Saves the project to a new file.");
        saveProjectAsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        saveProjectAsButton.addActionListener(e -> {
            window.saveProjectAs(false, true);
            if (JIPipeGeneralUIApplicationSettings.getInstance().isValidateOnSave()) {
                validateProject(true);
            }
        });
        projectMenu.add(saveProjectAsButton);

        // "Save project" entry
        JMenuItem saveProjectCopyAsButton = new JMenuItem("Save copy as ...", UIUtils.getIconFromResources("actions/filesave.png"));
        saveProjectCopyAsButton.setToolTipText("Saves the project to a new file without changing the path of the current project");
        saveProjectCopyAsButton.addActionListener(e -> {
            window.saveProjectAs(false, false);
            if (JIPipeGeneralUIApplicationSettings.getInstance().isValidateOnSave()) {
                validateProject(true);
            }
        });
        projectMenu.add(saveProjectCopyAsButton);

        // "Save project" entry
        JMenuItem saveProjectAndCache = new JMenuItem("Save project and cache ...", UIUtils.getIconFromResources("actions/document-export.png"));
        saveProjectAndCache.setToolTipText("Saves the project and all current cached data into a folder.");
        saveProjectAndCache.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        saveProjectAndCache.addActionListener(e -> {
            saveProjectAndCache("Save project and cache", true);
        });
        projectMenu.add(saveProjectAndCache);

        // "Restore cache" entry
        JMenuItem restoreCache = new JMenuItem("Restore cache ...", UIUtils.getIconFromResources("actions/document-import.png"));
        restoreCache.setToolTipText("Restores the cache of the current project from a directory or ZIP file.");
        restoreCache.addActionListener(e -> {
            restoreCacheFromZIPOrDirectory();
        });
        projectMenu.add(restoreCache);

        JMenuItem archiveProjectButton = new JMenuItem("Archive project ...", UIUtils.getIconFromResources("actions/archive.png"));
        archiveProjectButton.setToolTipText("Copies the project and all data into a ZIP file or directory");
        archiveProjectButton.addActionListener(e -> {
            archiveProject();
        });
        projectMenu.add(archiveProjectButton);

        projectMenu.addSeparator();

        JMenuItem editCompartmentsButton = new JMenuItem("Edit compartments", UIUtils.getIconFromResources("actions/graph-compartments.png"));
        editCompartmentsButton.setToolTipText("Opens an editor that allows to add more compartments and edit connections");
//        editCompartmentsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        editCompartmentsButton.addActionListener(e -> openCompartmentEditor());
        projectMenu.add(editCompartmentsButton);

        JMenuItem openApplicationSettingsButton = new JMenuItem("Application settings", UIUtils.getIconFromResources("apps/jipipe.png"));
        openApplicationSettingsButton.setToolTipText("Opens the application settings");
        openApplicationSettingsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        openApplicationSettingsButton.addActionListener(e -> openApplicationSettings(null));
        projectMenu.add(openApplicationSettingsButton);

        projectMenu.addSeparator();

        JMenuItem projectInfo = new JMenuItem("Project settings/overview", UIUtils.getIconFromResources("actions/wrench.png"));
        projectInfo.setToolTipText("Opens the project overview");
        projectInfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        projectInfo.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW));
        projectMenu.add(projectInfo);

        JMenuItem projectReport = new JMenuItem("Project report", UIUtils.getIconFromResources("actions/document-preview.png"));
        projectReport.setToolTipText("Opens the project report");
        projectReport.addActionListener(e -> openProjectReport());
        projectMenu.add(projectReport);

        JMenuItem openProjectFolderItem = new JMenuItem("Open project folder", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openProjectFolderItem.setToolTipText("Opens the folder that contains the project file");
        openProjectFolderItem.addActionListener(e -> openProjectFolder());
        projectMenu.add(openProjectFolderItem);

        JMenuItem validateProjectItem = new JMenuItem("Validate project", UIUtils.getIconFromResources("actions/checkmark.png"));
        validateProjectItem.setToolTipText("Checks if the project and the parameters are valid");
        validateProjectItem.addActionListener(e -> validateProject(false));
        projectMenu.add(validateProjectItem);

        projectMenu.addSeparator();

        JMenuItem exitButton = new JMenuItem("Exit", UIUtils.getIconFromResources("actions/exit.png"));
        exitButton.addActionListener(e -> getWindow().dispatchEvent(new WindowEvent(getWindow(), WindowEvent.WINDOW_CLOSING)));
        projectMenu.add(exitButton);

        menu.add(projectMenu);

        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");

        JMenuItem openCacheBrowserButton = new JMenuItem("Cache browser", UIUtils.getIconFromResources("actions/database.png"));
        openCacheBrowserButton.addActionListener(e -> openCacheBrowser());
        toolsMenu.add(openCacheBrowserButton);

        JMenuItem openLogsButton = new JMenuItem("Logs", UIUtils.getIconFromResources("actions/show_log.png"));
        openLogsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_LOG));
        toolsMenu.add(openLogsButton);

        JMenuItem openNotificationsButton = new JMenuItem("Notifications", UIUtils.getIconFromResources("actions/preferences-desktop-notification.png"));
        openNotificationsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_NOTIFICATIONS));
        toolsMenu.add(openNotificationsButton);

        UIUtils.installMenuExtension(this, toolsMenu, JIPipeMenuExtensionTarget.ProjectToolsMenu, false);
        if (toolsMenu.getItemCount() > 0)
            menu.add(toolsMenu);

        menu.add(Box.createHorizontalGlue());

        // Overview link
        JButton openProjectOverviewButton = new JButton("Info & Settings", UIUtils.getIconFromResources("actions/configure.png"));
        openProjectOverviewButton.setToolTipText("Opens the project info & settings tab or jumps to the existing tab if it is already open.");
        openProjectOverviewButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW));
        UIUtils.setStandardButtonBorder(openProjectOverviewButton);
        menu.add(openProjectOverviewButton);

        // Compartments link
        JButton openCompartmentsButton = new JButton("Compartments", UIUtils.getIconFromResources("actions/graph-compartments.png"));
        openCompartmentsButton.setToolTipText("Opens the compartment editor if it was closed or switches to the existing tab if it is currently open.");
        openCompartmentsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_COMPARTMENT_EDITOR));
        UIUtils.setStandardButtonBorder(openCompartmentsButton);
        menu.add(openCompartmentsButton);

        // Cache monitor
        JIPipeDesktopCacheManagerUI cacheManagerUI = new JIPipeDesktopCacheManagerUI(this);
        UIUtils.setStandardButtonBorder(cacheManagerUI);
        menu.add(cacheManagerUI);

        // Queue monitor
        menu.add(new JIPipeDesktopRunnableQueueButton(this));

        // Logs button
        menu.add(new JIPipeDesktopRunnableLogsButton(this));

        // "Run" entry
        JButton runProjectButton = new JButton("Run", UIUtils.getIconFromResources("actions/play.png"));
        runProjectButton.setToolTipText("Runs the whole pipeline");
        UIUtils.setStandardButtonBorder(runProjectButton);

        runProjectButton.addActionListener(e -> runWholeProject());
        menu.add(runProjectButton);

        // Plugins/artifacts management
        menu.add(new JIPipeDesktopManagePluginsButton(this));

        // Notification panel
        menu.add(notificationButton);

        // "Help" entry
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setIcon(UIUtils.getIconFromResources("actions/help.png"));

        JMenuItem offlineManual = new JMenuItem("Manual", UIUtils.getIconFromResources("actions/help.png"));
        offlineManual.setToolTipText("Opens the online manual in a browser.");
        offlineManual.addActionListener(e -> openManual());
        helpMenu.add(offlineManual);

        JMenuItem quickHelp = new JMenuItem("Getting started", UIUtils.getIconFromResources("actions/help-info.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_INTRODUCTION));
        helpMenu.add(quickHelp);

        JMenuItem projectInfo2 = new JMenuItem("Project overview", UIUtils.getIconFromResources("actions/help-info.png"));
        projectInfo2.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW));
        helpMenu.add(projectInfo2);

        JMenuItem algorithmCompendiumButton = new JMenuItem("Open node documentation", UIUtils.getIconFromResources("data-types/node.png"));
        algorithmCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Node documentation",
                    UIUtils.getIconFromResources("actions/help.png"),
                    new JIPipeDesktopAlgorithmCompendiumUI(),
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDocumentTabPane().switchToLastTab();
        });
        helpMenu.add(algorithmCompendiumButton);

        JMenuItem datatypeCompendiumButton = new JMenuItem("Open data type documentation", UIUtils.getIconFromResources("data-types/data-type.png"));
        datatypeCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Data type documentation",
                    UIUtils.getIconFromResources("actions/help.png"),
                    new JIPipeDataTypeCompendiumUI(),
                    JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDocumentTabPane().switchToLastTab();
        });
        helpMenu.add(datatypeCompendiumButton);

        JMenuItem license = new JMenuItem("License", UIUtils.getIconFromResources("actions/license.png"));
        license.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_LICENSE));
        helpMenu.add(license);

        menu.add(helpMenu);

        UIUtils.installMenuExtension(this, helpMenu, JIPipeMenuExtensionTarget.ProjectHelpMenu, true);

        add(menu, BorderLayout.NORTH);
    }

    public void openApplicationSettings(String navigateToCategory) {
        JDialog dialog = new JDialog(getWindow());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setTitle("JIPipe - Application settings");
        dialog.setModal(true);
        dialog.setIconImage(UIUtils.getJIPipeIcon128());
        JIPipeDesktopApplicationSettingsUI applicationSettingsUI = new JIPipeDesktopApplicationSettingsUI(this);
        if (navigateToCategory != null) {
            applicationSettingsUI.selectNode(navigateToCategory);
        }
        UIUtils.addEscapeListener(dialog);
        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
        contentPanel.add(applicationSettingsUI, BorderLayout.CENTER);

        AtomicBoolean saved = new AtomicBoolean(false);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if (!saved.get()) {
                    JIPipe.getSettings().reload();
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton resetButton = new JButton("Reset", UIUtils.getIconFromResources("actions/edit-reset.png"));
        resetButton.addActionListener(e -> {
            JIPipe.getSettings().reload();
            applicationSettingsUI.selectNode("/General");
        });
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createHorizontalGlue());
        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            JIPipe.getSettings().reload();
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);
        JButton saveButton = new JButton("Save", UIUtils.getIconFromResources("actions/filesave.png"));
        saveButton.addActionListener(e -> {
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
            saved.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(saveButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setSize(1280, 768);
        dialog.setLocationRelativeTo(getWindow());
        dialog.setVisible(true);
    }

    public void openProjectReport() {
        JIPipeDesktopProjectReportUI reportUI = new JIPipeDesktopProjectReportUI(this);
        documentTabPane.addTab("Project report",
                UIUtils.getIconFromResources("actions/document-preview.png"),
                reportUI,
                JIPipeDesktopTabPane.CloseMode.withSilentCloseButton);
        documentTabPane.switchToLastTab();
    }

    private void openManual() {
        try {
            Desktop.getDesktop().browse(URI.create("https://www.jipipe.org/"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void archiveProject() {
        if (getProject().getWorkDirectory() == null) {
            JOptionPane.showMessageDialog(this, "Please save the project once before using the archive function.", "Archive project", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int option = JOptionPane.showOptionDialog(this,
                "You can archive the project as directory or ZIP file. Please choose the most convenient option.\nPlease note that the archiving function cannot currently handle projects that involve advanced path processing.",
                "Archive project",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"ZIP file", "Directory", "Cancel"},
                "ZIP file");
        switch (option) {
            case JOptionPane.YES_OPTION:
                archiveProjectAsZIP();
                break;
            case JOptionPane.NO_OPTION:
                archiveProjectAsDirectory();
                break;
        }
    }

    private void archiveProjectAsDirectory() {
        Path directory = JIPipeFileChooserApplicationSettings.saveDirectory(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Archive project as directory");
        if (directory != null) {
            JIPipeDesktopRunExecuteUI.runInDialog(this, this, new JIPipeArchiveProjectToDirectoryRun(getProject(), directory));
        }
    }

    private void archiveProjectAsZIP() {
        Path file = JIPipeFileChooserApplicationSettings.saveFile(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Archive project as ZIP", UIUtils.EXTENSION_FILTER_ZIP);
        if (file != null) {
            JIPipeDesktopRunExecuteUI.runInDialog(this, this, new JIPipeArchiveProjectToZIPRun(getProject(), file));
        }
    }

    public void saveProjectAndCache(String title, boolean addAsRecentProject) {
        int option = JOptionPane.showOptionDialog(this,
                "You can save cached data as directory or ZIP file. Please choose the most convenient option.",
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"ZIP file", "Directory", "Cancel"},
                "ZIP file");
        switch (option) {
            case JOptionPane.YES_OPTION:
                saveProjectAndCacheToZIP(title);
                break;
            case JOptionPane.NO_OPTION:
                saveProjectAndCacheToDirectory(title, addAsRecentProject);
                break;
        }
    }

    public void saveProjectAndCacheToDirectory(String title, boolean addAsRecentProject) {
        window.saveProjectAndCacheToDirectory(title, addAsRecentProject);
    }

    public void saveProjectAndCacheToZIP(String title) {
        window.saveProjectAndCacheToZIP(title);
    }


    public void restoreCacheFromZIPOrDirectory() {
        Path path = JIPipeFileChooserApplicationSettings.openPath(this, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Select exported cache (ZIP/directory)");
        if (path != null) {
            if (Files.isRegularFile(path)) {
                // Load into cache with a run
                JIPipeDesktopRunExecuteUI.runInDialog(this, this, new JIPipeDesktopLoadResultZipIntoCacheRun(this, project, path, true));
            } else {
                // Load into cache with a run
                JIPipeDesktopRunExecuteUI.runInDialog(this, this, new JIPipeDesktopLoadResultDirectoryIntoCacheRun(this, project, path, true));
            }
        }
    }

    private void openProjectFolder() {
        if (getProject().getWorkDirectory() == null || !Files.isDirectory(getProject().getWorkDirectory())) {
            JOptionPane.showMessageDialog(this, "This project does not have a folder. Please save it first.",
                    "Open project folder", JOptionPane.ERROR_MESSAGE);
            return;
        }
        UIUtils.openFileInNative(getProject().getWorkDirectory());
    }

    public void openCacheBrowser() {
        JIPipeDesktopCacheBrowserUI cacheTable = new JIPipeDesktopCacheBrowserUI(this);
        getDocumentTabPane().addTab("Cache browser",
                UIUtils.getIconFromResources("actions/database.png"),
                cacheTable,
                JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                true);
        getDocumentTabPane().switchToLastTab();
    }

    /**
     * Validates the project
     *
     * @param avoidSwitching Do not switch to the validity checker tab if the project is OK
     */
    public void validateProject(boolean avoidSwitching) {
        validityCheckerPanel.recheckValidity();
        if (!avoidSwitching || !validityCheckerPanel.getReport().isValid())
            documentTabPane.selectSingletonTab(TAB_VALIDITY_CHECK);
    }

    /**
     * Validates the plugins
     *
     * @param avoidSwitching Do not switch to the validity checker tab if the plugins are OK
     */
    public void validatePlugins(boolean avoidSwitching) {
        pluginValidityCheckerPanel.recheckValidity();
        if (!avoidSwitching || !pluginValidityCheckerPanel.getReport().isValid())
            documentTabPane.selectSingletonTab(TAB_PLUGIN_VALIDITY_CHECK);
    }

    private void openCompartmentEditor() {
        documentTabPane.selectSingletonTab("COMPARTMENT_EDITOR");
    }

    private void newCompartmentAfterCurrent() {
        if (documentTabPane.getCurrentContent() instanceof JIPipeDesktopPipelineGraphEditorUI) {
            JIPipeDesktopPipelineGraphEditorUI ui = (JIPipeDesktopPipelineGraphEditorUI) documentTabPane.getCurrentContent();
            String compartmentName = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the compartment",
                    "Compartment", s -> project.getCompartments().containsKey(s));
            if (compartmentName != null && !compartmentName.trim().isEmpty()) {
                JIPipeProjectCompartment compartment = project.addCompartment(compartmentName);
                project.connectCompartments(project.getCompartments().get(ui.getCompartment()), compartment);
                getOrOpenPipelineEditorTab(compartment, true);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please switch to a graph compartment editor.", "New compartment after current", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void runWholeProject() {
        JIPipeDesktopRunSettingsUI ui = new JIPipeDesktopRunSettingsUI(this);
        documentTabPane.addTab("Run", UIUtils.getIconFromResources("actions/run-build.png"), ui,
                JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton, true);
        documentTabPane.switchToLastTab();
    }

    /**
     * @return The tab pane
     */
    public JIPipeDesktopTabPane getDocumentTabPane() {
        return documentTabPane;
    }

    /**
     * @return The project
     */
    @Override
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * @return The parent window
     */
    public Window getWindow() {
        return window;
    }

    public JIPipeDesktopProjectWindow getProjectWindow() {
        return window;
    }

    /**
     * @return SciJava context
     */
    @Override
    public Context getContext() {
        return context;
    }

    public boolean isProjectModified() {
        return projectModified;
    }

    public void setProjectModified(boolean projectModified) {
        if (this.projectModified != projectModified) {
            this.projectModified = projectModified;
            window.updateTitle();
        }
    }

    @Override
    public JIPipeNotificationInbox getNotificationInbox() {
        return notificationInbox;
    }

    @Override
    public void showMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(getWindow(), message, title, JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(getWindow(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void unload() {
        project.getCache().clearAll(new JIPipeProgressInfo());
    }

    /**
     * Triggered when a compartment is deleted.
     * Closes corresponding tabs.
     *
     * @param event Generated event
     */
    @Override
    public void onProjectCompartmentRemoved(JIPipeProject.CompartmentRemovedEvent event) {
        for (JIPipeDesktopPipelineGraphEditorUI compartmentUI : findOpenPipelineEditorTabs(event.getCompartmentUUID())) {
            documentTabPane.remove(compartmentUI);
        }
    }

    @Override
    public void onJIPipePluginRegistered(JIPipeService.ExtensionRegisteredEvent event) {
        sendStatusBarText("Registered extension: '" + event.getExtension().getMetadata().getName() + "' with id '" + event.getExtension().getDependencyId() + "'. We recommend to restart ImageJ.");
    }

}
