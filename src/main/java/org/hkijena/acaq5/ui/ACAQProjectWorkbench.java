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

package org.hkijena.acaq5.ui;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.events.CompartmentRemovedEvent;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.extensions.settings.GeneralUISettings;
import org.hkijena.acaq5.extensions.settings.ProjectsSettings;
import org.hkijena.acaq5.ui.cache.ACAQCacheBrowserUI;
import org.hkijena.acaq5.ui.cache.ACAQCacheManagerUI;
import org.hkijena.acaq5.ui.compartments.ACAQCompartmentGraphUI;
import org.hkijena.acaq5.ui.compartments.ACAQCompartmentUI;
import org.hkijena.acaq5.ui.compendium.ACAQAlgorithmCompendiumUI;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.MemoryStatusUI;
import org.hkijena.acaq5.ui.components.RecentProjectsMenu;
import org.hkijena.acaq5.ui.components.ReloadableValidityChecker;
import org.hkijena.acaq5.ui.components.SplashScreen;
import org.hkijena.acaq5.ui.extension.MenuTarget;
import org.hkijena.acaq5.ui.extensionbuilder.ACAQJsonExporter;
import org.hkijena.acaq5.ui.extensions.ACAQPluginManagerUIPanel;
import org.hkijena.acaq5.ui.extensions.ACAQPluginValidityCheckerPanel;
import org.hkijena.acaq5.ui.running.ACAQRunSettingsUI;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueueUI;
import org.hkijena.acaq5.ui.settings.ACAQApplicationSettingsUI;
import org.hkijena.acaq5.ui.settings.ACAQProjectSettingsUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;
import org.scijava.Context;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * UI around an {@link ACAQProject}
 */
public class ACAQProjectWorkbench extends JPanel implements ACAQWorkbench {

    public static final String TAB_INTRODUCTION = "INTRODUCTION";
    public static final String TAB_COMPARTMENT_EDITOR = "COMPARTMENT_EDITOR";
    public static final String TAB_PROJECT_SETTINGS = "PROJECT_SETTINGS";
    public static final String TAB_APPLICATION_SETTINGS = "APPLICATION_SETTINGS";
    public static final String TAB_PLUGIN_MANAGER = "PLUGIN_MANAGER";
    public static final String TAB_VALIDITY_CHECK = "VALIDITY_CHECK";
    public static final String TAB_PLUGIN_VALIDITY_CHECK = "PLUGIN_VALIDITY_CHECK";
    private static final String TAB_PROJECT_OVERVIEW = "PROJECT_OVERVIEW";
    public DocumentTabPane documentTabPane;
    private ACAQProjectWindow window;
    private ACAQProject project;
    private JLabel statusText;
    private Context context;
    private ReloadableValidityChecker validityCheckerPanel;
    private ACAQPluginValidityCheckerPanel pluginValidityCheckerPanel;

    /**
     * @param window  Parent window
     * @param context SciJava context
     * @param project The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject if the project is an empty project
     */
    public ACAQProjectWorkbench(ACAQProjectWindow window, Context context, ACAQProject project, boolean showIntroduction, boolean isNewProject) {
        this.window = window;
        this.project = project;
        this.context = context;
        initialize(showIntroduction, isNewProject);
        initializeDefaultProject();
        project.getEventBus().register(this);
        ACAQDefaultRegistry.getInstance().getEventBus().register(this);

        validatePlugins(true);
        SplashScreen.getInstance().hideSplash();
    }

