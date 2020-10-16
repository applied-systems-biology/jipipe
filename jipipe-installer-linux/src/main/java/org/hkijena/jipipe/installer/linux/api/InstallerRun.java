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

package org.hkijena.jipipe.installer.linux.api;

import org.hkijena.jipipe.installer.linux.ui.utils.StringUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class InstallerRun implements JIPipeRunnable{
    private Path installationPath;
    private boolean createLauncher = true;

    public InstallerRun() {
        setInitialInstallationPath();
    }

    private void setInitialInstallationPath() {
        String dataHome = System.getenv().getOrDefault("XDG_DATA_HOME", null);
        if(StringUtils.isNullOrEmpty(dataHome)) {
            dataHome = System.getProperty("user.home") + "/.local/share/";
        }
        installationPath = Paths.get(dataHome).resolve("JIPipe-installer");
    }

    public Path getInstallationPath() {
        return installationPath;
    }

    public void setInstallationPath(Path installationPath) {
        this.installationPath = installationPath;
    }

    public boolean isCreateLauncher() {
        return createLauncher;
    }

    public void setCreateLauncher(boolean createLauncher) {
        this.createLauncher = createLauncher;
    }

    @Override
    public void run(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        createInstallationDirectory();
    }

    private void createInstallationDirectory() {
        if(!Files.isDirectory(installationPath)) {
            try {
                Files.createDirectories(installationPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
