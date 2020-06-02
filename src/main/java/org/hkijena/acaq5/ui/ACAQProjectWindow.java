package org.hkijena.acaq5.ui;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQGUICommand;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.extensions.settings.ProjectsSettings;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.project.UnsatisfiedDependenciesDialog;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Window that holds an {@link ACAQProjectWorkbench} instance
 */
public class ACAQProjectWindow extends JFrame {

    private static Set<ACAQProjectWindow> OPEN_WINDOWS = new HashSet<>();
    private ACAQGUICommand command;
    private ACAQProject project;
    private ACAQProjectWorkbench projectUI;
    private Path projectSavePath;

    /**
     * @param command GUI command
     * @param project The project
     */
    public ACAQProjectWindow(ACAQGUICommand command, ACAQProject project) {
        this.command = command;
        OPEN_WINDOWS.add(this);
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
    public void dispose() {
        OPEN_WINDOWS.remove(this);
        super.dispose();
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("ACAQ5 - " + title);
    }

    /**
     * Loads a project into the window
     *
     * @param project The project
     */
    public void loadProject(ACAQProject project) {
        this.project = project;
        this.projectUI = new ACAQProjectWorkbench(this, command, project);
        setContentPane(projectUI);
    }

    /**
     * Creates a new project.
     * Asks the user if it should replace the currently displayed project
     */
    public void newProject() {
        ACAQProject project = new ACAQProject();
        ACAQProjectWindow window = openProjectInThisOrNewWindow("New project", project);
        if (window == null)
            return;
        window.projectSavePath = null;
        window.setTitle("New project");
        window.getProjectUI().sendStatusBarText("Created new project");
    }

    /**
     * Opens a project from a file or folder
     * Asks the user if it should replace the currently displayed project
     *
     * @param path JSON project file or result folder
     */
    public void openProject(Path path) {
        if (Files.isRegularFile(path)) {
            try {
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
                Set<ACAQDependency> dependencySet = ACAQProject.loadDependenciesFromJson(jsonData);
                Set<ACAQDependency> missingDependencies = ACAQDependency.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty()) {
                    if (!UnsatisfiedDependenciesDialog.showDialog(this, path, missingDependencies))
                        return;
                }

                ACAQProject project = ACAQProject.loadProject(jsonData);
                project.setWorkDirectory(path.getParent());
                ACAQProjectWindow window = openProjectInThisOrNewWindow("Open project", project);
                if (window == null)
                    return;
                window.projectSavePath = path;
                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.setTitle(window.projectSavePath.toString());
                ProjectsSettings.getInstance().addRecentProject(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (Files.isDirectory(path)) {
            try {
                Path parameterFilePath = path.resolve("parameters.json");
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(parameterFilePath.toFile(), JsonNode.class);
                Set<ACAQDependency> dependencySet = ACAQProject.loadDependenciesFromJson(jsonData);
                Set<ACAQDependency> missingDependencies = ACAQDependency.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty()) {
                    if (!UnsatisfiedDependenciesDialog.showDialog(this, path, missingDependencies))
                        return;
                }

                ACAQRun run = ACAQRun.loadFromFolder(path);
                run.getProject().setWorkDirectory(path);
                ACAQProjectWindow window = openProjectInThisOrNewWindow("Open ACAQ output", run.getProject());
                if (window == null)
                    return;
                window.projectSavePath = path.resolve("parameters.json");
                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.setTitle(window.projectSavePath.toString());

                // Create a new tab
                window.getProjectUI().getDocumentTabPane().addTab("Run",
                        UIUtils.getIconFromResources("run.png"),
                        new ACAQResultUI(window.projectUI, run),
                        DocumentTabPane.CloseMode.withAskOnCloseButton,
                        true);
                ProjectsSettings.getInstance().addRecentProject(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Opens a file chooser where the user can select a project file
     */
    public void openProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Open project (*.json)");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openProject(fileChooser.getSelectedFile().toPath());
        }
    }

    /**
     * Opens a file chooser where the user can select a result folder
     */
    public void openProjectAndOutput() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Open project output folder");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openProject(fileChooser.getSelectedFile().toPath());
        }
    }

    /**
     * Saves the project
     *
     * @param avoidDialog If true, the project is stored in the last known valid output location if possible
     */
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
            getProject().setWorkDirectory(savePath.getParent());
            getProject().saveProject(savePath);
            setTitle(savePath.toString());
            projectSavePath = savePath;
            projectUI.sendStatusBarText("Saved project to " + savePath);
            ProjectsSettings.getInstance().addRecentProject(savePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param messageTitle Description of the project source
     * @param project      The project
     * @return The window that holds the project
     */
    private ACAQProjectWindow openProjectInThisOrNewWindow(String messageTitle, ACAQProject project) {
        switch (UIUtils.askOpenInCurrentWindow(this, messageTitle)) {
            case JOptionPane.YES_OPTION:
                loadProject(project);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(command, project);
        }
        return null;
    }

    /**
     * @return GUI command
     */
    public ACAQGUICommand getCommand() {
        return command;
    }

    /**
     * @return The current project
     */
    public ACAQProject getProject() {
        return project;
    }

    /**
     * @return The current project UI
     */
    public ACAQProjectWorkbench getProjectUI() {
        return projectUI;
    }

    /**
     * @return Last known project save path
     */
    public Path getProjectSavePath() {
        return projectSavePath;
    }

    /**
     * Creates a new window
     *
     * @param command GUI command
     * @param project The project
     * @return The window
     */
    public static ACAQProjectWindow newWindow(ACAQGUICommand command, ACAQProject project) {
        ACAQProjectWindow frame = new ACAQProjectWindow(command, project);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        return frame;
    }

    /**
     * @return All open project windows
     */
    public static Set<ACAQProjectWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }
}
