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
 * Thrown when a write to a setting is rejected by the access policy: the
 * write guard installed on the settings component
 * ({@code JIPipeSettingsServiceComponent#setWriteGuard}) denied it (e.g. a
 * remote runtime host that owns the runtime document), or the key is not
 * writable by the calling party.
 * <p>
 * A rejected write is rejected <em>before</em> the document is mutated: the
 * setting keeps its previous value (see {@code SettingsBinding#set} and
 * {@code SettingsBinding#reset}).
 */
public class SettingsAccessDeniedException extends RuntimeException {

    private final String blockPath;
    private final String subPath;
    private final SettingsAccessPolicy access;

    /**
     * @param blockPath the canonical block path of the rejected setting
     * @param subPath the slash-separated sub-path of the rejected setting
     * @param access the access policy of the rejected setting
     */
    public SettingsAccessDeniedException(String blockPath, String subPath, SettingsAccessPolicy access) {
        super("Write access denied for setting " + blockPath + "/" + subPath
                + " (access policy: " + access + ")");
        this.blockPath = blockPath;
        this.subPath = subPath;
        this.access = access;
    }

    /**
     * @return the canonical block path of the rejected setting
     */
    public String getBlockPath() {
        return blockPath;
    }

    /**
     * @return the slash-separated sub-path of the rejected setting
     */
    public String getSubPath() {
        return subPath;
    }

    /**
     * @return the access policy of the rejected setting
     */
    public SettingsAccessPolicy getAccess() {
        return access;
    }
}