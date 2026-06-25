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

package org.hkijena.jipipe.api.servers;

/**
 * Strategy interface for checking if a server is healthy and responsive.
 */
public interface JIPipeServerHealthCheck {
    /**
     * Checks if the server at the given host and port is healthy.
     *
     * @param host the host (typically "127.0.0.1")
     * @param port the port
     * @return true if the server is healthy
     */
    boolean isHealthy(String host, int port);
}
