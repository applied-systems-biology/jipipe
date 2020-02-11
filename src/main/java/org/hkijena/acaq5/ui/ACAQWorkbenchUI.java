package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQCommand;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

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
        JButton newProject = new JButton("New project ...", UIUtils.getIconFromResources("new.png"));
        newProject.addActionListener(e -> newWindow(command, new ACAQProject()));
        toolBar.add(newProject);

        // "Open project" entry
        JButton openProject = new JButton("Open project ...", UIUtils.getIconFromResources("open.png"));
        toolBar.add(openProject);

        // "Save project" entry
        JButton saveProject = new JButton("Save project ...", UIUtils.getIconFromResources("save.png"));
        toolBar.add(saveProject);

        toolBar.add(Box.createHorizontalGlue());

        // "Run" entry
        JButton runProject = new JButton("Run", UIUtils.getIconFromResources("run.png"));
        toolBar.add(runProject);

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