    /**
     * Informs the user about registered extensions.
     *
     * @param event the event
     */
    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        sendStatusBarText("Registered extension: '" + event.getExtension().getMetadata().getName() + "' with id '" + event.getExtension().getDependencyId() + "'. We recommend to restart ImageJ.");
    }

    private void initializeDefaultProject() {
        if (project.getCompartments().isEmpty()) {
            if (ProjectsSettings.getInstance().getStarterProject() == ProjectsSettings.StarterProject.PreprocessingAnalysisPostprocessing) {
                ACAQProjectCompartment preprocessing = project.addCompartment("Preprocessing");
                ACAQProjectCompartment analysis = project.addCompartment("Analysis");
                ACAQProjectCompartment postprocessing = project.addCompartment("Postprocessing");
                project.connectCompartments(preprocessing, analysis);
                project.connectCompartments(analysis, postprocessing);
                openCompartmentGraph(preprocessing, false);
                openCompartmentGraph(analysis, false);
                openCompartmentGraph(postprocessing, false);
            } else {
                ACAQProjectCompartment analysis = project.addCompartment("Analysis");
                openCompartmentGraph(analysis, false);
            }
        }
    }

    private void initialize(boolean showIntroduction, boolean isNewProject) {
        setLayout(new BorderLayout());

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab(TAB_INTRODUCTION,
                "Getting started",
                UIUtils.getIconFromResources("info.png"),
                new ACAQInfoUI(this),
                !GeneralUISettings.getInstance().isShowIntroduction() || !showIntroduction);
        documentTabPane.addSingletonTab(TAB_PROJECT_OVERVIEW,
                "Project overview",
                UIUtils.getIconFromResources("info.png"),
                new ACAQProjectInfoUI(this),
                !GeneralUISettings.getInstance().isShowProjectInfo() || isNewProject);
        documentTabPane.addSingletonTab(TAB_COMPARTMENT_EDITOR,
                "Compartments",
                UIUtils.getIconFromResources("connect.png"),
                new ACAQCompartmentGraphUI(this),
                false);
        documentTabPane.addSingletonTab(TAB_PROJECT_SETTINGS,
                "Project settings",
                UIUtils.getIconFromResources("wrench.png"),
                new ACAQProjectSettingsUI(this),
                true);
        documentTabPane.addSingletonTab(TAB_APPLICATION_SETTINGS,
                "Application settings",
                UIUtils.getIconFromResources("acaq5.png"),
                new ACAQApplicationSettingsUI(this),
                true);
        documentTabPane.addSingletonTab(TAB_PLUGIN_MANAGER,
                "Plugin manager",
                UIUtils.getIconFromResources("module.png"),
                new ACAQPluginManagerUIPanel(this),
                true);
        validityCheckerPanel = new ReloadableValidityChecker(project);
        documentTabPane.addSingletonTab(TAB_VALIDITY_CHECK,
                "Project validation",
                UIUtils.getIconFromResources("checkmark.png"),
                validityCheckerPanel,
                true);
        pluginValidityCheckerPanel = new ACAQPluginValidityCheckerPanel();
        documentTabPane.addSingletonTab(TAB_PLUGIN_VALIDITY_CHECK,
                "Plugin validation",
                UIUtils.getIconFromResources("module.png"),
                pluginValidityCheckerPanel,
                true);
        if (GeneralUISettings.getInstance().isShowIntroduction() && showIntroduction)
            documentTabPane.selectSingletonTab(TAB_INTRODUCTION);
        else {
            if(GeneralUISettings.getInstance().isShowProjectInfo() && !isNewProject) {
                documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW);
            }
            else {
                documentTabPane.selectSingletonTab(TAB_COMPARTMENT_EDITOR);
            }
        }
        add(documentTabPane, BorderLayout.CENTER);

        initializeMenu();
        initializeStatusBar();
        sendStatusBarText("Welcome to ACAQ5");
    }

    /**
     * Finds open {@link ACAQCompartmentUI} tabs
     *
     * @param compartment Targeted compartment
     * @return List of UIs
     */
    public List<ACAQCompartmentUI> findCompartmentUIs(ACAQProjectCompartment compartment) {
        List<ACAQCompartmentUI> result = new ArrayList<>();
        for (DocumentTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if (tab.getContent() instanceof ACAQCompartmentUI) {
                if (((ACAQCompartmentUI) tab.getContent()).getCompartment() == compartment)
                    result.add((ACAQCompartmentUI) tab.getContent());
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
    public void openCompartmentGraph(ACAQProjectCompartment compartment, boolean switchToTab) {
        List<ACAQCompartmentUI> compartmentUIs = findCompartmentUIs(compartment);
        if (compartmentUIs.isEmpty()) {
            ACAQCompartmentUI compartmentUI = new ACAQCompartmentUI(this, compartment);
            DocumentTabPane.DocumentTab documentTab = documentTabPane.addTab(compartment.getName(),
                    UIUtils.getIconFromResources("graph-compartment.png"),
                    compartmentUI,
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    false);
            if (switchToTab)
                documentTabPane.switchToLastTab();
        } else if (switchToTab) {
            documentTabPane.switchToContent(compartmentUIs.get(0));
        }
    }

    private void initializeStatusBar() {
        JXStatusBar statusBar = new JXStatusBar();
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        statusText = new JLabel("Ready ...");
        statusBar.add(statusText);
        statusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
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

        JMenu projectMenu = new JMenu("Project");

        // Add "New project" toolbar entry
        JMenuItem newProjectButton = new JMenuItem("New", UIUtils.getIconFromResources("new.png"));
        newProjectButton.setToolTipText("Creates a new project");
        newProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newProjectButton.addActionListener(e -> window.newProject());
        projectMenu.add(newProjectButton);

        UIUtils.installMenuExtension(this, projectMenu, MenuTarget.ProjectMainMenu, true);
        projectMenu.addSeparator();

        // "Open project" entry
        JMenuItem openProjectButton = new JMenuItem("Open ...", UIUtils.getIconFromResources("open.png"));
        openProjectButton.setToolTipText("Opens a project from a parameter file");
        openProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openProjectButton.addActionListener(e -> window.openProject());
        projectMenu.add(openProjectButton);

        // "Open output" entry
        JMenuItem openProjectOutputButton = new JMenuItem("Open analysis output ...", UIUtils.getIconFromResources("open.png"));
        openProjectOutputButton.setToolTipText("<html>Opens a project and its analysis output from an output folder.<br/>" +
                "<i>Note: The output folder must contain a parameters.json file</i></html>");
        openProjectOutputButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        openProjectOutputButton.addActionListener(e -> window.openProjectAndOutput());
        projectMenu.add(openProjectOutputButton);

        // Recent projects entry
        projectMenu.add(new RecentProjectsMenu("Recent projects", UIUtils.getIconFromResources("clock.png"), (ACAQProjectWindow) getWindow()));

        projectMenu.addSeparator();

        // "Save project" entry
        JMenuItem saveProjectButton = new JMenuItem("Save ...", UIUtils.getIconFromResources("save.png"));
        saveProjectButton.setToolTipText("Saves the project. If the project was opened from a file or previously saved, the file will be overwritten.");
        saveProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveProjectButton.addActionListener(e -> {
            window.saveProjectAs(true);
            validateProject(true);
        });
        projectMenu.add(saveProjectButton);

        // "Save project" entry
        JMenuItem saveProjectAsButton = new JMenuItem("Save as ...", UIUtils.getIconFromResources("save.png"));
        saveProjectAsButton.setToolTipText("Saves the project to a new file.");
        saveProjectAsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        saveProjectAsButton.addActionListener(e -> {
            window.saveProjectAs(false);
            validateProject(true);
        });
        projectMenu.add(saveProjectAsButton);

        // "Export as algorithm" entry
        JMenuItem exportProjectAsAlgorithmButton = new JMenuItem("Export as custom algorithm", UIUtils.getIconFromResources("export.png"));
        exportProjectAsAlgorithmButton.setToolTipText("Exports the whole pipeline (all compartments) as custom algorithm");
        exportProjectAsAlgorithmButton.addActionListener(e -> exportProjectAsAlgorithm());
        projectMenu.add(exportProjectAsAlgorithmButton);

        projectMenu.addSeparator();

        JMenuItem openProjectSettingsButton = new JMenuItem("Project settings", UIUtils.getIconFromResources("wrench.png"));
        openProjectSettingsButton.setToolTipText("Opens the project settings");
        openProjectSettingsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        openProjectSettingsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_SETTINGS));
        projectMenu.add(openProjectSettingsButton);

        JMenuItem openApplicationSettingsButton = new JMenuItem("Application settings", UIUtils.getIconFromResources("acaq5.png"));
        openApplicationSettingsButton.setToolTipText("Opens the application settings");
        openApplicationSettingsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        openApplicationSettingsButton.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_APPLICATION_SETTINGS));
        projectMenu.add(openApplicationSettingsButton);

        projectMenu.addSeparator();

        JMenuItem projectInfo = new JMenuItem("Project overview", UIUtils.getIconFromResources("info.png"));
        projectInfo.setToolTipText("Opens the project overview");
        projectInfo.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW));
        projectMenu.add(projectInfo);

        projectMenu.addSeparator();

        JMenuItem exitButton = new JMenuItem("Exit", UIUtils.getIconFromResources("remove.png"));
        exitButton.addActionListener(e -> getWindow().dispatchEvent(new WindowEvent(getWindow(), WindowEvent.WINDOW_CLOSING)));
        projectMenu.add(exitButton);

        menu.add(projectMenu);

        JMenu compartmentMenu = new JMenu("Compartment");

        JMenuItem editCompartmentsButton = new JMenuItem("Edit compartments", UIUtils.getIconFromResources("edit.png"));
        editCompartmentsButton.setToolTipText("Opens an editor that allows to add more compartments and edit connections");
        editCompartmentsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK));
        editCompartmentsButton.addActionListener(e -> openCompartmentEditor());
        compartmentMenu.add(editCompartmentsButton);

        JMenuItem newCompartmentButton = new JMenuItem("New compartment after current", UIUtils.getIconFromResources("new.png"));
        newCompartmentButton.setToolTipText("Adds a new compartment after the currently selected output into the project");
        newCompartmentButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK));
        newCompartmentButton.addActionListener(e -> newCompartmentAfterCurrent());
        compartmentMenu.add(newCompartmentButton);

        UIUtils.installMenuExtension(this, compartmentMenu, MenuTarget.ProjectCompartmentMenu, true);

        menu.add(compartmentMenu);

        // Plugins menu
        JMenu pluginsMenu = new JMenu("Plugins");

        JMenuItem newPluginButton = new JMenuItem("New JSON extension ...", UIUtils.getIconFromResources("new.png"));
        newPluginButton.setToolTipText("Opens the extension builder");
        newPluginButton.addActionListener(e -> {
            ACAQJsonExtensionWindow window = ACAQJsonExtensionWindow.newWindow(context, new ACAQJsonExtension());
            window.setTitle("New extension");
        });
        pluginsMenu.add(newPluginButton);

        JMenuItem installPluginButton = new JMenuItem("Install ...", UIUtils.getIconFromResources("download.png"));
        installPluginButton.addActionListener(e -> ACAQJsonExtensionWindow.installExtensions(this));
        pluginsMenu.add(installPluginButton);

        JMenuItem managePluginsButton = new JMenuItem("Manage plugins", UIUtils.getIconFromResources("wrench.png"));
        managePluginsButton.addActionListener(e -> managePlugins());
        pluginsMenu.add(managePluginsButton);

        UIUtils.installMenuExtension(this, compartmentMenu, MenuTarget.ProjectPluginsMenu, true);

        menu.add(pluginsMenu);

        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");

        JMenuItem openCacheBrowserButton = new JMenuItem("Cache browser", UIUtils.getIconFromResources("database.png"));
        openCacheBrowserButton.addActionListener(e -> openCacheBrowser());
        toolsMenu.add(openCacheBrowserButton);

        UIUtils.installMenuExtension(this, toolsMenu, MenuTarget.ProjectToolsMenu, false);
        if (toolsMenu.getItemCount() > 0)
            menu.add(toolsMenu);

        menu.add(Box.createHorizontalGlue());
        // Cache monitor
