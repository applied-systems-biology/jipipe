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

import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutBinding;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.KeymapSettings;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The pure model layer of the per-window command bar (design Section 4):
 * it turns the registered shortcut actions plus the window's ephemeral
 * commands (window/tab switching) into a flat list of
 * {@link Entry entries} with category group headers, effective bindings,
 * live availability, and the chord-prefix filter.
 * <p>
 * Everything here is side-effect-free: the list layout never depends on
 * component state, and availability is injected as a resolver function
 * (in the UI this is a composition of
 * {@code JIPipeDesktopShortcutDispatcher.scopesContaining} and
 * {@code resolveAction}). No Swing component is created or queried, so the
 * model is fully testable headless.
 */
public final class JIPipeDesktopCommandBarModel {
    private JIPipeDesktopCommandBarModel() {
    }

    /**
     * How the list treats registry commands that are not available in the
     * current window state (design Section 4.1, decision 4), and which
     * window-kind filter applies (issue #1346 review): GRAYED and
     * AVAILABLE_ONLY hide graph-family actions while no graph editor hosts
     * the context (the filter is applied by the bar before it calls
     * {@link #collect}; see {@code JIPipeDesktopCommandBar#modeFilteredActions}),
     * while EVERYTHING is a real mode without that filter.
     */
    public enum ListMode {
        /**
         * Unavailable commands are shown, grayed and not fireable. The
         * default mode. Graph-family commands are additionally hidden
         * while no graph editor hosts the context.
         */
        GRAYED("grayed"),
        /**
         * Unavailable commands are hidden; a category whose commands are
         * all unavailable loses its header as well. Graph-family commands
         * are additionally hidden while no graph editor hosts the context.
         */
        AVAILABLE_ONLY("available-only"),
        /**
         * A real mode of its own (issue #1346 review): no window-kind
         * filtering - every registry action the caller passes is listed,
         * and unavailable entries render grayed like in {@link #GRAYED}.
         */
        EVERYTHING("everything");

        private final String id;

        ListMode(String id) {
            this.id = id;
        }

        /**
         * @return the stable id this mode is persisted under (string
         * literal, never code-derived)
         */
        public String id() {
            return id;
        }

        /**
         * Parses a persisted mode id.
         *
         * @param id the stored id, may be {@code null}
         * @return the matching mode, or {@link #GRAYED} for {@code null}
         * and unknown values (a setting of a future version must not break
         * an older build)
         */
        public static ListMode parse(String id) {
            if (id == null) {
                return GRAYED;
            }
            for (ListMode mode : values()) {
                if (mode.id.equals(id)) {
                    return mode;
                }
            }
            return GRAYED;
        }
    }

    /**
     * One row of the command bar list: a category group header, a registry
     * command, or an ephemeral command.
     */
    public interface Entry {
        /**
         * @return the display name of the row (the category name for headers)
         */
        String name();

        /**
         * @return the category the row belongs to, for grouping and for the
         * search filter
         */
        String category();

        /**
         * @return the row icon, or {@code null}
         */
        Icon icon();

        /**
         * @return the rendered key binding for the row (e.g.
         * {@code "Ctrl+T then M"}); an empty string for a registry command
         * with no binding (the unassigned {@code formatVariants} renders
         * empty), or {@code null} for rows that carry no binding at all
         * (category headers, ephemeral entries)
         */
        String bindingText();

        /**
         * @return the effective binding variants; empty for ephemeral
         * entries and unassigned commands
         */
        List<List<KeyStroke>> variants();

        /**
         * @return whether the command is available in the current window
         * state (always true for ephemeral entries)
         */
        boolean isAvailable();

        /**
         * @return whether the row is a registry command with a configurable
         * key binding (true for registry commands, false for headers and
         * ephemeral entries)
         */
        boolean isConfigurable();

        /**
         * @return the id of the underlying shortcut action, or
         * {@code null} for headers and ephemeral entries
         */
        String actionId();

        /**
         * @return the runnable that fires the command, or {@code null} when
         * the entry is not available (category headers never carry a
         * command)
         */
        Runnable command();

        /**
         * @return whether this row is a category group header
         */
        boolean isCategoryHeader();

        /**
         * Creates a category group header.
         *
         * @param category the category to group
         */
        static Entry categoryHeader(String category) {
            Objects.requireNonNull(category, "category");
            return new Entry() {
                @Override
                public String name() {
                    return category;
                }

                @Override
                public String category() {
                    return category;
                }

                @Override
                public Icon icon() {
                    return null;
                }

                @Override
                public String bindingText() {
                    return null;
                }

                @Override
                public List<List<KeyStroke>> variants() {
                    return List.of();
                }

                @Override
                public boolean isAvailable() {
                    return false;
                }

                @Override
                public boolean isConfigurable() {
                    return false;
                }

                @Override
                public String actionId() {
                    return null;
                }

                @Override
                public Runnable command() {
                    return null;
                }

                @Override
                public boolean isCategoryHeader() {
                    return true;
                }
            };
        }

