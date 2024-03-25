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

package org.hkijena.jipipe.plugins.python.adapter;

import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.desktop.api.environments.JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.python.PythonPlugin;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JIPipePythonAdapterUpdateChecker extends AbstractJIPipeRunnable {
    @Override
    public String getTaskLabel() {
        return "JIPipe Python adapter: check for updates";
    }

    @Override
    public boolean isLogSilent() {
        return true;
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.log("INFO: To disable this update check, uncheck Project > Application settings > Extensions > Python integration (adapter) > Automatically check for updates");

        // Check the version of the current library
        JIPipePythonAdapterLibraryEnvironment currentEnvironment = PythonAdapterExtensionSettings.getInstance().getPythonAdapterLibraryEnvironment();
        if (currentEnvironment.isProvidedByEnvironment()) {
            progressInfo.log("Adapter provided by environment. Update check skipped.");
            return;
        }
        if (currentEnvironment.getLibraryDirectory() == null) {
            progressInfo.log("Existing environment not set. Update check skipped.");
            return;
        }

        String currentVersion = null;

        // Look for the easyinstall package metadata
        Path easyInstallVersionFile = currentEnvironment.getAbsoluteLibraryDirectory().getParent().resolve("jipipe-easyinstall-package.json");
        if (Files.isRegularFile(easyInstallVersionFile)) {
            try {
                JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage installerPackage = JsonUtils.readFromFile(easyInstallVersionFile, JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage.class);
                if (StringUtils.isNotBlank(installerPackage.getVersion())) {
                    currentVersion = installerPackage.getVersion();
                }
            } catch (Throwable e) {
                progressInfo.log("Unable to read " + easyInstallVersionFile + ": " + e);
            }
        } else {
            progressInfo.log("Missing file: " + easyInstallVersionFile + ". Falling back to pyproject.toml version information.");
        }

        // Look for the pyproject metadata
        if (currentVersion == null) {
            Path versionFile = currentEnvironment.getAbsoluteLibraryDirectory().resolve("pyproject.toml");
            if (!Files.isRegularFile(versionFile)) {
                progressInfo.log("Version file does not exist: " + versionFile);
                progressInfo.log("Update check skipped.");
                return;
            }

            try {
                for (String line_ : Files.readAllLines(versionFile)) {
                    String line = line_.trim();
                    if (line.startsWith("version")) {
                        currentVersion = StringUtils.strip(line.split("=")[1], "\" =");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (currentVersion == null) {
            progressInfo.log("Unable to detect current version of the adapter library. Update check skipped.");
            return;
        }

        progressInfo.log("INFO: Current version is '" + currentVersion + "'");


        StringList repositories = PythonAdapterExtensionSettings.getInstance().getEasyInstallerRepositories();
        List<JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage> packages = JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage.loadFromURLs(repositories, progressInfo);

        // Compare versions
        JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage update = null;
        String updateVersion = currentVersion;

        for (JIPipeDesktopEasyInstallExternalEnvironmentInstallerPackage installerPackage : packages) {
            if (org.hkijena.jipipe.utils.StringUtils.compareVersions(updateVersion, installerPackage.getVersion()) < 0) {
                updateVersion = installerPackage.getVersion();
                update = installerPackage;
            }
        }

        if (update != null) {
            progressInfo.log("Update found: " + update);
            PythonPlugin.createOldLibJIPipePythonNotification(JIPipeNotificationInbox.getInstance(), currentVersion, updateVersion);
        } else {
            progressInfo.log("No update found.");
        }
    }
}
