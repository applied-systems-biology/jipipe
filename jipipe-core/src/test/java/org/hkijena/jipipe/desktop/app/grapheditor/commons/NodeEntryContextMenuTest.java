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
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the shared node-entry context menu (spec §7.3, the 1-pane
 * context-menu bug fix):
 * <p>
 * <ul>
 * <li>{@link NodeEntryContextMenu#specs(JIPipeNodeDatabaseEntry, java.util.Set)} is the
 * SINGLE source of truth for which menu items a node-entry row gets, in order —
 * the same method serves the node-list rows AND the compact-mode tree leaves,
 * so both surfaces are structurally in parity;</li>
 * <li>the scope gating of the template quick actions (Global/Project get
 * Edit/Delete, System is read-only);</li>
 * <li>{@link NodeEntryContextMenu#buildJPopupMenu(List, Map)} renders only the
 * items whose actions are actually supplied — the seam Task 9 closes by
 * supplying the template management actions (a spec without an action is
 * SKIPPED, not rendered disabled).</li>
 * </ul>
 * Boots the {@link JIPipe} instance so real entries (a registered stub node +
 * template entries) can be constructed. Never shows a menu — a headless
 * {@link JPopupMenu} is only built and inspected, never displayed.
 */
class NodeEntryContextMenuTest {

    private static final String STUB_ID = "node-entry-menu-test:stub-node";
    private static final String PLAIN_ENTRY_ID = "node-entry-menu-test:plain-node";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
        new JIPipeMutableDependency("node-entry-menu-test", "1.0.0", "node-entry-menu-test");

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
        // A namespaced stub node so the plain entry and the template graphs
        // can be built from the live registry without depending on which
        // bundled core plugins booted.
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        if (!nodes.hasNodeInfoWithId(STUB_ID)) {
            nodes.register(new JIPipeJavaNodeInfo(STUB_ID, StubMenuNode.class), TEST_DEPENDENCY);
        }
    }

    @AfterAll
    static void cleanup() {
        // Do not leak the stub into the shared registry (mirrors the other
        // template tests).
        JIPipe.getNodes().unregister(STUB_ID);
    }

    // ------------------------------------------------------------------
    // specs(): the ordered, single-source item set
    // ------------------------------------------------------------------

    @Test
    void listAndTreeProduceIdenticalSpecs() {
        // The SAME specs() method is the single source of truth for a single
        // entry. The tree leaves call specs(entry, pinned) directly, and the
        // list surface's specsForSelection DELEGATES to specs(first, pinned)
        // for a single-entry selection — so a single-selection list row and a
        // tree leaf of the same entry yield the identical ordered item set.
        JIPipeNodeDatabaseEntry nodeEntry = anyNodeEntry();
        List<NodeEntryContextMenu.MenuItemSpec> treeSpecs = NodeEntryContextMenu.specs(nodeEntry, java.util.Set.of());
        assertThat(treeSpecs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .containsExactly("insert-at-cursor", "pin", "show-doc");
        assertThat(treeSpecs).allSatisfy(spec -> assertThat(spec.enabled()).isTrue());
        // The single-entry list path must equal the tree path exactly.
        List<NodeEntryContextMenu.MenuItemSpec> listSpecs =
            NodeEntryContextMenu.specsForSelection(java.util.List.of(nodeEntry), java.util.Set.of());
        assertThat(listSpecs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .containsExactlyElementsOf(treeSpecs.stream().map(NodeEntryContextMenu.MenuItemSpec::id).toList());
    }

    @Test
    void mixedMultiSelectionShowsBothPinAndUnpin() {
        // The pre-refactor list menu showed BOTH "Pin" and "Unpin" for a
        // MIXED multi-selection (some pinned, some not). The list surface's
        // specsForSelection must restore that: pin-then-unpin, in the same
        // group, while keeping the other base items in place. (specs() itself
        // stays single-entry and shows only the first entry's — the tree's
        // source of truth, unchanged.)
        JIPipeNodeDatabaseEntry a = nodeEntry("node-entry-menu-test:plain-node-a");
        JIPipeNodeDatabaseEntry b = nodeEntry("node-entry-menu-test:plain-node-b");
        java.util.Set<String> pinned = java.util.Set.of(a.getId()); // a pinned, b not

        List<NodeEntryContextMenu.MenuItemSpec> specs =
            NodeEntryContextMenu.specsForSelection(java.util.List.of(a, b), pinned);
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .containsExactly("insert-at-cursor", "pin", "unpin", "show-doc");

        // ... and an all-unpinned (or all-pinned) multi-selection is NOT mixed
        // → it keeps the single pin (or unpin) item, like specs(first, pinned).
        JIPipeNodeDatabaseEntry c = nodeEntry("node-entry-menu-test:plain-node-c");
        List<NodeEntryContextMenu.MenuItemSpec> allUnpinned =
            NodeEntryContextMenu.specsForSelection(java.util.List.of(a, b, c), java.util.Set.of());
        assertThat(allUnpinned).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .contains("pin")
            .doesNotContain("unpin");
    }

    @Test
    void pinnedNodeShowsUnpinNotPin() {
        JIPipeNodeDatabaseEntry entry = anyNodeEntry();
        List<NodeEntryContextMenu.MenuItemSpec> specs =
            NodeEntryContextMenu.specs(entry, java.util.Set.of(entry.getId()));
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .contains("unpin")
            .doesNotContain("pin");
    }

    @Test
    void globalTemplateHasEditDeleteConfigure() {
        CreateNewNodesByTemplateDatabaseEntry t = templateEntry(TemplateScope.Global);
        List<NodeEntryContextMenu.MenuItemSpec> specs = NodeEntryContextMenu.specs(t, java.util.Set.of());
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .contains("edit", "delete", "configure-templates", "copy-to-project");
        // Global is not Project scope → no "Copy to Project" target ... it
        // HAS "copy-to-project" (copy INTO the project), and NOT
        // "copy-to-global" (it already is global).
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .doesNotContain("copy-to-global");
    }

    @Test
    void projectTemplateHasCopyToGlobal() {
        CreateNewNodesByTemplateDatabaseEntry t = templateEntry(TemplateScope.Project);
        List<NodeEntryContextMenu.MenuItemSpec> specs = NodeEntryContextMenu.specs(t, java.util.Set.of());
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .contains("edit", "delete", "copy-to-global", "configure-templates");
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .doesNotContain("copy-to-project");
    }

    @Test
    void systemTemplateIsReadOnly() {
        CreateNewNodesByTemplateDatabaseEntry t = templateEntry(TemplateScope.System);
        List<NodeEntryContextMenu.MenuItemSpec> specs = NodeEntryContextMenu.specs(t, java.util.Set.of());
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .doesNotContain("edit", "delete");
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .contains("copy-to-global", "configure-templates");
    }

    @Test
    void templateItemsFollowTheBaseItems() {
        // Ordering contract: the base items come first, the scope-gated
        // template quick actions are appended after them (spec §7.3/§8.2).
        CreateNewNodesByTemplateDatabaseEntry t = templateEntry(TemplateScope.Global);
        List<String> ids = NodeEntryContextMenu.specs(t, java.util.Set.of())
            .stream().map(NodeEntryContextMenu.MenuItemSpec::id).toList();
        int baseEnd = ids.indexOf("show-doc");
        assertThat(baseEnd).isNotNegative();
        assertThat(ids.subList(baseEnd + 1, ids.size()))
            .containsExactly("edit", "copy-to-project", "delete", "configure-templates");
    }

    // ------------------------------------------------------------------
    // buildJPopupMenu(): the Task-9 seam (skip unsupplied actions)
    // ------------------------------------------------------------------

    @Test
    void buildJPopupMenuSkipsUnsuppliedActions() {
        // A template entry's spec includes "edit", but the actions map
        // supplies ONLY the base actions (the Task-7 state): the rendered
        // menu must omit the unsupplied template items — not render them
        // disabled. This is the seam Task 9 closes by adding its actions.
        CreateNewNodesByTemplateDatabaseEntry t = templateEntry(TemplateScope.Global);
        List<NodeEntryContextMenu.MenuItemSpec> specs = NodeEntryContextMenu.specs(t, java.util.Set.of());
        assertThat(specs).extracting(NodeEntryContextMenu.MenuItemSpec::id)
            .contains("edit"); // present in the spec ...

        Map<String, Runnable> baseActions = Map.of(
            "insert-at-cursor", () -> {
            },
            "pin", () -> {
            },
            "show-doc", () -> {
            });
        JPopupMenu menu = NodeEntryContextMenu.buildJPopupMenu(specs, baseActions);
        List<String> renderedLabels = menuItemsOf(menu).stream().map(JMenuItem::getText).toList();
        assertThat(renderedLabels)
            .containsExactly("Insert at cursor", "Pin", "Show documentation");
        // ... and absent from the rendered menu (skipped, not disabled).
        assertThat(menu.getComponentCount()).isGreaterThanOrEqualTo(3);
        for (Object component : menu.getComponents()) {
            if (component instanceof JMenuItem item) {
                assertThat(item.isEnabled()).isTrue();
            }
        }
    }

    @Test
    void buildJPopupMenuRendersEverythingWhenAllActionsSupplied() {
        // With the full action set the menu renders every spec item in
        // order (what Task 9 produces once it supplies the management
        // actions).
        CreateNewNodesByTemplateDatabaseEntry t = templateEntry(TemplateScope.Global);
        List<NodeEntryContextMenu.MenuItemSpec> specs = NodeEntryContextMenu.specs(t, java.util.Set.of());
        Map<String, Runnable> allActions = specs.stream()
            .collect(java.util.stream.Collectors.toMap(NodeEntryContextMenu.MenuItemSpec::id, s -> () -> {
            }));
        JPopupMenu menu = NodeEntryContextMenu.buildJPopupMenu(specs, allActions);
        assertThat(menuItemsOf(menu)).hasSize(specs.size());
        // Each rendered item is tagged with its spec id (client property),
        // so the rendered order can be checked against the spec order.
        List<String> ids = menuItemsOf(menu).stream()
            .map(item -> (String) item.getClientProperty(NodeEntryContextMenu.SPEC_ID_KEY))
            .toList();
        assertThat(ids).containsExactlyElementsOf(specs.stream().map(NodeEntryContextMenu.MenuItemSpec::id).toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static List<JMenuItem> menuItemsOf(JPopupMenu menu) {
        List<JMenuItem> items = new java.util.ArrayList<>();
        for (java.awt.Component component : menu.getComponents()) {
            if (component instanceof JMenuItem item) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * A plain (non-template) node database entry built from the registered
     * stub node — the common case: base items only.
     */
    private static JIPipeNodeDatabaseEntry anyNodeEntry() {
        return nodeEntry(PLAIN_ENTRY_ID);
    }

    /**
     * A plain (non-template) node database entry with the given (distinct)
     * id — needed to build a multi-selection where some entries are pinned
     * and some are not (the mixed pin/unpin display case).
     */
    private static JIPipeNodeDatabaseEntry nodeEntry(String id) {
        return new CreateNewNodeByInfoDatabaseEntry(id, JIPipe.getNodes().getInfoById(STUB_ID));
    }

    /**
     * A real template database entry (single stub node) in the given scope.
     */
    private static CreateNewNodesByTemplateDatabaseEntry templateEntry(TemplateScope scope) {
        JIPipeGraph graph = new JIPipeGraph();
        JIPipeGraphNode node = JIPipe.getNodes().getInfoById(STUB_ID).newInstance();
        graph.insertNode(node);
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Menu Test Template");
        template.setGraph(graph);
        return new CreateNewNodesByTemplateDatabaseEntry(template, scope);
    }

    // ------------------------------------------------------------------
    // Stub node
    // ------------------------------------------------------------------

    /**
     * A minimal, unconverted node used purely as material for the plain
     * entry and the template graphs (mirrors the other template tests).
     */
    @SetJIPipeDocumentation(name = "Menu Stub Node", description = "A stub for the node-entry context menu tests.")
    @ConfigureJIPipeNode
    public static class StubMenuNode extends JIPipeGraphNode {
        public StubMenuNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }
    }
}
