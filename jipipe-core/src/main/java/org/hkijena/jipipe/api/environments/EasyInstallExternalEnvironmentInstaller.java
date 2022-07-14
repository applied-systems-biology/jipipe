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
import edu.mines.jtk.mesh.TetMesh;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class EasyInstallExternalEnvironmentInstaller extends ExternalEnvironmentInstaller {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    private List<AvailablePackage> availablePackages = new ArrayList<>();

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

    @Override
    public void run() {
        progressInfo.setProgress(0,3);
        loadAvailablePackages(progressInfo.resolve("Load available packages"));

        progressInfo.incrementProgress();
        progressInfo.log("Waiting for user input ...");
        try {
            SwingUtilities.invokeAndWait(this::runSetupDialog);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AvailablePackage> getAvailablePackages() {
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
            JIPipeProgressInfo repositoryProgress = progressInfo.resolve("Repostiory " + i);
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
                AvailablePackage availablePackage = new AvailablePackage();
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

        }
    }

    private void runSetupDialog() {
        EasyInstallExternalEnvironmentInstallerDialog dialog = new EasyInstallExternalEnvironmentInstallerDialog(this);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(getWorkbench().getWindow());
        dialog.setVisible(true);
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

    public static class AvailablePackage {
        private String name;
        private String installDir;
        private String description;
        private boolean supportsWindows;
        private boolean supportsLinux;
        private boolean supportsMacOS;
        private JsonNode additionalData;

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getInstallDir() {
            return installDir;
        }

        public void setInstallDir(String installDir) {
            this.installDir = installDir;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isSupportsWindows() {
            return supportsWindows;
        }

        public void setSupportsWindows(boolean supportsWindows) {
            this.supportsWindows = supportsWindows;
        }

        public boolean isSupportsLinux() {
            return supportsLinux;
        }

        public void setSupportsLinux(boolean supportsLinux) {
            this.supportsLinux = supportsLinux;
        }

        public boolean isSupportsMacOS() {
            return supportsMacOS;
        }

        public void setSupportsMacOS(boolean supportsMacOS) {
            this.supportsMacOS = supportsMacOS;
        }

        public JsonNode getAdditionalData() {
            return additionalData;
        }

        public void setAdditionalData(JsonNode additionalData) {
            this.additionalData = additionalData;
        }

        public boolean isSupported() {
            if(SystemUtils.IS_OS_WINDOWS)
                return supportsWindows;
            else if(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX)
                return supportsMacOS;
            else if(SystemUtils.IS_OS_LINUX)
                return supportsLinux;
            else
                return false;
        }

        public boolean isUnsupported() {
            return !isSupported();
        }

        @Override
        public String toString() {
            return getName() + " [" + getUrl() + "] -> " + getInstallDir() + " on " + "win=" + isSupportsWindows() + ",mac=" + isSupportsMacOS() + ",linux=" + isSupportsMacOS();
        }
    }
}
