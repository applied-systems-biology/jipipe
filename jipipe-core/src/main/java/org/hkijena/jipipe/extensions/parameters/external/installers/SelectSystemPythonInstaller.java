package org.hkijena.jipipe.extensions.parameters.external.installers;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.external.PythonEnvironmentInstaller;
import org.hkijena.jipipe.extensions.parameters.external.PythonEnvironmentParameter;
import org.hkijena.jipipe.extensions.parameters.external.PythonEnvironmentType;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Select existing system Python ...", description = "Chooses an existing system Python (not a virtual environment)")
public class SelectSystemPythonInstaller extends PythonEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private PythonEnvironmentParameter generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public SelectSystemPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
    public PythonEnvironmentParameter getInstalledEnvironment() {
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
                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# System Python\n\n" +
                                "Please choose the Python executable file and click OK."), "Select system Python",
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

        generatedEnvironment = new PythonEnvironmentParameter();
        generatedEnvironment.setType(PythonEnvironmentType.System);
        generatedEnvironment.setArguments(new DefaultExpressionParameter("ARRAY(script_file)"));
        generatedEnvironment.setExecutablePath(configuration.pythonExecutable);
        if(getParameterAccess() != null) {
            getParameterAccess().set(generatedEnvironment);
        }
    }

    public static class Configuration implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private Path pythonExecutable = Paths.get("");

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Python executable", description = "The executable of the system Python. " +
                "On Windows this is usually %appdata%\\..\\Local\\Programs\\Python\\PythonXX\\python.exe (XX is the Python version). " +
                "On Linux, Python is located in /usr/local/bin/python")
        @FilePathParameterSettings(ioMode = PathEditor.IOMode.Open, pathMode = PathEditor.PathMode.FilesOnly, key = FileChooserSettings.KEY_EXTERNAL)
        @JIPipeParameter("python-executable")
        public Path getPythonExecutable() {
            return pythonExecutable;
        }

        @JIPipeParameter("python-executable")
        public void setPythonExecutable(Path pythonExecutable) {
            this.pythonExecutable = pythonExecutable;
        }
    }
}
