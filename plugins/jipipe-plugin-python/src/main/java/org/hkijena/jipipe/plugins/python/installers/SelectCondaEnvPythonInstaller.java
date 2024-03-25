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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluator;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironmentType;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@SetJIPipeDocumentation(name = "Select existing Conda environment ...", description = "Chooses an existing Conda environment")
public class SelectCondaEnvPythonInstaller extends JIPipeExternalEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private PythonEnvironment generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public SelectCondaEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    public static PythonEnvironment createCondaEnvironment(Configuration configuration) {
        PythonEnvironment generatedEnvironment = new PythonEnvironment();
        generatedEnvironment.setType(PythonEnvironmentType.Conda);
        generatedEnvironment.setExecutablePath(configuration.condaExecutable);
        if (configuration.overrideEnvironment.isEnabled()) {
            generatedEnvironment.setArguments(new JIPipeExpressionParameter(
                    String.format("ARRAY(\"run\", \"--no-capture-output\", \"-p\", \"%s\", \"python\", \"-u\", script_file)",
                            JIPipeExpressionEvaluator.escapeString(configuration.overrideEnvironment.getContent().toString()))));
        } else {
            generatedEnvironment.setArguments(new JIPipeExpressionParameter(
                    String.format("ARRAY(\"run\", \"--no-capture-output\", \"-n\", \"%s\", \"python\", \"-u\", script_file)",
                            JIPipeExpressionEvaluator.escapeString(configuration.environmentName))));
        }
        generatedEnvironment.setName(configuration.getName());
        return generatedEnvironment;
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
                boolean result = JIPipeDesktopParameterPanel.showDialog((JIPipeDesktopWorkbench) getWorkbench(), configuration, new MarkdownText("# Conda environment\n\n" +
                                "Please choose the Conda executable and the environment name."), "Select Conda environment",
                        JIPipeDesktopParameterPanel.NO_GROUP_HEADERS | JIPipeDesktopParameterPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.WITH_SCROLLING);
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

        generatedEnvironment = createCondaEnvironment(configuration);
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
        private Path condaExecutable = Paths.get("");
        private String environmentName = "base";
        private OptionalPathParameter overrideEnvironment = new OptionalPathParameter();
        private String name = "Conda";

        @SetJIPipeDocumentation(name = "Conda executable", description = "The conda executable. Located in the Miniconda/Anaconda folder. On Windows it is inside the Scripts directory.")
        @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.FilesOnly, key = FileChooserSettings.LastDirectoryKey.External)
        @JIPipeParameter("conda-executable")
        public Path getCondaExecutable() {
            return condaExecutable;
        }

        @JIPipeParameter("conda-executable")
        public void setCondaExecutable(Path condaExecutable) {
            this.condaExecutable = condaExecutable;
        }

        @SetJIPipeDocumentation(name = "Environment name", description = "The name of the selected Conda environment")
        @JIPipeParameter("environment-name")
        @StringParameterSettings(monospace = true)
        public String getEnvironmentName() {
            return environmentName;
        }

        @JIPipeParameter("environment-name")
        public void setEnvironmentName(String environmentName) {
            this.environmentName = environmentName;
        }

        @SetJIPipeDocumentation(name = "Override environment path", description = "Alternative to using an environment name. You can provide " +
                "the environment directory here.")
        @PathParameterSettings(ioMode = PathIOMode.Open, pathMode = PathType.DirectoriesOnly, key = FileChooserSettings.LastDirectoryKey.External)
        @JIPipeParameter("override-environment")
        public OptionalPathParameter getOverrideEnvironment() {
            return overrideEnvironment;
        }

        @JIPipeParameter("override-environment")
        public void setOverrideEnvironment(OptionalPathParameter overrideEnvironment) {
            this.overrideEnvironment = overrideEnvironment;
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
