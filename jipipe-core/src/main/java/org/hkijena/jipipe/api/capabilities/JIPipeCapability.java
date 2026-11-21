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

import java.util.List;
import java.util.Objects;

/**
 * A capability that users may want to have, e.g. an embedding model or the
 * MCP service, and the requirements that must be satisfied for it to be
 * available.
 * <p>
 * The {@code id} is a stable string literal that is persisted in settings and
 * pipeline descriptions: it must never be derived from code. Requirements are
 * other capability ids; the registry rejects circular requirement graphs at
 * registration, so the graph is always a DAG.
 */
public final class JIPipeCapability {
    private final String id;
    private final String domain;
    private final String name;
    private final String description;
    private final String iconPath;
    private final List<String> requires;

    /**
     * Creates a capability definition.
     *
     * @param id          the stable capability id (a string literal, never code-derived)
     * @param domain      the id of the domain that owns this capability
     * @param name        the display name
     * @param description the human-readable description
     * @param iconPath    path to an icon in the plugin resources, or null
     * @param requires    ids of the capabilities that must be available, may be empty
     */
    public JIPipeCapability(String id, String domain, String name, String description, String iconPath,
                            List<String> requires) {
        this.id = Objects.requireNonNull(id, "id");
        this.domain = Objects.requireNonNull(domain, "domain");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.iconPath = iconPath;
        this.requires = requires == null ? List.of() : List.copyOf(requires);
    }

    public String getId() {
        return id;
    }

    public String getDomain() {
        return domain;
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

    public List<String> getRequires() {
        return requires;
    }
}
