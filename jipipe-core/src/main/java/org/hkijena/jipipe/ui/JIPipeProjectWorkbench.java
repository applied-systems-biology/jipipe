/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.settings.AutoSaveSettings;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.cache.JIPipeCacheBrowserUI;
import org.hkijena.jipipe.ui.cache.JIPipeCacheManagerUI;
import org.hkijena.jipipe.ui.compartments.JIPipeCompartmentGraphUI;
import org.hkijena.jipipe.ui.compartments.JIPipeCompartmentUI;
import org.hkijena.jipipe.ui.compendium.JIPipeAlgorithmCompendiumUI;
import org.hkijena.jipipe.ui.compendium.JIPipeDataTypeCompendiumUI;
import org.hkijena.jipipe.ui.components.*;
import org.hkijena.jipipe.ui.data.VirtualDataControl;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExporter;
import org.hkijena.jipipe.ui.extensions.JIPipePluginManagerUIPanel;
import org.hkijena.jipipe.ui.extensions.JIPipePluginValidityCheckerPanel;
import org.hkijena.jipipe.ui.ijupdater.JIPipeImageJPluginManager;
import org.hkijena.jipipe.ui.project.JIPipeProjectTabMetadata;
import org.hkijena.jipipe.ui.running.*;
import org.hkijena.jipipe.ui.settings.JIPipeApplicationSettingsUI;
import org.hkijena.jipipe.ui.settings.JIPipeProjectSettingsUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * UI around an {@link JIPipeProject}
 */
public class JIPipeProjectWorkbench extends JPanel implements JIPipeWorkbench {

    public static final String TAB_INTRODUCTION = "INTRODUCTION";
    public static final String TAB_LICENSE = "LICENSE";
    public static final String TAB_COMPARTMENT_EDITOR = "COMPARTMENT_EDITOR";
    public static final String TAB_PROJECT_SETTINGS = "PROJECT_SETTINGS";
    public static final String TAB_APPLICATION_SETTINGS = "APPLICATION_SETTINGS";
    public static final String TAB_PLUGIN_MANAGER = "PLUGIN_MANAGER";
    public static final String TAB_VALIDITY_CHECK = "VALIDITY_CHECK";
    public static final String TAB_PLUGIN_VALIDITY_CHECK = "PLUGIN_VALIDITY_CHECK";
    private static final String TAB_PROJECT_OVERVIEW = "PROJECT_OVERVIEW";
    private static final String TAB_LOG = "LOG";
    public DocumentTabPane documentTabPane;
    private JIPipeProjectWindow window;
    private JIPipeProject project;
    private JLabel statusText;
    private Context context;
    private ReloadableValidityChecker validityCheckerPanel;
    private JIPipePluginValidityCheckerPanel pluginValidityCheckerPanel;
    private RealTimeProjectRunner realTimeProjectRunner;
    private VirtualDataControl virtualDataControl;
    private boolean projectModified;

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
        this.realTimeProjectRunner = new RealTimeProjectRunner(this);
        this.virtualDataControl = new VirtualDataControl(this);
        initialize(showIntroduction, isNewProject);
        project.getEventBus().register(this);
        JIPipe.getInstance().getEventBus().register(this);

        validatePlugins(true);

        restoreStandardTabs(showIntroduction, isNewProject);
        if (ProjectsSettings.getInstance().isRestoreTabs())
            restoreTabs();
        if (GeneralUISettings.getInstance().isShowIntroduction() && showIntroduction)
            documentTabPane.selectSingletonTab(TAB_INTRODUCTION);
        if (!isNewProject && RuntimeSettings.getInstance().isRealTimeRunEnabled()) {
            SwingUtilities.invokeLater(() -> realTimeProjectRunner.scheduleRun());
        }

