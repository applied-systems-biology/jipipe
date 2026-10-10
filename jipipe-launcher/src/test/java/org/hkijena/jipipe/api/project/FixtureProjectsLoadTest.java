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

package org.hkijena.jipipe.api.project;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration fixtures for the node taxonomy redesign (phase 1b, task 23).
 * <p>
 * These hand-written, minimal 5.3.0-format project files prove the
 * <b>"projects unaffected"</b> guarantee: a project saved by JIPipe 5.3.0 —
 * which stores only node <i>ids</i> and no taxonomy information — loads
 * unchanged after the taxonomy conversion, every loaded node resolves to a
 * node type with a non-{@code null} primary taxonomy function, and the
 * project round-trips (save → reload → same nodes).
 * <p>
 * <b>Scope: jipipe-launcher.</b> The fixtures reference node ids from the
 * ij-algorithms, trackmate, and tables plugins. This test must live in the
 * launcher (the only module with all plugin modules on its classpath), the
 * same reason as the full taxonomy gate
 * ({@code TaxonomyFullValidationTest}): on jipipe-core's classpath
 * {@code JIPipeGraph.nodesFromJson} would silently skip every fixture node
 * (unknown id → "Unable to find node type") and a 0-node load would pass
 * the count assertion vacuously.
 * <p>
 * <b>The count assertion is the non-vacuity guard.</b>
 * {@code nodesFromJson} skips unknown node ids without throwing, so if a
 * fixture id stops existing after a refactor, the loaded node count would
 * drop below the fixture's node count and this test fails.
 */
class FixtureProjectsLoadTest {

    private static final String FIXTURE_DIR = "/projects/5.3.0-taxonomy-fixtures/";

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    @TempDir
    Path tmp;

    @Test
    void basicImagesProjectLoads() throws IOException, URISyntaxException {
        checkFixture("basic-images.jipipe", 1,
            new String[] {"ij1-blur-gaussian2d"});
    }

    @Test
    void trackmateSetupProjectLoads() throws IOException, URISyntaxException {
        checkFixture("trackmate-setup.jipipe", 2,
            new String[] {"trackmate-detector", "trackmate-tracking"});
    }

    @Test
    void tablesProjectLoads() throws IOException, URISyntaxException {
        checkFixture("tables.jipipe", 1,
            new String[] {"table-add-columns-generate"});
    }

    /**
     * Loads the given 5.3.0 fixture, asserts that it loads cleanly, that the
     * expected nodes are present (non-vacuous count + exact node ids), that
     * every node has a taxonomy function, and that the project round-trips
     * with an identical uuid → node-info-id mapping.
     *
     * @param fileName          fixture file name inside {@link #FIXTURE_DIR}
     * @param expectedNodeCount the number of pipeline nodes in the fixture
     * @param expectedInfoIds   the expected node ids, in fixture order
     */
    private void checkFixture(String fileName, int expectedNodeCount, String[] expectedInfoIds) throws IOException, URISyntaxException {
        Path path = Path.of(FixtureProjectsLoadTest.class.getResource(FIXTURE_DIR + fileName).toURI());

        // (1) The 4-arg loadProject overload used by the desktop project
        // window: must not throw.
        JIPipeValidationReport report = new JIPipeValidationReport();
        JIPipeProject p = JIPipeProject.loadProject(path,
            new UnspecifiedValidationReportContext(), report, JIPipeProgressInfo.STDOUT);

        List<JIPipeGraphNode> nodes = p.getGraph().traverse();

        // (2) CRITICAL (non-vacuous): the loaded node count equals the
        // fixture's node count. nodesFromJson silently skips unknown node
        // ids, so a 0-node (or fewer) load MUST fail.
        assertEquals(expectedNodeCount, nodes.size(),
            "Expected " + expectedNodeCount + " loaded nodes for " + fileName
                + " but got " + nodes.size()
                + " — nodes silently skipped (unknown id or missing compartment?)");

        // (2b) The exact expected node ids are present.
        Map<String, Integer> infoIdCounts = new HashMap<>();
        for (JIPipeGraphNode n : nodes) {
            infoIdCounts.merge(n.getInfo().getId(), 1, Integer::sum);
        }
        for (String expectedId : expectedInfoIds) {
            assertEquals(1, infoIdCounts.getOrDefault(expectedId, 0),
                "Node id '" + expectedId + "' missing or duplicated in loaded graph of " + fileName
                    + "; present ids: " + infoIdCounts);
        }

        // (3) The "projects unaffected" guarantee: every loaded node
        // resolves to a node type with a non-null primary taxonomy function.
        for (JIPipeGraphNode n : nodes) {
            assertNotNull(n.getInfo().getPrimaryFunction(),
                "Loaded node " + n.getUUIDInParentGraph() + " (" + n.getInfo().getId()
                    + ") has no taxonomy function");
        }

        // (4) The load reported no validation issues. (Node-level load
        // errors are not forwarded to this report by the loader — the
        // count assertion above is the real guard — but compartment and
        // edge issues are, so a clean load must be issue-free.)
        assertTrue(report.isEmpty(), "Load issues: " + report);

        // (5) Round-trip: save → reload → identical uuid → node-info-id
        // mapping (same nodes, same identities, no lost or added nodes).
        Path out = tmp.resolve(fileName + "-rt.jipipe");
        p.saveProject(out, false);
        JIPipeProject p2 = JIPipeProject.loadProject(out,
            new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), JIPipeProgressInfo.STDOUT);
        assertEquals(nodeIdMapping(p.getGraph()), nodeIdMapping(p2.getGraph()),
            "Round-tripped project " + fileName + " does not preserve the uuid → node-info-id mapping");
    }

    private static Map<UUID, String> nodeIdMapping(JIPipeGraph graph) {
        Map<UUID, String> mapping = new HashMap<>();
        for (JIPipeGraphNode n : graph.getGraphNodes()) {
            mapping.put(n.getUUIDInParentGraph(), n.getInfo().getId());
        }
        return mapping;
    }
}
