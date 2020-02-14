package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQCommand;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphUI;
import org.hkijena.acaq5.ui.running.ACAQRunUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ACAQWorkbenchUI extends JFrame {

    private ACAQProject project;
    private ACAQCommand command;
    private DocumentTabPane documentTabPane;
    private ACAQInfoUI infoUI;

    public ACAQWorkbenchUI(ACAQCommand command, ACAQProject project) {
        this.project = project;
        this.command = command;
        initialize();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout(8, 8));
        setTitle("ACAQ 5");
        setIconImage(UIUtils.getIconFromResources("acaq5.png").getImage());
        UIUtils.setToAskOnClose(this, "Do you really want to close ACAQ5?", "Close window");

        infoUI = new ACAQInfoUI(this);

        documentTabPane = new DocumentTabPane();
        documentTabPane.addSingletonTab("INTRODUCTION",
                "Introduction",
                UIUtils.getIconFromResources("info.png"),
                infoUI,
                false);
        documentTabPane.addTab("Data",
                UIUtils.getIconFromResources("sample.png"),
                new ACAQDataUI(this),
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        documentTabPane.addTab("Analysis",
                UIUtils.getIconFromResources("cog.png"),
                new ACAQAlgorithmGraphUI(this, project.getAnalysis()),
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);
        documentTabPane.selectSingletonTab("INTRODUCTION");
        add(documentTabPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();

        // Add "New project" toolbar entry
        JButton newProjectButton = new JButton("New project ...", UIUtils.getIconFromResources("new.png"));
        newProjectButton.addActionListener(e -> newWindow(command, new ACAQProject()));
        toolBar.add(newProjectButton);

        // "Open project" entry
        JButton openProjectButton = new JButton("Open project ...", UIUtils.getIconFromResources("open.png"));
        openProjectButton.addActionListener(e -> openProject());
        toolBar.add(openProjectButton);

        // "Save project" entry
        JButton saveProjectButton = new JButton("Save project ...", UIUtils.getIconFromResources("save.png"));
        saveProjectButton.addActionListener(e -> saveProject());
        toolBar.add(saveProjectButton);

        toolBar.add(Box.createHorizontalGlue());

        // "Run" entry
        JButton runProjectButton = new JButton("Run", UIUtils.getIconFromResources("run.png"));
        runProjectButton.addActionListener(e -> openRunUI());
        toolBar.add(runProjectButton);

        initializeToolbarHelpMenu(toolBar);

        add(toolBar, BorderLayout.NORTH);
    }

    private void initializeToolbarHelpMenu(JToolBar toolBar) {
        JButton helpButton = new JButton(UIUtils.getIconFromResources("help.png"));
        JPopupMenu menu = UIUtils.addPopupMenuToComponent(helpButton);

        JMenuItem quickHelp = new JMenuItem("Quick introduction", UIUtils.getIconFromResources("quickload.png"));
        quickHelp.addActionListener(e -> documentTabPane.selectSingletonTab("INTRODUCTION"));
        menu.add(quickHelp);

        toolBar.add(helpButton);
    }

    private void openProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Open project (*.json");
        if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                newWindow(command, ACAQProject.loadProject(fileChooser.getSelectedFile().toPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Save project (*.json");
        if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                getProject().saveProject(fileChooser.getSelectedFile().toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void openRunUI() {
        ACAQRunUI ui = new ACAQRunUI(this);
        documentTabPane.addTab("Run", UIUtils.getIconFromResources("run.png"), ui,
                DocumentTabPane.CloseMode.withAskOnCloseButton, false);
        documentTabPane.switchToLastTab();
    }

    public DocumentTabPane getDocumentTabPane() {
        return  documentTabPane;
    }

    public ACAQProject getProject() {
        return project;
    }

    public static void newWindow(ACAQCommand command, ACAQProject project) {
        ACAQWorkbenchUI frame = new ACAQWorkbenchUI(command, project);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }


}
