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

import java.awt.*;
import java.util.UUID;

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
        // The location reads as the template's FUNCTION ("Images" — the same
        // form the node entries use), not as storage bookkeeping ("Templates /
        // Global"). The scope line remains only as the fallback for an unset
        // function.
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(1, "jipipe:function:images"), TemplateScope.Global);
        assertThat(e.getLocationInfos()).containsExactly("Images");
    }

    @Test
    void locationInfoFallsBackToScopeWithoutFunction() {
        // Unset taxonomy function: the catch-all placement keeps the scope
        // line as the location (there is no function to show).
        var e = new CreateNewNodesByTemplateDatabaseEntry(tmpl(1, null), TemplateScope.Global);
        assertThat(e.getLocationInfos()).containsExactly("Templates / Global");
    }

    // ------------------------------------------------------------------
    // Round 2: the "Add to graph" / double-click phantom-compartment bug.
    // All 64 mined System patterns store the SOURCE project's pipeline
    // compartment UUID in every node ("jipipe:graph-compartment").
    // mergeWith preserves that UUID, so the clicked-into-canvas nodes land
    // in a compartment the canvas does not render: isVisibleIn
    // (canvasCompartment) is false for all of them, no node UIs are
    // created, and nothing happens — no error, no log. Drag & drop was
    // unaffected because the drop is re-parsed through pasteNodes, which
    // re-keys the compartment to the target canvas. The fix
    // (rekeyTemplateGraphToCompartment) must restore canvas visibility and
    // keep the stored locations resolvable under the new compartment key.
    // ------------------------------------------------------------------

    @Test
    void phantomSourceCompartmentIsRekeyedSoNodesAppearInTheCanvas() {
        // Build a template exactly like a mined pattern: two nodes in a
        // random (source-project) compartment, with locations stored under
        // that compartment.
        JIPipeGraph source = new JIPipeGraph();
        UUID phantomCompartment = UUID.randomUUID();
        JIPipeGraphNode a = JIPipe.getNodes().getInfoById(STUB_ID).newInstance();
        JIPipeGraphNode b = JIPipe.getNodes().getInfoById(STUB_ID).newInstance();
        source.insertNode(a, phantomCompartment);
        source.insertNode(b, phantomCompartment);
        a.setNodeUILocationWithin(phantomCompartment.toString(), new Point(5, 3));
        b.setNodeUILocationWithin(phantomCompartment.toString(), new Point(9, 3));
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Phantom Compartment Probe");
        template.setGraph(source); // serialize (the mined patterns' data shape)

        // The stored graph genuinely carries the phantom compartment (the
        // precondition of the bug).
        JIPipeGraph stored = template.getGraph();
        for (JIPipeGraphNode node : stored.getGraphNodes()) {
            assertThat(stored.getCompartmentUUIDOf(node)).isEqualTo(phantomCompartment);
        }

        // The canvas' (compartment) graph — e.g. the project's pipeline.
        UUID canvasCompartment = UUID.randomUUID();
        JIPipeGraph canvas = new JIPipeGraph();

        // Without the fix, the merge puts the nodes in the phantom
        // compartment and the canvas renders none of them:
        JIPipeGraph brokenMerge = new JIPipeGraph();
        brokenMerge.mergeWith(template.getGraph().duplicate());
        for (JIPipeGraphNode node : brokenMerge.getGraphNodes()) {
            assertThat(node.isVisibleIn(canvasCompartment))
                .as("without the re-key the node is invisible on the canvas (the bug)")
                .isFalse();
        }

        // With the fix: re-key the duplicated graph, then merge.
        JIPipeGraph graph = template.getGraph().duplicate();
        CreateNewNodesByTemplateDatabaseEntry.rekeyTemplateGraphToCompartment(graph, canvasCompartment);
        canvas.mergeWith(graph);
        assertThat(canvas.getGraphNodes()).hasSize(2);
        for (JIPipeGraphNode node : canvas.getGraphNodes()) {
            assertThat(node.getCompartmentUUIDInParentGraph()).isEqualTo(canvasCompartment);
            assertThat(node.isVisibleIn(canvasCompartment))
                .as("the canvas renders the node now")
                .isTrue();
            // The stored location survived the re-key under the NEW
            // compartment (resolvable by the canvas' moveToStoredGridLocation)
            // and the stale entry under the phantom compartment is gone (no
            // metadata bloat in saved projects).
            assertThat(node.getNodeUILocationWithin(canvasCompartment.toString()))
                .as("location resolvable under the canvas compartment")
                .isNotNull();
            assertThat(node.getNodeUILocationWithin(phantomCompartment.toString()))
                .as("stale location entry under the phantom compartment removed")
                .isNull();
        }
    }

    @Test
    void defaultCompartmentTemplateIsRekeyedOnlyForSubCompartments() {
        // User-authored templates store the empty/default compartment ("").
        JIPipeGraph source = new JIPipeGraph();
        JIPipeGraphNode a = JIPipe.getNodes().getInfoById(STUB_ID).newInstance();
        source.insertNode(a); // default (null) compartment
        a.setNodeUILocationWithin("_", new Point(1, 2));
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Default Compartment Probe");
        template.setGraph(source);

        // Main canvas (null compartment, i.e. key "_"): the node already
        // belongs to the right compartment — the location stays under "_".
        JIPipeGraph mainCanvas = new JIPipeGraph();
        mainCanvas.mergeWith(rekeyedDuplicate(template, null));
        JIPipeGraphNode onMain = mainCanvas.getGraphNodes().iterator().next();
        assertThat(onMain.getCompartmentUUIDInParentGraph()).isNull();
        assertThat(onMain.isVisibleIn(null)).isTrue();
        assertThat(onMain.getNodeUILocationWithin("_")).isEqualTo(new Point(1, 2));

        // Sub-compartment canvas: the node must be re-keyed into it (exactly
        // what pasteNodes does for dropped templates), location moved.
        UUID sub = UUID.randomUUID();
        JIPipeGraph subCanvas = new JIPipeGraph();
        subCanvas.mergeWith(rekeyedDuplicate(template, sub));
        JIPipeGraphNode onSub = subCanvas.getGraphNodes().iterator().next();
        assertThat(onSub.getCompartmentUUIDInParentGraph()).isEqualTo(sub);
        assertThat(onSub.isVisibleIn(sub)).isTrue();
        assertThat(onSub.getNodeUILocationWithin(sub.toString())).isEqualTo(new Point(1, 2));
        assertThat(onSub.getNodeUILocationWithin("_")).isNull();
    }

    private static JIPipeGraph rekeyedDuplicate(JIPipeNodeTemplate template, UUID target) {
        JIPipeGraph graph = template.getGraph().duplicate();
        CreateNewNodesByTemplateDatabaseEntry.rekeyTemplateGraphToCompartment(graph, target);
        return graph;
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

        // Required by JIPipeGraph.duplicate() (the addToGraph path) — the
        // production copy constructor. The base copy constructor carries
        // name, slot configuration, and the node UI location metadata (the
        // re-key logic under test needs the locations to survive the
        // duplicate()).
        public StubTemplateNode(StubTemplateNode other) {
            super(other);
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
