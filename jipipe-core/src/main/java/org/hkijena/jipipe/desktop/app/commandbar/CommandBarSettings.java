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

package org.hkijena.jipipe.desktop.app.commandbar;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;

/**
 * Static access to the persisted settings of the per-window command bar
 * (issue #1346, Task 6).
 * <p>
 * The command bar's list mode lives in the application settings document
 * under the {@value #BLOCK_PATH} block, addressed through {@link #LIST_MODE}.
 * The {@link #binding()} method hands out the one live, observable binding
 * over that key: it lazily registers the key with the settings service (the
 * same idempotent pattern as {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings}
 * and
 * {@link org.hkijena.jipipe.api.service.components.JIPipeRecentProjectsRegistry})
 * and then binds it, so every caller of {@link #currentMode()} and every
 * writer through the same binding (the Keyboard settings page) sees the same
 * value and change events.
 * <p>
 * <b>Settings rule.</b> The default value is never materialized into the
 * document: the binding only writes on an actual user change, and writing a
 * value that equals the default removes the sub-path again (the
 * {@link SettingsBinding} contract), so a pristine settings file stays free
 * of this key.
 */
public final class CommandBarSettings {
    /**
     * The canonical block path of the command bar state in the application
     * settings document.
     */
    public static final String BLOCK_PATH = "registry:command-bar";

    /**
     * The typed key of the command bar's list mode: the persisted id of a
     * {@link JIPipeDesktopCommandBarModel.ListMode} at the {@code list-mode}
     * sub-path of the {@value #BLOCK_PATH} block.
     */
    public static final JIPipeSettingKey<String> LIST_MODE =
            JIPipeSettingKey.string(BLOCK_PATH, "list-mode", String.class)
                    .defaultValue("grayed")
                    .name("Command bar list mode")
                    .description("Which commands the command bar lists: grayed-out unavailable commands, only available ones, or everything");

    /**
     * The one live binding over {@link #LIST_MODE}, created on first use.
     */
    private static volatile SettingsBinding<String> binding;

    private CommandBarSettings() {
    }

    /**
     * @return the live, observable binding over the list mode. The first
     * call registers {@link #LIST_MODE} with the settings service
     * (idempotently) and then binds it; later calls return the same binding.
     */
    public static synchronized SettingsBinding<String> binding() {
        if (binding == null) {
            JIPipe.getSettingsService().registerKey(LIST_MODE);
            binding = JIPipe.getSettingsService().bind(LIST_MODE);
        }
        return binding;
    }

    /**
     * @return the currently persisted list mode, never {@code null}: an empty
     * store reports the key's default (which parses to
     * {@link JIPipeDesktopCommandBarModel.ListMode#GRAYED}), and a hand-edited
     * settings document holding an unknown id falls back to
     * {@link JIPipeDesktopCommandBarModel.ListMode#GRAYED} through
     * {@link JIPipeDesktopCommandBarModel.ListMode#parse(String)}.
     */
    public static JIPipeDesktopCommandBarModel.ListMode currentMode() {
        // binding().get() falls back to the key's default for an absent or
        // malformed value (including a stored JSON null), and this key's
        // default is the non-null "grayed"; parse is null-safe as well, so
        // the read is correct even if a null ever slips through.
        return JIPipeDesktopCommandBarModel.ListMode.parse(binding().get());
    }
}
