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
 * Exception thrown when a server fails to start.
 */
public class ServerStartException extends RuntimeException {
    private final Reason reason;

    public enum Reason {
        PortUnavailable,
        ProcessFailed,
        HealthCheckFailed,
        Timeout,
        ConfigurationError,
        Unknown
    }

    public ServerStartException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public ServerStartException(String message, Reason reason, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