        // Register modification state watchers
        project.getGraph().getEventBus().register(new Object() {
            @Subscribe
            public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
                setProjectModified(true);
            }
        });
        project.getCompartmentGraph().getEventBus().register(new Object() {
            @Subscribe
            public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
                setProjectModified(true);
            }
        });

        // Install the run notifier
        JIPipeRunQueueNotifier.install();
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
        try {
            Object metadata = project.getAdditionalMetadata().getOrDefault(JIPipeProjectTabMetadata.METADATA_KEY, null);
            if (metadata instanceof JIPipeProjectTabMetadata) {
                ((JIPipeProjectTabMetadata) metadata).restore(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Informs the user about registered extensions.
     *
     * @param event the event
     */
    @Subscribe
    public void onExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
        sendStatusBarText("Registered extension: '" + event.getExtension().getMetadata().getName() + "' with id '" + event.getExtension().getDependencyId() + "'. We recommend to restart ImageJ.");
    }

    private void initialize(boolean showIntroduction, boolean isNewProject) {
        setLayout(new BorderLayout());

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab(TAB_INTRODUCTION,
                "Getting started",
                UIUtils.getIconFromResources("actions/help-info.png"),
                new JIPipeInfoUI(this),
                !GeneralUISettings.getInstance().isShowIntroduction() || !showIntroduction);
        documentTabPane.addSingletonTab(TAB_PROJECT_OVERVIEW,
                "Project overview",
                UIUtils.getIconFromResources("actions/help-info.png"),
                new JIPipeProjectInfoUI(this),
                !GeneralUISettings.getInstance().isShowProjectInfo() || isNewProject);
        documentTabPane.addSingletonTab(TAB_LICENSE,
                "License",
                UIUtils.getIconFromResources("actions/license.png"),
                new MarkdownReader(true, MarkdownDocument.fromPluginResource("documentation/license.md")),
                true);
        documentTabPane.addSingletonTab(TAB_COMPARTMENT_EDITOR,
                "Compartments",
                UIUtils.getIconFromResources("actions/straight-connector.png"),
                new JIPipeCompartmentGraphUI(this),
                false);
        documentTabPane.addSingletonTab(TAB_PROJECT_SETTINGS,
                "Project settings",
                UIUtils.getIconFromResources("actions/wrench.png"),
                new JIPipeProjectSettingsUI(this),
                true);
        documentTabPane.addSingletonTab(TAB_APPLICATION_SETTINGS,
                "Application settings",
                UIUtils.getIconFromResources("apps/jipipe.png"),
                new JIPipeApplicationSettingsUI(this),
                true);
        documentTabPane.addSingletonTab(TAB_PLUGIN_MANAGER,
                "Plugin manager",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new JIPipePluginManagerUIPanel(this),
                true);
        validityCheckerPanel = new ReloadableValidityChecker(project);
        documentTabPane.addSingletonTab(TAB_VALIDITY_CHECK,
                "Project validation",
                UIUtils.getIconFromResources("actions/checkmark.png"),
                validityCheckerPanel,
                true);
        pluginValidityCheckerPanel = new JIPipePluginValidityCheckerPanel();
        documentTabPane.addSingletonTab(TAB_PLUGIN_VALIDITY_CHECK,
                "Plugin validation",
                UIUtils.getIconFromResources("actions/plugins.png"),
                pluginValidityCheckerPanel,
                true);
        documentTabPane.addSingletonTab(TAB_LOG,
                "Log viewer",
                UIUtils.getIconFromResources("actions/show_log.png"),
                new JIPipeLogViewer(this),
                true);
        add(documentTabPane, BorderLayout.CENTER);

        initializeMenu();
        initializeStatusBar();
        sendStatusBarText("Welcome to JIPipe");
    }

    /**
     * Finds open {@link JIPipeCompartmentUI} tabs
     *
     * @param compartment Targeted compartment
     * @return List of UIs
     */
    public List<JIPipeCompartmentUI> findCompartmentUIs(JIPipeProjectCompartment compartment) {
        List<JIPipeCompartmentUI> result = new ArrayList<>();
        for (DocumentTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if (tab.getContent() instanceof JIPipeCompartmentUI) {
                if (((JIPipeCompartmentUI) tab.getContent()).getCompartment() == compartment)
                    result.add((JIPipeCompartmentUI) tab.getContent());
            }
        }
        return result;
    }

    /**
     * Opens the graph editor for specified compartment
     *
     * @param compartment The compartment
     * @param switchToTab If true, switch to the tab
     */
    public DocumentTabPane.DocumentTab openCompartmentGraph(JIPipeProjectCompartment compartment, boolean switchToTab) {
        List<JIPipeCompartmentUI> compartmentUIs = findCompartmentUIs(compartment);
        if (compartmentUIs.isEmpty()) {
            JIPipeCompartmentUI compartmentUI = new JIPipeCompartmentUI(this, compartment);
            DocumentTabPane.DocumentTab documentTab = documentTabPane.addTab(compartment.getName(),
                    UIUtils.getIconFromResources("data-types/graph-compartment.png"),
                    compartmentUI,
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    false);
            compartment.getEventBus().register(new Object() {
                @Subscribe
                public void onRenamed(JIPipeParameterCollection.ParameterChangedEvent event) {
                    if (event.getKey().equals("jipipe:node:name")) {
                        documentTab.setTitle(compartment.getName());
                        documentTab.getEventBus().post(new JIPipeParameterCollection.ParameterChangedEvent(compartment, "title"));
                    }
                }
            });
            project.getEventBus().register(new Object() {
                @Subscribe
                public void onCompartmentRemoved(JIPipeProject.CompartmentRemovedEvent event) {
                    if (event.getCompartment() == compartment) {
                        documentTabPane.closeTab(documentTab);
                    }
                }
            });
            if (switchToTab)
                documentTabPane.switchToLastTab();
            return documentTab;
        } else if (switchToTab) {
            DocumentTabPane.DocumentTab tab = documentTabPane.getTabContaining(compartmentUIs.get(0));
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

        // Virtual control
        JToggleButton virtualControlToggle = virtualDataControl.createToggleButton();
        UIUtils.makeFlatH25(virtualControlToggle);
        virtualControlToggle.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));

        statusBar.add(virtualControlToggle);
        JButton optionsButton = virtualDataControl.createOptionsButton();
        UIUtils.makeFlat25x25(optionsButton);
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

        UIUtils.installMenuExtension(this, projectMenu, MenuTarget.ProjectMainMenu, true);
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

        JMenuItem restoreMenuItem = new JMenuItem("Restore backup ...", UIUtils.getIconFromResources("actions/reload.png"));
        restoreMenuItem.setToolTipText("Restores an automatically created backup (auto-save)");
        restoreMenuItem.addActionListener(e -> AutoSaveSettings.getInstance().openRestoreMenu(this));
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
            window.saveProjectAndCache();
        });
        projectMenu.add(saveProjectAndCache);

        // "Export as algorithm" entry
        JMenuItem exportProjectAsAlgorithmButton = new JMenuItem("Export as custom algorithm", UIUtils.getIconFromResources("actions/document-export.png"));
        exportProjectAsAlgorithmButton.setToolTipText("Exports the whole pipeline (all compartments) as custom algorithm");
        exportProjectAsAlgorithmButton.addActionListener(e -> exportProjectAsAlgorithm());
        projectMenu.add(exportProjectAsAlgorithmButton);

        projectMenu.addSeparator();

        JMenuItem openProjectSettingsButton = new JMenuItem("Project settings", UIUtils.getIconFromResources("actions/wrench.png"));
        openProjectSettingsButton.setToolTipText("Opens the project settings");
        openProjectSettingsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        openProjectSettingsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_SETTINGS));
        projectMenu.add(openProjectSettingsButton);

        JMenuItem openApplicationSettingsButton = new JMenuItem("Application settings", UIUtils.getIconFromResources("apps/jipipe.png"));
        openApplicationSettingsButton.setToolTipText("Opens the application settings");
        openApplicationSettingsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        openApplicationSettingsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_APPLICATION_SETTINGS));
        projectMenu.add(openApplicationSettingsButton);

        projectMenu.addSeparator();

        JMenuItem projectInfo = new JMenuItem("Project overview", UIUtils.getIconFromResources("actions/help-info.png"));
        projectInfo.setToolTipText("Opens the project overview");
        projectInfo.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW));
        projectMenu.add(projectInfo);

        JMenuItem openProjectFolderItem = new JMenuItem("Open project folder", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openProjectFolderItem.setToolTipText("Opens the folder that contains the project file");
        openProjectFolderItem.addActionListener(e -> openProjectFolder());
        projectMenu.add(openProjectFolderItem);

        projectMenu.addSeparator();

        JMenuItem exitButton = new JMenuItem("Exit", UIUtils.getIconFromResources("actions/exit.png"));
        exitButton.addActionListener(e -> getWindow().dispatchEvent(new WindowEvent(getWindow(), WindowEvent.WINDOW_CLOSING)));
        projectMenu.add(exitButton);

        menu.add(projectMenu);

        JMenu compartmentMenu = new JMenu("Compartment");

        JMenuItem editCompartmentsButton = new JMenuItem("Edit compartments", UIUtils.getIconFromResources("actions/edit.png"));
        editCompartmentsButton.setToolTipText("Opens an editor that allows to add more compartments and edit connections");
        editCompartmentsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK));
        editCompartmentsButton.addActionListener(e -> openCompartmentEditor());
        compartmentMenu.add(editCompartmentsButton);

        JMenuItem newCompartmentButton = new JMenuItem("New compartment after current", UIUtils.getIconFromResources("actions/document-new.png"));
        newCompartmentButton.setToolTipText("Adds a new compartment after the currently selected output into the project");
        newCompartmentButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        newCompartmentButton.addActionListener(e -> newCompartmentAfterCurrent());
        compartmentMenu.add(newCompartmentButton);

        UIUtils.installMenuExtension(this, compartmentMenu, MenuTarget.ProjectCompartmentMenu, true);

        menu.add(compartmentMenu);

        // Plugins menu
        JMenu pluginsMenu = new JMenu("Plugins");

        JMenuItem newPluginButton = new JMenuItem("New JSON extension ...", UIUtils.getIconFromResources("actions/document-new.png"));
        newPluginButton.setToolTipText("Opens the extension builder");
        newPluginButton.addActionListener(e -> {
            JIPipeJsonExtensionWindow window = JIPipeJsonExtensionWindow.newWindow(context, new JIPipeJsonExtension(), true);
            window.setTitle("New extension");
        });
        pluginsMenu.add(newPluginButton);

        JMenuItem installPluginButton = new JMenuItem("Install ...", UIUtils.getIconFromResources("actions/run-install.png"));
        installPluginButton.addActionListener(e -> JIPipeJsonExtensionWindow.installExtensions(this));
        pluginsMenu.add(installPluginButton);

        JMenuItem managePluginsButton = new JMenuItem("Manage plugins", UIUtils.getIconFromResources("actions/wrench.png"));
        managePluginsButton.addActionListener(e -> managePlugins());
        pluginsMenu.add(managePluginsButton);

        JMenuItem manageImageJPlugins = new JMenuItem("Manage ImageJ plugins", UIUtils.getIconFromResources("apps/imagej.png"));
        manageImageJPlugins.addActionListener(e -> manageImageJPlugins());
        pluginsMenu.add(manageImageJPlugins);

        UIUtils.installMenuExtension(this, compartmentMenu, MenuTarget.ProjectPluginsMenu, true);

        menu.add(pluginsMenu);

        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");

        JMenuItem openCacheBrowserButton = new JMenuItem("Cache browser", UIUtils.getIconFromResources("actions/database.png"));
        openCacheBrowserButton.addActionListener(e -> openCacheBrowser());
        toolsMenu.add(openCacheBrowserButton);

        JMenuItem openLogsButton = new JMenuItem("Logs", UIUtils.getIconFromResources("actions/show_log.png"));
        openLogsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_LOG));
        toolsMenu.add(openLogsButton);

        UIUtils.installMenuExtension(this, toolsMenu, MenuTarget.ProjectToolsMenu, false);
        if (toolsMenu.getItemCount() > 0)
            menu.add(toolsMenu);

        menu.add(Box.createHorizontalGlue());

