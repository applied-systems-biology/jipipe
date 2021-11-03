package org.hkijena.jipipe.extensions.r.installers;

import com.google.common.eventbus.EventBus;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.r.REnvironment;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.WebUtils;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@JIPipeDocumentation(name = "Install R", description = "Downloads and installs R")
public class REnvInstaller extends ExternalEnvironmentInstaller {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Configuration configuration = new Configuration();
    private REnvironment generatedEnvironment;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public REnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public String getTaskLabel() {
        return "Install R";
    }

    @Override
    public REnvironment getInstalledEnvironment() {
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

                if (!SystemUtils.IS_OS_WINDOWS) {
                    JOptionPane.showMessageDialog(getWorkbench().getWindow(), "We are sorry, but there is unfortunately no " +
                                    "precompiled package for Linux or Mac. Please install R manually and change the file paths.",
                            "Unsupported operating system",
                            JOptionPane.ERROR_MESSAGE);
                    userCancelled.set(true);
                    windowOpened.set(false);
                    synchronized (lock) {
                        lock.notify();
                    }
                    return;
                }

                boolean result = ParameterPanel.showDialog(getWorkbench(), configuration, new MarkdownDocument("# Install R\n\n" +
                                "Please review the settings on the left-hand side. Click OK to install R.\n\n" +
                                "For more information, please visit this page: https://cloud.r-project.org/bin/windows/base/"), "Download & install R",
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

        if (userCancelled.get())
            return;

        progressInfo.setProgress(1, 2);
        Path installerPath;
        if (configuration.getCustomInstallerPath().isEnabled())
            installerPath = configuration.getCustomInstallerPath().getContent();
        else
            installerPath = downloadMiniconda(progressInfo.resolveAndLog("Download R"));
        if (progressInfo.isCancelled())
            return;

        progressInfo.incrementProgress();
        if (SystemUtils.IS_OS_WINDOWS) {
            installRWindows(installerPath, progressInfo.resolveAndLog("Install R"));
        } else {
            throw new UnsupportedOperationException();
        }

        // Generate result
        generatedEnvironment = new REnvironment();
        generatedEnvironment.setRExecutablePath(configuration.installationPath.resolve("bin").resolve("R.exe"));
        generatedEnvironment.setRScriptExecutablePath(configuration.installationPath.resolve("bin").resolve("Rscript.exe"));
        generatedEnvironment.setName(configuration.getName());
        if (getParameterAccess() != null) {
            SwingUtilities.invokeLater(() -> getParameterAccess().set(generatedEnvironment));
        }
    }

    private void installRWindows(Path installerPath, JIPipeProgressInfo progressInfo) {
        progressInfo.log("R is currently installing. Unfortunately, the installer provides no log.");

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
                progressInfo.log(s);
            }
        };

        progressInfo.log("Installation path: " + PathUtils.relativeToImageJToAbsolute(configuration.installationPath));
        progressInfo.log("If you have issues, please visit this site and install R manually: https://cloud.r-project.org/bin/windows/base/");
        CommandLine commandLine = new CommandLine(installerPath.toFile());
        commandLine.addArgument("/VERYSILENT");
        commandLine.addArgument("/DIR=" + PathUtils.relativeToImageJToAbsolute(configuration.installationPath));

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
            url = new URL(configuration.getRDownloadURL());
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
     *
     * @return the download URL
     */
    public static String getLatestDownload() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "https://github.com/applied-systems-biology/jipipe/releases/download/1.48.0-installers/R-4.1.0-win.exe";
        } else {
            return "https://cloud.r-project.org/bin/";
        }
    }

    public static class Configuration implements JIPipeParameterCollection {

        private final EventBus eventBus = new EventBus();
        private String RDownloadURL = getLatestDownload();
        private Path installationPath;
        private OptionalPathParameter customInstallerPath = new OptionalPathParameter();
        private String name = "R";

        public Configuration() {
            installationPath = Paths.get("r");
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Download URL", description = "This URL is used to download R. If you change it, please ensure that URL " +
                "is the correct one for your current operating system. You might want to use a newer version. Just replace it " +
                "with the link from this site: https://cloud.r-project.org/bin/windows/base/")
        @JIPipeParameter("r-download-url")
        @StringParameterSettings(monospace = true)
        public String getRDownloadURL() {
            return RDownloadURL;
        }

        @JIPipeParameter("r-download-url")
        public void setRDownloadURL(String RDownloadURL) {
            this.RDownloadURL = RDownloadURL;
        }

        @JIPipeDocumentation(name = "Installation path", description = "The folder where R is installed. Please choose an non-existing or empty folder.")
        @JIPipeParameter("installation-path")
        public Path getInstallationPath() {
            return installationPath;
        }

        @JIPipeParameter("installation-path")
        public void setInstallationPath(Path installationPath) {
            this.installationPath = installationPath;
        }

        @JIPipeDocumentation(name = "Use custom installer", description = "Instead of downloading R, use a custom installer executable.")
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
