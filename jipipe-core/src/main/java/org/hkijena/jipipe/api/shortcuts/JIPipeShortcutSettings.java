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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;

/**
 * Static access to the persisted keymap state of the application.
 * <p>
 * The {@link KeymapSettings} live in the application settings document under
 * the {@value #BLOCK_PATH} block, addressed through {@link #KEYMAP}. The
 * {@link #binding()} method hands out the one live, observable binding over
 * that key: it lazily registers the key with the settings service (the same
 * idempotent pattern as
 * {@link org.hkijena.jipipe.api.service.components.JIPipeRecentProjectsRegistry})
 * and then binds it, so every caller of {@link #current()} and every writer
 * through the same binding sees the same value and change events.
 */
public final class JIPipeShortcutSettings {
    /**
     * The canonical block path of the keymap state in the application settings
     * document.
     */
    public static final String BLOCK_PATH = "registry:keymap";

    /**
     * The typed key of the keymap state: one {@link KeymapSettings} object at
     * the block level (empty sub-path) of the {@value #BLOCK_PATH} block.
     */
    public static final JIPipeSettingKey<KeymapSettings> KEYMAP =
            JIPipeSettingKey.object(BLOCK_PATH, "", KeymapSettings.class);

    /**
     * The one live binding over {@link #KEYMAP}, created on first use.
     */
    private static volatile SettingsBinding<KeymapSettings> binding;

    private JIPipeShortcutSettings() {
    }

    /**
     * @return the live, observable binding over the keymap state. The first
     * call registers {@link #KEYMAP} with the settings service (idempotently)
     * and then binds it; later calls return the same binding.
     */
    public static synchronized SettingsBinding<KeymapSettings> binding() {
        if (binding == null) {
            JIPipe.getSettingsService().registerKey(KEYMAP);
            binding = JIPipe.getSettingsService().bind(KEYMAP);
        }
        return binding;
    }

    /**
     * @return the current keymap state, i.e. the bound value, or the key's
     * default when nothing is stored yet
     */
    public static KeymapSettings current() {
        return binding().get();
    }
}
