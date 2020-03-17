package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class ACAQWorkbenchWindow extends JFrame {

    private ACAQGUICommand command;
    private ACAQProject project;
    private ACAQWorkbenchUI projectUI;
    private Path projectSavePath;

    public ACAQWorkbenchWindow(ACAQGUICommand command, ACAQProject project) {
        this.command = command;
        initialize();
        loadProject(project);
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout(8, 8));
        super.setTitle("ACAQ5");
        setIconImage(UIUtils.getIconFromResources("acaq5-128.png").getImage());
        UIUtils.setToAskOnClose(this, "Do you really want to close ACAQ5?", "Close window");
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("ACAQ5 - " + title);
    }

    public void loadProject(ACAQProject project) {
        this.project = project;
        this.projectUI = new ACAQWorkbenchUI(this, command, project);
        setContentPane(projectUI);
    }

    public void newProject() {
        ACAQProject project = new ACAQProject();
        ACAQWorkbenchWindow window = openProjectInThisOrNewWindow("New project", project);
        if (window == null)
            return;
        window.projectSavePath = null;
        window.setTitle("New project");
        window.getProjectUI().sendStatusBarText("Created new project");
    }

    public void openProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Open project (*.json)");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ACAQProject project = ACAQProject.loadProject(fileChooser.getSelectedFile().toPath());
                ACAQWorkbenchWindow window = openProjectInThisOrNewWindow("Open project", project);
                if (window == null)
                    return;
                window.projectSavePath = fileChooser.getSelectedFile().toPath();
                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.setTitle(window.projectSavePath.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void openProjectAndOutput() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Open project output folder");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ACAQRun run = ACAQRun.loadFromFolder(fileChooser.getSelectedFile().toPath());
                ACAQWorkbenchWindow window = openProjectInThisOrNewWindow("Open ACAQ output", run.getProject());
                if (window == null)
                    return;
                window.projectSavePath = fileChooser.getSelectedFile().toPath().resolve("parameters.json");
                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.setTitle(window.projectSavePath.toString());

                // Create a new tab
                window.getProjectUI().getDocumentTabPane().addTab("Run",
                        UIUtils.getIconFromResources("run.png"),
                        new ACAQResultUI(window.projectUI, run),
                        DocumentTabPane.CloseMode.withAskOnCloseButton,
                        true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void saveProjectAs(boolean avoidDialog) {
        Path savePath = null;
        if (avoidDialog && projectSavePath != null)
            savePath = projectSavePath;
        if (savePath == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Save project (*.json)");
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                savePath = fileChooser.getSelectedFile().toPath();
            } else {
                return;
            }
        }

        try {
            getProject().saveProject(savePath);
            setTitle(savePath.toString());
            projectSavePath = savePath;
            projectUI.sendStatusBarText("Saved project to " + savePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param messageTitle
     * @param project
     * @return
     */
    private ACAQWorkbenchWindow openProjectInThisOrNewWindow(String messageTitle, ACAQProject project) {
        switch (UIUtils.askOpenInCurrentWindow(this, messageTitle)) {
            case JOptionPane.YES_OPTION:
                loadProject(project);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(command, project);
        }
        return null;
    }

    public ACAQGUICommand getCommand() {
        return command;
    }

    public ACAQProject getProject() {
        return project;
    }

    public ACAQWorkbenchUI getProjectUI() {
        return projectUI;
    }

    public Path getProjectSavePath() {
        return projectSavePath;
    }

    public static ACAQWorkbenchWindow newWindow(ACAQGUICommand command, ACAQProject project) {
        ACAQWorkbenchWindow frame = new ACAQWorkbenchWindow(command, project);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        return frame;
    }
}
