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
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the template database entry extension (spec §7.1): the entry carries a
 * {@link TemplateScope}, a computed {@code Pattern}/{@code Custom} badge (graph node
 * count, never stored), taxonomy function ids derived from the template's
 * {@link JIPipeTaxonomyFunctionRef} (with the {@code jipipe:function:templates}
 * catch-all for unset/stale refs), and a scoped {@code "Templates / <scope>"}
 * location info. Boots the JIPipe instance so real (stub) registry nodes can be
 * instantiated into the template graphs.
 */
class CreateNewNodesByTemplateDatabaseEntryTest {

    private static final String STUB_ID = "template-entry-test:stub-node";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
        new JIPipeMutableDependency("template-entry-test", "1.0.0", "template-entry-test");

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
        // Register a minimal, namespaced stub node so the test graphs can be
        // built from the live registry without depending on which bundled core
        // plugins happened to boot in this run.
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        if (!nodes.hasNodeInfoWithId(STUB_ID)) {
            nodes.register(new JIPipeJavaNodeInfo(STUB_ID, StubTemplateNode.class), TEST_DEPENDENCY);
        }
    }

    @AfterAll
    static void cleanup() {
        // Do not leak the stub into the shared registry for other test classes
        // (mirrors TokenTaxonomyTest).
        JIPipe.getNodes().unregister(STUB_ID);
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    @Test
    void badgeIsCustomForSingleNode() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(1, "jipipe:function:images"), TemplateScope.Project);
        assertThat(e.getBadge()).isEqualTo("Custom");
    }

    @Test
    void badgeIsPatternForMultiNode() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(2, "jipipe:function:images"), TemplateScope.Global);
        assertThat(e.getBadge()).isEqualTo("Pattern");
    }

    @Test
    void functionIdsFromRef() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(2, "jipipe:function:tables"), TemplateScope.System);
        assertThat(e.getFunctionIds()).containsExactly("jipipe:function:tables");
    }

    @Test
    void unresolvedRefFallsBackToCatchAll() {
        JIPipeNodeTemplate t = tmpl(2, null);
        t.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef("jipipe:function:gone"));
        var e = new CreateNewNodesByTemplateDatabaseEntry(t, TemplateScope.Project);
        assertThat(e.getFunctionIds()).containsExactly("jipipe:function:templates");
    }

    @Test
    void locationInfoIsScoped() {
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(1, "jipipe:function:images"), TemplateScope.Global);
        assertThat(e.getLocationInfos()).containsExactly("Templates / Global");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Builds a real {@link JIPipeNodeTemplate} whose graph contains {@code nodes}
     * instances of the registered stub node (no edges — the badge counts nodes,
     * not edges). When {@code fnId} is non-null, the template's taxonomy function
     * ref is set to that id; otherwise the ref is left unset (the default no-arg
     * ref). {@code template.setGraph(graph)} serializes the graph, and
     * {@code template.getGraph()} deserializes it back, so the entry under test
     * sees a genuinely reconstructed graph.
     */
    private JIPipeNodeTemplate tmpl(int nodes, String fnId) {
        JIPipeGraph graph = new JIPipeGraph();
        for (int i = 0; i < nodes; i++) {
            JIPipeGraphNode node = JIPipe.getNodes().getInfoById(STUB_ID).newInstance();
            graph.insertNode(node);
        }
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Test template " + nodes + "n");
        template.setGraph(graph);
        if (fnId != null) {
            template.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef(fnId));
        }
        return template;
    }

    // ------------------------------------------------------------------
    // Stub node
    // ------------------------------------------------------------------

    /**
     * A minimal, unconverted node used purely as material for the template
     * graphs. It declares no taxonomy, so it carries no behavior flags that
     * would flip the entry's pipeline visibility — the entry's constructor
     * (which walks the graph) stays deterministic.
     */
    @SetJIPipeDocumentation(name = "Template Stub Node", description = "A stub for template entry testing.")
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
