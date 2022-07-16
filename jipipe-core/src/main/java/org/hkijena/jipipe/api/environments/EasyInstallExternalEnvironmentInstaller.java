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
 *
 */

package org.hkijena.jipipe.api.environments;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Environment installer that extracts a premade package of the environment and generates the appropriate environment
 * Supported are .tar.gz and .zip archives that should contain the hierarchy of files as expected in the environment installation directory.
 * The run() method will ensure that
 */
public abstract class EasyInstallExternalEnvironmentInstaller<T extends ExternalEnvironment> extends ExternalEnvironmentInstaller {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    private List<EasyInstallExternalEnvironmentInstallerPackage> availablePackages = new ArrayList<>();

    private EasyInstallExternalEnvironmentInstallerPackage targetPackage;

    private Path absoluteInstallationPath;

    private T generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public EasyInstallExternalEnvironmentInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    public EasyInstallExternalEnvironmentInstallerPackage getTargetPackage() {
        return targetPackage;
    }

    /**
     * Installation path relative to the ImageJ root
     * @return the installation path
     */
    public Path getRelativeInstallationPath() {
        return PathUtils.absoluteToImageJRelative(absoluteInstallationPath);
    }

    public Path getAbsoluteInstallationPath() {
        return absoluteInstallationPath;
    }

    @Override
    public ExternalEnvironment getInstalledEnvironment() {
        return generatedEnvironment;
    }

    @Override
    public void run() {
        progressInfo.setProgress(0,5);
        loadAvailablePackages(progressInfo.resolve("Load available packages"));
        progressInfo.setProgress(1);

        executeUserConfiguration();
        if(targetPackage != null && absoluteInstallationPath != null) {
            executeOutputDirectoryPreparation();
            executeArchiveDownload();
            executePostprocess();
            generatedEnvironment = generateEnvironment();

            if (getParameterAccess() != null && generatedEnvironment != null) {
                SwingUtilities.invokeLater(() -> {
                   writeEnvironmentToParameters(generatedEnvironment, getParameterAccess());
                });
            }
        }
        progressInfo.setProgress(5);
    }

    /**
     * Postprocessing steps (optional) that are executed after the extraction of the archive
     */
    protected void executePostprocess() {

    }

    /**
     * This method should write the environment to the parameter access
     * Will be executed on the Swing thread (via invokeLater)
     * @param environment the environment
     * @param parameterAccess the parameter
     */
    protected abstract void writeEnvironmentToParameters(T environment, JIPipeParameterAccess parameterAccess);

    /**
     * Generates the final environment. Has access to the target package (getTargetPackage()) and the installation path
     * @return the environment or null if there is an error
     */
    protected abstract T generateEnvironment();

