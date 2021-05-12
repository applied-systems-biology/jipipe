package org.hkijena.jipipe.extensions.environments.installers;

import com.google.common.eventbus.EventBus;
import org.apache.commons.lang3.SystemUtils;
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
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Select existing Python virtual environment ...", description = "Chooses an existing Python virtual environment")
public class SelectVirtualEnvPythonInstaller extends ExternalEnvironmentInstaller {

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
                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# Python virtual environment\n\n" +
                                "Please choose the directory that contains the virtual environment."), "Select Python virtual environment",
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

       Path selectedPath = configuration.virtualEnvDirectory;
        generatedEnvironment = new PythonEnvironment();
        generatedEnvironment.setType(PythonEnvironmentType.VirtualEnvironment);
        if(SystemUtils.IS_OS_WINDOWS) {
            generatedEnvironment.setExecutablePath(selectedPath.resolve("Scripts").resolve("python.exe"));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + DefaultExpressionEvaluator.escapeString(selectedPath.resolve("Scripts").toString()) + ";\"" + " + Path",
                    "Path"
            ));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + DefaultExpressionEvaluator.escapeString(selectedPath.toString()) +"\"",
                    "VIRTUAL_ENV"
            ));
        }
        else {
            generatedEnvironment.setExecutablePath(selectedPath.resolve("bin").resolve("python"));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + DefaultExpressionEvaluator.escapeString(selectedPath.resolve("bin").toString()) + ":\"" + " + PATH",
                    "PATH"
            ));
            generatedEnvironment.getEnvironmentVariables().add(new StringQueryExpressionAndStringPairParameter(
                    "\"" + DefaultExpressionEvaluator.escapeString(selectedPath.toString()) +"\"",
                    "VIRTUAL_ENV"
            ));
        }

        generatedEnvironment.setArguments(new DefaultExpressionParameter("ARRAY(\"-u\", script_file)"));
        if(getParameterAccess() != null) {
            getParameterAccess().set(generatedEnvironment);
        }
    }

    public static class Configuration implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private Path virtualEnvDirectory = Paths.get("");

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Virtual environment directory", description = "The directory of the virtual environment. It usually contains a file pyvenv.cfg.")
        @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.DirectoriesOnly, key = FileChooserSettings.KEY_EXTERNAL)
        @JIPipeParameter("venv-dir")
        public Path getVirtualEnvDirectory() {
            return virtualEnvDirectory;
        }

        @JIPipeParameter("venv-dir")
        public void setVirtualEnvDirectory(Path virtualEnvDirectory) {
            this.virtualEnvDirectory = virtualEnvDirectory;
        }
    }
}
