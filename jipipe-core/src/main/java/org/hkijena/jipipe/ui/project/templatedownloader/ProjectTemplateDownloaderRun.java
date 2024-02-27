/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.project.templatedownloader;

import org.apache.commons.io.FilenameUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectTemplateDownloaderRun implements JIPipeRunnable {

    private final JIPipeWorkbench workbench;
    private final List<ProjectTemplateDownloaderPackage> availablePackages = new ArrayList<>();
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Set<ProjectTemplateDownloaderPackage> targetPackages = new HashSet<>();


    public ProjectTemplateDownloaderRun(JIPipeWorkbench workbench) {
        this.workbench = workbench;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Download project templates";
    }

    @Override
    public void run() {
        progressInfo.setProgress(0, 5);
        loadAvailablePackages(progressInfo.resolve("Load available packages"));
        progressInfo.setProgress(1);

        executeUserConfiguration();
        if (!targetPackages.isEmpty()) {
            try {
                executeArchiveDownload();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        progressInfo.setProgress(5);
    }

    private void executeArchiveDownload() throws IOException {
        progressInfo.log("Downloading selected templates ...");

        // Find and create output directory
        Path outputDir = PathUtils.getJIPipeUserDir().resolve("jipipe").resolve("templates");
        if (!Files.isDirectory(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Find existing files
        Set<String> existing = Files.list(outputDir).map(path -> FilenameUtils.removeExtension(path.getFileName().toString())).collect(Collectors.toSet());
        for (ProjectTemplateDownloaderPackage targetPackage : targetPackages) {
            progressInfo.log("The following URL will be downloaded: " + targetPackage.getUrl());
            String expectedFileName = FilenameUtils.removeExtension(targetPackage.getOutputFile());
            String newFileName = StringUtils.makeUniqueString(expectedFileName, "-", existing);
            newFileName = newFileName + targetPackage.getOutputFile().substring(expectedFileName.length());
            Path outputFile = outputDir.resolve(newFileName);
            progressInfo.log(" -> File will be downloaded to " + newFileName);
            try {
                WebUtils.download(new URL(targetPackage.getUrl()), outputFile, "Download template", progressInfo.resolve("Download template"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            // Register new template
            progressInfo.log("Download successful. Registering ...");
            JIPipe.getInstance().getProjectTemplateRegistry().register(outputFile);
        }
    }

    private void executeUserConfiguration() {
        progressInfo.log("Waiting for user input ...");
        try {
            SwingUtilities.invokeAndWait(this::runSetupDialog);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        progressInfo.log("Setup complete.");
        progressInfo.setProgress(2);
    }

    public List<ProjectTemplateDownloaderPackage> getAvailablePackages() {
        return Collections.unmodifiableList(availablePackages);
    }

    private void loadAvailablePackages(JIPipeProgressInfo progressInfo) {
        StringList repositories = ProjectsSettings.getInstance().getProjectTemplateDownloadRepositories();

        if (repositories.isEmpty()) {
            throw new UnsupportedOperationException("No repositories set! Cancelling.");
        }
        progressInfo.log("Following repositories will be contacted:");
        for (int i = 0; i < repositories.size(); i++) {
            String repository = repositories.get(i);
            progressInfo.log(" - [Repository " + i + "] " + repository);
        }
        for (int i = 0; i < repositories.size(); i++) {
            String repositoryURL = repositories.get(i);
            JIPipeProgressInfo repositoryProgress = progressInfo.resolve("Repository " + i);
            Path outputFile = RuntimeSettings.generateTempFile("repository", ".json");
            try {
                WebUtils.download(new URL(repositoryURL), outputFile, "Download repository", repositoryProgress);
            } catch (MalformedURLException e) {
                repositoryProgress.log(e.toString());
                repositoryProgress.log(e.getMessage());
                repositoryProgress.log("-> Skipping repository " + repositoryURL + ". Please check the URL!");
            }

            // Import the repository
            try {
                ProjectTemplateDownloaderRepository repository = JsonUtils.getObjectMapper().readerFor(ProjectTemplateDownloaderRepository.class).readValue(outputFile.toFile());
                availablePackages.addAll(repository.getFiles());
            } catch (IOException e) {
                repositoryProgress.log("Could not read repository " + outputFile);
                repositoryProgress.log(e.toString());
            }

            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                repositoryProgress.log("Could not clean up temporary file " + outputFile);
                repositoryProgress.log(e.toString());
            }

        }
    }

    private void runSetupDialog() {
        ProjectTemplateDownloaderDialog dialog = new ProjectTemplateDownloaderDialog(this);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getWorkbench().getWindow());
        dialog.setVisible(true);

        targetPackages = dialog.getTargetPackages();
        if (targetPackages.isEmpty()) {
            getProgressInfo().log("Cancelled by user.");
        }
    }

}
