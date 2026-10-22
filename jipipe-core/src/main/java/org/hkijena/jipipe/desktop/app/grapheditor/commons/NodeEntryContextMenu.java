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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shared context menu for node-database-entry rows (spec §7.3, the
 * 1-pane context-menu bug fix).
 * <p>
 * The docked node browser shows its entries in two places: the node
 * <em>list</em> (two-pane / comfortable width) and, in the compact
 * single-pane, the function tree's <em>node leaves</em> (the tree takes
 * over the node-list role). Before this class the two surfaces disagreed —
 * the tree leaves had NO context menu, so Insert at cursor / Pin / Unpin /
 * Show documentation were unreachable in the compact pane. The parity is
 * structural, not copy-paste: BOTH surfaces call
 * {@link #specs(JIPipeNodeDatabaseEntry, Set)} for the item set and
 * {@link #buildJPopupMenu(List, Map)} for the rendered menu, with a shared
 * actions map, so a tree leaf and a list row of the same entry get the
 * identical menu.
 * <p>
 * {@link #specs} and {@link MenuItemSpec} are deliberately Swing-free — the
 * single source of truth for WHICH items an entry gets is unit-testable
 * without a UI. {@link #buildJPopupMenu} is the thin Swing assembler (the
 * Swing-side dressing — icons, tooltips — lives there, not in the spec).
 */
public final class NodeEntryContextMenu {

    private NodeEntryContextMenu() {
    }

    /**
     * The stable ids of the node-entry context-menu items. Both surfaces
     * wire their actions under these ids; the template ids are the exact
     * contract Task 9's management actions will supply.
     */
    public static final String INSERT_AT_CURSOR = "insert-at-cursor";
    public static final String PIN = "pin";
    public static final String UNPIN = "unpin";
    public static final String SHOW_DOC = "show-doc";
    public static final String EDIT = "edit";
    public static final String COPY_TO_GLOBAL = "copy-to-global";
    public static final String COPY_TO_PROJECT = "copy-to-project";
    public static final String DELETE = "delete";
    public static final String CONFIGURE_TEMPLATES = "configure-templates";

    /**
     * The client-property key under which {@link #buildJPopupMenu(List, Map)}
     * tags each rendered {@link JMenuItem} with its spec id (a stable
     * mapping back from the rendered menu to the spec that produced it).
     */
    public static final String SPEC_ID_KEY = "jipipe.node-entry-menu.spec-id";

    /**
     * A single context-menu item specification — the Swing-free contract
     * both surfaces share.
     *
     * @param id      the stable item id (see the {@code INSERT_AT_CURSOR} ...
     *                {@code CONFIGURE_TEMPLATES} constants)
     * @param label   the user-facing label
     * @param enabled whether the item is enabled (always {@code true} here —
     *                an item's <em>presence</em>, not a disabled state, is
     *                the signal; an unsupplied action simply omits the item,
     *                see {@link #buildJPopupMenu(List, Map)})
     */
    public record MenuItemSpec(String id, String label, boolean enabled) {
    }

    /**
     * The single source of truth for WHICH menu items an entry gets, in
     * order. The SAME method serves the node-list rows and the compact-mode
     * tree leaves, so both surfaces are structurally in parity.
     * <p>
     * Every entry gets the base items first: {@code insert-at-cursor}, then
     * {@code pin} (when the entry is NOT in {@code pinnedIds}) or
     * {@code unpin} (when it IS), then {@code show-doc}.
     * <p>
     * A {@link CreateNewNodesByTemplateDatabaseEntry} additionally gets the
     * scope-gated template quick actions (spec §7.3/§8.2), appended AFTER
     * the base items:
     * <ul>
     * <li><b>Global</b> (the user's own templates) — {@code edit},
     * {@code copy-to-project}, {@code delete}, {@code configure-templates};</li>
     * <li><b>Project</b> — {@code edit}, {@code copy-to-global},
     * {@code delete}, {@code configure-templates};</li>
     * <li><b>System</b> (read-only) — {@code copy-to-global},
     * {@code configure-templates} (no edit, no delete);</li>
     * <li><b>Downloaded</b> (read-only repository artifacts) —
     * {@code copy-to-global} (make a local editable version),
     * {@code delete} (remove the download), {@code configure-templates}
     * (no edit).</li>
     * </ul>
     *
     * @param entry     the node database entry the menu is for (non-null)
     * @param pinnedIds the currently pinned node ids (the entry is "pinned"
     *                  when its own id is in this set); null-safe
     * @return the ordered item specs
     */
    public static List<MenuItemSpec> specs(JIPipeNodeDatabaseEntry entry, Set<String> pinnedIds) {
        Set<String> pinned = pinnedIds == null ? Set.of() : pinnedIds;
        List<MenuItemSpec> specs = new ArrayList<>();
        // --- Base items (every entry) ---
        specs.add(new MenuItemSpec(INSERT_AT_CURSOR, "Insert at cursor", true));
        specs.add(pinned.contains(entry.getId())
            ? new MenuItemSpec(UNPIN, "Unpin", true)
            : new MenuItemSpec(PIN, "Pin", true));
        specs.add(new MenuItemSpec(SHOW_DOC, "Show documentation", true));
        // --- Scope-gated template quick actions (template entries only) ---
        if (entry instanceof CreateNewNodesByTemplateDatabaseEntry template) {
            switch (template.getScope()) {
                case Global -> specs.addAll(List.of(
                    new MenuItemSpec(EDIT, "Edit template", true),
                    new MenuItemSpec(COPY_TO_PROJECT, "Copy to Project", true),
                    new MenuItemSpec(DELETE, "Delete template", true),
                    new MenuItemSpec(CONFIGURE_TEMPLATES, "Configure templates", true)));
                case Project -> specs.addAll(List.of(
                    new MenuItemSpec(EDIT, "Edit template", true),
                    new MenuItemSpec(COPY_TO_GLOBAL, "Copy to Global", true),
                    new MenuItemSpec(DELETE, "Delete template", true),
                    new MenuItemSpec(CONFIGURE_TEMPLATES, "Configure templates", true)));
                case System -> specs.addAll(List.of(
                    new MenuItemSpec(COPY_TO_GLOBAL, "Copy to Global", true),
                    new MenuItemSpec(CONFIGURE_TEMPLATES, "Configure templates", true)));
                case Downloaded -> specs.addAll(List.of(
                    new MenuItemSpec(COPY_TO_GLOBAL, "Copy to Your templates", true),
                    new MenuItemSpec(DELETE, "Remove download", true),
                    new MenuItemSpec(CONFIGURE_TEMPLATES, "Configure templates", true)));
            }
        }
        return specs;
    }

    /**
     * The ordered item set for a (possibly multi-) selection of the node
     * <em>list</em> (spec §7.3). This is the list surface's entry point: it
     * delegates to {@link #specs(JIPipeNodeDatabaseEntry, Set)} for the FIRST
     * selected entry (so a single-entry selection — and therefore the
     * tree's single-selection path, which calls {@code specs} directly —
     * yields exactly the same result), and only for a MIXED multi-selection
     * (some selected entries pinned, some not) restores the pre-refactor
     * list-menu display: BOTH {@code pin} and {@code unpin}, ordered
     * pin-then-unpin, in the same separator group.
     * <p>
     * An all-pinned or all-unpinned multi-selection yields the same single
     * pin/unpin item as {@code specs(first, pinned)} already does, so no
     * adjustment is needed there. The actions still act on the WHOLE
     * selection (the caller's actions map) — only which item(s) are DISPLAYED
     * changes here.
     * <p>
     * This stays Swing-free and is deliberately SEPARATE from
     * {@link #specs(JIPipeNodeDatabaseEntry, Set)} (which remains the
     * single-entry source of truth the compact-mode tree leaves call).
     *
     * @param selected the selected entries (non-empty; the list's selection)
     * @param pinnedIds the currently pinned node ids; null-safe
     * @return the ordered item specs for the selection
     */
    public static List<MenuItemSpec> specsForSelection(List<JIPipeNodeDatabaseEntry> selected, Set<String> pinnedIds) {
        Set<String> pinned = pinnedIds == null ? Set.of() : pinnedIds;
        List<MenuItemSpec> specs = specs(selected.get(0), pinned);
        if (selected.size() <= 1) {
            return specs;
        }
        boolean anyPinned = selected.stream().anyMatch(entry -> pinned.contains(entry.getId()));
        boolean anyUnpinned = selected.stream().anyMatch(entry -> !pinned.contains(entry.getId()));
        if (!anyPinned || !anyUnpinned) {
            // All-pinned or all-unpinned: specs(first, pinned) already shows
            // the correct single item.
            return specs;
        }
        // Mixed multi-selection: replace the single pin/unpin member with BOTH
        // (ordered pin-then-unpin, same group), keeping every other item
        // (insert-at-cursor, show-doc, the template block) in place.
        List<MenuItemSpec> out = new ArrayList<>(specs.size() + 1);
        for (MenuItemSpec spec : specs) {
            if (spec.id().equals(PIN) || spec.id().equals(UNPIN)) {
                out.add(new MenuItemSpec(PIN, "Pin", true));
                out.add(new MenuItemSpec(UNPIN, "Unpin", true));
            } else {
                out.add(spec);
            }
        }
        return out;
    }

    /**
     * The thin Swing assembler: renders the given specs, IN ORDER, into a
     * {@link JPopupMenu}.
     * <p>
     * For each spec, when {@code actions} contains the spec's id, an enabled
     * {@link JMenuItem} (label, icon, tooltip, wired action) is added; when
     * it does NOT, the spec is <b>skipped</b> — a disabled item is
     * deliberately NOT rendered. That skip is the seam Task 9 closes: until
     * it supplies the template management actions, the template items are
     * present in {@link #specs(JIPipeNodeDatabaseEntry, Set)} but absent
     * from the rendered menu.
     * <p>
     * Separators mirror the pre-existing node-list menu's grouping: one
     * before the Pin/Unpin group and one before the Show documentation /
     * template group, so a node entry renders EXACTLY like today's list
     * menu. Pin and Unpin share a group (no separator between them, as in
     * the original); Show documentation and the template quick actions
     * share the final group (one separator before the group, none within).
     *
     * @param specs   the ordered item specs (from {@link #specs})
     * @param actions the available actions keyed by spec id
     * @return the assembled popup menu (empty when no spec has an action)
     */
    public static JPopupMenu buildJPopupMenu(List<MenuItemSpec> specs, Map<String, Runnable> actions) {
        JPopupMenu menu = new JPopupMenu();
        Integer lastGroup = null;
        for (MenuItemSpec spec : specs) {
            Runnable action = actions == null ? null : actions.get(spec.id());
            if (action == null) {
                // The Task-9 seam: an unsupplied action is skipped, not
                // rendered disabled. A skipped item must not, however,
                // swallow the group boundary of the item that follows it,
                // so lastGroup is only advanced when an item is actually
                // rendered.
                continue;
            }
            int group = groupOf(spec.id());
            if (lastGroup != null && group != lastGroup) {
                menu.addSeparator();
            }
            menu.add(createItem(spec, action));
            lastGroup = group;
        }
        return menu;
    }

    /**
     * The visual group an item id belongs to, for separator placement
     * (mirrors the original node-list menu): insert-at-cursor is group 0,
     * the pin/unpin pair is group 1, and show-doc + the template quick
     * actions share group 2.
     */
    private static int groupOf(String id) {
        return switch (id) {
            case INSERT_AT_CURSOR -> 0;
            case PIN, UNPIN -> 1;
            default -> 2;
        };
    }

    /**
     * A single rendered menu item: label, the item's icon, a tooltip, and
     * the wired action. The item is tagged with its spec id (client
     * property, see {@link #SPEC_ID_KEY}) so the rendered menu maps back to
     * the spec that produced it.
     */
    private static JMenuItem createItem(MenuItemSpec spec, Runnable action) {
        JMenuItem item = new JMenuItem(spec.label(), iconFor(spec.id()));
        item.setToolTipText(tooltipFor(spec.id(), spec.label()));
        item.putClientProperty(SPEC_ID_KEY, spec.id());
        item.addActionListener(e -> action.run());
        return item;
    }

    /**
     * The Swing-side icon for an item id (the pre-existing node-list menu's
     * icons, so the rendered menu looks like today's); no icon yet for the
     * template quick actions (Task 9 supplies their actions — and can
     * refine the dressing with them).
     */
    private static Icon iconFor(String id) {
        return switch (id) {
            case INSERT_AT_CURSOR -> JIPipe.RESOURCES.getIcon16("actions/add.png");
            case PIN -> JIPipe.RESOURCES.getIcon16("actions/window-pin.png");
            case UNPIN -> JIPipe.RESOURCES.getIcon16("actions/window-unpin.png");
            case SHOW_DOC -> JIPipe.RESOURCES.getIcon16("actions/help.png");
            default -> null;
        };
    }

    /**
     * The Swing-side tooltip for an item id (the pre-existing node-list
     * menu's descriptions); falls back to the label.
     */
    private static String tooltipFor(String id, String label) {
        return switch (id) {
            case INSERT_AT_CURSOR -> "Inserts the node at the cursor";
            case PIN -> "Adds the item to the list of pinned items";
            case UNPIN -> "Removes the item from the list of pinned items";
            case SHOW_DOC -> "Displays the documentation of this node";
            default -> label;
        };
    }
}
