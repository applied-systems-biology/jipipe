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

package org.hkijena.jipipe.plugins.python.installers;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluator;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironmentType;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@SetJIPipeDocumentation(name = "Select existing Python virtual environment ...", description = "Chooses an existing Python virtual environment")
public class SelectVirtualEnvPythonInstaller extends JIPipeExternalEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private PythonEnvironment generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public SelectVirtualEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
    public String getTaskLabel() {
        return "Select Python";
    }

    @Override
    public PythonEnvironment getInstalledEnvironment() {
        return generatedEnvironment;
    }

    @Override
    public void run() {
        AtomicBoolean windowOpened = new AtomicBoolean(true);
        AtomicBoolean userCancelled = new AtomicBoolean(true);
        Configuration configuration = new Configuration();
        Object lock = new Object();

        progressInfo.log("Waiting for user input ...");
        synchronized (lock) {
            SwingUtilities.invokeLater(() -> {
                boolean result = JIPipeDesktopParameterFormPanel.showDialog((JIPipeDesktopWorkbench) getWorkbench(), configuration, new MarkdownText("# Python virtual environment\n\n" +
                                "Please choose the directory that contains the virtual environment."), "Select Python virtual environment",
                        JIPipeDesktopParameterFormPanel.NO_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.WITH_SCROLLING);
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

        if (userCancelled.get())
            return;

        Path selectedPath = configuration.virtualEnvDirectory;
        generatedEnvironment = new PythonEnvironment();
        generatedEnvironment.setType(PythonEnvironmentType.VirtualEnvironment);
        if (SystemUtils.IS_OS_WINDOWS) {
            generatedEnvironment.setExecutablePath(selectedPath.resolve("Scripts").resolve("python.exe"));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.resolve("Scripts").toString()) + ";\"" + " + Path",
                    "Path"
            ));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.toString()) + "\"",
                    "VIRTUAL_ENV"
            ));
        } else {
            generatedEnvironment.setExecutablePath(selectedPath.resolve("bin").resolve("python"));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.resolve("bin").toString()) + ":\"" + " + PATH",
                    "PATH"
            ));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + JIPipeExpressionEvaluator.escapeString(selectedPath.toString()) + "\"",
                    "VIRTUAL_ENV"
            ));
        }

        generatedEnvironment.setArguments(new JIPipeExpressionParameter("ARRAY(\"-u\", script_file)"));
        generatedEnvironment.setName(configuration.getName());
        if (getParameterAccess() != null) {
            SwingUtilities.invokeLater(() -> {
                if (getParameterAccess().getFieldClass().isAssignableFrom(generatedEnvironment.getClass())) {
                    getParameterAccess().set(generatedEnvironment);
                } else {
                    // It's probably an optional
                    getParameterAccess().set(new OptionalPythonEnvironment(generatedEnvironment));
                }
            });
        }
    }

    public static class Configuration extends AbstractJIPipeParameterCollection {
        private Path virtualEnvDirectory = Paths.get("");
        private String name = "Virtual env";

        @SetJIPipeDocumentation(name = "Virtual environment directory", description = "The directory of the virtual environment. It usually contains a file pyvenv.cfg.")
        @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.DirectoriesOnly, key = JIPipeFileChooserApplicationSettings.LastDirectoryKey.External)
        @JIPipeParameter("venv-dir")
        public Path getVirtualEnvDirectory() {
            return virtualEnvDirectory;
        }

        @JIPipeParameter("venv-dir")
        public void setVirtualEnvDirectory(Path virtualEnvDirectory) {
            this.virtualEnvDirectory = virtualEnvDirectory;
        }

        @SetJIPipeDocumentation(name = "Name", description = "Name of the created environment")
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
