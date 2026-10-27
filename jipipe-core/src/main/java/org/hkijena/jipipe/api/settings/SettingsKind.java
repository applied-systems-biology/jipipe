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

package org.hkijena.jipipe.api.settings;

/**
 * The kinds of settings blocks.
 * <p>
 * Each kind is identified by a string prefix that leads the block ID in a
 * settings document, e.g. {@code "sheet:org.hkijena.jipipe:general-ui"}.
 * Block IDs with a prefix that does not match any kind are passed through
 * unchanged (see {@link SettingsBlockPath}).
 */
public enum SettingsKind {
    SHEET("sheet"),
    REGISTRY("registry"),
    STATE("state");

    private final String prefix;

    SettingsKind(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return the string prefix that identifies blocks of this kind in settings documents
     */
    public String prefix() {
        return prefix;
    }

    /**
     * Resolves a prefix to a block kind.
     *
     * @param prefix the prefix to resolve, case-sensitive
     * @return the kind with this prefix, or {@code null} if the prefix is unknown (pass-through marker)
     */
    public static SettingsKind fromPrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        for (SettingsKind kind : values()) {
            if (kind.prefix.equals(prefix)) {
                return kind;
            }
        }
        return null;
    }
}