//        // Virtual control
//        JToggleButton virtualControlToggle = virtualDataControl.createToggleButton();
//        UIUtils.makeFlat(virtualControlToggle);
//        menu.add(virtualControlToggle);

        // Real-time runner control
        JToggleButton realtimeToggleButton = realTimeProjectRunner.createToggleButton();
        UIUtils.makeFlat(realtimeToggleButton);
        menu.add(realtimeToggleButton);

        // Cache monitor
//        menu.add(new JIPipeCacheManagerUI(this));

        menu.add(new JIPipeCacheManagerUI(this));

        // Queue monitor
        menu.add(new JIPipeRunnerQueueUI());
//        menu.add(Box.createHorizontalStrut(1));

        // "Validate" entry
        JButton validateProjectButton = new JButton("Validate", UIUtils.getIconFromResources("actions/checkmark.png"));
        validateProjectButton.setToolTipText("Opens a new tab to check parameters and graph for validity.");
        validateProjectButton.addActionListener(e -> validateProject(false));
        UIUtils.makeFlat(validateProjectButton);
        menu.add(validateProjectButton);

        // "Run" entry
        JButton runProjectButton = new JButton("Run", UIUtils.getIconFromResources("actions/run-build.png"));
        runProjectButton.setToolTipText("Opens a new interface to run the analysis.");
        UIUtils.makeFlat(runProjectButton);

        runProjectButton.addActionListener(e -> openRunUI());
        menu.add(runProjectButton);

        // "Help" entry
        JMenu helpMenu = new JMenu();
        helpMenu.setIcon(UIUtils.getIconFromResources("actions/help.png"));

        JMenuItem offlineManual = new JMenuItem("Manual", UIUtils.getIconFromResources("actions/help.png"));
        offlineManual.setToolTipText("Opens the offline manual in a browser. If the manual is not available, it will be downloaded.");
        offlineManual.addActionListener(e -> openManual());
        helpMenu.add(offlineManual);

        JMenuItem quickHelp = new JMenuItem("Getting started", UIUtils.getIconFromResources("actions/help-info.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_INTRODUCTION));
        helpMenu.add(quickHelp);

        JMenuItem projectInfo2 = new JMenuItem("Project overview", UIUtils.getIconFromResources("actions/help-info.png"));
        projectInfo2.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW));
        helpMenu.add(projectInfo2);

        JMenuItem algorithmCompendiumButton = new JMenuItem("Open algorithm compendium", UIUtils.getIconFromResources("data-types/node.png"));
        algorithmCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Algorithm compendium",
                    UIUtils.getIconFromResources("actions/help.png"),
                    new JIPipeAlgorithmCompendiumUI(),
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDocumentTabPane().switchToLastTab();
        });
        helpMenu.add(algorithmCompendiumButton);

        JMenuItem datatypeCompendiumButton = new JMenuItem("Open data type compendium", UIUtils.getIconFromResources("data-types/data-type.png"));
        datatypeCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Data type compendium",
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

        UIUtils.installMenuExtension(this, helpMenu, MenuTarget.ProjectHelpMenu, true);

        add(menu, BorderLayout.NORTH);
    }

    private void openManual() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        Path indexFile = imageJDir.resolve("jipipe").resolve("offline-manual").resolve("docs").resolve("index.html");
        DownloadOfflineManualRun run = new DownloadOfflineManualRun();
        if (!Files.exists(indexFile)) {
            if (JOptionPane.showConfirmDialog(this, "The manual needs to be downloaded, first." +
                            "\nDo you want to download it now?\n\n" +
                            "This needs to be only done once.\n\n" +
                            "URL: " + DownloadOfflineManualRun.DOWNLOAD_URL,
                    "Open manual",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                return;
            }
            JIPipeRunExecuterUI.runInDialog(getWindow(), run);
        }
        if (Files.exists(indexFile)) {
            try {
                Desktop.getDesktop().open(indexFile.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "The manual does not exist!",
                    "Open manual",
                    JOptionPane.ERROR_MESSAGE);
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

    private void manageImageJPlugins() {
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
    }

    private void openCacheBrowser() {
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
        JIPipeValidityReport report = new JIPipeValidityReport();
        report.report(getProject().getGraph());
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }
        NodeGroup nodeGroup = new NodeGroup(new JIPipeGraph(getProject().getGraph()), true);
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
     * @param avoidSwitching Do no switch to the validity checker tab if the project is OK
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
        if (documentTabPane.getCurrentContent() instanceof JIPipeCompartmentUI) {
            JIPipeCompartmentUI ui = (JIPipeCompartmentUI) documentTabPane.getCurrentContent();
            String compartmentName = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the compartment",
                    "Compartment", s -> project.getCompartments().containsKey(s));
            if (compartmentName != null && !compartmentName.trim().isEmpty()) {
                JIPipeProjectCompartment compartment = project.addCompartment(compartmentName);
                project.connectCompartments(ui.getCompartment(), compartment);
                openCompartmentGraph(compartment, true);
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

    /**
     * Triggered when a compartment is deleted.
     * Closes corresponding tabs.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onCompartmentRemoved(JIPipeProject.CompartmentRemovedEvent event) {
        for (JIPipeCompartmentUI compartmentUI : findCompartmentUIs(event.getCompartment())) {
            documentTabPane.remove(compartmentUI);
        }
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
}
