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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

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
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the template-aware selection predicate of
 * {@link NodeBrowserModel} (spec §7.2, the non-Swing testable core): the
 * synthetic {@code Templates / <scope>} group and a taxonomy-function path
 * are mutually-exclusive selections, and a template entry is listed when
 * EITHER its scope group OR its taxonomy function (the dual placement) is
 * selected.
 * <p>
 * This class boots the {@link JIPipe} instance so that real template
 * entries (built from a registered stub node) and the live
 * {@link FunctionRegistry} (for {@link NodeBrowserModel#descendantIds}) can
 * be exercised. It never touches Swing - no tree, no list.
 */
class NodeBrowserModelTemplateTest {

    private static final String STUB_ID = "template-browser-model-test:stub-node";
    /**
     * A real, registry-registered branch the stub function ids can be filed under.
     */
    private static final String IMAGES_FN_ID = "jipipe:function:images";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("template-browser-model-test", "1.0.0", "template-browser-model-test");

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
        // A namespaced stub node so the template graphs can be built from the
        // live registry without depending on which bundled core plugins booted.
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        if (!nodes.hasNodeInfoWithId(STUB_ID)) {
            nodes.register(new JIPipeJavaNodeInfo(STUB_ID, StubTemplateNode.class), TEST_DEPENDENCY);
        }
    }

    @AfterAll
    static void cleanup() {
        // Do not leak the stub into the shared registry (mirrors the other
        // template tests).
        JIPipe.getNodes().unregister(STUB_ID);
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    @Test
    void scopeSelectionFiltersToThatScope() {
        NodeBrowserModel model = new NodeBrowserModel();
        CreateNewNodesByTemplateDatabaseEntry global = entry(1, IMAGES_FN_ID, TemplateScope.Global);
        CreateNewNodesByTemplateDatabaseEntry system = entry(2, IMAGES_FN_ID, TemplateScope.System);

        model.setSelectedScope(TemplateScope.Global);
        assertThat(model.getSelectedScope()).isEqualTo(TemplateScope.Global);

        // The Global template passes, the System one does not (a scope
        // selection needs no function registry, so it takes null).
        assertThat(model.matchesSelection(global, null)).isTrue();
        assertThat(model.matchesSelection(system, null)).isFalse();
    }

    @Test
    void scopeAndFunctionAreMutuallyExclusive() {
        NodeBrowserModel model = new NodeBrowserModel();
        JIPipeNodeTaxonomyFunction images = JIPipe.getNodes().getFunctions().byId(IMAGES_FN_ID);
        assertThat(images).isNotNull();

        model.setSelectedScope(TemplateScope.Project);
        assertThat(model.getSelectedScope()).isEqualTo(TemplateScope.Project);

        // Setting a function path clears the scope ...
        model.setTreePath(java.util.List.of(images));
        assertThat(model.getTreePath()).hasSize(1);
        assertThat(model.getSelectedScope()).isNull();

        // ... and setting a scope clears the function path.
        model.setSelectedScope(TemplateScope.System);
        assertThat(model.getSelectedScope()).isEqualTo(TemplateScope.System);
        assertThat(model.getTreePath()).isEmpty();
    }

    @Test
    void functionPathIncludesTemplateUnderItsFunction() {
        NodeBrowserModel model = new NodeBrowserModel();
        FunctionRegistry registry = JIPipe.getNodes().getFunctions();
        JIPipeNodeTaxonomyFunction images = registry.byId(IMAGES_FN_ID);

        // A template filed under the Images branch (the dual-placement
        // "taxonomy" side) passes when that branch is selected ...
        CreateNewNodesByTemplateDatabaseEntry underImages = entry(1, IMAGES_FN_ID, TemplateScope.Project);
        model.setTreePath(java.util.List.of(images));
        assertThat(model.matchesSelection(underImages, registry)).isTrue();
        // ... but is excluded when a different (non-ancestor) branch is
        // selected.
        JIPipeNodeTaxonomyFunction tables = registry.byId("jipipe:function:tables");
        assertThat(tables).isNotNull();
        model.setTreePath(java.util.List.of(tables));
        assertThat(model.matchesSelection(underImages, registry)).isFalse();

        // A scope selection still wins over the function path (mutually
        // exclusive; a System scope does not list a Project template).
        model.setSelectedScope(TemplateScope.System);
        assertThat(model.getSelectedScope()).isEqualTo(TemplateScope.System);
        assertThat(model.matchesSelection(underImages, null)).isFalse();
    }

    @Test
    void rootSelectionListsEachTemplateOnce() {
        NodeBrowserModel model = new NodeBrowserModel();
        // Root (no scope, empty path): every entry passes the selection
        // predicate - no scope, no function filter. This guards the
        // predicate against double-counting a template (it is listed once;
        // de-dup is inherent to the single query-driven list).
        CreateNewNodesByTemplateDatabaseEntry a = entry(1, IMAGES_FN_ID, TemplateScope.Global);
        CreateNewNodesByTemplateDatabaseEntry b = entry(2, IMAGES_FN_ID, TemplateScope.Project);

        assertThat(model.getSelectedScope()).isNull();
        assertThat(model.getTreePath()).isEmpty();
        // Both dually-placed templates pass the root selection (each exactly
        // once - no scope/function dimension to count them a second time).
        assertThat(model.matchesSelection(a, null)).isTrue();
        assertThat(model.matchesSelection(b, null)).isTrue();
    }

    @Test
    void patternsFunctionSelectionFiltersToTemplatesOfThatFunction() {
        NodeBrowserModel model = new NodeBrowserModel();
        JIPipeNodeTaxonomyFunction images = JIPipe.getNodes().getFunctions().byId(IMAGES_FN_ID);
        assertThat(images).isNotNull();
        JIPipeNodeTaxonomyFunction tables = JIPipe.getNodes().getFunctions().byId("jipipe:function:tables");
        assertThat(tables).isNotNull();

        CreateNewNodesByTemplateDatabaseEntry underImages = entry(1, IMAGES_FN_ID, TemplateScope.System);
        CreateNewNodesByTemplateDatabaseEntry underTables = entry(2, "jipipe:function:tables", TemplateScope.System);

        model.setPatternsFunction(images);
        assertThat(model.getPatternsFunction()).isEqualTo(images);
        // The <function>/Patterns row lists the template entries filed under
        // exactly that function ...
        assertThat(model.matchesSelection(underImages, null)).isTrue();
        // ... and not templates of a different function.
        assertThat(model.matchesSelection(underTables, null)).isFalse();
    }

    @Test
    void patternsFunctionIsMutuallyExclusiveWithScopeAndPath() {
        NodeBrowserModel model = new NodeBrowserModel();
        JIPipeNodeTaxonomyFunction images = JIPipe.getNodes().getFunctions().byId(IMAGES_FN_ID);
        assertThat(images).isNotNull();

        model.setPatternsFunction(images);
        model.setSelectedScope(TemplateScope.System);
        assertThat(model.getPatternsFunction()).isNull();
        assertThat(model.getSelectedScope()).isEqualTo(TemplateScope.System);

        model.setPatternsFunction(images);
        model.setTreePath(java.util.List.of(images));
        assertThat(model.getPatternsFunction()).isNull();
        assertThat(model.getTreePath()).hasSize(1);

        model.setPatternsFunction(images);
        assertThat(model.getPatternsFunction()).isEqualTo(images);
        assertThat(model.getTreePath()).isEmpty();
        assertThat(model.getSelectedScope()).isNull();
    }

    @Test
    void badgeReflectsNodeCount() {
        // Task 4 behavior, smoke-checked here: 1 node → Custom, ≥2 → Pattern.
        assertThat(entry(1, IMAGES_FN_ID, TemplateScope.Project).getBadge()).isEqualTo("Custom");
        assertThat(entry(2, IMAGES_FN_ID, TemplateScope.Global).getBadge()).isEqualTo("Pattern");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * A real template database entry whose graph contains {@code nodes}
     * instances of the registered stub node, filed under {@code fnId} (the
     * live registry id) and in {@code scope}.
     */
    private CreateNewNodesByTemplateDatabaseEntry entry(int nodes, String fnId, TemplateScope scope) {
        JIPipeGraph graph = new JIPipeGraph();
        for (int i = 0; i < nodes; i++) {
            JIPipeGraphNode node = JIPipe.getNodes().getInfoById(STUB_ID).newInstance();
            graph.insertNode(node);
        }
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Browser Test Template " + nodes + "n");
        template.setGraph(graph);
        template.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef(fnId));
        return new CreateNewNodesByTemplateDatabaseEntry(template, scope);
    }

    // ------------------------------------------------------------------
    // Stub node
    // ------------------------------------------------------------------

    /**
     * A minimal, unconverted node used purely as material for the template
     * graphs (the badge counts nodes, not edges).
     */
    @SetJIPipeDocumentation(name = "Browser Template Stub Node", description = "A stub for node-browser template testing.")
    @ConfigureJIPipeNode
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
