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

package org.hkijena.jipipe.api.nodes.database.relations;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseBuilderRun;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration gate for the relations generalization (spec §10): mines the
 * workflow relations of a <em>real</em> node template graph through the full
 * {@link JIPipeNodeDatabase#relations(JIPipeNodeDatabaseEntry, int)} API and
 * asserts the Similar / Previous / Next model.
 * <p>
 * The template is a 2-node subgraph of the production {@code Thresholding}
 * function. Both nodes share the same function and the ImageJ 1 framework, so
 * they are natural "similar" candidates of one another — which is exactly what
 * makes the exclusion assertion meaningful (Review Focus #3 — the three lists
 * stay disjoint):
 * <pre>
 *   nuclei → hysteresis      (the mined A→B edge)
 * </pre>
 * <ul>
 * <li>{@code relations(nuclei).next()} contains hysteresis — the template edge
 * is mined into the <em>next</em> direction;</li>
 * <li>{@code relations(hysteresis).previous()} contains nuclei — the template
 * edge is mined into the <em>previous</em> direction (the direction the legacy
 * {@code followUps} API could not express);</li>
 * <li>{@code relations(nuclei).similar()} is non-empty (the same-function /
 * same-framework tiers still produce suggestions — there are many other
 * Thresholding nodes in the registry) but does <b>not</b> contain hysteresis
 * (a direct next-neighbor is excluded so the three lists stay distinct), and
 * does not contain nuclei itself (self-exclusion preserved).</li>
 * </ul>
 * <p>
 * The template is added to a <em>project</em> (not the global store) so it is
 * mined (all scopes are mined, spec §10.2) without writing to the user's
 * {@code node-templates/} directory or leaking into other tests. The node
 * database is scoped to that project and rebuilt <em>synchronously</em> on the
 * test thread (the async {@code SwingWorker} builder is bypassed via a
 * {@code rebuildImmediately()} override — its {@code done()} callback needs an
 * EDT that a headless JUnit JVM does not reliably provide). The real
 * {@link JIPipeNodeDatabaseBuilderRun} runs in full, so the entries come from
 * the real registry and the relation index is rebuilt from the real family
 * steps + the project template — exercising the production wiring end to end.
 * <p>
 * Mirrors {@link org.hkijena.jipipe.api.nodes.taxonomy.RegistryDumperTest}'s
 * bootstrap: this module's registered ImageJ legacy-patcher
 * {@code LauncherSessionListener} preinits the patcher before any test class
 * loads, so {@code JIPipe.ensureInstance()} is safe; no new listeners or
 * {@code --add-opens} are needed.
 */
class TemplateRelationSourceTest {

    /** "A" — the template edge source (a Thresholding node). */
    private static final String NODE_A = "ij3d-segmentation-nuclei-3d";
    /** "B" — the template edge target (a Thresholding node, same function as A). */
    private static final String NODE_B = "ij3d-segmentation-hysteresis";

    /** A generous cap so the similar-list assertions are not affected by the
     * per-list cap (the registry carries many Thresholding-function nodes). */
    private static final int MAX = 1000;

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();

        // The template nodes must exist in the registry (the ij-3d plugin is a
        // bundled launcher dependency) — fail fast with a clear message.
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        for (String id : new String[]{NODE_A, NODE_B}) {
            assertTrue(nodes.hasNodeInfoWithId(id),
                "expected node id to be registered in the launcher: " + id);
        }
    }

    @Test
    void templateGraphMiningPopulatesNextPreviousAndExcludesNeighborsFromSimilar() {
        JIPipeProject project = new JIPipeProject();

        // Build a real 2-node template graph: A -> B (a data-type-compatible
        // edge; both are ImagePlusGreyscaleData, so JIPipeGraph.connect
        // accepts it).
        JIPipeNodeTemplate template = buildTemplate();
        // Project scope: mined (all scopes are mined) but not persisted to the
        // user's global node-templates directory.
        assertTrue(JIPipe.getNodeTemplates().addToProject(template, project),
            "the template must be added to the project");

        // A database scoped to the project (so its TemplateRelationSource mines
        // the project templates). Rebuilt synchronously — see the class
        // Javadoc — so by the time the constructor returns the entries are set
        // and the relation index (family steps + this template) is current.
        JIPipeNodeDatabase database = new TestNodeDatabase(project);

        JIPipeNodeDatabaseEntry entryA = entryByOriginNodeId(database, NODE_A);
        JIPipeNodeDatabaseEntry entryB = entryByOriginNodeId(database, NODE_B);
        assertNotNull(entryA, "database must hold an entry for " + NODE_A);
        assertNotNull(entryB, "database must hold an entry for " + NODE_B);

        NodeRelations relationsA = database.relations(entryA, MAX);

        // (1) The template edge is mined into the NEXT direction.
        assertTrue(relationsA.next().contains(entryB),
            "next(" + NODE_A + ") must contain " + NODE_B + " (the template edge), got: " + names(relationsA.next()));

        // (2) The template edge is mined into the PREVIOUS direction.
        NodeRelations relationsB = database.relations(entryB, MAX);
        assertTrue(relationsB.previous().contains(entryA),
            "previous(" + NODE_B + ") must contain " + NODE_A + " (the template edge), got: " + names(relationsB.previous()));

        // (3) similar() is populated by the same-function/framework tiers...
        assertFalse(relationsA.similar().isEmpty(),
            "similar(" + NODE_A + ") must be non-empty (other Thresholding nodes exist)");

        // ...but the direct next-neighbor (B) is EXCLUDED from similar so the
        // three lists stay disjoint, and the entry is not similar to itself.
        assertFalse(relationsA.similar().contains(entryB),
            "similar(" + NODE_A + ") must NOT contain the next-neighbor " + NODE_B + "; got: " + names(relationsA.similar()));
        assertFalse(relationsA.similar().contains(entryA),
            "similar(" + NODE_A + ") must NOT contain itself; got: " + names(relationsA.similar()));
    }

    /**
     * A template whose {@link JIPipeNodeTemplate#getGraph()} throws must be
     * <em>skipped with a warning</em>, not abort the whole relation-index
     * build — and a valid template alongside it must still be mined. This
     * pins the per-template {@code catch (Throwable)} guard in
     * {@link TemplateRelationSource}: without it, the corrupted template's
     * exception would propagate out of the index rebuild on every
     * {@code relations()} call, violating the spec's "never fail" intent
     * (the database builder already tolerates per-template corruption the
     * same way — {@code catch (Throwable ignored)} around entry
     * construction).
     * <p>
     * A throwing {@code getGraph()} is used (rather than merely invalid
     * {@code data} JSON, which {@code getGraph()} itself swallows and turns
     * into a {@code null} graph) so that the test exercises the guard
     * specifically, not just the null-graph early return.
     */
    @Test
    void corruptedTemplateIsSkippedWithoutAbortingTheOthers() {
        JIPipeProject project = new JIPipeProject();

        // A valid 2-node template (A -> B) and a corrupted one whose
        // getGraph() throws. Both in the project scope.
        JIPipeNodeTemplate valid = buildTemplate();
        JIPipeNodeTemplate corrupted = new JIPipeNodeTemplate() {
            @Override
            public JIPipeGraph getGraph() {
                throw new IllegalStateException("simulated corrupted template data");
            }
        };
        corrupted.setName("Corrupted relation test template");
        assertTrue(JIPipe.getNodeTemplates().addToProject(valid, project));
        assertTrue(JIPipe.getNodeTemplates().addToProject(corrupted, project));

        // The synchronous rebuild runs the real builder + relation-index
        // mining on this thread; it must NOT throw despite the corrupted
        // template.
        JIPipeNodeDatabase database = new TestNodeDatabase(project);

        JIPipeNodeDatabaseEntry entryA = entryByOriginNodeId(database, NODE_A);
        JIPipeNodeDatabaseEntry entryB = entryByOriginNodeId(database, NODE_B);
        assertNotNull(entryA, "database must hold an entry for " + NODE_A);
        assertNotNull(entryB, "database must hold an entry for " + NODE_B);

        // The valid template's edge is still mined alongside the corrupted one
        // (and relations() does not throw).
        NodeRelations relationsA = database.relations(entryA, MAX);
        assertTrue(relationsA.next().contains(entryB),
            "the valid template's edge must still be mined alongside a corrupted template; got: "
                + names(relationsA.next()));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Builds a real {@link JIPipeNodeTemplate} whose graph is the 2-node
     * subgraph {@code A → B}. The A→B edge is data-type compatible (both
     * {@code ImagePlusGreyscaleData}), so {@code JIPipeGraph.connect} accepts
     * it and the edge survives the template's serialize/deserialize round-trip.
     */
    private JIPipeNodeTemplate buildTemplate() {
        JIPipeGraphNode nodeA = JIPipe.getNodes().getInfoById(NODE_A).newInstance();
        JIPipeGraphNode nodeB = JIPipe.getNodes().getInfoById(NODE_B).newInstance();

        JIPipeGraph graph = new JIPipeGraph();
        graph.insertNode(nodeA);
        graph.insertNode(nodeB);
        // A.Output (ImagePlusGreyscaleData) -> B.Input (ImagePlusGreyscaleData)
        graph.connect(nodeA.getOutputSlot("Output"), nodeB.getInputSlot("Input"));

        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName("Relation test template");
        template.setGraph(graph);
        return template;
    }

    private static JIPipeNodeDatabaseEntry entryByOriginNodeId(JIPipeNodeDatabase database, String nodeId) {
        for (JIPipeNodeDatabaseEntry entry : database.getEntries()) {
            if (nodeId.equals(entry.getOriginNodeId())) {
                return entry;
            }
        }
        return null;
    }

    private static String names(java.util.List<JIPipeNodeDatabaseEntry> entries) {
        return entries.stream().map(JIPipeNodeDatabaseEntry::getName).toList().toString();
    }

    /**
     * A node database that runs its {@link JIPipeNodeDatabaseBuilderRun}
     * <em>synchronously on the calling thread</em> instead of enqueueing it on
     * the async {@code SwingWorker} executor. The builder run sets the entries
     * (invalidating the relation index) and then calls
     * {@link JIPipeNodeDatabase#rebuildRelationIndex()}, which mines the family
     * steps + template graphs on this thread — so after the constructor
     * returns, the entries and the relation index are both current, with no
     * dependence on an EDT for the worker's {@code done()} callback.
     */
    private static class TestNodeDatabase extends JIPipeNodeDatabase {
        private TestNodeDatabase(JIPipeProject project) {
            super(project);
        }

        @Override
        public void rebuildImmediately() {
            new JIPipeNodeDatabaseBuilderRun(this).run();
        }
    }
}
