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
 * The nature of what a {@link JIPipeCapabilityProvider} is based on.
 * <p>
 * Each kind owns a prefix that is stripped when a provider id is derived down
 * into the plain capability space, e.g. {@code artifact:system:python3}
 * derives {@code system:python3}.
 */
public enum JIPipeProviderKind {
    /**
     * A downloadable artifact from an artifact repository.
     */
    ARTIFACT("artifact:"),
    /**
     * A configured environment, e.g. a Python or R interpreter setup.
     */
    ENVIRONMENT("environment:"),
    /**
     * A service that can be started or enabled, e.g. the MCP server.
     */
    SERVICE("service:"),
    /**
     * An executable on the machine or in a managed location.
     */
    EXECUTABLE("executable:"),
    /**
     * Anything else that does not fit the other kinds.
     */
    CUSTOM("custom:");

    private final String prefix;

    JIPipeProviderKind(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the id prefix owned by this kind, including the trailing colon
     */
    public String getPrefix() {
        return prefix;
    }
}
