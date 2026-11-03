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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link SettingsTreeModel}, the pure (non-Swing) view model behind
 * the settings tab: global search matching, the hide-other-scope contract
 * (each scope view lists only its own sections), and the scope-qualified
 * identity rule.
 * <p>
 * The former dimmed cross-scope view (spec §3.4, where the application and
 * runtime views listed each other's sections read-only) is intentionally
 * gone: sections of another scope are simply not shown in the tree, and
 * search is global so a setting can still be found without knowing its
 * scope - the tab auto-switches the scope when a cross-scope result is
 * selected.
 */
class SettingsTreeModelTest {

    @Test
    void searchIsCaseInsensitiveAndMatchesBlockPath() {
        SettingsTreeModel m = model();
        assertEquals(List.of("org.hkijena.jipipe:general-ui"), ids(m.search("GENERAL-UI")));
        assertEquals(List.of("org.hkijena.jipipe:general-ui"), ids(m.search("theme")));
    }

    /**
     * The hide-other-scope contract: each scope view lists only its own
     * sections - no dimmed foreign entries.
     */
    @Test
    void eachScopeViewListsOnlyItsOwnSections() {
        SettingsTreeModel m = model();
        assertEquals(List.of("org.hkijena.jipipe:general-ui"), categoryIds(m.categories(SettingsScope.APPLICATION)));
        assertEquals(List.of("org.hkijena.jipipe:runtime"), categoryIds(m.categories(SettingsScope.RUNTIME)));
        assertEquals(List.of("org.hkijena.jipipe:project-defaults"), categoryIds(m.categories(SettingsScope.PROJECT)));
        // The editability rule itself is unchanged: a section is editable
        // only in its own scope (the tab relies on it for search results
        // and deep links).
        SettingsTreeModel.SheetNode runtimeSheet = m.nodeById(SettingsScope.RUNTIME, "org.hkijena.jipipe:runtime");
        assertFalse(m.isEditable(runtimeSheet, SettingsScope.PROJECT));
        assertTrue(m.isEditable(runtimeSheet, SettingsScope.RUNTIME));
    }

    /**
     * Global search: a query matches sections of EVERY scope, so a setting
     * can be found without knowing its scope; each result carries its own
     * scope (the tab uses it to auto-switch when one is selected). Results
     * keep the category-then-name ordering.
     */
    @Test
    void searchIsGlobalAcrossAllScopes() {
        SettingsTreeModel m = new SettingsTreeModel(List.of(
                new SettingsTreeModel.SheetNode(
                        "org.hkijena.jipipe:general-ui",
                        "General UI",
                        "Contains theme and general user interface settings",
                        "General",
                        SettingsScope.APPLICATION,
                        "org.hkijena.jipipe:general-ui"),
                new SettingsTreeModel.SheetNode(
                        "org.hkijena.jipipe:runtime",
                        "Theme and caches",
                        "Threads, temporary directories and caches",
                        "Execution",
                        SettingsScope.RUNTIME,
                        "org.hkijena.jipipe:runtime")));

        List<SettingsTreeModel.SheetNode> result = m.search("theme");
        assertEquals(List.of("org.hkijena.jipipe:general-ui", "org.hkijena.jipipe:runtime"), ids(result),
                "both the application and the runtime section match, General category first");
        assertSame(SettingsScope.APPLICATION, result.get(0).scope());
        assertSame(SettingsScope.RUNTIME, result.get(1).scope());
        // The cross-scope match is editable only in its own scope.
        assertFalse(m.isEditable(result.get(1), SettingsScope.APPLICATION));
        assertTrue(m.isEditable(result.get(1), SettingsScope.RUNTIME));
    }

    /**
     * A blank query matches everything, globally (in category order).
     */
    @Test
    void aBlankQueryMatchesAllSectionsOfAllScopes() {
        SettingsTreeModel m = model();
        // Categories: Execution (Runtime), General (General UI, Project defaults - name order)
        assertEquals(List.of("org.hkijena.jipipe:general-ui", "org.hkijena.jipipe:project-defaults", "org.hkijena.jipipe:runtime"),
                ids(m.search(" ")));
    }

    /**
     * Section identity is <b>(scope, id)</b>, not the bare id: two genuinely
     * distinct sections share one registration id in different scopes —
     * {@code org.hkijena.jipipe:default-environments} is both a RUNTIME
     * application sheet (installed environments) and a PROJECT sheet (the
     * project's default environment). Under global search both appear for
     * the same query; they must not collide or overwrite each other.
     */
    @Test
    void sameIdInDifferentScopesIsAllowedAndResolvedScopeQualified() {
        SettingsTreeModel.SheetNode appDefaults = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:default-environments", "Default environments",
                "Installed environments on this machine", "Execution",
                SettingsScope.RUNTIME, "org.hkijena.jipipe:default-environments");
        SettingsTreeModel.SheetNode projectDefaults = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:default-environments", "Default environments",
                "The default environment of projects", "Execution",
                SettingsScope.PROJECT, "org.hkijena.jipipe:default-environments");

        SettingsTreeModel m = new SettingsTreeModel(List.of(appDefaults, projectDefaults));

        assertSame(appDefaults, m.nodeById(SettingsScope.RUNTIME, "org.hkijena.jipipe:default-environments"));
        assertSame(projectDefaults, m.nodeById(SettingsScope.PROJECT, "org.hkijena.jipipe:default-environments"));

        // Global search finds BOTH same-id sections (each in its own scope view).
        List<SettingsTreeModel.SheetNode> search = m.search("default-environments");
        assertEquals(List.of("org.hkijena.jipipe:default-environments", "org.hkijena.jipipe:default-environments"),
                ids(search));
        assertSame(appDefaults, search.get(0));
        assertSame(projectDefaults, search.get(1));

        // Each scope view lists only its own section.
        assertEquals(List.of("org.hkijena.jipipe:default-environments"),
                categoryIds(m.categories(SettingsScope.PROJECT)));

        assertFalse(m.isEditable(appDefaults, SettingsScope.APPLICATION));
        assertTrue(m.isEditable(projectDefaults, SettingsScope.PROJECT));
    }

    /**
     * The same id in the <em>same</em> scope is a genuine registration bug and
     * must still be rejected.
     */
    @Test
    void sameIdInTheSameScopeIsStillRejected() {
        SettingsTreeModel.SheetNode first = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:general-ui", "General UI", null, "General",
                SettingsScope.APPLICATION, "org.hkijena.jipipe:general-ui");
        SettingsTreeModel.SheetNode second = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:general-ui", "General UI (duplicate)", null, "General",
                SettingsScope.APPLICATION, "org.hkijena.jipipe:general-ui");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new SettingsTreeModel(List.of(first, second)));
        assertTrue(error.getMessage().contains("org.hkijena.jipipe:general-ui"), error.getMessage());
    }

    private static SettingsTreeModel model() {
        SettingsTreeModel.SheetNode generalUi = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:general-ui",
                "General UI",
                "Contains theme and general user interface settings",
                "General",
                SettingsScope.APPLICATION,
                "org.hkijena.jipipe:general-ui");
        SettingsTreeModel.SheetNode runtime = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:runtime",
                "Runtime",
                "Threads, temporary directories and caches",
                "Execution",
                SettingsScope.RUNTIME,
                "org.hkijena.jipipe:runtime");
        SettingsTreeModel.SheetNode projectDefaults = new SettingsTreeModel.SheetNode(
                "org.hkijena.jipipe:project-defaults",
                "Project defaults",
                "Defaults applied to newly created projects",
                "General",
                SettingsScope.PROJECT,
                "org.hkijena.jipipe:project-defaults");
        return new SettingsTreeModel(List.of(generalUi, runtime, projectDefaults));
    }

    private static List<String> ids(List<SettingsTreeModel.SheetNode> nodes) {
        return nodes.stream().map(SettingsTreeModel.SheetNode::id).collect(Collectors.toList());
    }

    private static List<String> categoryIds(List<SettingsTreeModel.CategoryNode> categories) {
        return categories.stream().flatMap(category -> category.sheets().stream())
                .map(SettingsTreeModel.SheetNode::id).collect(Collectors.toList());
    }
}
