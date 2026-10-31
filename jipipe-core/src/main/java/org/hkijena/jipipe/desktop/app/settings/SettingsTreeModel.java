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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.api.settings.SettingsScope;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure (non-Swing) view model behind {@link JIPipeDesktopSettingsTab}: the
 * category tree, the section search and the cross-scope editability rule.
 * <p>
 * Deliberately free of {@code java.awt}/{@code javax.swing} types so it can
 * be built and queried off the EDT (and in unit tests). The tab resolves the
 * per-category icon itself (the legacy dialog took the category icon from
 * the first sheet of the category); this model only carries names.
 * <p>
 * <b>Ordering.</b> {@link #categories(SettingsScope)} returns the category
 * names sorted alphabetically with {@code "General"} pinned first, and the
 * sheets of each category sorted by display name — exactly the ordering of
 * the modal settings UI this tab replaces.
 * <p>
 * <b>Scope content.</b> The model holds <em>all</em> known sections (across
 * all scopes). A scope view ({@link #categories(SettingsScope)} /
 * {@link #search(String, SettingsScope)}) shows the sections of that scope
 * as editable and, in addition, the non-project sections of the
 * <em>other</em> file-backed scope as present-but-not-editable (spec §3.4:
 * "the tree always shows all sections; sections of a different scope than
 * the switcher are shown dimmed and read-only"). The two file-backed scopes
 * ({@code APPLICATION}, {@code RUNTIME}) are interchangeable views of the
 * same profile; {@code PROJECT} sections belong to the open project and are
 * shown only in the project view.
 */
public final class SettingsTreeModel {

    private static final String GENERAL_CATEGORY = "General";

    private final List<SheetNode> allNodes;
    private final Map<String, SheetNode> byId;
    private final Map<SettingsScope, List<SheetNode>> byScope;

    /**
     * @param nodes every known section (application, runtime and project),
     *              across all scopes. Duplicate ids are rejected: the id is
     *              the section's identity.
     */
    public SettingsTreeModel(Collection<SheetNode> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        this.allNodes = List.copyOf(nodes);
        this.byId = new LinkedHashMap<>();
        this.byScope = new EnumMap<>(SettingsScope.class);
        for (SettingsScope scope : SettingsScope.values()) {
            byScope.put(scope, new ArrayList<>());
        }
        for (SheetNode node : allNodes) {
            Objects.requireNonNull(node, "node");
            if (byId.put(node.id(), node) != null) {
                throw new IllegalArgumentException("Duplicate settings section id: " + node.id());
            }
            byScope.get(node.scope()).add(node);
        }
    }

    /**
     * Sorts alphabetically (case-insensitive, so the order does not depend
     * on capitalization quirks of plugin-authored categories), then pins
     * {@link #GENERAL_CATEGORY} first.
     */
    private static List<String> orderCategories(Collection<String> names) {
        List<String> categories = names.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        if (categories.remove(GENERAL_CATEGORY)) {
            categories.add(0, GENERAL_CATEGORY);
        }
        return categories;
    }

    private static List<SheetNode> sortByName(Collection<SheetNode> nodes) {
        return nodes.stream()
                .sorted(Comparator.comparing(SheetNode::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(SheetNode::id))
                .collect(Collectors.toList());
    }

    /**
     * Same ordering rule as {@link #orderCategories(Collection)}: a query of
     * {@code "GENERAL-UI"} matches the block path {@code
     * org.hkijena.jipipe:general-ui} case-insensitively.
     *
     * @param query the search text; a blank query matches everything
     * @param scope the scope view
     * @return the matching sections of the view, category-ordered and
     * name-sorted within each category
     */
    public List<SheetNode> search(String query, SettingsScope scope) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<SheetNode> result = new ArrayList<>();
        for (SheetNode node : visibleIn(scope)) {
            if (needle.isEmpty() || matches(node, needle)) {
                result.add(node);
            }
        }
        return sortInCategoryOrder(result);
    }

    /**
     * Matches the query case-insensitively against the section's name, its
     * description and its block path.
     *
     * @param node   the section
     * @param needle the lower-cased, trimmed search text (non-empty)
     * @return whether the section matches
     */
    private static boolean matches(SheetNode node, String needle) {
        return containsIgnoreCase(node.name(), needle)
                || containsIgnoreCase(node.description(), needle)
                || containsIgnoreCase(node.blockPath(), needle);
    }

    private static boolean containsIgnoreCase(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static List<SheetNode> sortInCategoryOrder(List<SheetNode> nodes) {
        Map<String, List<SheetNode>> byCategory = new LinkedHashMap<>();
        for (SheetNode node : nodes) {
            byCategory.computeIfAbsent(node.category(), k -> new ArrayList<>()).add(node);
        }
        Map<String, List<SheetNode>> sortedCategories = new LinkedHashMap<>();
        for (String category : orderCategories(byCategory.keySet())) {
            sortedCategories.put(category, sortByName(byCategory.get(category)));
        }
        return sortedCategories.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * @param scope the scope view
     * @return the sections shown in the view: the view's own scope sections
     * plus the non-project sections of the other file-backed scope (shown
     * dimmed, see {@link #isEditable(SheetNode, SettingsScope)})
     */
    private List<SheetNode> visibleIn(SettingsScope scope) {
        Objects.requireNonNull(scope, "scope");
        List<SheetNode> result = new ArrayList<>(byScope.get(scope));
        switch (scope) {
            case APPLICATION:
                result.addAll(byScope.get(SettingsScope.RUNTIME));
                break;
            case RUNTIME:
                result.addAll(byScope.get(SettingsScope.APPLICATION));
                break;
            case PROJECT:
            default:
                break;
        }
        return result;
    }

    /**
     * @param scope the scope view
     * @return the view's categories, "General" first, the rest alphabetical;
     * the sheets within each category are sorted by name
     */
    public List<CategoryNode> categories(SettingsScope scope) {
        Map<String, List<SheetNode>> byCategory = new LinkedHashMap<>();
        for (SheetNode node : visibleIn(scope)) {
            byCategory.computeIfAbsent(node.category(), k -> new ArrayList<>()).add(node);
        }
        List<CategoryNode> result = new ArrayList<>();
        for (String category : orderCategories(byCategory.keySet())) {
            result.add(new CategoryNode(category, sortByName(byCategory.get(category))));
        }
        return result;
    }

    /**
     * The editability rule: a section is editable iff it belongs to the
     * active scope. Sections of the other scope are shown for orientation
     * but are read-only.
     *
     * @param node        the section
     * @param activeScope the scope view currently displayed
     * @return whether the section can be edited in that view
     */
    public boolean isEditable(SheetNode node, SettingsScope activeScope) {
        return node != null && node.scope() == activeScope;
    }

    /**
     * @param id the section id
     * @return the section, or {@code null} if unknown
     */
    public SheetNode nodeById(String id) {
        return byId.get(id);
    }

    /**
     * @return all known sections, in construction order
     */
    public List<SheetNode> allNodes() {
        return allNodes;
    }

    /**
     * A category of the settings tree.
     *
     * @param name   the category name
     * @param sheets the category's sections, sorted by display name
     */
    public record CategoryNode(String name, List<SheetNode> sheets) {
        public CategoryNode {
            sheets = List.copyOf(sheets);
        }
    }

    /**
     * A settings section (a sheet instance) in the tree.
     *
     * @param id          the sheet id (registration id, persisted in profiles)
     * @param name        the display name
     * @param description the description (may be null)
     * @param category    the category name
     * @param scope       the scope the section belongs to
     * @param blockPath   the canonical settings block path, e.g.
     *                    {@code org.hkijena.jipipe:general-ui} (searched;
     *                    may be null)
     */
    public record SheetNode(String id, String name, String description, String category,
                            SettingsScope scope, String blockPath) {
        public SheetNode {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(scope, "scope");
        }
    }
}