        /**
         * Creates an ephemeral command: a row that is generated at open
         * time (window and tab switching), never bindable, and never
         * rendered with a binding.
         *
         * @param name     the display name
         * @param category the category, e.g. "Windows" or "Tabs"
         * @param icon     the row icon, may be {@code null}
         * @param command  the runnable to fire
         */
        static Entry ephemeral(String name, String category, Icon icon, Runnable command) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(command, "command");
            return new Entry() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public String category() {
                    return category;
                }

                @Override
                public Icon icon() {
                    return icon;
                }

                @Override
                public String bindingText() {
                    return null;
                }

                @Override
                public List<List<KeyStroke>> variants() {
                    return List.of();
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                public boolean isConfigurable() {
                    return false;
                }

                @Override
                public String actionId() {
                    return null;
                }

                @Override
                public Runnable command() {
                    return command;
                }

                @Override
                public boolean isCategoryHeader() {
                    return false;
                }
            };
        }
    }

    /**
     * Builds the command bar list from the given actions (design Section
     * 4.1): one {@link Entry#categoryHeader category header} before each
     * category group (the groups appear in the <em>given order of the
     * actions</em>, which in the UI is {@link JIPipeShortcutRegistry#all()}
     * - already category-then-name - so this method never re-sorts), then
     * the category's command entries; the given ephemeral entries are
     * appended afterwards in the given order, without their own headers
     * (their categories, e.g. "Windows" and "Tabs", are documented to sort
     * after all registry categories).
     * <p>
     * For each action the entry carries: the name, category, the effective
     * binding rendered with
     * {@link JIPipeShortcutRegistry#formatVariants(List)} (empty string for
     * an unassigned action), and the live availability as reported by
     * {@code resolver} (an entry is available exactly when the resolver
     * returns a non-null runnable; that runnable becomes the entry's
     * {@link Entry#command()}).
     * <p>
     * The mode is applied after construction (design Section 4.1, decision
     * 4):
     * <ul>
     * <li>{@link ListMode#GRAYED} keeps unavailable entries, with
     * {@code command()} {@code null} (the UI renders them grayed and
     * non-fireable);</li>
     * <li>{@link ListMode#AVAILABLE_ONLY} drops unavailable entries, and
     * drops a category header that would have no entries left under it;</li>
     * <li>{@link ListMode#EVERYTHING} behaves like {@link ListMode#GRAYED}
     * in this method: no family filtering happens here. The window-kind
     * filter of GRAYED/AVAILABLE_ONLY (graph-family actions hidden while no
     * graph editor hosts the context) is the bar's job, applied to the
     * action list before this method is called, so EVERYTHING receives the
     * full registry list and lists commands foreign to the current window
     * kind (grayed when they cannot fire in the current live state).</li>
     * </ul>
     *
     * @param actions   the actions to list, in the order to emit their
     *                  category groups (never re-sorted here)
     * @param settings  the keymap state the effective bindings resolve
     *                  against (user overrides win), may be {@code null}
     * @param mode      how unavailable entries are treated
     * @param resolver  resolves an action id to the runnable that performs
     *                  it in the current window state, or {@code null}
     *                  when the action is not available (in the UI: the
     *                  scope-chain walk of the shortcut dispatcher)
     * @param ephemeral the ephemeral commands to append after all registry
     *                  entries, in the given order
     * @return the flat list of headers and entries
     */
    public static List<Entry> collect(List<JIPipeShortcutAction> actions, KeymapSettings settings,
                                      ListMode mode, Function<String, Runnable> resolver, List<Entry> ephemeral) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(ephemeral, "ephemeral");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        boolean dropUnavailable = mode == ListMode.AVAILABLE_ONLY;

        List<Entry> entries = new ArrayList<>(actions.size() * 2 + ephemeral.size());
        String currentCategory = null;
        int currentHeaderIndex = -1;
        for (JIPipeShortcutAction action : actions) {
            if (!action.category().equals(currentCategory)) {
                currentCategory = action.category();
                if (!dropUnavailable) {
                    // A group header is always emitted in GRAYED/EVERYTHING;
                    // in AVAILABLE_ONLY it is emitted lazily (see below) so
                    // an empty group leaves no header behind.
                    entries.add(Entry.categoryHeader(currentCategory));
                    currentHeaderIndex = entries.size() - 1;
                } else {
                    currentHeaderIndex = -1;
                }
            }
            JIPipeShortcutBinding binding = registry.bindingFor(action.id(), settings);
            Runnable command = resolver.apply(action.id());
            boolean available = command != null;
            Entry entry = registryEntry(action, binding, available ? command : null);
            if (available || !dropUnavailable) {
                if (dropUnavailable && currentHeaderIndex < 0) {
                    entries.add(Entry.categoryHeader(currentCategory));
                    currentHeaderIndex = entries.size() - 1;
                }
                entries.add(entry);
            }
        }
        entries.addAll(ephemeral);
        return List.copyOf(entries);
    }

    /**
     * Builds plain registry rows for the "Recently used" section of the
     * command bar (issue #1346, Task 8): one entry per given action, in
     * the given order (most recent first), resolved exactly like the
     * entries of {@link #collect} (effective binding against the settings,
     * availability through the resolver) with the same mode rule
     * ({@link ListMode#AVAILABLE_ONLY} drops unavailable entries). No
     * category headers are emitted: the caller prepends the single
     * "Recently used" header itself, and commands that also appear in
     * their own category below are NOT deduplicated (VS Code behavior).
     *
     * @param actions  the recently fired actions, in the order to emit
     *                 (never re-sorted here)
     * @param settings the keymap state the effective bindings resolve
     *                 against (user overrides win), may be {@code null}
     * @param mode     how unavailable entries are treated
     * @param resolver resolves an action id to the runnable that performs
     *                 it in the current window state, or {@code null}
     *                 when the action is not available
     * @return the plain registry entries (no headers)
     */
    public static List<Entry> collectRecent(List<JIPipeShortcutAction> actions, KeymapSettings settings,
                                            ListMode mode, Function<String, Runnable> resolver) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(resolver, "resolver");
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        boolean dropUnavailable = mode == ListMode.AVAILABLE_ONLY;
        List<Entry> entries = new ArrayList<>(actions.size());
        for (JIPipeShortcutAction action : actions) {
            Runnable command = resolver.apply(action.id());
            boolean available = command != null;
            if (available || !dropUnavailable) {
                JIPipeShortcutBinding binding = registry.bindingFor(action.id(), settings);
                entries.add(registryEntry(action, binding, available ? command : null));
            }
        }
        return List.copyOf(entries);
    }

    /**
     * Builds the command bar list restricted to the chord layer: the
     * registry entries (category headers and ephemeral entries are
     * excluded) whose effective binding has a length-2 variant whose first
     * stroke equals {@code prefix}, in the given (registry) order, with a
     * stable partition that puts the available entries before the
     * unavailable ones (design Section 6.2 chord mode: the second key of
     * the pressed prefix).
     *
     * @param registryEntries the entries of {@link #collect} for the
     *                        current window state
     * @param prefix          the first stroke of the pending chord, may be
     *                        {@code null} (then nothing matches)
     * @return the matching entries, available first, original order inside
     * each bucket
     */
    public static List<Entry> chordEntries(List<Entry> registryEntries, KeyStroke prefix) {
        Objects.requireNonNull(registryEntries, "registryEntries");
        if (prefix == null) {
            return List.of();
        }
        List<Entry> available = new ArrayList<>();
        List<Entry> unavailable = new ArrayList<>();
        for (Entry entry : registryEntries) {
            if (entry.isCategoryHeader() || entry.actionId() == null) {
                continue;
            }
            for (List<KeyStroke> variant : entry.variants()) {
                if (variant.size() == 2 && variant.get(0).equals(prefix)) {
                    (entry.isAvailable() ? available : unavailable).add(entry);
                    break;
                }
            }
        }
        List<Entry> result = new ArrayList<>(available.size() + unavailable.size());
        result.addAll(available);
        result.addAll(unavailable);
        return List.copyOf(result);
    }

    /**
     * The registry command row: name and category from the action, binding
     * rendered from the effective variants (empty string when unassigned),
     * availability and command from the resolver's verdict.
     */
    private static Entry registryEntry(JIPipeShortcutAction action,
                                       JIPipeShortcutBinding binding,
                                       Runnable command) {
        return new Entry() {
            @Override
            public String name() {
                return action.name();
            }

            @Override
            public String category() {
                return action.category();
            }

            @Override
            public Icon icon() {
                return null;
            }

            @Override
            public String bindingText() {
                return JIPipeShortcutRegistry.formatVariants(binding.variants());
            }

            @Override
            public List<List<KeyStroke>> variants() {
                return binding.variants();
            }

            @Override
            public boolean isAvailable() {
                return command != null;
            }

            @Override
            public boolean isConfigurable() {
                return true;
            }

            @Override
            public String actionId() {
                return action.id();
            }

            @Override
            public Runnable command() {
                return command;
            }

            @Override
            public boolean isCategoryHeader() {
                return false;
            }
        };
    }
}
