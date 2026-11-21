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

package org.hkijena.jipipe.api.capabilities;

/**
 * How a provider that is not present right now can be installed or
 * configured, and what it costs.
 * <p>
 * A hint is attached to a {@link JIPipeCapabilityProvider} and answered by
 * {@link JIPipeCapabilityQuery} for the highest-ranked absent candidate. The
 * {@code method} is one of the constants below.
 *
 * @param method      how to obtain the provider: {@link #METHOD_ARTIFACT},
 *                    {@link #METHOD_AUTO_ON_DEMAND}, {@link #METHOD_MANUAL}
 *                    or {@link #METHOD_NONE}
 * @param size        a human-readable size hint, e.g. {@code "4GB"}, or
 *                    {@link #UNKNOWN_SIZE} when it is not known, or null
 * @param instruction human-readable steps for a manual install, or null
 */
public record JIPipeInstallHint(String method, String size, String instruction) {
    /**
     * The provider is a downloadable artifact from an artifact repository.
     */
    public static final String METHOD_ARTIFACT = "artifact";
    /**
     * The provider is installed automatically on first use.
     */
    public static final String METHOD_AUTO_ON_DEMAND = "auto_on_demand";
    /**
     * The provider must be installed or configured by hand; {@code instruction}
     * carries the steps.
     */
    public static final String METHOD_MANUAL = "manual";
    /**
     * The provider cannot be installed or configured through JIPipe. This is
     * also the hint returned by a query when no absent candidate carries a
     * usable hint.
     */
    public static final String METHOD_NONE = "none";

    /**
     * The value for a hint whose size is not known.
     */
    public static final String UNKNOWN_SIZE = "unknown";

    /**
     * @return the method constant for a hint that does not describe how to
     * install the provider
     */
    public static JIPipeInstallHint none() {
        return new JIPipeInstallHint(METHOD_NONE, null, null);
    }

    /**
     * @return true when the hint describes a way to install the provider,
     * i.e. its method is not {@link #METHOD_NONE}
     */
    public boolean isInstallable() {
        return method != null && !METHOD_NONE.equals(method);
    }
}
