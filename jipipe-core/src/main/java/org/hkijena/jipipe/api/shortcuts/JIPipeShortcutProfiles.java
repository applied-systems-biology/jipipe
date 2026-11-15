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
import java.util.List;
import java.util.Map;

/**
 * The bundled keyboard shortcut presets.
 * <p>
 * A preset is a named, frozen {@link JIPipeShortcutProfile}: a set of
 * overrides that restores a particular keymap. Applying a preset merges its
 * overrides one-way into the user's {@link KeymapSettings}
 * (see {@link #apply(JIPipeShortcutProfile, KeymapSettings)}); afterwards
 * every binding is individually editable again, and a per-action reset returns
 * that action to its registry default.
 * <p>
 * Two presets ship in the first release (design Section 9.3):
 * <ul>
 * <li>{@link #DEFAULT}: clears every override, restoring the registry's
 * revised defaults (design Section 8).</li>
 * <li>{@link #LEGACY}: restores every pre-6.0 binding for the actions whose
 * default changed, so users who relied on the old keys can switch back
 * without re-binding everything by hand.</li>
 * </ul>
 * Applying either preset is idempotent.
 */
public final class JIPipeShortcutProfiles {
    /**
     * The "Default" preset: an empty override map. Applying it clears all
     * user overrides, restoring the registry's default keymap.
     */
    public static final JIPipeShortcutProfile DEFAULT =
            new JIPipeShortcutProfile("default", "Default", Map.of());

    /**
     * The "Legacy (pre-6.0)" preset: restores every pre-6.0 binding that the
     * revised defaults changed (bare F-key tools, F-key notes tools, ESC-cycled
     * notes edit, numpad-only zoom, the {@code L}/{@code Ctrl+L} lock pair, and
     * the {@code Ctrl+P}/{@code Ctrl+Shift+P} pass-through pair).
     */
    public static final JIPipeShortcutProfile LEGACY =
            new JIPipeShortcutProfile("legacy", "Legacy (pre-6.0)", JIPipeShortcutActions.legacyOverrides());

    private JIPipeShortcutProfiles() {
    }

    /**
     * @return the bundled presets, in display order: {@link #DEFAULT} first,
     * then {@link #LEGACY}
     */
    public static List<JIPipeShortcutProfile> bundled() {
        return List.of(DEFAULT, LEGACY);
    }

    /**
     * Applies one preset to the user's keymap state, one-way.
     * <p>
     * The {@link #DEFAULT} preset clears the whole override map, so every
     * action falls back to its registry default. Any other preset merges its
     * overrides into the map: each action in the preset gets its preset chord
     * (replacing a previous override for that action), while overrides for
     * actions the preset does not mention are left untouched.
     *
     * @param profile  the preset to apply, must not be {@code null}
     * @param settings the keymap state to modify in place, must not be
     *                 {@code null}
     */
    public static void apply(JIPipeShortcutProfile profile, KeymapSettings settings) {
        if (profile == null) {
            throw new NullPointerException("profile");
        }
        if (settings == null) {
            throw new NullPointerException("settings");
        }
        if (profile.id().equals(DEFAULT.id())) {
            settings.getOverrides().clear();
            return;
        }
        for (Map.Entry<String, List<List<KeyStroke>>> entry : profile.overrides().entrySet()) {
            settings.setOverride(entry.getKey(), entry.getValue());
        }
    }
}