//        menu.add(new ACAQCacheManagerUI(this));

        menu.add(new ACAQCacheManagerUI(this));

        // Queue monitor
        menu.add(new ACAQRunnerQueueUI());
//        menu.add(Box.createHorizontalStrut(1));

        // "Validate" entry
        JButton validateProjectButton = new JButton("Validate", UIUtils.getIconFromResources("checkmark.png"));
        validateProjectButton.setToolTipText("Opens a new tab to check parameters and graph for validity.");
        validateProjectButton.addActionListener(e -> validateProject(false));
        UIUtils.makeFlat(validateProjectButton);
        menu.add(validateProjectButton);

        // "Run" entry
        JButton runProjectButton = new JButton("Run", UIUtils.getIconFromResources("run.png"));
        runProjectButton.setToolTipText("Opens a new interface to run the analysis.");
        UIUtils.makeFlat(runProjectButton);

        runProjectButton.addActionListener(e -> openRunUI());
        menu.add(runProjectButton);

        // "Help" entry
        JMenu helpMenu = new JMenu();
        helpMenu.setIcon(UIUtils.getIconFromResources("help.png"));

        JMenuItem quickHelp = new JMenuItem("Quick introduction", UIUtils.getIconFromResources("quickload.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_INTRODUCTION));
        helpMenu.add(quickHelp);

        JMenuItem projectInfo2 = new JMenuItem("Project overview", UIUtils.getIconFromResources("info.png"));
        projectInfo2.addActionListener(e -> documentTabPane.selectSingletonTab(TAB_PROJECT_OVERVIEW));
        helpMenu.add(projectInfo2);

        JMenuItem algorithmCompendiumButton = new JMenuItem("Open algorithm compendium", UIUtils.getIconFromResources("cog.png"));
        algorithmCompendiumButton.addActionListener(e -> {
            getDocumentTabPane().addTab("Algorithm compendium",
                    UIUtils.getIconFromResources("help.png"),
                    new ACAQAlgorithmCompendiumUI(),
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    true);
            getDocumentTabPane().switchToLastTab();
        });
        helpMenu.add(algorithmCompendiumButton);

        menu.add(helpMenu);

        add(menu, BorderLayout.NORTH);
    }

    private void openCacheBrowser() {
        ACAQCacheBrowserUI cacheTable = new ACAQCacheBrowserUI(this);
        getDocumentTabPane().addTab("Cache browser",
                UIUtils.getIconFromResources("database.png"),
                cacheTable,
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getDocumentTabPane().switchToLastTab();
    }

    /**
     * Exports the whole graph as pipeline
     */
    private void exportProjectAsAlgorithm() {
        ACAQValidityReport report = new ACAQValidityReport();
        report.report(getProject().getGraph());
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(this, report, false);
            return;
        }
        NodeGroup nodeGroup = new NodeGroup(new ACAQGraph(getProject().getGraph()), true);
        ACAQJsonExporter exporter = new ACAQJsonExporter(this, nodeGroup);
        exporter.getAlgorithmDeclaration().setName("Custom algorithm");
        exporter.getAlgorithmDeclaration().setDescription("A custom algorithm");
        getDocumentTabPane().addTab("Export custom algorithm",
                UIUtils.getIconFromResources("export.png"),
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
        if (documentTabPane.getCurrentContent() instanceof ACAQCompartmentUI) {
            ACAQCompartmentUI ui = (ACAQCompartmentUI) documentTabPane.getCurrentContent();
            String compartmentName = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the compartment",
                    "Compartment", s -> project.getCompartments().containsKey(s));
            if (compartmentName != null && !compartmentName.trim().isEmpty()) {
                ACAQProjectCompartment compartment = project.addCompartment(compartmentName);
                project.connectCompartments(ui.getCompartment(), compartment);
                openCompartmentGraph(compartment, true);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please switch to a graph compartment editor.", "New compartment after current", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRunUI() {
        ACAQRunSettingsUI ui = new ACAQRunSettingsUI(this);
        documentTabPane.addTab("Run", UIUtils.getIconFromResources("run.png"), ui,
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
    public ACAQProject getProject() {
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
    public void onCompartmentRemoved(CompartmentRemovedEvent event) {
        for (ACAQCompartmentUI compartmentUI : findCompartmentUIs(event.getCompartment())) {
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
}
