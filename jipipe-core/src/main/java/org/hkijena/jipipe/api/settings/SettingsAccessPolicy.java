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
 * Who may write a setting.
 * <p>
 * {@link #USER_EDITABLE} settings can be changed from the settings UI and
 * programmatically; {@link #RUNTIME_ADMIN} settings may only be written by
 * runtime administration (e.g. the runtime process), not by user edits in
 * the settings UI; {@link #READ_ONLY} settings may only be read — they
 * mirror externally-managed state and any write is rejected.
 */
public enum SettingsAccessPolicy {
    USER_EDITABLE,
    RUNTIME_ADMIN,
    READ_ONLY
}
