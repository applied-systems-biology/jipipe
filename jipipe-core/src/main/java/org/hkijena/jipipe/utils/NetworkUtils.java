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

import net.imagej.ui.swing.updater.ImageJUpdater;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class NetworkUtils {
    /**
     * Taken from {@link ImageJUpdater}
     * Check whether we can connect to the Internet. If we cannot connect, we will
     * not be able to update.
     *
     * @throws IOException if anything goes wrong.
     */
    public static void testNetworkConnection() throws IOException {
        // NB: Remember initial static state, to be reset afterward.
        final boolean followRedirects = HttpURLConnection.getFollowRedirects();

        try {
            HttpURLConnection.setFollowRedirects(false);
            final URL url = new URL("http://imagej.net/");
            final URLConnection urlConn = url.openConnection();
            if (!(urlConn instanceof HttpURLConnection)) {
                throw new IOException("Unexpected connection type: " + //
                        urlConn.getClass().getName());
            }
            final HttpURLConnection httpConn = (HttpURLConnection) urlConn;

            // Perform some sanity checks.
            final int code = httpConn.getResponseCode();
            if (code != 301) {
                throw new IOException("Unexpected response code: " + code);
            }
            final String message = httpConn.getResponseMessage();
            if (!"Moved Permanently".equals(message)) {
                throw new IOException("Unexpected response message: " + message);
            }
            final long length = httpConn.getContentLengthLong();
            if (length < 250 || length > 500) {
                throw new IOException("Unexpected response length: " + length);
            }

            // Header looks reasonable; now let's check the content to be sure.
            final byte[] content = new byte[(int) length];
            try (final DataInputStream din = //
                         new DataInputStream(httpConn.getInputStream())) {
                din.readFully(content);
            }
            final String s = new String(content, "UTF-8");
            if (!s.matches("(?s).*<html>.*" +
                    "<head>.*<title>301 Moved Permanently</title>.*</head>.*" + //
                    "<body>.*<h1>Moved Permanently</h1>.*" + //
                    "<a href=\"http://imagej.net/Welcome\">" + //
                    ".*</body></html>.*")) {
                throw new IOException("Unexpected response:\n" + s);
            }
        } finally {
            // NB: Reset static state back to previous.
            if (followRedirects != HttpURLConnection.getFollowRedirects()) {
                HttpURLConnection.setFollowRedirects(followRedirects);
            }
        }
    }

    public static boolean hasInternetConnection() {
        try {
            testNetworkConnection();
            return true;
        } catch (final SecurityException | IOException exc) {
            return false;
        }
    }
}
