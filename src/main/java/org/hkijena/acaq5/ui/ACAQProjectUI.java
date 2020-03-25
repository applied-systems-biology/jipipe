package org.hkijena.acaq5.ui;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.events.CompartmentRemovedEvent;
import org.hkijena.acaq5.ui.compartments.ACAQCompartmentGraphUI;
import org.hkijena.acaq5.ui.compartments.ACAQCompartmentUI;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.RecentProjectsMenu;
import org.hkijena.acaq5.ui.running.ACAQRunSettingsUI;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueueUI;
import org.hkijena.acaq5.ui.settings.ACAQProjectSettingsUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ACAQProjectUI extends JPanel {

    public DocumentTabPane documentTabPane;
    private ACAQProjectWindow window;
    private ACAQProject project;
    private ACAQGUICommand command;
    private JLabel statusText;
    private Context context;

    public ACAQProjectUI(ACAQProjectWindow window, ACAQGUICommand command, ACAQProject project) {
        this.window = window;
        this.project = project;
        this.command = command;
        this.context = command.getContext();
        initialize();
        initializeDefaultProject();
        project.getEventBus().register(this);
    }

    private void initializeDefaultProject() {
        if (project.getCompartments().isEmpty()) {
            ACAQProjectCompartment preprocessing = project.addCompartment("Preprocessing");
            ACAQProjectCompartment analysis = project.addCompartment("Analysis");
            ACAQProjectCompartment postprocessing = project.addCompartment("Postprocessing");
            project.connectCompartments(preprocessing, analysis);
            project.connectCompartments(analysis, postprocessing);
            openCompartmentGraph(preprocessing, false);
            openCompartmentGraph(analysis, false);
            openCompartmentGraph(postprocessing, false);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab("INTRODUCTION",
                "Introduction",
                UIUtils.getIconFromResources("info.png"),
                new ACAQInfoUI(this),
                false);
        documentTabPane.addSingletonTab("COMPARTMENT_EDITOR",
                "Compartments",
                UIUtils.getIconFromResources("connect.png"),
                new ACAQCompartmentGraphUI(this),
                false);
        documentTabPane.addSingletonTab("PROJECT_SETTINGS",
                "Project settings",
                UIUtils.getIconFromResources("wrench.png"),
                new ACAQProjectSettingsUI(this),
                true);
        documentTabPane.selectSingletonTab("INTRODUCTION");
        add(documentTabPane, BorderLayout.CENTER);

        initializeMenu();
        initializeStatusBar();
        sendStatusBarText("Welcome to ACAQ5");
    }

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
            documentTabPane.setSelectedComponent(compartmentUIs.get(0));
        }
    }

    private void initializeStatusBar() {
        JXStatusBar statusBar = new JXStatusBar();
        statusText = new JLabel("Ready ...");
        statusBar.add(statusText);
        add(statusBar, BorderLayout.SOUTH);
    }

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
        projectMenu.add(new RecentProjectsMenu("Recent projects", UIUtils.getIconFromResources("clock.png"), getWindow()));

        projectMenu.addSeparator();

        // "Save project" entry
        JMenuItem saveProjectButton = new JMenuItem("Save ...", UIUtils.getIconFromResources("save.png"));
        saveProjectButton.setToolTipText("Saves the project. If the project was opened from a file or previously saved, the file will be overwritten.");
        saveProjectButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveProjectButton.addActionListener(e -> window.saveProjectAs(true));
        projectMenu.add(saveProjectButton);

        // "Save project" entry
        JMenuItem saveProjectAsButton = new JMenuItem("Save as ...", UIUtils.getIconFromResources("save.png"));
        saveProjectAsButton.setToolTipText("Saves the project to a new file.");
        saveProjectAsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
        saveProjectAsButton.addActionListener(e -> window.saveProjectAs(false));
        projectMenu.add(saveProjectAsButton);

        projectMenu.addSeparator();

        JMenuItem openProjectSettingsButton = new JMenuItem("Project settings", UIUtils.getIconFromResources("wrench.png"));
        openProjectSettingsButton.setToolTipText("Opens the project settings");
        openProjectSettingsButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK));
        openProjectSettingsButton.addActionListener(e -> documentTabPane.selectSingletonTab("PROJECT_SETTINGS"));
        projectMenu.add(openProjectSettingsButton);

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

        menu.add(compartmentMenu);

        menu.add(Box.createHorizontalGlue());

        // Queue monitor
        menu.add(new ACAQRunnerQueueUI());
        menu.add(Box.createHorizontalStrut(1));

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
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab("INTRODUCTION"));
        helpMenu.add(quickHelp);
        menu.add(helpMenu);

        add(menu, BorderLayout.NORTH);
    }

    private void openCompartmentEditor() {
        documentTabPane.selectSingletonTab("COMPARTMENT_EDITOR");
    }

    private void newCompartmentAfterCurrent() {
        if (documentTabPane.getSelectedComponent() instanceof ACAQCompartmentUI) {
            ACAQCompartmentUI ui = (ACAQCompartmentUI) documentTabPane.getSelectedComponent();
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

    public DocumentTabPane getDocumentTabPane() {
        return documentTabPane;
    }

    public ACAQProject getProject() {
        return project;
    }

    public ACAQProjectWindow getWindow() {
        return window;
    }

    @Subscribe
    public void onCompartmentRemoved(CompartmentRemovedEvent event) {
        for (ACAQCompartmentUI compartmentUI : findCompartmentUIs(event.getCompartment())) {
            documentTabPane.remove(compartmentUI);
        }
    }

    public Context getContext() {
        return context;
    }

    public ACAQGUICommand getCommand() {
        return command;
    }
}