    private void executeArchiveDownload() {
        progressInfo.log("Downloading archive ...");
        progressInfo.log("The following URL will be downloaded: " + targetPackage.getUrl());

        String extension;
        if(targetPackage.getUrl().contains(".tar.gz")) {
            extension = ".tar.gz";

        }
        else if(targetPackage.getUrl().contains(".tar.xz")) {
            extension = ".tar.xz";
        }
        else {
            String[] split = targetPackage.getUrl().split("\\.");
            extension = "." + split[split.length - 1];
        }
        progressInfo.log("Archive extension detected as " + extension);

        Path outputFile = RuntimeSettings.generateTempFile("repository", extension);
        try {
            WebUtils.download(new URL(targetPackage.getUrl()), outputFile, "Download package", progressInfo.resolve("Download package"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // Extract archive
        progressInfo.log("Extracting archive ...");
        if(extension.equals(".zip")) {
            try {
                ArchiveUtils.decompressZipFile(outputFile, absoluteInstallationPath, progressInfo.resolve("Extract package"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                ArchiveUtils.decompressTarGZ(outputFile, absoluteInstallationPath, progressInfo.resolve("Extract package"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Files.delete(outputFile);
        } catch (IOException e) {
            progressInfo.log("Could not clean up temporary file " + outputFile);
            progressInfo.log(e.toString());
        }
    }

    private void executeOutputDirectoryPreparation() {
        progressInfo.log("Environment will be installed to " + absoluteInstallationPath);
        progressInfo.log("Preparing installation directory");

        if (Files.exists(absoluteInstallationPath)) {
            progressInfo.log("Deleting old installation");
            progressInfo.log("Deleting: " + absoluteInstallationPath);
            PathUtils.deleteDirectoryRecursively(absoluteInstallationPath, progressInfo.resolve("Delete old installation"));
        }
        if (!Files.isDirectory(absoluteInstallationPath)) {
            try {
                Files.createDirectories(absoluteInstallationPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        progressInfo.setProgress(3);
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

    public List<EasyInstallExternalEnvironmentInstallerPackage> getAvailablePackages() {
        return Collections.unmodifiableList(availablePackages);
    }

    private void loadAvailablePackages(JIPipeProgressInfo progressInfo) {
        if(getRepositories().isEmpty()) {
            throw new UnsupportedOperationException("No repositories set! Cancelling.");
        }
        progressInfo.log("Following repositories will be contacted:");
        List<String> repositories = getRepositories();
        for (int i = 0; i < repositories.size(); i++) {
            String repository = repositories.get(i);
            progressInfo.log(" - [Repository " + i + "] "  + repository);
        }
        for (int i = 0; i < repositories.size(); i++) {
            String repository = repositories.get(i);
            JIPipeProgressInfo repositoryProgress = progressInfo.resolve("Repository " + i);
            Path outputFile = RuntimeSettings.generateTempFile("repository", ".json");
            try {
                WebUtils.download(new URL(repository), outputFile, "Download repository", repositoryProgress);
            } catch (MalformedURLException e) {
                repositoryProgress.log(e.toString());
                repositoryProgress.log(e.getMessage());
                repositoryProgress.log("-> Skipping repository " + repository + ". Please check the URL!");
            }

            // Import the repository
            JsonNode rootNode = JsonUtils.readFromFile(outputFile, JsonNode.class);
            for (JsonNode packageNodeEntry : ImmutableList.copyOf(rootNode.get("files").elements())) {
                EasyInstallExternalEnvironmentInstallerPackage availablePackage = new EasyInstallExternalEnvironmentInstallerPackage();
                availablePackage.setName(packageNodeEntry.get("name").textValue());
                availablePackage.setDescription(packageNodeEntry.get("description").textValue());
                availablePackage.setInstallDir(packageNodeEntry.get("install-dir").textValue());
                availablePackage.setUrl(packageNodeEntry.get("url").textValue());
                availablePackage.setAdditionalData(packageNodeEntry);
                JsonNode operatingSystemsNode = packageNodeEntry.path("operating-systems");
                if(operatingSystemsNode.isMissingNode()) {
                    availablePackage.setSupportsLinux(true);
                    availablePackage.setSupportsMacOS(true);
                    availablePackage.setSupportsWindows(true);
                }
                else {
                    Set<String> supported = new HashSet<>();
                    for (JsonNode element : ImmutableList.copyOf(operatingSystemsNode.elements())) {
                        supported.add(element.textValue().toLowerCase());
                    }
                    availablePackage.setSupportsLinux(supported.contains("linux"));
                    availablePackage.setSupportsMacOS(supported.contains("macos") || supported.contains("osx"));
                    availablePackage.setSupportsWindows(supported.contains("windows") || supported.contains("win"));
                }
                repositoryProgress.log("Detected package " + availablePackage);
                availablePackages.add(availablePackage);
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
        EasyInstallExternalEnvironmentInstallerDialog dialog = new EasyInstallExternalEnvironmentInstallerDialog(this);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getWorkbench().getWindow());
        dialog.setVisible(true);

        targetPackage = dialog.getTargetPackage();
        if(targetPackage == null) {
            getProgressInfo().log("Cancelled by user.");
            return;
        }

        absoluteInstallationPath = PathUtils.relativeToImageJToAbsolute(Paths.get("jipipe").resolve(targetPackage.getInstallDir()));
        if (Files.exists(absoluteInstallationPath)) {
            if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "The directory " + absoluteInstallationPath
                    + " already exists. Do you want to overwrite it?", getTaskLabel(), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                getProgressInfo().log("Cancelled by user.");
                return;
            }
        }
    }

    /**
     * This method should return all repositories that are available
     * @return the repositories
     */
    public abstract List<String> getRepositories();


    /**
     * The heading of the installation dialog
     * @return the heading
     */
    public abstract String getDialogHeading();

    /**
     * The description of the installation dialog
     * @return the description
     */
    public abstract HTMLText getDialogDescription();

}