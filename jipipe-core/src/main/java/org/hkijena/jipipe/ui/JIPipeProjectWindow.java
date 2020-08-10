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
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectMetadata;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.SplashScreen;
import org.hkijena.jipipe.ui.project.JIPipeProjectTabMetadata;
import org.hkijena.jipipe.ui.project.JIPipeTemplateSelectionDialog;
import org.hkijena.jipipe.ui.project.UnsatisfiedDependenciesDialog;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultUI;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Window that holds an {@link JIPipeProjectWorkbench} instance
 */
public class JIPipeProjectWindow extends JFrame {

    private static Set<JIPipeProjectWindow> OPEN_WINDOWS = new HashSet<>();
    private Context context;
    private JIPipeProject project;
    private JIPipeProjectWorkbench projectUI;
    private Path projectSavePath;

    /**
     * @param context          context
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     */
    public JIPipeProjectWindow(Context context, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        SwingUtilities.invokeLater(() -> SplashScreen.getInstance().hideSplash());
        this.context = context;
        OPEN_WINDOWS.add(this);
        initialize();
        loadProject(project, showIntroduction, isNewProject);
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout(8, 8));
        super.setTitle("JIPipe");
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        UIUtils.setToAskOnClose(this, "Do you really want to close JIPipe?", "Close window");
    }

    @Override
    public void dispose() {
        OPEN_WINDOWS.remove(this);
        super.dispose();
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("JIPipe - " + title);
    }

    /**
     * Loads a project into the window
     *
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     */
    public void loadProject(JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        this.project = project;
        this.projectUI = new JIPipeProjectWorkbench(this, context, project, showIntroduction, isNewProject);
        setContentPane(projectUI);
    }

    /**
     * Creates a new project.
     * Asks the user if it should replace the currently displayed project
     */
    public void newProject() {
        JIPipeProject project = getDefaultTemplateProject();
        JIPipeProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true);
        if (window == null)
            return;
        window.projectSavePath = null;
        window.setTitle("New project");
        window.getProjectUI().sendStatusBarText("Created new project");
    }

    /**
     * Creates a new project from template
     */
    public void newProjectFromTemplate() {
        JIPipeTemplateSelectionDialog dialog = new JIPipeTemplateSelectionDialog(this);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if(dialog.getSelectedTemplate() != null) {
            try {
                JIPipeProject project = dialog.getSelectedTemplate().load();
                JIPipeProjectWindow window = openProjectInThisOrNewWindow("New project", project, true, true);
                if (window == null)
                    return;
                window.projectSavePath = null;
                window.setTitle("New project");
                window.getProjectUI().sendStatusBarText("Created new project");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a new project instance based on the current template selection
     * @return the project
     */
    public static JIPipeProject getDefaultTemplateProject() {
        JIPipeProject project = null;
        if(ProjectsSettings.getInstance().getProjectTemplate().getValue() != null) {
            try {
                project = ProjectsSettings.getInstance().getProjectTemplate().getValue().load();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(project == null) {
            project = new JIPipeProject();
        }
        return project;
    }

    /**
     * Opens a project from a file or folder
     * Asks the user if it should replace the currently displayed project
     *
     * @param path JSON project file or result folder
     */
    public void openProject(Path path) {
        if (Files.isRegularFile(path)) {
            JIPipeValidityReport report = new JIPipeValidityReport();
            try {
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
                Set<JIPipeDependency> dependencySet = JIPipeProject.loadDependenciesFromJson(jsonData);

                JIPipeProjectMetadata metadata = JIPipeProject.loadMetadataFromJson(jsonData);

                Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites = new HashSet<>();
                if(JIPipeDefaultRegistry.getInstance().getImageJPlugins() != null) {
                    // Populate
                    for (JIPipeDependency dependency : dependencySet) {
                        missingUpdateSites.addAll(dependency.getImageJUpdateSiteDependencies());
                    }
                    missingUpdateSites.addAll(metadata.getUpdateSiteDependencies());
                    // Remove existing
                    for (UpdateSite updateSite : JIPipeDefaultRegistry.getInstance().getImageJPlugins().getUpdateSites(true)) {
                        if(updateSite.isActive()) {
                            missingUpdateSites.removeIf(site -> Objects.equals(site.getName(), updateSite.getName()));
                        }
                    }
                }
                Set<JIPipeDependency> missingDependencies = JIPipeDependency.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty() || !missingUpdateSites.isEmpty()) {
                    if (!UnsatisfiedDependenciesDialog.showDialog(getProjectUI(), path, missingDependencies, missingUpdateSites))
                        return;
                }

                JIPipeProject project = new JIPipeProject();
                project.fromJson(jsonData, report);
                project.setWorkDirectory(path.getParent());
                JIPipeProjectWindow window = openProjectInThisOrNewWindow("Open project", project, false, false);
                if (window == null)
                    return;
                window.projectSavePath = path;
                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.setTitle(window.projectSavePath.toString());
                ProjectsSettings.getInstance().addRecentProject(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(!report.isValid()) {
                UIUtils.openValidityReportDialog(this, report, false);
            }
        } else if (Files.isDirectory(path)) {
            JIPipeValidityReport report = new JIPipeValidityReport();
            try {
                Path parameterFilePath = path.resolve("project.jip");
                JsonNode jsonData = JsonUtils.getObjectMapper().readValue(parameterFilePath.toFile(), JsonNode.class);
                Set<JIPipeDependency> dependencySet = JIPipeProject.loadDependenciesFromJson(jsonData);
                Set<JIPipeDependency> missingDependencies = JIPipeDependency.findUnsatisfiedDependencies(dependencySet);
                if (!missingDependencies.isEmpty()) {
                    if (!UnsatisfiedDependenciesDialog.showDialog(getProjectUI(), path, missingDependencies, Collections.emptySet()))
                        return;
                }

                JIPipeRun run = JIPipeRun.loadFromFolder(path, report);
                run.getProject().setWorkDirectory(path);
                JIPipeProjectWindow window = openProjectInThisOrNewWindow("Open JIPipe output", run.getProject(), false, false);
                if (window == null)
                    return;
                window.projectSavePath = path.resolve("project.jip");
                window.getProjectUI().sendStatusBarText("Opened project from " + window.projectSavePath);
                window.setTitle(window.projectSavePath.toString());

                // Create a new tab
                window.getProjectUI().getDocumentTabPane().addTab("Run",
                        UIUtils.getIconFromResources("actions/run-build.png"),
                        new JIPipeResultUI(window.projectUI, run),
                        DocumentTabPane.CloseMode.withAskOnCloseButton,
                        true);
                ProjectsSettings.getInstance().addRecentProject(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(!report.isValid()) {
                UIUtils.openValidityReportDialog(this, report, false);
            }
        }
    }

    /**
     * Opens a file chooser where the user can select a project file
     */
    public void openProject() {
        Path file = FileChooserSettings.openFile(this, FileChooserSettings.KEY_PROJECT, "Open JIPipe project (*.jip)", UIUtils.EXTENSION_FILTER_JIP);
        if (file != null) {
            openProject(file);
        }
    }

    /**
     * Opens a file chooser where the user can select a result folder
     */
    public void openProjectAndOutput() {
        Path file = FileChooserSettings.openDirectory(this, FileChooserSettings.KEY_PROJECT, "Open JIPipe output folder");
        if (file != null) {
            openProject(file);
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
            savePath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Save JIPipe project (*.jip)", UIUtils.EXTENSION_FILTER_JIP);
            if (savePath == null)
                return;
        }

        try {
            Path tempFile = Files.createTempFile(savePath.getParent(), savePath.getFileName().toString(), ".part");
            getProject().setWorkDirectory(savePath.getParent());
            getProject().getAdditionalMetadata().put(JIPipeProjectTabMetadata.METADATA_KEY, new JIPipeProjectTabMetadata(getProjectUI()));
            getProject().saveProject(tempFile);

            // Check if the saved project can be loaded
            JIPipeProject.loadProject(tempFile, new JIPipeValidityReport());

            // Overwrite the target file
            if (Files.exists(savePath))
                Files.delete(savePath);
            Files.copy(tempFile, savePath);

            // Everything OK, now set the title
            setTitle(savePath.toString());
            projectSavePath = savePath;
            projectUI.sendStatusBarText("Saved project to " + savePath);
            ProjectsSettings.getInstance().addRecentProject(savePath);

            // Remove tmp file
            Files.delete(tempFile);
        } catch (IOException e) {
            UIUtils.openErrorDialog(this, new UserFriendlyRuntimeException(e,
                    "Error during saving!",
                    "While saving the project into '" + savePath + "'. Any existing file was not changed or overwritten.",
                    "The issue cannot be determined. Please contact the JIPipe authors.",
                    "Please check if you have write access to the temporary directory and the target directory. " +
                            "If this is the case, please contact the JIPipe authors."));
        }
    }

    /**
     * @param messageTitle     Description of the project source
     * @param project          The project
     * @param showIntroduction whether to show the introduction
     * @param isNewProject     if the project is an empty project
     * @return The window that holds the project
     */
    private JIPipeProjectWindow openProjectInThisOrNewWindow(String messageTitle, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        switch (UIUtils.askOpenInCurrentWindow(this, messageTitle)) {
            case JOptionPane.YES_OPTION:
                loadProject(project, false, isNewProject);
                return this;
            case JOptionPane.NO_OPTION:
                return newWindow(context, project, showIntroduction, isNewProject);
        }
        return null;
    }

    /**
     * @return GUI command
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return The current project
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * @return The current project UI
     */
    public JIPipeProjectWorkbench getProjectUI() {
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
     * @param context          context
     * @param project          The project
     * @param showIntroduction show an introduction
     * @param isNewProject     if the project is a new empty project
     * @return The window
     */
    public static JIPipeProjectWindow newWindow(Context context, JIPipeProject project, boolean showIntroduction, boolean isNewProject) {
        JIPipeProjectWindow frame = new JIPipeProjectWindow(context, project, showIntroduction, isNewProject);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
//        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        return frame;
    }

    /**
     * @return All open project windows
     */
    public static Set<JIPipeProjectWindow> getOpenWindows() {
        return Collections.unmodifiableSet(OPEN_WINDOWS);
    }
}
