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

import java.util.Objects;

/**
 * A group of related capabilities, e.g. "AI", "Python" or "Agents".
 * <p>
 * Domains own their corner of the registry: capability ids belong to exactly
 * one domain. Family alias edges (which plain ids derive to which, e.g.
 * {@code python3} to {@code python}) are declared on the providers of that
 * family and are consulted registry-wide during derivation.
 */
public final class JIPipeCapabilityDomain {
    private final String id;
    private final String name;
    private final String description;
    private final String iconPath;

    /**
     * Creates a domain.
     *
     * @param id          the stable domain id (a string literal, never code-derived)
     * @param name        the display name
     * @param description the human-readable description
     * @param iconPath    path to an icon in the plugin resources, or null
     */
    public JIPipeCapabilityDomain(String id, String name, String description, String iconPath) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.iconPath = iconPath;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconPath() {
        return iconPath;
    }
}
