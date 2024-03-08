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

package org.hkijena.jipipe.ui;

import net.imagej.ui.swing.updater.ImageJUpdater;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailGenerationQueue;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.backups.BackupManagerPanel;
import org.hkijena.jipipe.ui.cache.JIPipeCacheBrowserUI;
import org.hkijena.jipipe.ui.cache.JIPipeCacheManagerUI;
import org.hkijena.jipipe.ui.components.MemoryStatusUI;
import org.hkijena.jipipe.ui.components.RecentProjectsMenu;
import org.hkijena.jipipe.ui.components.ReloadableValidityChecker;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.data.MemoryOptionsControl;
import org.hkijena.jipipe.ui.documentation.JIPipeAlgorithmCompendiumUI;
import org.hkijena.jipipe.ui.documentation.JIPipeDataTypeCompendiumUI;
import org.hkijena.jipipe.ui.documentation.WelcomePanel;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExporter;
import org.hkijena.jipipe.ui.extensions.JIPipeModernPluginManagerUI;
import org.hkijena.jipipe.ui.extensions.JIPipePluginValidityCheckerPanel;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.compartments.JIPipeCompartmentsGraphEditorUI;
import org.hkijena.jipipe.ui.ijupdater.JIPipeImageJPluginManager;
import org.hkijena.jipipe.ui.notifications.NotificationButton;
import org.hkijena.jipipe.ui.notifications.WorkbenchNotificationInboxUI;
import org.hkijena.jipipe.ui.project.JIPipeProjectTabMetadata;
import org.hkijena.jipipe.ui.project.LoadResultDirectoryIntoCacheRun;
import org.hkijena.jipipe.ui.project.LoadResultZipIntoCacheRun;
import org.hkijena.jipipe.ui.quickrun.QuickRun;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.*;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.ui.settings.JIPipeProjectOverviewUI;
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
public class JIPipeProjectWorkbench extends JPanel implements JIPipeWorkbench, JIPipeProject.CompartmentRemovedEventListener, JIPipeService.ExtensionRegisteredEventListener {

    public static final String TAB_INTRODUCTION = "INTRODUCTION";
    public static final String TAB_LICENSE = "LICENSE";
    public static final String TAB_COMPARTMENT_EDITOR = "COMPARTMENT_EDITOR";
    public static final String TAB_PLUGIN_MANAGER = "PLUGIN_MANAGER";
    public static final String TAB_VALIDITY_CHECK = "VALIDITY_CHECK";
    public static final String TAB_PLUGIN_VALIDITY_CHECK = "PLUGIN_VALIDITY_CHECK";
    public static final String TAB_NOTIFICATIONS = "NOTIFICATIONS";
    public static final String TAB_PROJECT_OVERVIEW = "PROJECT_OVERVIEW";
    public static final String TAB_LOG = "LOG";
    private final JIPipeProjectWindow window;
    private final JIPipeProject project;
    private final Context context;
    private final MemoryOptionsControl memoryOptionsControl;
    private final JIPipeNotificationInbox notificationInbox = new JIPipeNotificationInbox();
    private final NotificationButton notificationButton = new NotificationButton(this);
    private final Map<JIPipeGraphNode, Timer> algorithmUpdateTimers = new WeakHashMap<>();
    public DocumentTabPane documentTabPane;
    private JLabel statusText;
    private ReloadableValidityChecker validityCheckerPanel;
    private JIPipePluginValidityCheckerPanel pluginValidityCheckerPanel;
    private boolean projectModified;
    private final JIPipeNodeDatabase nodeDatabase;

    /**
     * @param window           Parent window
     * @param context          SciJava context
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     */
    public JIPipeProjectWorkbench(JIPipeProjectWindow window, Context context, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        this.window = window;
        this.project = project;
        this.context = context;
        this.memoryOptionsControl = new MemoryOptionsControl(this);
        this.nodeDatabase = new JIPipeNodeDatabase(project);
        initialize(showIntroduction, isNewProject);
        project.getCompartmentRemovedEventEmitter().subscribe(this);
        JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeWeak(this);

        validatePlugins(true);

        restoreStandardTabs(showIntroduction, isNewProject);
        if (ProjectsSettings.getInstance().isRestoreTabs())
            restoreTabs();
        if (GeneralUISettings.getInstance().isShowIntroduction() && showIntroduction)
            documentTabPane.selectSingletonTab(TAB_INTRODUCTION);

        // Register modification state watchers
        project.getGraph().getGraphChangedEventEmitter().subscribeLambda((emitter, event) -> setProjectModified(true));
        project.getCompartmentGraph().getGraphChangedEventEmitter().subscribeLambda((emitter, event) -> setProjectModified(true));

        // Install the run notifier
        JIPipeRunQueueNotifier.install();
    }

