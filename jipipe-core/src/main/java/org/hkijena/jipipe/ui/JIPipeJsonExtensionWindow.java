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

import com.fasterxml.jackson.databind.JsonNode;
import ij.IJ;
import net.imagej.ui.swing.updater.ProgressDialog;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.jsonextensionloader.JsonExtensionLoaderExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.events.WindowClosedEvent;
import org.hkijena.jipipe.ui.events.WindowClosedEventEmitter;
import org.hkijena.jipipe.ui.events.WindowOpenedEvent;
import org.hkijena.jipipe.ui.events.WindowOpenedEventEmitter;
import org.hkijena.jipipe.ui.ijupdater.MissingRegistrationUpdateSiteResolver;
import org.hkijena.jipipe.ui.project.MissingProjectDependenciesDialog;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Window that displays {@link JIPipeJsonExtension} UIs
 */
public class JIPipeJsonExtensionWindow extends JFrame {


    public static final WindowOpenedEventEmitter WINDOW_OPENED_EVENT_EMITTER = new WindowOpenedEventEmitter();
    public static final WindowClosedEventEmitter WINDOW_CLOSED_EVENT_EMITTER = new WindowClosedEventEmitter();
    private static final Set<JIPipeJsonExtensionWindow> OPEN_WINDOWS = new HashSet<>();
    private Context context;
    private JIPipeJsonExtension project;
    private JIPipeJsonExtensionWorkbench projectUI;
    private Path projectSavePath;

    /**
     * Creates a new instance
     *
     * @param context          The command that issued the UI
     * @param project          The project
     * @param showIntroduction show intro
     */
    public JIPipeJsonExtensionWindow(Context context, JIPipeJsonExtension project, boolean showIntroduction) {
        OPEN_WINDOWS.add(this);
        WINDOW_OPENED_EVENT_EMITTER.emit(new WindowOpenedEvent(this));
        this.context = context;
        initialize();
        loadProject(project, showIntroduction);
        if (project.getJsonFilePath() != null)
            setTitle(project.getJsonFilePath().toString());
        else
            setTitle("New project");
    }

    /**
     * Opens a new window
     *
     * @param context          The context
     * @param project          The project
     * @param showIntroduction whether to show introduction
     * @return The window
     */
    public static JIPipeJsonExtensionWindow newWindow(Context context, JIPipeJsonExtension project, boolean showIntroduction) {
        JIPipeJsonExtensionWindow frame = new JIPipeJsonExtensionWindow(context, project, showIntroduction);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        return frame;
    }

