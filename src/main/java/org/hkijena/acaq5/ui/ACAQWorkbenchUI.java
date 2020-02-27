package org.hkijena.acaq5.ui;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.events.CompartmentAddedEvent;
import org.hkijena.acaq5.api.events.CompartmentRemovedEvent;
import org.hkijena.acaq5.api.events.CompartmentRenamedEvent;
import org.hkijena.acaq5.ui.compartments.ACAQCompartmentUI;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.running.ACAQRunSettingsUI;
import org.hkijena.acaq5.ui.running.ACAQRunnerQueueUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ACAQWorkbenchUI extends JPanel {

    private ACAQWorkbenchWindow window;
    private ACAQProject project;
    private ACAQGUICommand command;
    private DocumentTabPane documentTabPane;
    private ACAQInfoUI infoUI;
    private JLabel statusText;
    private Map<String, ACAQCompartmentUI> compartments = new HashMap<>();
    private Map<String, DocumentTabPane.DocumentTab> compartmentTabs = new HashMap<>();

    public ACAQWorkbenchUI(ACAQWorkbenchWindow window, ACAQGUICommand command, ACAQProject project) {
        this.window = window;
        this.project = project;
        this.command = command;
        initialize();
        initializeDefaultProject();
        updateCompartmentUIs();
        project.getEventBus().register(this);
    }

    private void initializeDefaultProject() {
        if(project.getCompartments().isEmpty()) {
            project.addCompartment("Preprocessing");
            project.addCompartment("Analysis");
            project.addCompartment("Postprocessing");
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        infoUI = new ACAQInfoUI(this);

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab("INTRODUCTION",
                "Introduction",
                UIUtils.getIconFromResources("info.png"),
                infoUI,
                false);
        documentTabPane.selectSingletonTab("INTRODUCTION");
        add(documentTabPane, BorderLayout.CENTER);

        initializeMenu();
        initializeStatusBar();
        sendStatusBarText("Welcome to ACAQ5");
    }

    private void updateCompartmentUIs() {
        for (String compartmentName : project.getCompartmentOrder()) {
            if(!compartments.containsKey(compartmentName)) {
                ACAQCompartmentUI compartmentUI = new ACAQCompartmentUI(this, project.getCompartments().get(compartmentName));
                DocumentTabPane.DocumentTab documentTab = documentTabPane.addTab(compartmentName,
                        UIUtils.getIconFromResources("cog.png"),
                        compartmentUI,
                        DocumentTabPane.CloseMode.withoutCloseButton,
                        false);
                compartmentTabs.put(compartmentName, documentTab);
                compartments.put(compartmentName, compartmentUI);
            }
        }
        List<String> toRemove = new ArrayList<>();
        for (String compartmentName : compartments.keySet()) {
            if(!project.getCompartments().containsKey(compartmentName)) {
                toRemove.add(compartmentName);
            }
        }
        for (String compartmentName : toRemove) {
            ACAQCompartmentUI compartmentUI = compartments.get(compartmentName);
            DocumentTabPane.DocumentTab documentTab = compartmentTabs.get(compartmentName);
            documentTabPane.remove(documentTab.getTabComponent());
            compartments.remove(compartmentName);
            compartmentTabs.remove(compartmentName);
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

        menu.add(projectMenu);

        JMenu compartmentMenu = new JMenu("Compartment");
        JMenuItem newCompartmentButton = new JMenuItem("New compartment", UIUtils.getIconFromResources("new.png"));
        newCompartmentButton.setToolTipText("Adds a new compartment into the project");
        newCompartmentButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK));
        newCompartmentButton.addActionListener(e -> newCompartmentAtEnd());
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

    private void newCompartmentAtEnd() {
        String compartmentName = UIUtils.getUniqueStringByDialog(this, "Please enter the name of the compartment",
                "Compartment", s -> project.getCompartments().containsKey(s));
        if(compartmentName != null && !compartmentName.trim().isEmpty()) {
            project.addCompartment(compartmentName);
        }
    }

    private void newCompartmentAfterCurrent() {

    }

    private void openRunUI() {
        ACAQRunSettingsUI ui = new ACAQRunSettingsUI(this);
        documentTabPane.addTab("Run", UIUtils.getIconFromResources("run.png"), ui,
                DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        documentTabPane.switchToLastTab();
    }

    public DocumentTabPane getDocumentTabPane() {
        return  documentTabPane;
    }

    public ACAQProject getProject() {
        return project;
    }

    public Frame getWindow() {
        return window;
    }

    @Subscribe
    public void onCompartmentAdded(CompartmentAddedEvent event) {
        updateCompartmentUIs();
    }

    @Subscribe
    public void onCompartmentRemoved(CompartmentRemovedEvent event) {
        updateCompartmentUIs();
    }

    @Subscribe
    public void onCompartmentRenamed(CompartmentRenamedEvent event) {
        updateCompartmentUIs();
    }
}
