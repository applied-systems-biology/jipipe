package org.hkijena.jipipe.extensions.environments.installers;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionEvaluator;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.extensions.environments.PythonEnvironment;
import org.hkijena.jipipe.extensions.environments.PythonEnvironmentType;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Select existing Conda environment ...", description = "Chooses an existing Conda environment")
public class SelectCondaEnvPythonInstaller extends ExternalEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private PythonEnvironment generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public SelectCondaEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# Conda environment\n\n" +
                                "Please choose the Conda executable and the environment name."), "Select Conda environment",
                        ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
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

        if(userCancelled.get())
            return;

        generatedEnvironment = createCondaEnvironment(configuration);
        if(getParameterAccess() != null) {
            getParameterAccess().set(generatedEnvironment);
        }
    }

    public static PythonEnvironment createCondaEnvironment(Configuration configuration) {
        PythonEnvironment generatedEnvironment = new PythonEnvironment();
        generatedEnvironment.setType(PythonEnvironmentType.Conda);
        generatedEnvironment.setExecutablePath(configuration.condaExecutable);
        if(configuration.overrideEnvironment.isEnabled()) {
            generatedEnvironment.setArguments(new DefaultExpressionParameter(
                    String.format("ARRAY(\"run\", \"--no-capture-output\", \"-p\", \"%s\", \"python\", \"-u\", script_file)",
                            DefaultExpressionEvaluator.escapeString(configuration.overrideEnvironment.getContent().toString()))));
        }
        else {
            generatedEnvironment.setArguments(new DefaultExpressionParameter(
                    String.format("ARRAY(\"run\", \"--no-capture-output\", \"-n\", \"%s\", \"python\", \"-u\", script_file)",
                            DefaultExpressionEvaluator.escapeString(configuration.environmentName))));
        }
        return generatedEnvironment;
    }

    public static class Configuration implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private Path condaExecutable = Paths.get("");
        private String environmentName = "base";
        private OptionalPathParameter overrideEnvironment = new OptionalPathParameter();

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Conda executable", description = "The conda executable. Located in the Miniconda/Anaconda folder. On Windows it is located " +
                "inside the Scripts directory.")
        @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesOnly, key = FileChooserSettings.KEY_EXTERNAL)
        @JIPipeParameter("conda-executable")
        public Path getCondaExecutable() {
            return condaExecutable;
        }

        @JIPipeParameter("conda-executable")
        public void setCondaExecutable(Path condaExecutable) {
            this.condaExecutable = condaExecutable;
        }

        @JIPipeDocumentation(name = "Environment name", description = "The name of the selected Conda environment")
        @JIPipeParameter("environment-name")
        @StringParameterSettings(monospace = true)
        public String getEnvironmentName() {
            return environmentName;
        }

        @JIPipeParameter("environment-name")
        public void setEnvironmentName(String environmentName) {
            this.environmentName = environmentName;
        }

        @JIPipeDocumentation(name = "Override environment path", description = "Alternative to using an environment name. You can provide " +
                "the environment directory here.")
        @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.DirectoriesOnly, key = FileChooserSettings.KEY_EXTERNAL)
        @JIPipeParameter("override-environment")
        public OptionalPathParameter getOverrideEnvironment() {
            return overrideEnvironment;
        }

        @JIPipeParameter("override-environment")
        public void setOverrideEnvironment(OptionalPathParameter overrideEnvironment) {
            this.overrideEnvironment = overrideEnvironment;
        }
    }
}
