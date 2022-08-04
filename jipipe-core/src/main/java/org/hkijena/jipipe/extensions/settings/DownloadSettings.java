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

    private DownloadTool downloadTool = DownloadTool.Native;

    private DownloadEnvironment wgetProcess = new DownloadEnvironment();

    private DownloadEnvironment curlProcess = new DownloadEnvironment();

    private DownloadEnvironment customProcess = new DownloadEnvironment();

    /**
     * Creates a new instance
     */
    public DownloadSettings() {
        initEnvironmentDefaults();
        autoDetectEnvironments();
    }

    private void initEnvironmentDefaults() {
        wgetProcess.setArguments(new DefaultExpressionParameter("ARRAY(\"-O\", output_file, url)"));
        curlProcess.setArguments(new DefaultExpressionParameter("ARRAY(url, \"--output\", output_file)"));
    }

    private void autoDetectEnvironments() {

        // Wget
        if(!wgetProcess.generateValidityReport().isValid()) {
            if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
                Path wgetPath = PathUtils.findAnyOf(Paths.get("/bin/wget"), Paths.get("/usr/local/bin/wget"), Paths.get("/usr/bin/wget"));
                if (wgetPath != null && Files.isRegularFile(wgetPath)) {
                    if(SystemUtils.IS_OS_LINUX) {
                        wgetProcess.setExecutablePathLinux(wgetPath);
                    }
                    else {
                        wgetProcess.setExecutablePathOSX(wgetPath);
                    }
                }
            }
        }

        // Curl
        if(!curlProcess.generateValidityReport().isValid()) {
            Path curlPath;
            if(SystemUtils.IS_OS_WINDOWS) {
                curlPath = PathUtils.findAnyOf(Paths.get("C:\\Windows\\System32\\curl.exe"));
            }
            else {
                curlPath = PathUtils.findAnyOf(Paths.get("/bin/wget"), Paths.get("/usr/local/bin/wget"), Paths.get("/usr/bin/wget"));
            }
            if (curlPath != null && Files.isRegularFile(curlPath)) {
                if(SystemUtils.IS_OS_LINUX) {
                    curlProcess.setExecutablePathLinux(curlPath);
                }
                else if(SystemUtils.IS_OS_MAC_OSX) {
                    curlProcess.setExecutablePathOSX(curlPath);
                }
                else if(SystemUtils.IS_OS_WINDOWS) {
                    curlProcess.setExecutablePathWindows(curlPath);
                }
            }
        }

        // Auto-select default
        if(wgetProcess.generateValidityReport().isValid()) {
            downloadTool = DownloadTool.wget;
        }
        else if(curlProcess.generateValidityReport().isValid()) {
            downloadTool = DownloadTool.cURL;
        }
    }

    @JIPipeDocumentation(name = "Download tool", description = "Determines how remote files are downloaded. " +
            "<ul>" +
            "<li>Native: the integrated download algorithm. Does not depend on any third-party tool, but cannot handle unstable networks.</li>" +
            "<li>Wget: use the wget command line tool. Might be only available on Linux.</li>" +
            "<li>cURL: use the cURL command line tool. It is available on Windows, Linux, and macOS.</li>" +
            "<li>Custom: use a custom tool</li>" +
            "</ul>")
    @JIPipeParameter(value = "download-tool")
    public DownloadTool getDownloadTool() {
        return downloadTool;
    }

    @JIPipeParameter("download-tool")
    public void setDownloadTool(DownloadTool downloadTool) {
        this.downloadTool = downloadTool;
    }

    @JIPipeDocumentation(name = "wget process", description = "The configuration for wget")
    @JIPipeParameter("wget-process")
    public DownloadEnvironment getWgetProcess() {
        return wgetProcess;
    }

    @JIPipeParameter("wget-process")
    public void setWgetProcess(DownloadEnvironment wgetProcess) {
        this.wgetProcess = wgetProcess;
    }

    @JIPipeDocumentation(name = "cURL process", description = "The configuration for cURL")
    @JIPipeParameter("curl-process")
    public DownloadEnvironment getCurlProcess() {
        return curlProcess;
    }

    @JIPipeParameter("curl-process")
    public void setCurlProcess(DownloadEnvironment curlProcess) {
        this.curlProcess = curlProcess;
    }

    @JIPipeDocumentation(name = "Custom process", description = "A custom process")
    @JIPipeParameter("custom-process")
    public DownloadEnvironment getCustomProcess() {
        return customProcess;
    }

    @JIPipeParameter("custom-process")
    public void setCustomProcess(DownloadEnvironment customProcess) {
        this.customProcess = customProcess;
    }

    public static DownloadSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, DownloadSettings.class);
    }

    public enum DownloadTool {
        Native,
        wget,
        cURL,
        Custom
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
