package org.hkijena.jipipe.extensions.python.installers;

import com.google.common.eventbus.EventBus;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.WebUtils;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Install Miniconda 3", description = "Installs Miniconda 3")
public class BasicMinicondaEnvPythonInstaller extends ExternalEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Configuration configuration = new Configuration();
    private PythonEnvironment generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public BasicMinicondaEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    /**
     * Gets the latest download link for Miniconda
     *
     * @return the download URL
     */
    public static String getLatestDownload() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "https://repo.anaconda.com/miniconda/Miniconda3-latest-Windows-x86_64.exe";
        } else if (SystemUtils.IS_OS_MAC) {
            return "https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-x86_64.sh";
        } else {
            return "https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh";
        }
    }

    @Override
    public String getTaskLabel() {
        return "Install Conda";
    }

    @Override
    public PythonEnvironment getInstalledEnvironment() {
        return generatedEnvironment;
    }

    @Override
    public void run() {
        progressInfo.setProgress(0, 5);

        // Config phase
        if (!configure()) return;
        if (progressInfo.isCancelled().get())
            return;
        progressInfo.incrementProgress();

        // Cleanup phase
        if(Files.exists(getConfiguration().getInstallationPath().toAbsolutePath())) {
            progressInfo.log("Deleting old installation");
            progressInfo.log("Deleting: " + getConfiguration().getInstallationPath().toAbsolutePath());
            try {
                FileUtils.deleteDirectory(getConfiguration().getInstallationPath().toAbsolutePath().toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Download phase
        progressInfo.log("Acquire setup ...");
        Path installerPath = download();
        if (progressInfo.isCancelled().get())
            return;
        progressInfo.incrementProgress();

        // Install phase
        progressInfo.log("Install ...");
        install(installerPath);
        if (progressInfo.isCancelled().get())
            return;
        progressInfo.incrementProgress();

        // Postprocess phase
        progressInfo.log("Postprocess install ...");
        postprocessInstall();
        if (progressInfo.isCancelled().get())
            return;
        progressInfo.incrementProgress();

        // Generate phase
        progressInfo.log("Generating config ...");
        SelectCondaEnvPythonInstaller.Configuration condaConfig = generateCondaConfig();
        if (progressInfo.isCancelled().get())
            return;
        progressInfo.incrementProgress();

        generatedEnvironment = SelectCondaEnvPythonInstaller.createCondaEnvironment(condaConfig);
        if (getParameterAccess() != null) {
            SwingUtilities.invokeLater(() -> {
                getParameterAccess().set(generatedEnvironment);
            });
        }
    }

    /**
     * Runs the conda executable (for postprocessing)
     *
     * @param args arguments
     */
    public void runConda(String... args) {
        CommandLine commandLine = new CommandLine(getCondaExecutableInInstallationPath().toFile());
        for (String arg : args) {
            commandLine.addArgument(arg);
        }

        // We must add Library/bin to Path. Otherwise, there SSL won't work
        Map<String, String> environmentVariables = new HashMap<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            environmentVariables.put(entry.getKey(), entry.getValue());
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            environmentVariables.put("Path", getConfiguration().getInstallationPath().resolve("Library").resolve("bin").toAbsolutePath() + ";" +
                    environmentVariables.getOrDefault("Path", ""));
        }

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        PythonUtils.setupLogger(commandLine, executor, progressInfo);

        // Set working directory, so conda can see its DLLs
        executor.setWorkingDirectory(getCondaExecutableInInstallationPath().toAbsolutePath().getParent().toFile());

        try {
            executor.execute(commandLine, environmentVariables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies postprocessing for the installation
     */
    protected void postprocessInstall() {
    }

    /**
     * Returns the path to the conda executable within the configured installation directory
     *
     * @return the conda path
     */
    public Path getCondaExecutableInInstallationPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return getConfiguration().getInstallationPath().resolve("Scripts").resolve("conda.exe");
        } else {
            return getConfiguration().getInstallationPath().resolve("bin").resolve("conda");
        }
    }

    /**
     * Generates the configuration for the conda environment
     *
     * @return the config
     */
    protected SelectCondaEnvPythonInstaller.Configuration generateCondaConfig() {
        SelectCondaEnvPythonInstaller.Configuration condaConfig = new SelectCondaEnvPythonInstaller.Configuration();
        if (SystemUtils.IS_OS_WINDOWS) {
            condaConfig.setCondaExecutable(getConfiguration().getInstallationPath().resolve("Scripts").resolve("conda.exe"));
        } else {
            condaConfig.setCondaExecutable(getConfiguration().getInstallationPath().resolve("bin").resolve("conda"));
        }
        condaConfig.setEnvironmentName("base");
        condaConfig.setName(getConfiguration().getName());
        return condaConfig;
    }

    /**
     * Installs Miniconda
     *
     * @param installerPath the setup
     */
    protected void install(Path installerPath) {
        if (SystemUtils.IS_OS_WINDOWS) {
            installMinicondaWindows(installerPath, progressInfo.resolveAndLog("Install Conda"));
        } else {
            installMinicondaLinuxMac(installerPath, progressInfo.resolveAndLog("Install Conda"));
        }
    }

    /**
     * Downloads the installer
     *
     * @return the installer path
     */
    protected Path download() {
        Path installerPath;
        if (configuration.getCustomInstallerPath().isEnabled())
            installerPath = configuration.getCustomInstallerPath().getContent();
        else
            installerPath = downloadMiniconda(progressInfo.resolveAndLog("Download Miniconda"));
        return installerPath;
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
                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# Install Miniconda\n\n" +
                                "Please review the settings on the left-hand side. Click OK to install Miniconda.\n\n" +
                                "You have to agree to the following license: https://docs.conda.io/en/latest/license.html"), "Download & install Miniconda",
                        ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
                if(result && Files.exists(getConfiguration().installationPath)) {
                    if(JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "The directory " + getConfiguration().getInstallationPath().toAbsolutePath()
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

    private void installMinicondaLinuxMac(Path installerPath, JIPipeProgressInfo progressInfo) {
        try {
            Files.setPosixFilePermissions(installerPath, PosixFilePermissions.fromString("rwxrwxr-x"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
                progressInfo.log(s);
            }
        };

        progressInfo.log("Installation path: " + configuration.installationPath.toAbsolutePath());
        progressInfo.log("Please note that you agreed to the Conda license: https://docs.conda.io/en/latest/license.html");
        CommandLine commandLine = new CommandLine(installerPath.toFile());
        commandLine.addArgument("-b");
        commandLine.addArgument("-f");
        commandLine.addArgument("-p");
        commandLine.addArgument(getConfiguration().installationPath.toAbsolutePath().toString());

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));

        try {
            executor.execute(commandLine);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        progressInfo.log("Installation path: " + getConfiguration().installationPath.toAbsolutePath());
        progressInfo.log("Please note that you agreed to the Conda license: https://docs.conda.io/en/latest/license.html");
        CommandLine commandLine = new CommandLine(installerPath.toFile());
        commandLine.addArgument("/InstallationType=JustMe");
        commandLine.addArgument("/AddToPath=0");
        commandLine.addArgument("/RegisterPython=0");
        commandLine.addArgument("/S");
        commandLine.addArgument("/D=" + getConfiguration().installationPath.toAbsolutePath());

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
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
            url = new URL(getConfiguration().getCondaDownloadURL());
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

    public static class Configuration implements JIPipeParameterCollection {

        private final EventBus eventBus = new EventBus();
        private String condaDownloadURL = getLatestDownload();
        private Path installationPath;
        private OptionalPathParameter customInstallerPath = new OptionalPathParameter();
        private String name = "Conda";

        public Configuration() {
            installationPath = Paths.get("miniconda");
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
