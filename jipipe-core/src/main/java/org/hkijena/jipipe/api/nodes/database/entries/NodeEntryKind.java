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

package org.hkijena.jipipe.api.nodes.database.entries;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;

import java.util.*;

/**
 * The filterable <em>kinds</em> of node-database entries, used by the
 * node browser's and the node finder dialog's result-type filter (1340).
 * <p>
 * The categories mirror the entry-kind labels the list renderers already
 * show (the "Example" / "Pattern" / "Custom" kind chips, see
 * {@code JIPipeNodeStateChips#entryKind}) and split the template kinds by
 * <b>origin</b>, so users can hide the system-provided entries while still
 * seeing their own and downloaded ones:
 * <ul>
 * <li>{@link #NODES} - plain node types
 * ({@link CreateNewNodeByInfoDatabaseEntry});</li>
 * <li>{@link #EXAMPLES} - node types offered with a sample configuration
 * ({@link CreateNewNodeByExampleDatabaseEntry});</li>
 * <li>{@link #PATTERNS_SYSTEM} / {@link #PATTERNS_USER} - multi-node
 * template graphs (badge "Pattern", see
 * {@link CreateNewNodesByTemplateDatabaseEntry#getBadge()}), split by
 * origin (system-bundled vs. user-created / downloaded);</li>
 * <li>{@link #CUSTOM_SYSTEM} / {@link #CUSTOM_USER} - single-node
 * templates (badge "Custom"), split by origin.</li>
 * </ul>
 * Entries that do not belong to any kind (existing nodes / compartments,
 * new compartments) return {@code null} from {@link #of} and therefore
 * bypass the kind filter entirely - their visibility is governed by other
 * mechanisms (the finder dialog's "Find existing" toggle).
 * <p>
 * This enum is free of any Swing and {@code JIPipe} instance dependency in
 * {@link #of} / {@link #matches} so it can be unit-tested in isolation;
 * only the persistence helpers below touch the settings registry.
 */
public enum NodeEntryKind {

    NODES("Nodes", "actions/polygon-add-nodes.png", "Nodes",
            "Regular node types (create a new node)"),
    EXAMPLES("Examples", "actions/star.png", "Nodes",
            "Node types with a preconfigured sample setup"),
    PATTERNS_SYSTEM("Pattern (system)", "actions/pattern.png", "Templates",
            "Multi-node workflow patterns provided by JIPipe"),
    PATTERNS_USER("Pattern (user)", "actions/pattern.png", "Templates",
            "Multi-node workflow patterns you created or downloaded"),
    CUSTOM_SYSTEM("Custom (system)", "actions/template.png", "Templates",
            "Single-node template setups provided by JIPipe"),
    CUSTOM_USER("Custom (user)", "actions/template.png", "Templates",
            "Single-node template setups you created or downloaded");

    private static final String SETTINGS_DATABASE = "node-db";
    private static final JIPipePathMetadataStore.Key SETTINGS_KEY =
            JIPipePathMetadataStore.key("node-browser-kinds");
    private final String label;
    private final String icon;
    private final String group;
    private final String tooltip;

    NodeEntryKind(String label, String icon, String group, String tooltip) {
        this.label = label;
        this.icon = icon;
        this.group = group;
        this.tooltip = tooltip;
    }

