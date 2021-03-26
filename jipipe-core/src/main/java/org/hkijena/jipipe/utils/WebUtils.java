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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WebUtils {
    private static boolean isRedirected(Map<String, List<String>> header) {
        for (String hv : header.get(null)) {
            if (hv.contains(" 301 ")
                    || hv.contains(" 302 ")) return true;
        }
        return false;
    }

    /**
     * Downloads the URL to the output file
     *
     * @param url             the URL
     * @param outputFile      the output file
     * @param bytesDownloaded progress on how many bytes were downloaded
     * @param isCancelled
     * @throws IOException thrown by streaming
     */
    public static void download(URL url, Path outputFile, Consumer<Integer> bytesDownloaded, Supplier<Boolean> isCancelled) throws IOException {
        HttpURLConnection http = (HttpURLConnection) url.openConnection();

        // Handle redirection
        Map<String, List<String>> header = http.getHeaderFields();
        while (isRedirected(header)) {
            String link = header.get("Location").get(0);
            url = new URL(link);
            http = (HttpURLConnection) url.openConnection();
            header = http.getHeaderFields();
        }

        // Download the file
        try (InputStream input = http.getInputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            int total = 0;
            try (OutputStream output = new FileOutputStream(outputFile.toFile())) {
                while ((n = input.read(buffer)) != -1) {
                    if (isCancelled.get())
                        return;
                    total += n;
                    output.write(buffer, 0, n);
                    bytesDownloaded.accept(total);
                }
            }
        }
    }
}
