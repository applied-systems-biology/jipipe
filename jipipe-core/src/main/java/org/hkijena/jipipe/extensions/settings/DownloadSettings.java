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

package org.hkijena.jipipe.extensions.settings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.processes.ProcessEnvironment;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Settings related to downloads
 */
public class DownloadSettings extends AbstractJIPipeParameterCollection {
    public static final String ID = "downloads";

    private boolean preferCustomDownloader = true;

    private DownloadEnvironment externalDownloaderProcess = new DownloadEnvironment();

    /**
     * Creates a new instance
     */
    public DownloadSettings() {
        autoDetectEnvironments();
    }

    public static DownloadSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, DownloadSettings.class);
    }

    private void autoDetectEnvironments() {

        if (!externalDownloaderProcess.generateValidityReport().isValid()) {
            if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
                // Attempt to get wget
                Path wgetPath = PathUtils.findAnyOf(Paths.get("/bin/wget"), Paths.get("/usr/local/bin/wget"), Paths.get("/usr/bin/wget"));
                if (wgetPath != null && Files.isRegularFile(wgetPath)) {
                    if (SystemUtils.IS_OS_LINUX) {
                        externalDownloaderProcess.setExecutablePathLinux(wgetPath);
                    } else {
                        externalDownloaderProcess.setExecutablePathOSX(wgetPath);
                    }
                    externalDownloaderProcess.setArguments(new DefaultExpressionParameter("ARRAY(\"-O\", output_file, url)"));
                }
                // Attempt to get cURL
                if (!externalDownloaderProcess.generateValidityReport().isValid()) {
                    Path curlPath = PathUtils.findAnyOf(Paths.get("/bin/curl"), Paths.get("/usr/local/bin/curl"), Paths.get("/usr/bin/curl"));
                    if (curlPath != null && Files.isRegularFile(curlPath)) {
                        if (SystemUtils.IS_OS_LINUX) {
                            externalDownloaderProcess.setExecutablePathLinux(curlPath);
                        } else {
                            externalDownloaderProcess.setExecutablePathOSX(curlPath);
                        }
                        externalDownloaderProcess.setArguments(new DefaultExpressionParameter("ARRAY(\"-L\", \"--retry\", \"5\", url, \"--output\", output_file)"));
                    }
                }
            } else if (SystemUtils.IS_OS_WINDOWS) {
//                // Configure powershell downloading
//                Path powerShellPath = PathUtils.findAnyOf(Paths.get("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe"));
//                if(powerShellPath != null) {
//                    externalDownloaderProcess.setExecutablePathWindows(powerShellPath);
//                    externalDownloaderProcess.setArguments(new DefaultExpressionParameter("ARRAY(\"-NonInteractive\", \"-Command\", \"Invoke-WebRequest \" + url + \" -O \" + output_file)"));
//                }
                // Windows has no good native downloader. Then we can just use the native one.
                preferCustomDownloader = false;
            }
        }
    }

    @JIPipeDocumentation(name = "Prefer custom downloader", description = "If enabled, a custom downloader process is preferred.")
    @JIPipeParameter("prefer-external-downloader")
    public boolean isPreferCustomDownloader() {
        return preferCustomDownloader;
    }

    @JIPipeParameter("prefer-external-downloader")
    public void setPreferCustomDownloader(boolean preferCustomDownloader) {
        this.preferCustomDownloader = preferCustomDownloader;
    }

    @JIPipeDocumentation(name = "Custom downloader", description = "A process for downloading files. If not set, JIPipe will fall back to its native Java-based downloading tool.")
    @JIPipeParameter("external-downloader-process")
    public DownloadEnvironment getExternalDownloaderProcess() {
        return externalDownloaderProcess;
    }

    @JIPipeParameter("external-downloader-process")
    public void setExternalDownloaderProcess(DownloadEnvironment externalDownloaderProcess) {
        this.externalDownloaderProcess = externalDownloaderProcess;
    }

    public static class DownloadEnvironment extends ProcessEnvironment {

        public DownloadEnvironment() {
        }

        public DownloadEnvironment(ProcessEnvironment other) {
            super(other);
        }

        @JIPipeDocumentation(name = "Arguments", description = "Arguments passed to the process.")
        @JIPipeParameter("arguments")
        @ExpressionParameterSettings(variableSource = VariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Output file", key = "output_file", description = "The downloaded file")
        @ExpressionParameterSettingsVariable(name = "URL", key = "url", description = "The URL")
        @JsonGetter("arguments")
        @Override
        public DefaultExpressionParameter getArguments() {
            return super.getArguments();
        }

        @JIPipeParameter("arguments")
        @JsonSetter("arguments")
        @Override
        public void setArguments(DefaultExpressionParameter arguments) {
            super.setArguments(arguments);
        }
    }
}