    /**
     * The filter options for the kind selection box, in declaration order
     * (so the group order Nodes → Templates is preserved). Both node-browser
     * surfaces build their box from this single supplier, so the option
     * labels / icons / grouping cannot diverge between the docked browser
     * and the finder dialog.
     *
     * @return one {@link JIPipeDesktopRichSelectionBox.Option} per kind
     */
    public static List<JIPipeDesktopRichSelectionBox.Option<?>> options() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> options = new ArrayList<>();
        for (NodeEntryKind kind : values()) {
            options.add(new JIPipeDesktopRichSelectionBox.Option<>(kind, kind.getLabel(),
                    JIPipe.RESOURCES.getIcon16(kind.getIcon()), kind.getGroup()));
        }
        return options;
    }

    /**
     * The kind of the given entry, or {@code null} when the entry has no
     * kind (existing node / compartment, new compartment) and therefore
     * bypasses the kind filter.
     * <p>
     * The template kinds combine the entry's existing badge
     * ({@link CreateNewNodesByTemplateDatabaseEntry#getBadge() Pattern /
     * Custom}) with its {@link TemplateScope}: the System scope is the
     * "system" side, every other scope (user-created Global/Project,
     * Downloaded) the "user" side.
     *
     * @param entry the entry to classify; must not be {@code null}
     * @return the matching kind, or {@code null}
     */
    public static NodeEntryKind of(JIPipeNodeDatabaseEntry entry) {
        if (entry instanceof CreateNewNodeByInfoDatabaseEntry) {
            return NODES;
        }
        if (entry instanceof CreateNewNodeByExampleDatabaseEntry) {
            return EXAMPLES;
        }
        if (entry instanceof CreateNewNodesByTemplateDatabaseEntry template) {
            boolean system = template.getScope() == TemplateScope.System;
            boolean pattern = "Pattern".equals(template.getBadge());
            if (pattern) {
                return system ? PATTERNS_SYSTEM : PATTERNS_USER;
            }
            return system ? CUSTOM_SYSTEM : CUSTOM_USER;
        }
        return null;
    }

    /**
     * Whether the given entry passes the kind filter: entries without a kind
     * (see {@link #of}) always pass, and an empty selection matches
     * everything (no filter active).
     *
     * @param entry the entry to test; must not be {@code null}
     * @param kinds the selected kinds; an empty (or {@code null}) set
     *              disables the filter
     * @return {@code true} if the entry passes
     */
    public static boolean matches(JIPipeNodeDatabaseEntry entry, Set<NodeEntryKind> kinds) {
        if (kinds == null || kinds.isEmpty()) {
            return true;
        }
        NodeEntryKind kind = of(entry);
        // Kindless entries (existing nodes / compartments, new compartments)
        // always pass - their visibility is governed by other mechanisms.
        return kind == null || kinds.contains(kind);
    }

    /**
     * The kind filter selection persisted for this user (empty = show all
     * kinds). Loaded on demand; unknown stored names are skipped.
     *
     * @return the persisted kinds, possibly empty
     */
    public static Set<NodeEntryKind> loadKinds() {
        List<String> stored = JIPipe.getSettings().getListFromRegistry(
                SETTINGS_DATABASE, SETTINGS_KEY, String.class, true);
        Set<NodeEntryKind> kinds = new LinkedHashSet<>();
        for (String name : stored) {
            try {
                kinds.add(NodeEntryKind.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // A stored name this version does not know - skip it.
            }
        }
        return Collections.unmodifiableSet(kinds);
    }

    /**
     * Persists the kind filter selection (empty = show all kinds) and saves
     * the settings, so the value is shared by every node browser instance
     * and the node finder dialog within and across runs.
     *
     * @param kinds the kinds to persist
     */
    public static void saveKinds(Set<NodeEntryKind> kinds) {
        List<String> stored = new ArrayList<>();
        for (NodeEntryKind kind : EnumSet.allOf(NodeEntryKind.class)) {
            if (kinds != null && kinds.contains(kind)) {
                stored.add(kind.name());
            }
        }
        JIPipe.getSettings().putIntoRegistry(SETTINGS_DATABASE, SETTINGS_KEY, stored);
        JIPipe.getSettings().save();
    }

    // ------------------------------------------------------------------
    // Persistence (shared setting of the node browser + node finder dialog,
    // the same settings-registry pattern as the pinned node ids)
    // ------------------------------------------------------------------

    public String getLabel() {
        return label;
    }

    /**
     * @return the 16px icon resource path (see {@code JIPipe.RESOURCES.getIcon16})
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @return the option group this kind is listed under in the filter box
     */
    public String getGroup() {
        return group;
    }

    public String getTooltip() {
        return tooltip;
    }
}
