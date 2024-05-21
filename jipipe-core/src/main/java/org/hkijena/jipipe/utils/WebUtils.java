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

package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.processes.ProcessEnvironment;
import org.hkijena.jipipe.plugins.settings.JIPipeDownloadsApplicationSettings;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WebUtils {
    private static boolean isRedirected(Map<String, List<String>> header) {
        for (String hv : header.get(null)) {
            if (hv.contains(" 301 ")
                    || hv.contains(" 302 ")) return true;
        }
        return false;
    }

    public static void download(URL url, Path outputFile, String label, JIPipeProgressInfo progressInfo) {
        boolean useExternalDownloader;
        if (JIPipe.isInstantiated()) {
            JIPipeDownloadsApplicationSettings settings = null;
            try {
                settings = JIPipeDownloadsApplicationSettings.getInstance();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            if (settings != null) {
                useExternalDownloader = settings.isPreferCustomDownloader() && settings.getExternalDownloaderProcess().generateValidityReport(new UnspecifiedValidationReportContext()).isValid();
                try {
                    if (!useExternalDownloader) {
                        downloadNative(url, outputFile, label, progressInfo);
                    } else {
                        ProcessEnvironment process = settings.getExternalDownloaderProcess();
                        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
                        variables.set("output_file", outputFile.toAbsolutePath().toString());
                        variables.set("url", url.toString());
                        ProcessUtils.runProcess(process, variables, Collections.emptyMap(), true, progressInfo);
                    }

                    if (Files.isRegularFile(outputFile)) {
                        return; // We are done here if it finishes
                    } else {
                        if (!useExternalDownloader) {
                            throw new RuntimeException("Output file " + outputFile + " does not exist!");
                        }
                        progressInfo.log("Output file " + outputFile + " does not exist!");
                        progressInfo.log("Falling back to native downloader");
                    }

                } catch (Throwable ex) {
                    if (!useExternalDownloader) {
                        throw ex;
                    }
                    progressInfo.log("Error: " + ex);
                    progressInfo.log("Falling back to native downloader");
                }
            } else {
                progressInfo.log("Falling back to native downloader");
            }
        }
        // Fall back to native downloader
        downloadNative(url, outputFile, label, progressInfo);
    }

    /**
     * The native download method written in Java
     *
     * @param url          the URL
     * @param outputFile   the output file
     * @param label        the label
     * @param progressInfo the progress info
     */
    public static void downloadNative(URL url, Path outputFile, String label, JIPipeProgressInfo progressInfo) {
        DecimalFormat df = new DecimalFormat("0.00");
        df.setGroupingUsed(false);
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.CEILING);
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            // Handle redirection
            Map<String, List<String>> header = http.getHeaderFields();
            while (isRedirected(header)) {
                String link = header.get("Location").get(0);
                url = new URL(link);
                http = (HttpURLConnection) url.openConnection();
                header = http.getHeaderFields();
            }

            long contentLength = http.getContentLengthLong();
            long lastMessageTime = System.currentTimeMillis();

            // Download the file
            try (InputStream input = http.getInputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                long total = 0;
                try (OutputStream output = new FileOutputStream(outputFile.toFile())) {
                    while ((n = input.read(buffer)) != -1) {
                        if (progressInfo.isCancelled())
                            return;
                        total += n;
                        output.write(buffer, 0, n);
                        long currentMessageTime = System.currentTimeMillis();
                        if (currentMessageTime - lastMessageTime > 1000) {
                            lastMessageTime = currentMessageTime;
                            String message;
                            if (contentLength <= 0) {
                                message = "Downloaded " + df.format(total / 1024.0 / 1024.0) + " MB";
                            } else {
                                message = "Downloaded " + df.format(total / 1024.0 / 1024.0) + " MB / " + df.format(contentLength / 1024.0 / 1024.0) + " MB";
                            }
                            progressInfo.log(message);
                            if (progressInfo.isCancelled())
                                return;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Error while downloading!",
                    "At " + label + ": there was an error downloading URL '" + url + "' to " + outputFile,
                    "Please check if the URL is valid, an internet connection is available, and the target device has enough space.");
        }
    }
}
