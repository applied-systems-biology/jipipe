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

import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.extensions.jsonextensionloader.JsonExtensionLoaderExtension;
import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.extensions.settings.ProjectsSettings;
import org.hkijena.acaq5.ui.project.UnsatisfiedDependenciesDialog;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Window that displays {@link ACAQJsonExtension} UIs
 */
public class ACAQJsonExtensionWindow extends JFrame {

    private static Set<ACAQJsonExtensionWindow> OPEN_WINDOWS = new HashSet<>();
    private Context context;
    private ACAQJsonExtension project;
    private ACAQJsonExtensionWorkbench projectUI;
    private Path projectSavePath;

    /**
     * Creates a new instance
     *
     * @param context The command that issued the UI
     * @param project The project
     */
    public ACAQJsonExtensionWindow(Context context, ACAQJsonExtension project) {
        OPEN_WINDOWS.add(this);
        this.context = context;
        initialize();
        loadProject(project);
        if (project.getJsonFilePath() != null)
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
        UIUtils.setToAskOnClose(this, "Do you really want to close this ACAQ5 extension builder?", "Close window");
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("ACAQ5 extension builder - " + title);
    }

    /**
     * Loads a project into the window and replaces the current project
     *
     * @param project The project
     */
    public void loadProject(ACAQJsonExtension project) {
        this.project = project;
        this.projectUI = new ACAQJsonExtensionWorkbench(this, context, project);
        setContentPane(projectUI);
    }

    /**
     * Creates a new project and asks the user if it should be opened in this or a new window
     */
    public void newProject() {
        ACAQJsonExtension project = new ACAQJsonExtension();
        ACAQJsonExtensionWindow window = openProjectInThisOrNewWindow("New extension", project);
        if (window == null)
            return;
        window.projectSavePath = null;
        window.setTitle("New extension");
        window.getProjectUI().sendStatusBarText("Created new extension");
    }

