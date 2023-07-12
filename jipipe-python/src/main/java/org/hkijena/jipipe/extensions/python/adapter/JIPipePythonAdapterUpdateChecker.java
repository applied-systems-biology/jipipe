package org.hkijena.jipipe.extensions.python.adapter;

import com.fasterxml.jackson.core.Version;
import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.EasyInstallExternalEnvironmentInstallerPackage;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.python.PythonExtension;
import org.hkijena.jipipe.utils.VersionUtils;
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
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.log("INFO: To disable this update check, uncheck Project > Application settings > Extensions > Python integration (adapter) > Automatically check for updates");

        // Check the version of the current library
        JIPipePythonAdapterLibraryEnvironment currentEnvironment = PythonAdapterExtensionSettings.getInstance().getPythonAdapterLibraryEnvironment();
        if(currentEnvironment.isProvidedByEnvironment()) {
            progressInfo.log("Adapter provided by environment. Update check skipped.");
            return;
        }
        if(currentEnvironment.getLibraryDirectory() == null) {
            progressInfo.log("Existing environment not set. Update check skipped.");
            return;
        }

        String currentVersion = null;

        // Look for the easyinstall package metadata
        Path easyInstallVersionFile = currentEnvironment.getAbsoluteLibraryDirectory().getParent().resolve("jipipe-easyinstall-package.json");
        if(Files.isRegularFile(easyInstallVersionFile)) {
            try {
                EasyInstallExternalEnvironmentInstallerPackage installerPackage = JsonUtils.readFromFile(easyInstallVersionFile, EasyInstallExternalEnvironmentInstallerPackage.class);
                if(StringUtils.isNotBlank(installerPackage.getVersion())) {
                    currentVersion = installerPackage.getVersion();
                }
            }
            catch (Throwable e) {
                progressInfo.log("Unable to read " + easyInstallVersionFile + ": " + e);
            }
        }
        else {
            progressInfo.log("Missing file: " + easyInstallVersionFile + ". Falling back to pyproject.toml version information.");
        }

        // Look for the pyproject metadata
        if(currentVersion == null) {
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

        if(currentVersion == null) {
            progressInfo.log("Unable to detect current version of the adapter library. Update check skipped.");
            return;
        }

        progressInfo.log("INFO: Current version is '" + currentVersion + "'");


        StringList repositories = PythonAdapterExtensionSettings.getInstance().getEasyInstallerRepositories();
        List<EasyInstallExternalEnvironmentInstallerPackage> packages = EasyInstallExternalEnvironmentInstallerPackage.loadFromURLs(repositories, progressInfo);

        // Compare versions
        EasyInstallExternalEnvironmentInstallerPackage update = null;
        String updateVersion = currentVersion;

        for (EasyInstallExternalEnvironmentInstallerPackage installerPackage : packages) {
            if(org.hkijena.jipipe.utils.StringUtils.compareVersions(updateVersion, installerPackage.getVersion()) < 0) {
                updateVersion = installerPackage.getVersion();
                update = installerPackage;
            }
        }

        if(update != null) {
            progressInfo.log("Update found: " + update);
            PythonExtension.createOldLibJIPipePythonNotification(JIPipeNotificationInbox.getInstance(), currentVersion, updateVersion);
        }
        else {
            progressInfo.log("No update found.");
        }
    }
}
