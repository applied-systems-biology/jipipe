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

package org.hkijena.jipipe.api.nodes.taxonomy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Template data deserialization check for the taxonomy research study
 * (work item 1324, follow-up research plan task 8, step 3).
 * <p>
 * Verifies that the minimal {@code JIPipeGraph}-shaped {@code data} object
 * generated for the pattern library (task 8,
 * {@code utils/taxonomy-research/patterns/templates/<pattern-id>.json})
 * actually deserializes through the EXACT path a node template uses:
 * {@link JsonUtils#readFromString(String, Class)} on
 * {@link JIPipeGraph} — {@code JIPipeNodeTemplate.getGraph()} calls exactly
 * this. The candidate is the highest-support curated pattern
 * ({@code fan-in-ij1-roi-to-rgb-core-io-1-other-1}, support 71; 3 nodes, 1
 * edge in its example subgraph).
 * <p>
 * Guards:
 * <ol>
 *   <li>deserialization does not throw (an NPE on a missing required field
 *       would surface here — in particular
 *       {@code jipipe:graph-compartment} is read via
 *       {@code .get(...).asText()} in {@code JIPipeGraph.nodesFromJson} and
 *       is REQUIRED);</li>
 *   <li>{@code getNodeCount()} equals the template's node count — a missing
 *       {@code nodes} key or unknown node ids make {@code fromJson} return an
 *       EMPTY graph silently (this assertion is the guard);</li>
 *   <li>at least one connection loads — a slot-name mismatch is a SOFT skip
 *       (edge dropped, not a crash), so this guards soft-skipped edges;</li>
 *   <li>every loaded node's id is a registry id AND the loaded id set equals
 *       the template's id set (guards individually soft-skipped nodes).</li>
 * </ol>
 * The minimal node object per template is exactly
 * {@code {"jipipe:node-info-id": ..., "jipipe:graph-compartment": ...,
 * "jipipe:node:name": ...}} — no {@code parameters} object; the node's own
 * {@code fromJson} guards every optional field and parameter deserialization
 * reads {@code node.path(key)}, so missing parameters are fine. If this test
 * ever fails, the contract above (task 8 report section 6 method note) is
 * the place to fix.
 * <p>
 * Mirrors {@link RegistryDumperTest}'s bootstrap: this module's registered
 * ImageJ legacy-patcher {@code LauncherSessionListener} preinits the patcher
 * before any test class loads, so {@code JIPipe.ensureInstance()} here is
 * safe; no new listeners or {@code --add-opens} are needed.
 */
class TemplateDataDeserializationTest {

    /**
     * Highest-support curated pattern (R8 curation): 3 nodes, 1 edge.
     */
    static final String CANDIDATE = "fan-in-ij1-roi-to-rgb-core-io-1-other-1";

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    @Test
    void templateDataDeserializesIntoALoadableGraph() throws Exception {
        // The test runs with working dir jipipe-launcher/; resolve to the
        // repo-relative path (same approach as RegistryDumperTest).
        Path template = Paths.get("..", "utils", "taxonomy-research",
                        "patterns", "templates", CANDIDATE + ".json")
                .toAbsolutePath().normalize();
        String data = Files.readString(template);

        JsonNode root = new ObjectMapper().readTree(data);
        int expectedNodes = root.get("nodes").size();
        int expectedEdges = root.get("edges").size();
        Set<String> expectedIds = new HashSet<>();
        for (Map.Entry<String, JsonNode> entry : root.get("nodes").properties()) {
            expectedIds.add(entry.getValue().get("jipipe:node-info-id").asText());
        }

        // (i) no exception — the exact JIPipeNodeTemplate.getGraph() path
        JIPipeGraph graph = JsonUtils.readFromString(data, JIPipeGraph.class);
        assertNotNull(graph, "deserialization must not return null");

        // (ii) silent-empty guard
        assertEquals(expectedNodes, graph.getNodeCount(),
                "every template node must load (an empty/short graph means "
                        + "fromJson dropped nodes silently)");

        // (iii) soft-skip guard for edges
        int connections = graph.getSlotEdges().size();
        assertTrue(connections >= 1,
                "at least one connection must load (slot-name mismatches are "
                        + "soft-skipped, so 0 means every edge was dropped)");

        // (iv) every loaded node id is a registry id; loaded set == template set
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        Set<String> loadedIds = new HashSet<>();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            String id = node.getInfo().getId();
            assertTrue(nodes.hasNodeInfoWithId(id),
                    "loaded node id must be a registry id: " + id);
            loadedIds.add(id);
        }
        assertEquals(expectedIds, loadedIds,
                "loaded node ids must be exactly the template's node ids");

        // Record the actual counts for the task report.
        System.out.println("TEMPLATE-CHECK: candidate=" + CANDIDATE
                + " nodes=" + graph.getNodeCount() + "/" + expectedNodes
                + " connections=" + connections + "/" + expectedEdges
                + " graph=[" + graph + "]");
    }
}