    public JIPipeNodeDatabase getNodeDatabase() {
        return nodeDatabase;
    }

    public static boolean canAddOrDeleteNodes(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeProjectWorkbench) {
            JIPipeProject project = ((JIPipeProjectWorkbench) workbench).getProject();
            if (project.getMetadata().getPermissions().isPreventAddingDeletingNodes()) {
                JOptionPane.showMessageDialog(workbench.getWindow(), "Deleting nodes & compartments is disabled for this project. " +
                        "\n\nIf this is not intentional, change this setting in Project > Project settings > Prevent adding/deleting nodes");
                return false;
            }
        }
        return true;
    }

    public static boolean canModifySlots(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeProjectWorkbench) {
            JIPipeProject project = ((JIPipeProjectWorkbench) workbench).getProject();
            if (project.getMetadata().getPermissions().isPreventModifyingSlots()) {
                JOptionPane.showMessageDialog(workbench.getWindow(), "Modifying slots is disabled for this project. " +
                        "\n\nIf this is not intentional, change this setting in Project > Project settings > Prevent modifying slots");
                return false;
            }
        }
        return true;
    }

    public void restoreStandardTabs(boolean showIntroduction, boolean isNewProject) {
        if (GeneralUISettings.getInstance().isShowIntroduction() && showIntroduction)
            documentTabPane.selectSingletonTab(TAB_INTRODUCTION);
        else {
            if (GeneralUISettings.getInstance().isShowProjectInfo() && !isNewProject) {
                documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW);
            } else {
                documentTabPane.selectSingletonTab(TAB_COMPARTMENT_EDITOR);
            }
        }
    }

    private void restoreTabs() {
        if (project.getMetadata().isRestoreTabs()) {
            try {
                Object metadata = project.getAdditionalMetadata().getOrDefault(JIPipeProjectTabMetadata.METADATA_KEY, null);
                if (metadata instanceof JIPipeProjectTabMetadata) {
                    ((JIPipeProjectTabMetadata) metadata).restore(this);
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
        JIPipeRunnableLogsCollection.getInstance();

        documentTabPane = new DocumentTabPane(true, DocumentTabPane.TabPlacement.Top);
        documentTabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        documentTabPane.registerSingletonTab(TAB_INTRODUCTION,
                "Getting started",
                UIUtils.getIconFromResources("actions/help-info.png"),
                () -> new WelcomePanel(this),
                (GeneralUISettings.getInstance().isShowIntroduction() && showIntroduction) ? DocumentTabPane.SingletonTabMode.Present : DocumentTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_PROJECT_OVERVIEW,
                "Project",
                UIUtils.getIconFromResources("actions/configure.png"),
                () -> new JIPipeProjectOverviewUI(this),
                (GeneralUISettings.getInstance().isShowProjectInfo() && !isNewProject) ? DocumentTabPane.SingletonTabMode.Present : DocumentTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_LICENSE,
                "License",
                UIUtils.getIconFromResources("actions/license.png"),
                () -> new MarkdownReader(true, MarkdownDocument.fromPluginResource("documentation/license.md", new HashMap<>())),
                DocumentTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_COMPARTMENT_EDITOR,
                "Compartments",
                UIUtils.getIconFromResources("actions/graph-compartments.png"),
                () -> new JIPipeCompartmentsGraphEditorUI(this),
                DocumentTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_PLUGIN_MANAGER,
                "Plugin manager",
                UIUtils.getIconFromResources("actions/plugins.png"),
                () -> new JIPipeModernPluginManagerUI(this),
                DocumentTabPane.SingletonTabMode.Hidden);
        validityCheckerPanel = new ReloadableValidityChecker(this, project);
        documentTabPane.registerSingletonTab(TAB_VALIDITY_CHECK,
                "Project validation",
                UIUtils.getIconFromResources("actions/checkmark.png"),
                () -> validityCheckerPanel,
                DocumentTabPane.SingletonTabMode.Hidden);
        pluginValidityCheckerPanel = new JIPipePluginValidityCheckerPanel(this);
        documentTabPane.registerSingletonTab(TAB_PLUGIN_VALIDITY_CHECK,
                "Plugin validation",
                UIUtils.getIconFromResources("actions/plugins.png"),
                () -> pluginValidityCheckerPanel,
                DocumentTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_LOG,
                "Log viewer",
                UIUtils.getIconFromResources("actions/show_log.png"),
                () -> new JIPipeLogViewer(this),
                DocumentTabPane.SingletonTabMode.Hidden);
        documentTabPane.registerSingletonTab(TAB_NOTIFICATIONS,
                "Notifications",
                UIUtils.getIconFromResources("emblems/warning.png"),
                () -> new WorkbenchNotificationInboxUI(this),
                DocumentTabPane.SingletonTabMode.Hidden);
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
        QuickRunSettings settings = new QuickRunSettings();
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(true);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        QuickRun testBench = new QuickRun(getProject(), algorithm, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    public void runUpdateCache(JIPipeGraphNode algorithm) {
        QuickRunSettings settings = new QuickRunSettings();
        settings.setSilent(true);
        settings.setLoadFromCache(true);
        settings.setStoreIntermediateResults(false);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        QuickRun testBench = new QuickRun(getProject(), algorithm, settings);
        JIPipeRunnerQueue.getInstance().enqueue(testBench);
    }

    /**
     * Finds open {@link JIPipePipelineGraphEditorUI} tabs
     *
     * @param compartment Targeted compartment
     * @return List of UIs
     */
    public List<JIPipePipelineGraphEditorUI> findOpenPipelineEditorTabs(JIPipeProjectCompartment compartment) {
        List<JIPipePipelineGraphEditorUI> result = new ArrayList<>();
        for (DocumentTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if (tab.getContent() instanceof JIPipePipelineGraphEditorUI) {
                if (Objects.equals(((JIPipePipelineGraphEditorUI) tab.getContent()).getCompartment(), compartment.getProjectCompartmentUUID()))
                    result.add((JIPipePipelineGraphEditorUI) tab.getContent());
            }
        }
        return result;
    }

    /**
     * Finds open {@link JIPipePipelineGraphEditorUI} tabs
     *
     * @param compartmentUUID Targeted compartment
     * @return List of UIs
     */
    public List<JIPipePipelineGraphEditorUI> findOpenPipelineEditorTabs(UUID compartmentUUID) {
        List<JIPipePipelineGraphEditorUI> result = new ArrayList<>();
        for (DocumentTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if (tab.getContent() instanceof JIPipePipelineGraphEditorUI) {
                if (Objects.equals(((JIPipePipelineGraphEditorUI) tab.getContent()).getCompartment(), compartmentUUID))
                    result.add((JIPipePipelineGraphEditorUI) tab.getContent());
            }
        }
        return result;
    }


    /**
     * Opens the graph editor for specified compartment
     *
     * @param compartment The compartment
     * @param switchToTab If true, switch to the tab
     * @return the tab. its content is a {@link JIPipePipelineGraphEditorUI}
     */
    public DocumentTabPane.DocumentTab getOrOpenPipelineEditorTab(JIPipeProjectCompartment compartment, boolean switchToTab) {
        List<JIPipePipelineGraphEditorUI> compartmentUIs = findOpenPipelineEditorTabs(compartment);
        if (compartmentUIs.isEmpty()) {
            JIPipePipelineGraphEditorUI compartmentUI = new JIPipePipelineGraphEditorUI(this, compartment.getRuntimeProject().getGraph(), compartment.getProjectCompartmentUUID());
            DocumentTabPane.DocumentTab documentTab = documentTabPane.addTab(compartment.getName(),
                    UIUtils.getIconFromResources("actions/graph-compartment.png"),
                    compartmentUI,
                    DocumentTabPane.CloseMode.withSilentCloseButton,
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
            DocumentTabPane.DocumentTab tab = documentTabPane.getTabContainingContent(compartmentUIs.get(0));
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
        JIPipeRunnerQueueButton thumbnailQueueButton = new JIPipeRunnerQueueButton(this, JIPipeThumbnailGenerationQueue.getInstance().getRunnerQueue());
        thumbnailQueueButton.makeFlat();
        thumbnailQueueButton.setReadyLabel("");
        thumbnailQueueButton.setTasksFinishedLabel("");
        thumbnailQueueButton.setTaskSingleRunningLabel("Generating thumbnail");
        thumbnailQueueButton.setTaskSingleEnqueuedRunningLabel("Generating thumbnails (%d)");
        statusBar.add(thumbnailQueueButton);

        // Memory control
        JButton optionsButton = memoryOptionsControl.createOptionsButton();
        UIUtils.makeFlat(optionsButton);
        statusBar.add(optionsButton);
        statusBar.add(Box.createHorizontalStrut(4));
        statusBar.add(new MemoryStatusUI());

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
        projectMenu.add(new RecentProjectsMenu("Recent projects", UIUtils.getIconFromResources("actions/clock.png"), (JIPipeProjectWindow) getWindow()));

        JMenuItem restoreMenuItem = new JMenuItem("Restore/manage backups ...", UIUtils.getIconFromResources("actions/reload.png"));
        restoreMenuItem.setToolTipText("Restores an automatically created backup and manages the collection of backups");
        restoreMenuItem.addActionListener(e -> BackupManagerPanel.openNewWindow(this));
        projectMenu.add(restoreMenuItem);

        projectMenu.addSeparator();

        // "Save project" entry
        JMenuItem saveProjectButton = new JMenuItem("Save ...", UIUtils.getIconFromResources("actions/save.png"));
        saveProjectButton.setToolTipText("Saves the project. If the project was opened from a file or previously saved, the file will be overwritten.");
        saveProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveProjectButton.addActionListener(e -> {
            window.saveProjectAs(true);
            if (GeneralUISettings.getInstance().isValidateOnSave()) {
                validateProject(true);
            }
        });
        projectMenu.add(saveProjectButton);

        // "Save project" entry
        JMenuItem saveProjectAsButton = new JMenuItem("Save as ...", UIUtils.getIconFromResources("actions/save.png"));
        saveProjectAsButton.setToolTipText("Saves the project to a new file.");
        saveProjectAsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        saveProjectAsButton.addActionListener(e -> {
            window.saveProjectAs(false);
            if (GeneralUISettings.getInstance().isValidateOnSave()) {
                validateProject(true);
            }
        });
        projectMenu.add(saveProjectAsButton);

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

        // "Export as algorithm" entry
        JMenuItem exportProjectAsAlgorithmButton = new JMenuItem("Export as custom algorithm", UIUtils.getIconFromResources("actions/document-export.png"));
        exportProjectAsAlgorithmButton.setToolTipText("Exports the whole pipeline (all compartments) as custom algorithm");
        exportProjectAsAlgorithmButton.addActionListener(e -> exportProjectAsAlgorithm());
        projectMenu.add(exportProjectAsAlgorithmButton);

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

        // Plugins menu
        JMenu pluginsMenu = new JMenu("Plugins");

        JMenuItem newPluginButton = new JMenuItem("New JSON extension ...", UIUtils.getIconFromResources("actions/document-new.png"));
        newPluginButton.setToolTipText("Opens the extension builder");
        newPluginButton.addActionListener(e -> {
            JIPipeJsonExtensionWindow window = JIPipeJsonExtensionWindow.newWindow(context, new JIPipeJsonPlugin(), true);
            window.setTitle("New extension");
        });
        pluginsMenu.add(newPluginButton);

        JMenuItem installPluginButton = new JMenuItem("Install ...", UIUtils.getIconFromResources("actions/run-install.png"));
        installPluginButton.addActionListener(e -> JIPipeJsonExtensionWindow.installExtensions(this));
        pluginsMenu.add(installPluginButton);

        JMenuItem managePluginsButton = new JMenuItem("Manage JIPipe plugins", UIUtils.getIconFromResources("apps/jipipe.png"));
        managePluginsButton.addActionListener(e -> managePlugins());
        pluginsMenu.add(managePluginsButton);

        JMenu pluginsImageJMenu = new JMenu("ImageJ");

        JMenuItem manageImageJPlugins = new JMenuItem("Manage ImageJ plugins (via JIPipe)", UIUtils.getIconFromResources("apps/imagej.png"));
        manageImageJPlugins.addActionListener(e -> manageImageJPlugins(true));
        pluginsImageJMenu.add(manageImageJPlugins);

        JMenuItem manageImageJPluginsViaUpdater = new JMenuItem("Run ImageJ updater", UIUtils.getIconFromResources("apps/imagej.png"));
        manageImageJPluginsViaUpdater.addActionListener(e -> manageImageJPlugins(false));
        pluginsImageJMenu.add(manageImageJPluginsViaUpdater);

        pluginsMenu.add(pluginsImageJMenu);

        UIUtils.installMenuExtension(this, pluginsMenu, JIPipeMenuExtensionTarget.ProjectPluginsMenu, true);

        menu.add(pluginsMenu);

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
        JIPipeCacheManagerUI cacheManagerUI = new JIPipeCacheManagerUI(this);
        UIUtils.setStandardButtonBorder(cacheManagerUI);
        menu.add(cacheManagerUI);

        // Queue monitor
        menu.add(new JIPipeRunnerQueueButton(this));

        // Logs button
        menu.add(new JIPipeRunnableLogsButton(this));

        // "Run" entry
        JButton runProjectButton = new JButton("Run", UIUtils.getIconFromResources("actions/play.png"));
        runProjectButton.setToolTipText("Runs the whole pipeline");
        UIUtils.setStandardButtonBorder(runProjectButton);

        runProjectButton.addActionListener(e -> openRunUI());
        menu.add(runProjectButton);

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
                    new JIPipeAlgorithmCompendiumUI(),
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDocumentTabPane().switchToLastTab();
        });
        helpMenu.add(algorithmCompendiumButton);

        JMenuItem datatypeCompendiumButton = new JMenuItem("Open data type documentation", UIUtils.getIconFromResources("data-types/data-type.png"));
        datatypeCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Data type documentation",
                    UIUtils.getIconFromResources("actions/help.png"),
                    new JIPipeDataTypeCompendiumUI(),
                    DocumentTabPane.CloseMode.withSilentCloseButton,
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
        JIPipeApplicationSettingsUI applicationSettingsUI = new JIPipeApplicationSettingsUI(this);
        if (navigateToCategory != null) {
            applicationSettingsUI.selectNode(navigateToCategory);
        }
        UIUtils.addEscapeListener(dialog);
        JPanel contentPanel = new JPanel(new BorderLayout(8,8));
        contentPanel.add(applicationSettingsUI, BorderLayout.CENTER);

        AtomicBoolean saved = new AtomicBoolean(false);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                if(!saved.get()) {
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
        JButton saveButton = new JButton("Save", UIUtils.getIconFromResources("actions/save.png"));
        saveButton.addActionListener(e -> {
            if(!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
            saved.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(saveButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setSize(1280,768);
        dialog.setLocationRelativeTo(getWindow());
        dialog.setVisible(true);
    }

    public void openProjectReport() {
        JIPipeProjectReportUI reportUI = new JIPipeProjectReportUI(this);
        documentTabPane.addTab("Project report",
                UIUtils.getIconFromResources("actions/document-preview.png"),
                reportUI,
                DocumentTabPane.CloseMode.withSilentCloseButton);
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
        Path directory = FileChooserSettings.saveDirectory(this, FileChooserSettings.LastDirectoryKey.Projects, "Archive project as directory");
        if (directory != null) {
            JIPipeRunExecuterUI.runInDialog(this, this, new ArchiveProjectToDirectoryRun(getProject(), directory));
        }
    }

    private void archiveProjectAsZIP() {
        Path file = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Archive project as ZIP", UIUtils.EXTENSION_FILTER_ZIP);
        if (file != null) {
            JIPipeRunExecuterUI.runInDialog(this, this, new ArchiveProjectToZIPRun(getProject(), file));
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
        Path path = FileChooserSettings.openPath(this, FileChooserSettings.LastDirectoryKey.Projects, "Select exported cache (ZIP/directory)");
        if (path != null) {
            if (Files.isRegularFile(path)) {
                // Load into cache with a run
                JIPipeRunExecuterUI.runInDialog(this, this, new LoadResultZipIntoCacheRun(this, project, path, true));
            } else {
                // Load into cache with a run
                JIPipeRunExecuterUI.runInDialog(this, this, new LoadResultDirectoryIntoCacheRun(this, project, path, true));
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

    private void manageImageJPlugins(boolean useJIPipeUpdater) {
        if (useJIPipeUpdater) {
            List<DocumentTabPane.DocumentTab> tabs = getDocumentTabPane().getTabsContaining(JIPipeImageJPluginManager.class);
            if (!tabs.isEmpty()) {
                getDocumentTabPane().switchToContent(tabs.get(0).getContent());
            } else {
                JIPipeImageJPluginManager pluginManager = new JIPipeImageJPluginManager(this);
                getDocumentTabPane().addTab("Manage ImageJ plugins",
                        UIUtils.getIconFromResources("apps/imagej.png"),
                        pluginManager,
                        DocumentTabPane.CloseMode.withSilentCloseButton,
                        false);
                getDocumentTabPane().switchToLastTab();
            }
        } else {
            ImageJUpdater updater = new ImageJUpdater();
            getContext().inject(updater);
            updater.run();
        }
    }

    public void openCacheBrowser() {
        JIPipeCacheBrowserUI cacheTable = new JIPipeCacheBrowserUI(this);
        getDocumentTabPane().addTab("Cache browser",
                UIUtils.getIconFromResources("actions/database.png"),
                cacheTable,
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getDocumentTabPane().switchToLastTab();
    }

    /**
     * Exports the whole graph as pipeline
     */
    private void exportProjectAsAlgorithm() {
        JIPipeValidationReport report = new JIPipeValidationReport();
        report.report(new UnspecifiedValidationReportContext(), getProject().getGraph());
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this,
                    this,
                    report,
                    "Error while exporting",
                    "There seem to be various issues with the project. Please resolve these and try to export the project again.",
                    false);
            return;
        }
        NodeGroup nodeGroup = new NodeGroup(new JIPipeGraph(getProject().getGraph()), true, false, true);
        JIPipeJsonExporter exporter = new JIPipeJsonExporter(this, nodeGroup);
        exporter.getNodeInfo().setName("Custom algorithm");
        exporter.getNodeInfo().setDescription(new HTMLText("A custom algorithm"));
        getDocumentTabPane().addTab("Export custom algorithm",
                UIUtils.getIconFromResources("actions/document-export.png"),
                exporter,
                DocumentTabPane.CloseMode.withAskOnCloseButton);
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
     * @param avoidSwitching Do no switch to the validity checker tab if the plugins are OK
     */
    public void validatePlugins(boolean avoidSwitching) {
        pluginValidityCheckerPanel.recheckValidity();
        if (!avoidSwitching || !pluginValidityCheckerPanel.getReport().isValid())
            documentTabPane.selectSingletonTab(TAB_PLUGIN_VALIDITY_CHECK);
    }

    private void managePlugins() {
        documentTabPane.selectSingletonTab(TAB_PLUGIN_MANAGER);
    }

    private void openCompartmentEditor() {
        documentTabPane.selectSingletonTab("COMPARTMENT_EDITOR");
    }

    private void newCompartmentAfterCurrent() {
        if (documentTabPane.getCurrentContent() instanceof JIPipePipelineGraphEditorUI) {
            JIPipePipelineGraphEditorUI ui = (JIPipePipelineGraphEditorUI) documentTabPane.getCurrentContent();
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

    private void openRunUI() {
        JIPipeRunSettingsUI ui = new JIPipeRunSettingsUI(this);
        documentTabPane.addTab("Run", UIUtils.getIconFromResources("actions/run-build.png"), ui,
                DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        documentTabPane.switchToLastTab();
    }

    /**
     * @return The tab pane
     */
    public DocumentTabPane getDocumentTabPane() {
        return documentTabPane;
    }

    /**
     * @return The project
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * @return The parent window
     */
    public Window getWindow() {
        return window;
    }

    public JIPipeProjectWindow getProjectWindow() {
        return window;
    }

    /**
     * @return SciJava context
     */
    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public boolean isProjectModified() {
        return projectModified;
    }

    @Override
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
        for (JIPipePipelineGraphEditorUI compartmentUI : findOpenPipelineEditorTabs(event.getCompartmentUUID())) {
            documentTabPane.remove(compartmentUI);
        }
    }

    @Override
    public void onJIPipeExtensionRegistered(JIPipeService.ExtensionRegisteredEvent event) {
        sendStatusBarText("Registered extension: '" + event.getExtension().getMetadata().getName() + "' with id '" + event.getExtension().getDependencyId() + "'. We recommend to restart ImageJ.");
    }

}
