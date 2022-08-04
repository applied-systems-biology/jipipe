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

package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.processes.ProcessEnvironment;
import org.hkijena.jipipe.extensions.settings.DownloadSettings;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
        boolean usedNative = false;
        if (JIPipe.isInstantiated()) {
            DownloadSettings settings = null;
            try {
                settings = DownloadSettings.getInstance();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            if(settings != null) {
                usedNative = settings.getDownloadTool() == DownloadSettings.DownloadTool.Native;
                try {

                    if(settings.getDownloadTool() == DownloadSettings.DownloadTool.Native) {
                        downloadNative(url, outputFile, label, progressInfo);
                    }
                    else {
                        ProcessEnvironment process = null;
                        switch (settings.getDownloadTool()) {
                            case cURL:
                                process = settings.getCurlProcess();
                                break;
                            case wget:
                                process = settings.getWgetProcess();
                                break;
                            case Custom:
                                process = settings.getCustomProcess();
                                break;
                            default:
                                throw new UnsupportedOperationException("Unsupported download tool: " + settings.getDownloadTool());
                        }
                        if(!process.generateValidityReport().isValid()) {
                            throw new RuntimeException("Process for " + settings.getDownloadTool() + " is invalid! Please check the settings!");
                        }
                        ExpressionVariables variables = new ExpressionVariables();
                        variables.set("output_file", outputFile.toAbsolutePath().toString());
                        variables.set("url", url.toString());
                        ProcessUtils.runProcess(process, variables, progressInfo);
                    }

                    return; // We are done here if it finishes
                } catch (Throwable ex) {
                    if(usedNative) {
                        throw ex;
                    }
                    progressInfo.log("Error: " + ex);
                    progressInfo.log("Falling back to native downloader");
                }
            }
            else {
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
            throw new UserFriendlyRuntimeException(e,
                    "Error while downloading!",
                    label,
                    "There was an error downloading URL '" + url + "' to " + outputFile,
                    "Please check if the URL is valid, an internet connection is available, and the target device has enough space.");
        }
    }
}
