package org.hkijena.jipipe.extensions.python.installers;

import com.google.common.eventbus.EventBus;
import ij.Prefs;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.external.PythonEnvironmentInstaller;
import org.hkijena.jipipe.extensions.parameters.external.PythonEnvironmentParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.WebUtils;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Install Miniconda 3", description = "Installs Miniconda 3")
public class MinicondaEnvPythonInstaller extends PythonEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Configuration configuration = new Configuration();
    private PythonEnvironmentParameter generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public MinicondaEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public String getTaskLabel() {
        return "Install Conda";
    }

    @Override
    public PythonEnvironmentParameter getInstalledEnvironment() {
        return generatedEnvironment;
    }

    @Override
    public void run() {
        AtomicBoolean windowOpened = new AtomicBoolean(true);
        AtomicBoolean userCancelled = new AtomicBoolean(true);
        Object lock = new Object();

        progressInfo.log("Waiting for user input ...");
        synchronized (lock) {
            SwingUtilities.invokeLater(() -> {
                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# Install Miniconda\n\n" +
                                "Please review the settings on the left-hand side. Click OK to install Miniconda."), "Download & install Miniconda",
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

        progressInfo.setProgress(1, 2);
        Path installerPath;
        if(configuration.getCustomInstallerPath().isEnabled())
            installerPath = configuration.getCustomInstallerPath().getContent();
        else
            installerPath = downloadMiniconda(progressInfo.resolveAndLog("Download Miniconda"));
        if(progressInfo.isCancelled().get())
            return;

        progressInfo.incrementProgress();
        if(SystemUtils.IS_OS_WINDOWS) {
            installMinicondaWindows(installerPath, progressInfo.resolveAndLog("Installing"));
        }

        // Generate result
        SelectCondaEnvPythonInstaller.Configuration condaConfig = new SelectCondaEnvPythonInstaller.Configuration();
        if(SystemUtils.IS_OS_WINDOWS) {
            condaConfig.setCondaExecutable(configuration.getInstallationPath().resolve("Scripts").resolve("conda.exe"));
        }
        else {
            condaConfig.setCondaExecutable(configuration.getInstallationPath().resolve("bin").resolve("conda"));
        }
        condaConfig.setEnvironmentName("base");
        generatedEnvironment = SelectCondaEnvPythonInstaller.createCondaEnvironment(condaConfig);
        if(getParameterAccess() != null) {
            getParameterAccess().set(generatedEnvironment);
        }
    }

    private void installMinicondaWindows(Path installerPath, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Miniconda is currently installing. Unfortunately, the installer provides no log.");

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
                progressInfo.log(s);
            }
        };

        CommandLine commandLine = new CommandLine(installerPath.toFile());
        commandLine.addArgument("/InstallationType=JustMe");
        commandLine.addArgument("/AddToPath=0");
        commandLine.addArgument("/RegisterPython=0");
        commandLine.addArgument("/S");
        commandLine.addArgument("/D=" + configuration.installationPath);

        DefaultExecutor executor = new DefaultExecutor();
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));

        try {
            executor.execute(commandLine);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path downloadMiniconda(JIPipeProgressInfo progressInfo) {
        Path targetFile = RuntimeSettings.generateTempFile("conda", SystemUtils.IS_OS_WINDOWS ? ".exe" : ".sh");

        URL url;
        try {
            url = new URL(configuration.getCondaDownloadURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        WebUtils.download(url, targetFile, getTaskLabel(), progressInfo);
        return targetFile;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Gets the latest download link for Miniconda
     * @return the download URL
     */
    public static String getLatestDownload() {
        if(SystemUtils.IS_OS_WINDOWS) {
            return "https://repo.anaconda.com/miniconda/Miniconda3-latest-Windows-x86_64.exe";
        }
        else if(SystemUtils.IS_OS_MAC) {
            return "https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-x86_64.sh";
        }
        else {
            return "https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh";
        }
    }

    public static class Configuration implements JIPipeParameterCollection {

        private final EventBus eventBus = new EventBus();
        private String condaDownloadURL = getLatestDownload();
        private Path installationPath;
        private OptionalPathParameter customInstallerPath = new OptionalPathParameter();

        public Configuration() {
            installationPath = Paths.get(Prefs.getImageJDir()).resolve("miniconda");
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Download URL", description = "This URL is used to download Conda. If you change it, please ensure that URL " +
                "is the correct one for your current operating system. The Python version that is installed can be viewed here: https://docs.conda.io/en/latest/miniconda.html")
        @JIPipeParameter("conda-download-url")
        @StringParameterSettings(monospace = true)
        public String getCondaDownloadURL() {
            return condaDownloadURL;
        }

        @JIPipeParameter("conda-download-url")
        public void setCondaDownloadURL(String condaDownloadURL) {
            this.condaDownloadURL = condaDownloadURL;
        }

        @JIPipeDocumentation(name = "Installation path", description = "The folder where Miniconda is installed. Please choose an non-existing or empty folder.")
        @JIPipeParameter("installation-path")
        public Path getInstallationPath() {
            return installationPath;
        }

        @JIPipeParameter("installation-path")
        public void setInstallationPath(Path installationPath) {
            this.installationPath = installationPath;
        }

        @JIPipeDocumentation(name = "Use custom installer", description = "Instead of downloading Miniconda, use a custom installer executable.")
        @JIPipeParameter("custom-installer-path")
        public OptionalPathParameter getCustomInstallerPath() {
            return customInstallerPath;
        }

        @JIPipeParameter("custom-installer-path")
        public void setCustomInstallerPath(OptionalPathParameter customInstallerPath) {
            this.customInstallerPath = customInstallerPath;
        }
    }
}
