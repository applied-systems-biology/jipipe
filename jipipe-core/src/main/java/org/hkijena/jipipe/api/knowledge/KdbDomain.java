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

package org.hkijena.jipipe.api.knowledge;

import java.util.Optional;

/**
 * The closed set of KDB domains (design spec 5.1). A domain is an entry
 * attribute derived from the first path segment under the corpus root: it is
 * not a slot and not a YAML field. The ids are stable string literals
 * (persisted as the first segment of every entry key and used as the exact
 * match value of the domain filter), so they must never be derived from code.
 */
public enum KdbDomain {

    CORE_SEMANTICS("core-semantics"),
    PARAMETERS("parameters"),
    EXPRESSIONS("expressions"),
    DATA_MANAGEMENT("data-management"),
    FILE_FORMATS("file-formats"),
    BIOIMAGE_ANALYSIS("bioimage-analysis"),
    INTEGRATION("integration"),
    PROJECT_MANAGEMENT("project-management"),
    RUNNING_PIPELINES("running-pipelines"),
    PLUGIN_DEV("plugin-dev"),
    TABLES("tables");

    private final String id;

    KdbDomain(String id) {
        this.id = id;
    }

    /**
     * The stable domain id, identical to the first path segment of entries
     * in this domain.
     *
     * @return the domain id
     */
    public String getId() {
        return id;
    }

    /**
     * Looks a domain up by its id.
     *
     * @param id the domain id
     * @return the domain, or empty if the id is not part of the closed set
     */
    public static Optional<KdbDomain> byId(String id) {
        for (KdbDomain domain : values()) {
            if (domain.id.equals(id)) {
                return Optional.of(domain);
            }
        }
        return Optional.empty();
    }
}
