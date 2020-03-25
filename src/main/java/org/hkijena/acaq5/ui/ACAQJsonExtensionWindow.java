package org.hkijena.acaq5.ui;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.ui.project.UnsatisfiedDependenciesDialog;
import org.hkijena.acaq5.ui.settings.ACAQApplicationSettings;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ACAQJsonExtensionWindow extends JFrame {

    private static Set<ACAQJsonExtensionWindow> OPEN_WINDOWS = new HashSet<>();
    private ACAQGUICommand command;
    private ACAQJsonExtension project;
    private ACAQJsonExtensionUI projectUI;
    private Path projectSavePath;

    public ACAQJsonExtensionWindow(ACAQGUICommand command, ACAQJsonExtension project) {
        OPEN_WINDOWS.add(this);
        this.command = command;
        initialize();
        loadProject(project);
        if(project.getJsonFilePath() != null)
            setTitle(project.getJsonFilePath().toString());
        else
            setTitle("New project");
    }

    @Override
    public void dispose() {
        OPEN_WINDOWS.remove(this);
        super.dispose();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout(8, 8));
        super.setTitle("ACAQ5 extension builder");
        setIconImage(UIUtils.getIconFromResources("acaq5-128.png").getImage());
        UIUtils.setToAskOnClose(this, "Do you really want to close ACAQ5?", "Close window");
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("ACAQ5 extension builder - " + title);
    }

    public void loadProject(ACAQJsonExtension project) {
        this.project = project;
        this.projectUI = new ACAQJsonExtensionUI(this, command, project);
        setContentPane(projectUI);
    }

    public void newProject() {
        ACAQJsonExtension project = new ACAQJsonExtension();
        ACAQJsonExtensionWindow window = openProjectInThisOrNewWindow("New extension", project);
        if (window == null)
            return;
        window.projectSavePath = null;
        window.setTitle("New extension");
        window.getProjectUI().sendStatusBarText("Created new extension");
    }

    public void openProject(Path path) {
        try {
            JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
            Set<ACAQDependency> dependencySet = ACAQProject.loadDependenciesFromJson(jsonData);
            Set<ACAQDependency> missingDependencies = ACAQDependency.findUnsatisfiedDepencencies(dependencySet);
            if (!missingDependencies.isEmpty()) {
                if (!UnsatisfiedDependenciesDialog.showDialog(this, path, missingDependencies))
                    return;
            }

            ACAQJsonExtension project = ACAQJsonExtension.loadProject(jsonData);
            ACAQJsonExtensionWindow window = openProjectInThisOrNewWindow("Open project", project);
            if (window == null)
                return;
            window.projectSavePath = path;
            window.getProjectUI().sendStatusBarText("Opened ACAQ5 JSON extension from " + window.projectSavePath);
            window.setTitle(window.projectSavePath.toString());
            ACAQApplicationSettings.getInstance().addRecentJsonExtension(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void openProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Open ACAQ5 JSON extension (*.json)");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openProject(fileChooser.getSelectedFile().toPath());
        }
    }

    public void saveProjectAs(boolean avoidDialog) {
        Path savePath = null;
        if (avoidDialog && projectSavePath != null)
            savePath = projectSavePath;
        if (savePath == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Save ACAQ5 JSON extension (*.json)");
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
            projectUI.sendStatusBarText("Saved ACAQ5 JSON extension to " + savePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param messageTitle
     * @param project
     * @return
     */
    private ACAQJsonExtensionWindow openProjectInThisOrNewWindow(String messageTitle, ACAQJsonExtension project) {
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

    public ACAQJsonExtension getProject() {
        return project;
    }

    public ACAQJsonExtensionUI getProjectUI() {
        return projectUI;
    }

    public Path getProjectSavePath() {
        return projectSavePath;
    }

    public static ACAQJsonExtensionWindow newWindow(ACAQGUICommand command, ACAQJsonExtension project) {
        ACAQJsonExtensionWindow frame = new ACAQJsonExtensionWindow(command, project);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        return frame;
    }

    /**
     * Allows the user to select files to install
     */
    public static void installExtensions() {

    }

    public static Set<ACAQJsonExtensionWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }
}
