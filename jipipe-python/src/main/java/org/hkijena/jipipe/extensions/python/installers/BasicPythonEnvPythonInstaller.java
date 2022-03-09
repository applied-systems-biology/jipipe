package org.hkijena.jipipe.extensions.python.installers;

import com.google.common.eventbus.EventBus;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.extensions.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonEnvironment;
import org.hkijena.jipipe.extensions.python.PythonUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.WebUtils;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Install Python 3", description = "Installs Python 3")
public class BasicPythonEnvPythonInstaller extends ExternalEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Configuration configuration = new Configuration();
    private PythonEnvironment generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public BasicPythonEnvPythonInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    /**
     * Gets the latest download link for Miniconda
     *
     * @return the download URL
     */
    public static String getLatestDownload() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "https://github.com/applied-systems-biology/jipipe/releases/download/current/cpython-3.8.12-windows.gz";
        } else if (SystemUtils.IS_OS_MAC) {
            return "https://github.com/applied-systems-biology/jipipe/releases/download/current/cpython-3.8.12-macos.tar.gz";
        } else {
            return "https://github.com/applied-systems-biology/jipipe/releases/download/current/cpython-3.8.12-linux.gz";
        }
    }

    @Override
    public String getTaskLabel() {
        return "Install Python";
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
        if (progressInfo.isCancelled())
            return;
        progressInfo.incrementProgress();

        // Cleanup phase
        Path installationPath = PathUtils.relativeToImageJToAbsolute(getConfiguration().getInstallationPath());
        if (Files.exists(installationPath)) {
            progressInfo.log("Deleting old installation");
            progressInfo.log("Deleting: " + installationPath);
            PathUtils.deleteDirectoryRecursively(installationPath, progressInfo.resolve("Delete old installation"));
        }
        if (!Files.isDirectory(installationPath)) {
            try {
                Files.createDirectories(installationPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Download phase
        progressInfo.log("Acquire Python binaries ...");
        Path archivePath = downloadPythonArchive();
        if (progressInfo.isCancelled())
            return;
        progressInfo.incrementProgress();

        // Install phase
        progressInfo.log("Extracting ...");
        extractPythonArchive(archivePath);
        if (progressInfo.isCancelled())
            return;
        progressInfo.incrementProgress();

        // Postprocess phase
        progressInfo.log("Postprocess install ...");
        postprocessInstall();
        if (progressInfo.isCancelled())
            return;
        progressInfo.incrementProgress();

        // Generate phase
        progressInfo.log("Generating config ...");
        SelectSystemPythonInstaller.Configuration finalConfig = generateFinalConfig();
        if (progressInfo.isCancelled())
            return;
        progressInfo.incrementProgress();

        generatedEnvironment = SelectSystemPythonInstaller.generateEnvironment(finalConfig);
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

    public Path getPythonExecutable() {
        if(SystemUtils.IS_OS_WINDOWS) {
            return getConfiguration().getInstallationPath().resolve("python").resolve("Python.exe");
        }
        else {
            return getConfiguration().getInstallationPath().resolve("python").resolve("bin").resolve("python3");
        }
    }

    protected void runPip(String... args) {
        CommandLine commandLine = new CommandLine(getPythonExecutable().toFile());
        commandLine.addArgument("-m");
        commandLine.addArgument("pip");
        for (String arg : args) {
            commandLine.addArgument(arg);
        }

        // We must add Library/bin to Path. Otherwise, there SSL won't work
        Map<String, String> environmentVariables = new HashMap<>(System.getenv());
        if (SystemUtils.IS_OS_WINDOWS) {
            environmentVariables.put("Path", getConfiguration().getInstallationPath().resolve("python").resolve("DLLs").toAbsolutePath() + ";" +
                    environmentVariables.getOrDefault("Path", ""));
        }

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, progressInfo);
        PythonUtils.setupLogger(commandLine, executor, progressInfo);

        // Set working directory, so conda can see its DLLs
        executor.setWorkingDirectory(getConfiguration().getInstallationPath().resolve("python").toAbsolutePath().toFile());

        try {
            executor.execute(commandLine, environmentVariables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SelectSystemPythonInstaller.Configuration generateFinalConfig() {
        SelectSystemPythonInstaller.Configuration configuration = new SelectSystemPythonInstaller.Configuration();
        configuration.setName(getConfiguration().getName());
        configuration.setPythonExecutable(getPythonExecutable());
        return configuration;
    }

    /**
     * Applies postprocessing for the installation
     */
    protected void postprocessInstall() {
    }

    /**
     * Installs Miniconda
     *
     * @param archivePath the setup
     */
    protected void extractPythonArchive(Path archivePath) {
        progressInfo.log("Installation path: " + configuration.installationPath.toAbsolutePath());
        progressInfo.log("The Python distribution was obtained from: https://github.com/indygreg/python-build-standalone/releases/");
        try {
            ArchiveUtils.decompressTarGZ(archivePath, configuration.installationPath.toAbsolutePath(), progressInfo.resolve("Extract Python"));
            Files.list(configuration.installationPath.toAbsolutePath().resolve("python").resolve("bin")).forEach(PathUtils::makeUnixExecutable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Downloads the archive
     *
     * @return the archive path
     */
    protected Path downloadPythonArchive() {
        Path archivePath;
        if (configuration.getPreDownloadedArchivePath().isEnabled())
            archivePath = configuration.getPreDownloadedArchivePath().getContent();
        else
            archivePath = downloadPython(progressInfo.resolveAndLog("Download Python distribution"));
        return archivePath;
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
                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# Install Python\n\n" +
                                "Please review the settings on the left-hand side. Click OK to download the Python distribution and install it.\n\n" +
                                "You have to agree to the following licenses: https://python-build-standalone.readthedocs.io/en/latest/running.html#licensing"), "Download & install Python",
                        ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
                if (result && Files.exists(getConfiguration().installationPath)) {
                    if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "The directory " + getConfiguration().getInstallationPath().toAbsolutePath()
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

    private Path downloadPython(JIPipeProgressInfo progressInfo) {
        Path targetFile = RuntimeSettings.generateTempFile("python", ".tar.gz");

        URL url;
        try {
            url = new URL(getConfiguration().getPythonDownloadURL());
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
        private String pythonDownloadURL = getLatestDownload();
        private Path installationPath;
        private OptionalPathParameter preDownloadedArchivePath = new OptionalPathParameter();
        private String name = "Python";

        public Configuration() {
            installationPath = Paths.get("jipipe").resolve("python");
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Download URL", description = "This URL is used to download Python. If you change it, please ensure that URL " +
                "is the correct one for your current operating system. Please find other Python packages here: https://github.com/indygreg/python-build-standalone/releases/ (use the one with install_ony)")
        @JIPipeParameter("python-download-url")
        @StringParameterSettings(monospace = true)
        public String getPythonDownloadURL() {
            return pythonDownloadURL;
        }

        @JIPipeParameter("python-download-url")
        public void setPythonDownloadURL(String pythonDownloadURL) {
            this.pythonDownloadURL = pythonDownloadURL;
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

        @JIPipeDocumentation(name = "Use pre-downloaded archive", description = "Instead of downloading the archive again, use a pre-downloaded archive.")
        @JIPipeParameter("pre-downloaded-archive-path")
        public OptionalPathParameter getPreDownloadedArchivePath() {
            return preDownloadedArchivePath;
        }

        @JIPipeParameter("pre-downloaded-archive-path")
        public void setPreDownloadedArchivePath(OptionalPathParameter preDownloadedArchivePath) {
            this.preDownloadedArchivePath = preDownloadedArchivePath;
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