    /**
     * Allows the user to select files to install
     *
     * @param workbench The parent component
     */
    public static void installExtensions(JIPipeWorkbench workbench) {
        List<Path> files = FileChooserSettings.openFiles(workbench.getWindow(), FileChooserSettings.LastDirectoryKey.Projects, "Open JIPipe JSON extension (*.jipe)");
        for (Path selectedFile : files) {
            installExtensionFromFile(workbench, selectedFile, true, false);
        }
        if (ExtensionSettings.getInstance().isValidateImageJDependencies()) {
            checkExtensionDependencies(workbench);
        }
        if (!files.isEmpty()) {
            JOptionPane.showMessageDialog(workbench.getWindow(), "The extensions were copied into the plugin directory. Please check if any errors occurred. " +
                    "We recommend to restart ImageJ, " +
                    "especially if you updated an existing extension.", "Extensions copied", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Loads a project and installs it
     *
     * @param workbench         The parent component
     * @param filePath          The file path
     * @param silent            if not dialogs should be  shown
     * @param checkDependencies whether to check dependencies
     */
    public static void installExtensionFromFile(JIPipeWorkbench workbench, Path filePath, boolean silent, boolean checkDependencies) {
        try {
            JsonNode jsonData = JsonUtils.getObjectMapper().readValue(filePath.toFile(), JsonNode.class);
            Set<JIPipeDependency> dependencySet = JIPipeProject.loadDependenciesFromJson(jsonData);
            Set<JIPipeDependency> missingDependencies = JIPipeExtensionRegistry.findUnsatisfiedDependencies(dependencySet);
            if (!missingDependencies.isEmpty()) {
                if (!MissingProjectDependenciesDialog.showDialog(workbench, filePath, missingDependencies, Collections.emptySet()))
                    return;
            }

            JIPipeJsonExtension project = JIPipeJsonExtension.loadProject(jsonData);
            installExtension(workbench, project, true, checkDependencies);

            if (!silent) {
                JOptionPane.showMessageDialog(workbench.getWindow(), "The extension was installed. We recommend to restart ImageJ, " +
                        "especially if you updated an existing extension.", "Extension installed", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Installs a loaded project
     *
     * @param workbench         The parent component
     * @param extension         The extension
     * @param silent            whether to show a dialog confirming the installation
     * @param checkDependencies whether to check dependencies
     */
    public static void installExtension(JIPipeWorkbench workbench, JIPipeJsonExtension extension, boolean silent, boolean checkDependencies) {
        boolean alreadyExists = JIPipe.getInstance().getRegisteredExtensionIds().contains(extension.getDependencyId());
        if (alreadyExists) {
            if (JOptionPane.showConfirmDialog(workbench.getWindow(), "There already exists an extension with ID '"
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
        Path suggestedPath = pluginFolder.resolve(StringUtils.makeFilesystemCompatible(extension.getDependencyId()) + ".jipipe.json");
        if (alreadyExists) {
            JIPipeDependency dependency = JIPipe.getInstance().findExtensionById(extension.getDependencyId());
            if (dependency instanceof JIPipeJsonExtension) {
                Path jsonExtensionPath = ((JIPipeJsonExtension) dependency).getJsonFilePath();
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
        if (fileChooser.showSaveDialog(workbench.getWindow()) != JFileChooser.APPROVE_OPTION)
            return;

        Path selectedPath = fileChooser.getSelectedFile().toPath();
        try {
            extension.saveProject(selectedPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Install extension by reloading (to be sure)
        try {
            JIPipeJsonExtension loadedExtension = JsonUtils.getObjectMapper().readValue(selectedPath.toFile(), JIPipeJsonExtension.class);
            JIPipe.getInstance().register(loadedExtension, JIPipe.getInstance().getProgressInfo());
            if (!silent) {
                JOptionPane.showMessageDialog(workbench.getWindow(), "The extension was installed. We recommend to restart ImageJ, " +
                        "especially if you updated an existing extension.", "Extension installed", JOptionPane.INFORMATION_MESSAGE);
            }
            if (checkDependencies && ExtensionSettings.getInstance().isValidateImageJDependencies()) {
                checkExtensionDependencies(workbench);
            }
        } catch (Exception e) {
            IJ.handleException(e);
        }
    }

    public static void checkExtensionDependencies(JIPipeWorkbench workbench) {
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        JIPipe.getInstance().checkUpdateSites(issues,
                JIPipe.getInstance().getRegisteredExtensions(),
                new ProgressDialog((Frame) workbench.getWindow(), "Checking dependencies ..."), JIPipe.getInstance().getProgressInfo());
        if (!issues.getMissingImageJSites().isEmpty()) {
            MissingRegistrationUpdateSiteResolver resolver = new MissingRegistrationUpdateSiteResolver(workbench.getContext(), issues);
            resolver.revalidate();
            resolver.repaint();
            resolver.setLocationRelativeTo(null);
            resolver.setVisible(true);
        }
    }

    /**
     * @return The list of open windows
     */
    public static Set<JIPipeJsonExtensionWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }

    @Override
    public void dispose() {
        OPEN_WINDOWS.remove(this);
        WINDOW_CLOSED_EVENT_EMITTER.emit(new WindowClosedEvent(this));
        super.dispose();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        super.setTitle("JIPipe extension builder");
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        UIUtils.setToAskOnClose(this, "Do you really want to close this JIPipe extension builder?", "Close window");
        if (GeneralUISettings.getInstance().isMaximizeWindows()) {
            SwingUtilities.invokeLater(() -> setExtendedState(getExtendedState() | MAXIMIZED_BOTH));
        }
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("JIPipe extension builder - " + title);
    }

    /**
     * Loads a project into the window and replaces the current project
     *
     * @param project          The project
     * @param showIntroduction show intro
     */
    public void loadProject(JIPipeJsonExtension project, boolean showIntroduction) {
        this.project = project;
        this.projectUI = new JIPipeJsonExtensionWorkbench(this, context, project, showIntroduction);
        setContentPane(projectUI);
    }

    /**
     * Creates a new project and asks the user if it should be opened in this or a new window
     *
     * @param showIntroduction show intro
     */
    public void newProject(boolean showIntroduction) {
        JIPipeJsonExtension project = new JIPipeJsonExtension();
        JIPipeJsonExtensionWindow window = openProjectInThisOrNewWindow("New extension", project, showIntroduction);
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
            Set<JIPipeDependency> dependencySet = JIPipeProject.loadDependenciesFromJson(jsonData);
            Set<JIPipeDependency> missingDependencies = JIPipeExtensionRegistry.findUnsatisfiedDependencies(dependencySet);

            // Check project version as well
            int projectFormat = 0;
            JsonNode projectFormatNode = jsonData.path("jipipe:project-format-version");
            if (!projectFormatNode.isMissingNode()) {
                try {
                    projectFormat = projectFormatNode.asInt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (projectFormat > JIPipeProject.CURRENT_PROJECT_FORMAT_VERSION) {
                int finalProjectFormat = projectFormat;
                missingDependencies.add(new JIPipeDependency() {
                    @Override
                    public JIPipeMetadata getMetadata() {
                        return new JIPipeMetadata() {
                            {
                                setTitle("Newer JIPipe version");
                                setDescription(new HTMLText("You might require a newer JIPipe version to load this"));
                                setWebsite("https://www.jipipe.org/");
                            }
                        };
                    }

                    @Override
                    public String getDependencyId() {
                        return "org.hkijena.jipipe";
                    }

                    @Override
                    public String getDependencyVersion() {
                        return "Project format " + finalProjectFormat;
                    }

                    @Override
                    public Path getDependencyLocation() {
                        return Paths.get("");
                    }

                    @Override
                    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {

                    }
                });
            }

            if (!missingDependencies.isEmpty()) {
                if (!MissingProjectDependenciesDialog.showDialog(getProjectUI(), path, missingDependencies, Collections.emptySet()))
                    return;
            }

            JIPipeJsonExtension project = JIPipeJsonExtension.loadProject(jsonData);
            JIPipeJsonExtensionWindow window = openProjectInThisOrNewWindow("Open project", project, false);
            if (window == null)
                return;
            window.projectSavePath = path;
            window.getProjectUI().sendStatusBarText("Opened JIPipe JSON extension from " + window.projectSavePath);
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
        Path file = FileChooserSettings.openFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Open JIPipe JSON extension (*.jipe)", UIUtils.EXTENSION_FILTER_JIPE);
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
            savePath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Save JIPipe JSON extension (*.jipe)", UIUtils.EXTENSION_FILTER_JIPE);
            if (savePath == null)
                return;
        }

        try {

            Path tempFile = Files.createTempFile(savePath.getParent(), savePath.getFileName().toString(), ".part");
            getProject().saveProject(tempFile);

            // Check if the saved project can be loaded
            JIPipeJsonExtension.loadProject(tempFile);

            // Overwrite the target file
            if (Files.exists(savePath))
                Files.delete(savePath);
            Files.copy(tempFile, savePath);

            // Everything OK, now set the title
            getProject().saveProject(savePath);
            setTitle(savePath.toString());
            projectSavePath = savePath;
            projectUI.sendStatusBarText("Saved JIPipe JSON extension to " + savePath);
            ProjectsSettings.getInstance().addRecentJsonExtension(savePath);

            // Remove tmp file
            Files.delete(tempFile);

        } catch (IOException e) {
            UIUtils.openErrorDialog(getProjectUI(), this, new JIPipeValidationRuntimeException(e,
                    "Error during saving!",
                    "While saving the project into '" + savePath + "'. Any existing file was not changed or overwritten. The issue cannot be determined. Please contact the JIPipe authors.",
                    "Please check if you have write access to the temporary directory and the target directory. " +
                            "If this is the case, please contact the JIPipe authors."));
        }
    }

    /**
     * Asks the user if a project should be opened in this or a new window
     *
     * @param messageTitle     How the project was loaded
     * @param project          The project
     * @param showIntroduction show intro
     * @return The window that hosts the porject UI
     */
    private JIPipeJsonExtensionWindow openProjectInThisOrNewWindow(String messageTitle, JIPipeJsonExtension project, boolean showIntroduction) {
        switch (UIUtils.askOpenInCurrentWindow(this, messageTitle)) {
            case JOptionPane.YES_OPTION:
                loadProject(project, showIntroduction);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(context, project, showIntroduction);
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
    public JIPipeJsonExtension getProject() {
        return project;
    }

    /**
     * @return The current UI
     */
    public JIPipeJsonExtensionWorkbench getProjectUI() {
        return projectUI;
    }

    /**
     * @return The path where the project was loaded from or saved last
     */
    public Path getProjectSavePath() {
        return projectSavePath;
    }
}
