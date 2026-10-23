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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.NodeBrowserModel;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the result-kind classifier of {@link NodeEntryKind} (1340): the
 * node / example / template kinds, the template split by the existing
 * Pattern/Custom badge AND system/user origin, and the "no kind" bypass for
 * existing nodes / compartments and new compartments.
 * <p>
 * The non-trivial contract under test:
 * <ul>
 * <li>the badge × scope matrix (a template entry lands in exactly one of
 * the four template kinds);</li>
 * <li>entries without a kind always pass {@link NodeEntryKind#matches}
 * (selecting e.g. only "Pattern (user)" must not hide existing nodes);</li>
 * <li>an empty selection matches everything (no filter active);</li>
 * <li>{@link NodeBrowserModel#matchesKindFilter} delegates to the same
 * predicate (both node-browser surfaces agree).</li>
 * </ul>
 * Boots the {@link JIPipe} instance for the real entry constructors, but
 * never touches Swing.
 */
class NodeEntryKindTest {

    private static final String STUB_ID = "node-entry-kind-test:stub-node";
    private static final String IMAGES_FN_ID = "jipipe:function:images";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
        new JIPipeMutableDependency("node-entry-kind-test", "1.0.0", "node-entry-kind-test");

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        if (!nodes.hasNodeInfoWithId(STUB_ID)) {
            nodes.register(new JIPipeJavaNodeInfo(STUB_ID, StubNode.class), TEST_DEPENDENCY);
        }
    }

    @AfterAll
    static void cleanup() {
        // Do not leak the stub into the shared registry.
        JIPipe.getNodes().unregister(STUB_ID);
    }

    @Test
    void classifiesNodeAndExampleEntries() {
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(STUB_ID);
        assertThat(NodeEntryKind.of(new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + STUB_ID, info)))
            .isEqualTo(NodeEntryKind.NODES);
        JIPipeNodeTemplate single = template(1, IMAGES_FN_ID);
        assertThat(NodeEntryKind.of(new CreateNewNodeByExampleDatabaseEntry("create-node-by-example:" + STUB_ID + ":[0]", new JIPipeNodeExample(single))))
            .isEqualTo(NodeEntryKind.EXAMPLES);
    }

    @Test
    void classifiesTemplatesByBadgeAndOrigin() {
        // The badge follows the graph node count (1 → Custom, ≥2 → Pattern);
        // the origin side follows the TemplateScope (System vs. the rest).
        assertThat(NodeEntryKind.of(templateEntry(2, TemplateScope.System))).isEqualTo(NodeEntryKind.PATTERNS_SYSTEM);
        assertThat(NodeEntryKind.of(templateEntry(2, TemplateScope.Global))).isEqualTo(NodeEntryKind.PATTERNS_USER);
        assertThat(NodeEntryKind.of(templateEntry(2, TemplateScope.Project))).isEqualTo(NodeEntryKind.PATTERNS_USER);
        assertThat(NodeEntryKind.of(templateEntry(2, TemplateScope.Downloaded))).isEqualTo(NodeEntryKind.PATTERNS_USER);

        assertThat(NodeEntryKind.of(templateEntry(1, TemplateScope.System))).isEqualTo(NodeEntryKind.CUSTOM_SYSTEM);
        assertThat(NodeEntryKind.of(templateEntry(1, TemplateScope.Global))).isEqualTo(NodeEntryKind.CUSTOM_USER);
        assertThat(NodeEntryKind.of(templateEntry(1, TemplateScope.Project))).isEqualTo(NodeEntryKind.CUSTOM_USER);
        assertThat(NodeEntryKind.of(templateEntry(1, TemplateScope.Downloaded))).isEqualTo(NodeEntryKind.CUSTOM_USER);
    }

    @Test
    void kindlessEntriesAreNull() {
        assertThat(NodeEntryKind.of(new CreateNewCompartmentNodeDatabaseEntry())).isNull();
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(STUB_ID);
        JIPipeGraphNode node = info.newInstance();
        assertThat(NodeEntryKind.of(new ExistingPipelineNodeDatabaseEntry("existing-pipeline-node:test", node))).isNull();
        assertThat(NodeEntryKind.of(new ExistingCompartmentDatabaseEntry("existing-compartment:test", new JIPipeProjectCompartment(info)))).isNull();
    }

    @Test
    void emptySelectionMatchesEverything() {
        assertThat(NodeEntryKind.matches(templateEntry(2, TemplateScope.System), Set.of())).isTrue();
        assertThat(NodeEntryKind.matches(templateEntry(2, TemplateScope.System), null)).isTrue();
        assertThat(NodeEntryKind.matches(new CreateNewCompartmentNodeDatabaseEntry(), Set.of())).isTrue();
    }

    @Test
    void selectedKindsFilterAndKindlessEntriesAlwaysPass() {
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(STUB_ID);
        Set<NodeEntryKind> onlyUserPatterns = Set.of(NodeEntryKind.PATTERNS_USER);

        assertThat(NodeEntryKind.matches(templateEntry(2, TemplateScope.Global), onlyUserPatterns)).isTrue();
        assertThat(NodeEntryKind.matches(templateEntry(2, TemplateScope.System), onlyUserPatterns)).isFalse();
        assertThat(NodeEntryKind.matches(templateEntry(1, TemplateScope.Global), onlyUserPatterns)).isFalse();
        assertThat(NodeEntryKind.of(templateEntry(1, TemplateScope.Global))).isEqualTo(NodeEntryKind.CUSTOM_USER);

        // Existing / compartment entries carry no kind and must NOT be
        // hidden by a kind selection (their visibility is governed by the
        // finder dialog's "Find existing" toggle, not this filter).
        JIPipeGraphNode node = info.newInstance();
        JIPipeNodeDatabaseEntry existing = new ExistingPipelineNodeDatabaseEntry("existing-pipeline-node:test", node);
        assertThat(NodeEntryKind.matches(existing, onlyUserPatterns)).isTrue();
        assertThat(NodeEntryKind.matches(new CreateNewCompartmentNodeDatabaseEntry(), onlyUserPatterns)).isTrue();
    }

    @Test
    void allKindsSelectionPassesEveryKindedEntry() {
        Set<NodeEntryKind> all = EnumSet.allOf(NodeEntryKind.class);
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(STUB_ID);
        assertThat(NodeEntryKind.matches(new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + STUB_ID, info), all)).isTrue();
        assertThat(NodeEntryKind.matches(templateEntry(2, TemplateScope.System), all)).isTrue();
        assertThat(NodeEntryKind.matches(templateEntry(1, TemplateScope.Downloaded), all)).isTrue();
    }

    @Test
    void modelDelegatesToSharedPredicate() {
        NodeBrowserModel model = new NodeBrowserModel();
        // Default (empty) = no filter: everything passes.
        assertThat(model.matchesKindFilter(templateEntry(2, TemplateScope.System))).isTrue();
        assertThat(model.matchesKindFilter(new CreateNewCompartmentNodeDatabaseEntry())).isTrue();

        model.setKindFilter(Set.of(NodeEntryKind.NODES));
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(STUB_ID);
        assertThat(model.matchesKindFilter(new CreateNewNodeByInfoDatabaseEntry("create-node-by-info:" + STUB_ID, info))).isTrue();
        assertThat(model.matchesKindFilter(templateEntry(2, TemplateScope.System))).isFalse();
        // Kindless entries still pass a non-empty selection.
        assertThat(model.matchesKindFilter(new CreateNewCompartmentNodeDatabaseEntry())).isTrue();

        model.setKindFilter(null);
        assertThat(model.getKindFilter()).isEmpty();
        assertThat(model.matchesKindFilter(templateEntry(1, TemplateScope.Global))).isTrue();
    }

    // ------------------------------------------------------------------
    // Helpers (mirrors NodeBrowserModelTemplateTest's fixture pattern)
    // ------------------------------------------------------------------

    private JIPipeNodeTemplate template(int nodes, String fnId) {
        JIPipeGraph graph = new JIPipeGraph();
        for (int i = 0; i < nodes; i++) {
            graph.insertNode(JIPipe.getNodes().getInfoById(STUB_ID).newInstance());
        }
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Kind Test Template " + nodes + "n");
        template.setGraph(graph);
        template.setTaxonomyFunction(new JIPipeTaxonomyFunctionRef(fnId));
        return template;
    }

    private CreateNewNodesByTemplateDatabaseEntry templateEntry(int nodes, TemplateScope scope) {
        return new CreateNewNodesByTemplateDatabaseEntry(template(nodes, IMAGES_FN_ID), scope);
    }

    /**
     * A minimal node used purely as material for the entries (the kind
     * classifier never reads node behavior).
     */
    @SetJIPipeDocumentation(name = "Node Entry Kind Stub Node", description = "A stub for the node entry kind test.")
    @ConfigureJIPipeNode
    public static class StubNode extends JIPipeGraphNode {
        public StubNode(JIPipeNodeInfo info) {
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
