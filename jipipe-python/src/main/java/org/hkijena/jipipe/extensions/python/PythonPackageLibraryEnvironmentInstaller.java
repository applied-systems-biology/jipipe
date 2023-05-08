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

package org.hkijena.jipipe.extensions.python;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Installer for a {@link PythonPackageLibraryEnvironment} that will just delete any existing directories and re-install the library.
 */
public abstract class PythonPackageLibraryEnvironmentInstaller<T extends PythonPackageLibraryEnvironment> extends ExternalEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Configuration configuration = new Configuration();
    private PythonPackageLibraryEnvironment environment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public PythonPackageLibraryEnvironmentInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    protected abstract T createEnvironment();

    protected abstract String getEnvironmentName();

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
        return "Install " + getEnvironmentName();
    }

    @Override
    public ExternalEnvironment getInstalledEnvironment() {
        return environment;
    }

    @Override
    public void run() {
        this.environment = createEnvironment();
        getConfiguration().setInstallationPath(environment.getLibraryDirectory());
        getConfiguration().setName(environment.getName());

        // Configure
        if (!configure())
            return;

        Path installationPath = PathUtils.relativeToImageJToAbsolute(getConfiguration().getInstallationPath());
        environment.setLibraryDirectory(getConfiguration().getInstallationPath());
        if (Files.exists(installationPath)) {
            PathUtils.deleteDirectoryRecursively(installationPath,
                    progressInfo.resolve("Cleanup"));
        }
        environment.install(getProgressInfo());

        if (getParameterAccess() != null) {
            SwingUtilities.invokeLater(() -> {
                if (getParameterAccess().getFieldClass().isAssignableFrom(environment.getClass())) {
                    getParameterAccess().set(environment);
                }
            });
        }
    }

    /**
     * UI configuration
     *
     * @return false if the operation was cancelled
     */
    protected boolean configure() {
        AtomicBoolean windowOpened = new AtomicBoolean(true);
        AtomicBoolean userCancelled = new AtomicBoolean(true);
        Object lock = new Object();

        progressInfo.log("Waiting for user input ...");
        synchronized (lock) {
            SwingUtilities.invokeLater(() -> {
                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# Install Python library\n\n" +
                                "Please review the settings on the left-hand side. Click OK to install the library."), "Download & install " + getEnvironmentName(),
                        ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
                Path installationPath = PathUtils.relativeToImageJToAbsolute(getConfiguration().getInstallationPath());
                if (result && Files.exists(installationPath)) {
                    if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "The directory " + installationPath
                            + " already exists. Do you want to overwrite it?", getTaskLabel(), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                        result = false;
                    }
                }
                userCancelled.set(!result);
                windowOpened.set(false);
                synchronized (lock) {
                    lock.notify();
                }
            });
            try {
                while (windowOpened.get()) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return !userCancelled.get();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public static class Configuration extends AbstractJIPipeParameterCollection {
        private String name;
        private Path installationPath;

        public Configuration() {
            name = "Default";
            installationPath = Paths.get("lib");
        }

        @JIPipeDocumentation(name = "Installation path", description = "The folder where the library folder is installed. Please note that existing folders will be deleted.")
        @JIPipeParameter("installation-path")
        public Path getInstallationPath() {
            return installationPath;
        }

        @JIPipeParameter("installation-path")
        public void setInstallationPath(Path installationPath) {
            this.installationPath = installationPath;
        }

        @JIPipeDocumentation(name = "Name", description = "Name of the created environment")
        @JIPipeParameter("name")
        public String getName() {
            return name;
        }

        @JIPipeParameter("name")
        public void setName(String name) {
            this.name = name;
        }
    }
}