    /**
     * Opens a project from a file
     * Asks the user if it should be opened in this or a new window
     *
     * @param path JSON file path
     */
    public void openProject(Path path) {
        try {
            JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
            Set<ACAQDependency> dependencySet = ACAQProject.loadDependenciesFromJson(jsonData);
            Set<ACAQDependency> missingDependencies = ACAQDependency.findUnsatisfiedDependencies(dependencySet);
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
            ProjectsSettings.getInstance().addRecentJsonExtension(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Displays a file chooser where the user can open a project.
     * Asks the user if it should be opened in this or a new window.
     */
    public void openProject() {
        Path file = FileChooserSettings.openFile(this, FileChooserSettings.KEY_PROJECT, "Open ACAQ5 JSON extension (*.json)");
        if (file != null) {
            openProject(file);
        }
    }

    /**
     * Saves the project
     *
     * @param avoidDialog If true, the project is silently written if a save path was set
     */
    public void saveProjectAs(boolean avoidDialog) {
        Path savePath = null;
        if (avoidDialog && projectSavePath != null)
            savePath = projectSavePath;
        if (savePath == null) {
            savePath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Save ACAQ5 JSON extension (*.json)");
            if (savePath == null)
                return;
        }

        try {
            getProject().saveProject(savePath);
            setTitle(savePath.toString());
            projectSavePath = savePath;
            projectUI.sendStatusBarText("Saved ACAQ5 JSON extension to " + savePath);
            ProjectsSettings.getInstance().addRecentJsonExtension(savePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Asks the user if a project should be opened in this or a new window
     *
     * @param messageTitle How the project was loaded
     * @param project      The project
     * @return The window that hosts the porject UI
     */
    private ACAQJsonExtensionWindow openProjectInThisOrNewWindow(String messageTitle, ACAQJsonExtension project) {
        switch (UIUtils.askOpenInCurrentWindow(this, messageTitle)) {
            case JOptionPane.YES_OPTION:
                loadProject(project);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(context, project);
        }
        return null;
    }

    /**
     * @return The command that issued the GUI
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return The project
     */
    public ACAQJsonExtension getProject() {
        return project;
    }

    /**
     * @return The current UI
     */
    public ACAQJsonExtensionWorkbench getProjectUI() {
        return projectUI;
    }

    /**
     * @return The path where the project was loaded from or saved last
     */
    public Path getProjectSavePath() {
        return projectSavePath;
    }

    /**
     * Opens a new window
     *
     * @param context The context
     * @param project The project
     * @return The window
     */
    public static ACAQJsonExtensionWindow newWindow(Context context, ACAQJsonExtension project) {
        ACAQJsonExtensionWindow frame = new ACAQJsonExtensionWindow(context, project);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        return frame;
    }

    /**
     * Allows the user to select files to install
     *
     * @param parent The parent component
     */
    public static void installExtensions(Component parent) {
        List<Path> files = FileChooserSettings.openFiles(parent, FileChooserSettings.KEY_PROJECT, "Open ACAQ5 JSON extension (*.json)");
        for (Path selectedFile : files) {
            installExtensionFromFile(parent, selectedFile, true);
        }
        if(!files.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "The extensions were copied into the plugin directory. Please check if any errors occurred. " +
                    "We recommend to restart ImageJ, " +
                    "especially if you updated an existing extension.", "Extensions copied", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Loads a project and installs it
     *  @param parent   The parent component
     * @param filePath The file path
     * @param silent
     */
    public static void installExtensionFromFile(Component parent, Path filePath, boolean silent) {
        try {
            JsonNode jsonData = JsonUtils.getObjectMapper().readValue(filePath.toFile(), JsonNode.class);
            Set<ACAQDependency> dependencySet = ACAQProject.loadDependenciesFromJson(jsonData);
            Set<ACAQDependency> missingDependencies = ACAQDependency.findUnsatisfiedDependencies(dependencySet);
            if (!missingDependencies.isEmpty()) {
                if (!UnsatisfiedDependenciesDialog.showDialog(parent, filePath, missingDependencies))
                    return;
            }

            ACAQJsonExtension project = ACAQJsonExtension.loadProject(jsonData);
            installExtension(parent, project, true);

            if(!silent) {
                JOptionPane.showMessageDialog(parent, "The extension was installed. We recommend to restart ImageJ, " +
                        "especially if you updated an existing extension.", "Extension installed", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Installs a loaded project
     *  @param parent    The parent component
     * @param extension The extension
     * @param silent whether to show a dialog confirming the installation
     */
    public static void installExtension(Component parent, ACAQJsonExtension extension, boolean silent) {
        boolean alreadyExists = ACAQDefaultRegistry.getInstance().getRegisteredExtensionIds().contains(extension.getDependencyId());
        if (alreadyExists) {
            if (JOptionPane.showConfirmDialog(parent, "There already exists an extension with ID '"
                    + extension.getDependencyId() + "'. Do you want to install this extension anyways?", "Install", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        Path pluginFolder = JsonExtensionLoaderExtension.getPluginDirectory();
        if (!Files.exists(pluginFolder)) {
            try {
                Files.createDirectories(pluginFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Suggest a path that the user can later change
        Path suggestedPath = pluginFolder.resolve(StringUtils.makeFilesystemCompatible(extension.getDependencyId()) + ".acaq5.json");
        if (alreadyExists) {
            ACAQDependency dependency = ACAQDefaultRegistry.getInstance().findExtensionById(extension.getDependencyId());
            if (dependency instanceof ACAQJsonExtension) {
                Path jsonExtensionPath = ((ACAQJsonExtension) dependency).getJsonFilePath();
                if (jsonExtensionPath != null && Files.exists(jsonExtensionPath)) {
                    suggestedPath = jsonExtensionPath;
                }
            }
        }

        // Let the user select a file path
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Install extension '" + extension.getMetadata().getName() + "'");
        fileChooser.setCurrentDirectory(pluginFolder.toFile());
        fileChooser.setSelectedFile(suggestedPath.toFile());
        if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
            return;

        Path selectedPath = fileChooser.getSelectedFile().toPath();
        try {
            extension.saveProject(selectedPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Install extension by reloading (to be sure)
        try {
            ACAQJsonExtension loadedExtension = JsonUtils.getObjectMapper().readValue(selectedPath.toFile(), ACAQJsonExtension.class);
            ACAQDefaultRegistry.getInstance().register(loadedExtension);
            if(!silent) {
                JOptionPane.showMessageDialog(parent, "The extension was installed. We recommend to restart ImageJ, " +
                        "especially if you updated an existing extension.", "Extension installed", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            IJ.handleException(e);
        }
    }

    /**
     * @return The list of open windows
     */
    public static Set<ACAQJsonExtensionWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }
}
