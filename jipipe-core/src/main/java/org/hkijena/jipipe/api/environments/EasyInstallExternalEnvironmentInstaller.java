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
import org.hkijena.jipipe.JIPipe;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
     *
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
        progressInfo.setProgress(0, 5);
        loadAvailablePackages(progressInfo.resolve("Load available packages"));
        progressInfo.setProgress(1);

        executeUserConfiguration();
        if (targetPackage != null && absoluteInstallationPath != null) {
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
        JIPipe.getSettings().save();
        SwingUtilities.invokeLater(this::showFinishedDialog);
    }

    /**
     * Postprocessing steps (optional) that are executed after the extraction of the archive
     */
    protected void executePostprocess() {

    }

    /**
     * This method should write the environment to the parameter access
     * Will be executed on the Swing thread (via invokeLater)
     *
     * @param environment     the environment
     * @param parameterAccess the parameter
     */
    protected abstract void writeEnvironmentToParameters(T environment, JIPipeParameterAccess parameterAccess);

    /**
     * Generates the final environment. Has access to the target package (getTargetPackage()) and the installation path
     *
     * @return the environment or null if there is an error
     */
    protected abstract T generateEnvironment();

    private void executeArchiveDownload() {
        progressInfo.log("Downloading archive ...");

        Path outputFile;
        String extension;

        if (targetPackage.getUrlMultiPart() != null && !targetPackage.getUrlMultiPart().isEmpty()) {
            progressInfo.log("The archive was split into the following parts by the developer: ");
            for (String url : targetPackage.getUrlMultiPart()) {
                progressInfo.log(" - " + url);
            }

            // Detect extension
            if (targetPackage.getMultiPartOutputName().endsWith(".tar.gz")) {
                extension = ".tar.gz";
            } else if (targetPackage.getMultiPartOutputName().endsWith(".tar.xz")) {
                extension = ".tar.xz";
            } else {
                String[] split = targetPackage.getMultiPartOutputName().split("\\.");
                extension = "." + split[split.length - 1];
            }

            List<Path> multiPartFiles = new ArrayList<>();

            List<String> urlMultiPart = targetPackage.getUrlMultiPart();
            for (int i = 0; i < urlMultiPart.size(); i++) {
                JIPipeProgressInfo partProgress = progressInfo.resolveAndLog("Download part", i, urlMultiPart.size());
                String url = urlMultiPart.get(i);
                Path multiPartTmpFile = RuntimeSettings.generateTempFile("repository", extension);

                try {
                    WebUtils.download(new URL(url), multiPartTmpFile, "Download part", partProgress);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

                multiPartFiles.add(multiPartTmpFile);
            }

            // Concat the multipart files
            outputFile = RuntimeSettings.generateTempFile("repository", extension);
            progressInfo.log("Following files will be combined into " + outputFile);
            for (Path multiPartFile : multiPartFiles) {
                progressInfo.log(" - " + multiPartFile);
            }
            try (FileOutputStream stream = new FileOutputStream(outputFile.toFile())) {
                for (int i = 0; i < multiPartFiles.size(); i++) {
                    JIPipeProgressInfo partProgress = progressInfo.resolveAndLog("Merge part", i, urlMultiPart.size());
                    Path partFile = multiPartFiles.get(i);
                    Files.copy(partFile, stream);
                    stream.flush();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Cleanup
            for (Path path : multiPartFiles) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    progressInfo.log("Could not clean up temporary file " + path);
                    progressInfo.log(e.toString());
                }
            }

        } else {
            progressInfo.log("The following URL will be downloaded: " + targetPackage.getUrl());

            // Detect extension
            if (targetPackage.getUrl().endsWith(".tar.gz")) {
                extension = ".tar.gz";
            } else if (targetPackage.getUrl().endsWith(".tar.xz")) {
                extension = ".tar.xz";
            } else {
                String[] split = targetPackage.getUrl().split("\\.");
                extension = "." + split[split.length - 1];
            }

            // Set output file and download directly
            outputFile = RuntimeSettings.generateTempFile("repository", extension);
            try {
                WebUtils.download(new URL(targetPackage.getUrl()), outputFile, "Download package", progressInfo.resolve("Download package"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        progressInfo.log("Archive extension detected as " + extension);

        // Extract archive
        progressInfo.log("Extracting archive ... " + outputFile);
        if (extension.equals(".zip")) {
            try {
                ArchiveUtils.decompressZipFile(outputFile, absoluteInstallationPath, progressInfo.resolve("Extract package"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                ArchiveUtils.decompressTarGZ(outputFile, absoluteInstallationPath, progressInfo.resolve("Extract package"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Write metadata
        try {
            Path metadataPath = absoluteInstallationPath.resolve("jipipe-easyinstall-package.json");
            JsonUtils.saveToFile(targetPackage, metadataPath);
        }
        catch (Throwable e) {
            progressInfo.log("Could not write package metadata!");
            progressInfo.log(e.toString());
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
        if (getRepositories().isEmpty()) {
            throw new UnsupportedOperationException("No repositories set! Cancelling.");
        }
        availablePackages = EasyInstallExternalEnvironmentInstallerPackage.loadFromURLs(getRepositories(), progressInfo);
    }

    private void runSetupDialog() {
        EasyInstallExternalEnvironmentInstallerDialog dialog = new EasyInstallExternalEnvironmentInstallerDialog(this);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getWorkbench().getWindow());
        dialog.setVisible(true);

        targetPackage = dialog.getTargetPackage();
        if (targetPackage == null) {
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

    private void showFinishedDialog() {
        JOptionPane.showMessageDialog(getWorkbench().getWindow(), getFinishedMessage().getHtml(), getTaskLabel(), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * This method should return all repositories that are available
     *
     * @return the repositories
     */
    public abstract List<String> getRepositories();


    /**
     * The heading of the installation dialog
     *
     * @return the heading
     */
    public abstract String getDialogHeading();

    /**
     * The description of the installation dialog
     *
     * @return the description
     */
    public abstract HTMLText getDialogDescription();

    /**
     * The message shown after the operation is finished
     * @return the message
     */
    public abstract HTMLText getFinishedMessage();

}
