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

package org.hkijena.jipipe.desktop.app.project.templatedownloader;

import org.apache.commons.io.FilenameUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.settings.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
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

public class JIPipeDesktopProjectTemplateDownloaderRun extends AbstractJIPipeRunnable {

    private final JIPipeDesktopWorkbench desktopWorkbench;
    private final List<JIPipeDesktopProjectTemplateDownloaderPackage> availablePackages = new ArrayList<>();
    private Set<JIPipeDesktopProjectTemplateDownloaderPackage> targetPackages = new HashSet<>();


    public JIPipeDesktopProjectTemplateDownloaderRun(JIPipeDesktopWorkbench desktopWorkbench) {
        this.desktopWorkbench = desktopWorkbench;
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    @Override
    public String getTaskLabel() {
        return "Download project templates";
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
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
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.log("Downloading selected templates ...");

        // Find and create output directory
        Path outputDir = PathUtils.getJIPipeUserDir().resolve("templates");
        if (!Files.isDirectory(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Find existing files
        Set<String> existing = Files.list(outputDir).map(path -> FilenameUtils.removeExtension(path.getFileName().toString())).collect(Collectors.toSet());
        for (JIPipeDesktopProjectTemplateDownloaderPackage targetPackage : targetPackages) {
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
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.log("Waiting for user input ...");
        try {
            SwingUtilities.invokeAndWait(this::runSetupDialog);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        progressInfo.log("Setup complete.");
        progressInfo.setProgress(2);
    }

    public List<JIPipeDesktopProjectTemplateDownloaderPackage> getAvailablePackages() {
        return Collections.unmodifiableList(availablePackages);
    }

    private void loadAvailablePackages(JIPipeProgressInfo progressInfo) {
        StringList repositories = JIPipeProjectDefaultsApplicationSettings.getInstance().getProjectTemplateDownloadRepositories();

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
            Path outputFile = JIPipeRuntimeApplicationSettings.getTemporaryFile("repository", ".json");
            try {
                WebUtils.download(new URL(repositoryURL), outputFile, "Download repository", repositoryProgress);
            } catch (MalformedURLException e) {
                repositoryProgress.log(e.toString());
                repositoryProgress.log(e.getMessage());
                repositoryProgress.log("-> Skipping repository " + repositoryURL + ". Please check the URL!");
            }

            // Import the repository
            try {
                JIPipeDesktopProjectTemplateDownloaderRepository repository = JsonUtils.getObjectMapper().readerFor(JIPipeDesktopProjectTemplateDownloaderRepository.class).readValue(outputFile.toFile());
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
        JIPipeDesktopProjectTemplateDownloaderUI dialog = new JIPipeDesktopProjectTemplateDownloaderUI(this);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getDesktopWorkbench().getWindow());
        dialog.setVisible(true);

        targetPackages = dialog.getTargetPackages();
        if (targetPackages.isEmpty()) {
            getProgressInfo().log("Cancelled by user.");
        }
    }

}
