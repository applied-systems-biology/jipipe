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
 * The command bar's list mode and recently-fired list live in the
 * application settings document under the {@value #BLOCK_PATH} block,
 * addressed through {@link #LIST_MODE} and {@link #STATE} respectively.
 * {@link #binding()} and {@link #stateBinding()} hand out the one live,
 * observable binding over each key: they lazily register the key with the
 * settings service (the same idempotent pattern as
 * {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings} and
 * {@link org.hkijena.jipipe.api.service.components.JIPipeRecentProjectsRegistry})
 * and then bind it, so every caller of {@link #currentMode()} or
 * {@link #recordFired(String)} and every writer through the same binding
 * (the Keyboard settings page) sees the same value and change events.
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
     * The typed key of the command bar's recently-fired list: one
     * {@link CommandBarState} at the {@code state} sub-path of the
     * {@value #BLOCK_PATH} block. Its default is {@code null}: an absent
     * sub-path reads as a state without content, and the
     * {@link SettingsBinding} contract removes it on reset, so a fresh
     * profile stays free of this key.
     */
    public static final JIPipeSettingKey<CommandBarState> STATE =
            JIPipeSettingKey.object(BLOCK_PATH, "state", CommandBarState.class)
                    .name("Command bar recently used commands")
                    .description("The ids of the recently fired command bar commands, most recent first (bounded at 20)");

    /**
     * The one live binding over {@link #LIST_MODE}, created on first use.
     */
    private static volatile SettingsBinding<String> binding;

    /**
     * The one live binding over {@link #STATE}, created on first use.
     * Independent of the {@link #LIST_MODE} binding: the two keys of this
     * block register lazily and separately.
     */
    private static volatile SettingsBinding<CommandBarState> stateBinding;

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
     * @return the live, observable binding over {@link #STATE}. The first
     * call registers {@link #STATE} with the settings service
     * (idempotently) and then binds it; later calls return the same binding.
     */
    public static synchronized SettingsBinding<CommandBarState> stateBinding() {
        if (stateBinding == null) {
            JIPipe.getSettingsService().registerKey(STATE);
            stateBinding = JIPipe.getSettingsService().bind(STATE);
        }
        return stateBinding;
    }

    /**
     * Records a fired registry command as recently used: reads the current
     * state (an absent value counts as an empty state), moves the action id
     * to the front (deduplicated, capped), and writes the result back
     * through the binding only when it has content. An empty state is never
     * materialized into the document (the settings rule), so firing an
     * unrecordable id - or a bar that never fired a registry command -
     * leaves a pristine profile pristine.
     *
     * @param actionId the id of the fired registry command; {@code null}
     *                 (ephemeral entries carry none) is not recorded
     */
    public static void recordFired(String actionId) {
        CommandBarState next = CommandBarState.updated(stateBinding().get(), actionId);
        if (next != null) {
            stateBinding().set(next);
        }
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
