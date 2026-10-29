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

package org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser;

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
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ColorStackFunction;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the node-leaf contract of {@link NodeBrowserTreeController} that
 * the add-nodes panel depends on (issues from the 1324 manual review):
 * the tree carries entry leaves from the FULL current result set -
 * independent of which function/scope is selected - so branches are
 * expandable and scope groups (System/Global/Project) show their entries
 * without first selecting them.
 */
class NodeBrowserTreeControllerTest {

    private static final String STUB_ID = "tree-controller-test:stub-node";
    /**
     * A real, registry-registered branch the stub function ids can be filed under.
     */
    private static final String IMAGES_FN_ID = "jipipe:function:images";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("tree-controller-test", "1.0.0", "tree-controller-test");

    private static NodeBrowserTreeController controller;

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
        // A namespaced stub node so the template graphs can be built from the
        // live registry (the jipipe-core test JVM does not load all plugin
        // nodes, e.g. folder-list-files).
        if (!JIPipe.getNodes().hasNodeInfoWithId(STUB_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(STUB_ID, StubTemplateNode.class), TEST_DEPENDENCY);
        }
        try {
            controller = new NodeBrowserTreeController();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "AWT not available: " + e);
        }
        // Seed the tree WITH the entries' function branches (the non-empty
        // populateTree overload prunes by the entries' own function ids, so
        // the branches the leaves attach to are present). Individual tests
        // then drive the leaves via rebuildNodeLeaves.
        controller.populateTree(JIPipeNodeDatabasePipelineVisibility.Pipeline,
                List.of(nodeEntry(), templateEntry(TemplateScope.System, IMAGES_FN_ID)));
    }

    @AfterAll
    static void cleanup() {
        JIPipe.getNodes().unregister(STUB_ID);
    }

    /**
     * The stub's own node entry (a real, insertable database entry). The stub
     * is registered under the Images branch (the ColorStackFunction leaf), so
     * its entry attaches to the same branch as the test templates.
     */
    private static JIPipeNodeDatabaseEntry nodeEntry() {
        return new CreateNewNodeByInfoDatabaseEntry(STUB_ID, JIPipe.getNodes().getInfoById(STUB_ID));
    }

    private static CreateNewNodesByTemplateDatabaseEntry templateEntry(TemplateScope scope, String fnId) {
        JIPipeGraph graph = new JIPipeGraph();
        JIPipeNodeInfo stub = JIPipe.getNodes().getInfoById(STUB_ID);
        graph.insertNode(stub.newInstance());
        graph.insertNode(stub.newInstance());
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Controller Test Template " + scope);
        template.setGraph(graph);
        template.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef(fnId));
        return new CreateNewNodesByTemplateDatabaseEntry(template, scope);
    }

    @Test
    void leavesReflectTheFullEntrySetNotTheSelectionFilter() {
        // A template filed under the Images branch (System scope) + a node
        // entry. The tree must carry leaves for BOTH from the full set, and
        // the function branch must be expandable (the root cause of "[+] only
        // visible if the node is selected"): a branch is expandable exactly
        // when it carries entry leaves.
        JIPipeNodeDatabaseEntry node = nodeEntry();
        CreateNewNodesByTemplateDatabaseEntry template =
                templateEntry(TemplateScope.System, IMAGES_FN_ID);

        controller.rebuildNodeLeaves(List.of(node, template));
        // The template is dual-placed (under its function branch AND under
        // Templates/System), so the whole-tree count is node + 2 = 3.
        assertThat(leafCount()).isEqualTo(3);

        JIPipeNodeTaxonomyFunction images = JIPipe.getNodes().getFunctions().byId(IMAGES_FN_ID);
        assertThat(images).isNotNull();
        TreePath row = controller.functionRow(images);
        assertThat(row).isNotNull();
        expandTo(row);
        assertThat(leafCount()).isEqualTo(3);
        assertThat(controller.nodeLeafRow(template)).isNotNull();
        assertThat(controller.nodeLeafRow(node)).isNotNull();
    }

    @Test
    void systemScopeLeavesPersistWhileAFunctionIsSelected() {
        // Root cause of "System templates only shown when System is selected":
        // the browser rebuilt the leaves from the SELECTION-FILTERED set, so a
        // System template left the tree the moment a function was selected.
        // With full-set attachment the System leaf must persist - re-attaching
        // the FULL set (as the fixed browser does) must not drop it.
        CreateNewNodesByTemplateDatabaseEntry systemTemplate =
                templateEntry(TemplateScope.System, IMAGES_FN_ID);
        JIPipeNodeDatabaseEntry node = nodeEntry();
        List<JIPipeNodeDatabaseEntry> full = List.of(systemTemplate, node);
        controller.rebuildNodeLeaves(full);

        // The "selection" (a function path or another scope) never changes the
        // attached set: the System template's leaf persists, and so does the
        // non-matching node entry's leaf.
        TreePath scopeSystem = scopeRow(TemplateScope.System);
        assertThat(scopeSystem).isNotNull();
        expandTo(scopeSystem);
        assertThat(controller.nodeLeafRow(systemTemplate)).isNotNull();
        assertThat(leafCount()).isEqualTo(3);
    }

    /**
     * The round-2 review bug: the tree ROOT (a String user object, "All
     * functions") never forwarded into the synthetic Templates header, so the
     * System/Global/Project scope rows received NO leaves in any tier - the
     * Templates/System branch appeared empty while the same entries were in
     * the results list. The leaf must hang directly under the scope row
     * (Templates → System), not only under the function branch.
     */
    @Test
    void scopeRowsCarryTheirLeavesDirectly() {
        CreateNewNodesByTemplateDatabaseEntry systemTemplate =
                templateEntry(TemplateScope.System, IMAGES_FN_ID);
        CreateNewNodesByTemplateDatabaseEntry globalTemplate =
                templateEntry(TemplateScope.Global, IMAGES_FN_ID);
        JIPipeNodeDatabaseEntry node = nodeEntry();
        controller.rebuildNodeLeaves(List.of(systemTemplate, globalTemplate, node));

        // Each scope row's OWN leaf count (children of the scope row, not the
        // whole tree): System carries the System template, Global the Global
        // one, Project none.
        assertThat(scopeRowLeafCount(TemplateScope.System)).isEqualTo(1);
        assertThat(scopeRowLeafCount(TemplateScope.Global)).isEqualTo(1);
        assertThat(scopeRowLeafCount(TemplateScope.Project)).isEqualTo(0);
        // ... and the whole-tree count reflects the dual placement (each
        // template hangs under its function branch AND its scope row: 2x2
        // templates + 1 node = 5).
        assertThat(leafCount()).isEqualTo(5);
    }

    /**
     * The "Patterns" grouping: a template entry must NOT hang directly under
     * a function row (that mixed templates into the node lists - the
     * "functions full of templates" clutter). It hangs one level below, under
     * a synthetic "Patterns" child of the function that carries it (or the
     * scope row on the Templates branch); the function level itself shows
     * only real nodes. The dual placement count is unchanged (the
     * Templates/<scope> branch still carries the leaf too).
     */
    @Test
    void templatesHangUnderThePatternsChildNotTheFunctionLevel() {
        JIPipeNodeDatabaseEntry node = nodeEntry();
        CreateNewNodesByTemplateDatabaseEntry template =
                templateEntry(TemplateScope.System, IMAGES_FN_ID);
        controller.rebuildNodeLeaves(List.of(node, template));

        Map<Object, List<Object>> parentsByLeaf = new HashMap<>();
        collectLeafParents((DefaultMutableTreeNode) controller.getTreeModel().getRoot(), parentsByLeaf);

        // The template leaf (dual-placed: function side + scope side) - EVERY
        // occurrence hangs under a grouping row (Patterns or
        // Templates/<scope>), never directly under a function row.
        List<Object> templateParents = parentsByLeaf.get(template);
        assertThat(templateParents)
                .as("the dual-placed template leaf must be present, got %s", parentsByLeaf.keySet())
                .hasSize(2);
        boolean hasPatterns = false, hasScope = false;
        for (Object parent : templateParents) {
            if (parent instanceof PatternsTreeNode patterns) {
                hasPatterns = true;
                assertThat(patterns.getFunction().getId()).isEqualTo(IMAGES_FN_ID);
            } else if (parent instanceof TemplateScopeTreeNode) {
                hasScope = true;
            } else {
                fail("a template leaf must hang under a Patterns or scope row, got parent: " + parent);
            }
        }
        assertThat(hasPatterns)
                .as("at least one template occurrence hangs under the <function>/Patterns row")
                .isTrue();
        assertThat(hasScope)
                .as("the Templates/<scope> placement is kept (dual view)")
                .isTrue();

        // The real node's leaf hangs directly under a FUNCTION row (never a
        // Patterns row).
        List<Object> nodeParents = parentsByLeaf.get(node);
        assertThat(nodeParents).hasSize(1);
        assertThat(nodeParents.get(0))
                .as("the real node stays at the function's own level, not under Patterns")
                .isInstanceOf(JIPipeNodeTaxonomyFunction.class);

        // The dual placement count holds (node + Patterns leaf +
        // Templates/System leaf = 3).
        assertThat(leafCount()).isEqualTo(3);
    }

    /**
     * Maps each entry leaf to the user objects of the rows it hangs
     * DIRECTLY under (a template is dual-placed, so it may have two).
     */
    private void collectLeafParents(DefaultMutableTreeNode node, Map<Object, List<Object>> out) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (!(child instanceof DefaultMutableTreeNode mtn)) {
                continue;
            }
            if (mtn.getUserObject() instanceof JIPipeNodeDatabaseEntry entry) {
                out.computeIfAbsent(entry, k -> new ArrayList<>()).add(node.getUserObject());
            }
            collectLeafParents(mtn, out);
        }
    }

    @Test
    void functionWithoutTemplatesCarriesNoPatternsChild() {
        // A function that has real nodes but NO template entries must not
        // gain an empty Patterns row.
        JIPipeNodeDatabaseEntry node = nodeEntry();
        controller.rebuildNodeLeaves(List.of(node));
        JIPipeNodeTaxonomyFunction images = JIPipe.getNodes().getFunctions().byId(IMAGES_FN_ID);
        assertThat(patternRow(images)).isNull();
        assertThat(leafCount()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * The entry leaves that hang DIRECTLY under the given scope row.
     */
    private int scopeRowLeafCount(TemplateScope scope) {
        TreePath row = scopeRow(scope);
        assertThat(row).isNotNull();
        Object last = row.getLastPathComponent();
        if (!(last instanceof DefaultMutableTreeNode mtn)) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < mtn.getChildCount(); i++) {
            if (mtn.getChildAt(i) instanceof DefaultMutableTreeNode child
                    && child.getUserObject() instanceof JIPipeNodeDatabaseEntry) {
                count++;
            }
        }
        return count;
    }

    @Test
    void removingAnEntryFromTheFullSetRemovesItsLeaf() {
        // The leaves follow the current (search+facet) result set: an entry
        // that no longer matches the search must lose its leaf, the others keep theirs.
        CreateNewNodesByTemplateDatabaseEntry template =
                templateEntry(TemplateScope.Global, IMAGES_FN_ID);
        JIPipeNodeDatabaseEntry node = nodeEntry();
        controller.rebuildNodeLeaves(List.of(template, node));
        assertThat(leafCount()).isEqualTo(3);

        controller.rebuildNodeLeaves(List.of(node));
        assertThat(leafCount()).isEqualTo(1);
        assertThat(controller.nodeLeafRow(template)).isNull();
        assertThat(controller.nodeLeafRow(node)).isNotNull();
    }

    /**
     * Counts the entry leaves in the tree MODEL (independent of expansion
     * state - visible-row counting would miss leaves under collapsed branches).
     */
    private int leafCount() {
        return countLeaves((DefaultMutableTreeNode) controller.getTreeModel().getRoot());
    }

    private int countLeaves(DefaultMutableTreeNode node) {
        int count = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn) {
                if (mtn.getUserObject() instanceof JIPipeNodeDatabaseEntry) {
                    count++;
                } else {
                    count += countLeaves(mtn);
                }
            }
        }
        return count;
    }

    private TreePath scopeRow(TemplateScope scope) {
        TreePath rootPath = new TreePath(new Object[]{controller.getTreeModel().getRoot()});
        List<TreePath> paths = new ArrayList<>();
        collectPaths(rootPath, paths);
        for (TreePath p : paths) {
            if (scope == controller.scopeFromTreePath(p)) {
                return p;
            }
        }
        return null;
    }

    private TreePath patternRow(JIPipeNodeTaxonomyFunction function) {
        TreePath rootPath = new TreePath(new Object[]{controller.getTreeModel().getRoot()});
        List<TreePath> paths = new ArrayList<>();
        collectPaths(rootPath, paths);
        for (TreePath p : paths) {
            if (function == controller.functionOfPatternsPath(p)) {
                return p;
            }
        }
        return null;
    }

    private void collectPaths(TreePath parent, List<TreePath> out) {
        out.add(parent);
        Object last = parent.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode mtn) {
            for (int i = 0; i < mtn.getChildCount(); i++) {
                collectPaths(parent.pathByAddingChild(mtn.getChildAt(i)), out);
            }
        }
    }

    private void expandTo(TreePath path) {
        Object[] components = path.getPath();
        for (int i = 1; i < components.length; i++) {
            controller.getTree().expandPath(new TreePath(java.util.Arrays.copyOfRange(components, 0, i)));
        }
    }

    /**
     * A minimal, unconverted stub node used purely as material for the
     * template graphs.
     */
    @SetJIPipeDocumentation(name = "Tree Controller Stub Node", description = "A stub for the tree controller test.")
    @ConfigureJIPipeNode(function = ColorStackFunction.class)
    public static class StubTemplateNode extends JIPipeGraphNode {
        public StubTemplateNode(JIPipeNodeInfo info) {
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
