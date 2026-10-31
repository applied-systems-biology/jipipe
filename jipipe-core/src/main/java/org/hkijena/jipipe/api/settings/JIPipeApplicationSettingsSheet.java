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

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

/**
 * A settings sheet used for storing application-wide settings
 */
public interface JIPipeApplicationSettingsSheet extends JIPipeParameterCollection, JIPipeSettingsSheet {

    /**
     * Whether the engine must not seed a document block for this sheet.
     * <p>
     * The settings engine seeds an absent block from the sheet's current values
     * when the sheet is attached (and on first materialization), which is how a
     * fresh profile gets the old built-in defaults. A sheet that has no
     * parameters of its own — an <em>anchor</em> sheet that only hosts a merged
     * page of the settings tab — would leave an empty block behind, pure profile
     * noise. Such anchors override this to {@code true}: their block is only
     * created if something actually writes to it.
     * <p>
     * Seeding is also skipped when the sheet has no parameters at all, so an
     * anchor that forgets the override does not pollute the profile either; the
     * flag documents the intent and keeps a future parameter-carrying anchor
     * seedable without editing the engine.
     *
     * @return whether the engine skips seeding this sheet's block
     */
    default boolean isSeedExempt() {
        return false;
    }
}
