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

package org.hkijena.jipipe.api.shortcuts;

import javax.swing.KeyStroke;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A named, frozen set of keymap overrides: a shortcut preset.
 * <p>
 * A preset captures a whole keymap as an override map keyed by the stable
 * string-literal {@link JIPipeShortcutAction} id. Applying a preset
 * one-way-merges these overrides into the user's {@link KeymapSettings}
 * (see {@link JIPipeShortcutProfiles#apply(JIPipeShortcutProfile, KeymapSettings)});
 * afterwards every individual binding remains editable, and a per-action reset
 * returns that action to its registry default.
 * <p>
 * The bundled presets are {@link JIPipeShortcutProfiles#DEFAULT} (clears all
 * overrides, restoring the registry defaults) and
 * {@link JIPipeShortcutProfiles#LEGACY} (restores the pre-6.0 bindings).
 *
 * @param id        the stable preset id, e.g. {@code "default"} or {@code "legacy"}
 * @param name      the human-readable preset name shown in the settings UI
 * @param overrides the frozen override map, keyed by action id; each value is a
 *                  list of chord variants, each variant a list of key strokes
 */
public record JIPipeShortcutProfile(String id, String name, Map<String, List<List<KeyStroke>>> overrides) {
    /**
     * Defensive compact constructor: the override map is copied so the preset
     * stays frozen no matter what the caller passes in.
     */
    public JIPipeShortcutProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(overrides, "overrides");
        overrides = Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
    }
}
