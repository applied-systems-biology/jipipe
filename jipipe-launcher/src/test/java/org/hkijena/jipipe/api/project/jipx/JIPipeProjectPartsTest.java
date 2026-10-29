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

package org.hkijena.jipipe.api.project.jipx;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JIPipeProjectParts}, the JIPX part (de)serialization utility.
 * <p>
 * Fixture: {@code /projects/5.3.0-taxonomy-fixtures/tables.jipipe}
 * (1 node, {@code table-add-columns-generate}).
 * <p>
 * <b>Scope: jipipe-launcher.</b> Same reason as
 * {@code FixtureProjectsLoadTest}: only the launcher has all plugin modules
 * on its classpath, so the fixture node actually resolves to a node type.
 */
class JIPipeProjectPartsTest {

    private static final String FIXTURE = "/projects/5.3.0-taxonomy-fixtures/tables.jipipe";

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe.
        JIPipe.ensureInstance();
    }

    private static Map<UUID, String> nodeIdMapping(JIPipeGraph graph) {
        Map<UUID, String> mapping = new HashMap<>();
        for (JIPipeGraphNode n : graph.getGraphNodes()) {
            mapping.put(n.getUUIDInParentGraph(), n.getInfo().getId());
        }
        return mapping;
    }

    /**
     * Reorders a dependencies array into a canonical, order-stable form:
     * elements sorted by their dependency {@code id}, keeping the full
     * element content for equality.
     */
    private static JsonNode sortByDependencyId(JsonNode dependenciesArray) {
        com.fasterxml.jackson.databind.node.ArrayNode sorted =
                JsonUtils.getObjectMapper().createArrayNode();
        List<JsonNode> elements = new ArrayList<>();
        dependenciesArray.forEach(elements::add);
        elements.sort(Comparator.comparing(e -> e.path("id").asText()));
        for (JsonNode element : elements) {
            sorted.add(element);
        }
        return sorted;
    }

    @Test
    void slicePartsAreExactMonolithFieldsAndRoundTrip() throws IOException, URISyntaxException {
        UnspecifiedValidationReportContext ctx = new UnspecifiedValidationReportContext();
        JIPipeValidationReport report = new JIPipeValidationReport();
        Path path = Path.of(JIPipeProjectPartsTest.class.getResource(FIXTURE).toURI());
        JIPipeProject load = JIPipeProject.loadProject(path, ctx, report,
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);

        // Non-vacuity: the loaded graph must have 1+ nodes
        // (nodesFromJson silently skips unknown node ids, so a 0-node
        // load would make the mapping comparison vacuous).
        Set<UUID> nodeUuids = load.getGraph().getGraphNodeUUIDs();
        assertTrue(!nodeUuids.isEmpty(),
                "Fixture loaded 0 nodes - table-add-columns-generate not resolved; test would be vacuous");

        Map<String, JsonNode> parts = JIPipeProjectParts.sliceParts(load);
        JsonNode monolith = JsonUtils.getObjectMapper()
                .readValue(JsonUtils.getObjectMapper().writeValueAsString(load), JsonNode.class);

        // (1) Fidelity contract: for every part key present in the monolith,
        // the slice is the monolith's corresponding top-level field itself.
        for (String key : JIPipeProjectParts.PART_KEYS) {
            if (monolith.has(key)) {
                assertTrue(parts.containsKey(key), "Part '" + key + "' missing from slice");
                if ("dependencies".equals(key)) {
                    // The monolith's dependencies array is serialized from a
                    // HashSet of identity-hashed JIPipeDependency instances
                    // (getSimplifiedMinimalDependencies), so its element order
                    // is non-deterministic across serializations - an existing
                    // production quirk. Compare order-insensitively (by
                    // dependency id), everything else byte-exact.
                    assertEquals(sortByDependencyId(monolith.get("dependencies")),
                            sortByDependencyId(parts.get("dependencies")),
                            "Part 'dependencies' is not the exact monolith field (order-insensitive)");
                } else {
                    assertEquals(monolith.get(key), parts.get(key),
                            "Part '" + key + "' is not the exact monolith field");
                }
            }
        }

        // (2) Never-slice keys are absent from the slice.
        for (String forbidden : new String[]{"external-environments", "jipipe:project-type", "jipipe:project-jipipe-version"}) {
            assertFalse(parts.containsKey(forbidden), "Part '" + forbidden + "' must never be sliced");
        }

        // (3) Round-trip: assemble the parts into the tree JIPipeProject.fromJson
        // consumes, load it into a fresh project, and require the identical
        // uuid → node-info-id mapping.
        JsonNode tree = JIPipeProjectParts.assembleParts(parts, JIPipe.getJIPipeVersion());
        JIPipeProject p2 = new JIPipeProject();
        p2.fromJson(tree, ctx, new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        assertEquals(nodeIdMapping(load.getGraph()), nodeIdMapping(p2.getGraph()),
                "Assembled parts do not reload to the same uuid → node-info-id mapping");
        assertNotNull(p2.getMetadata(), "Reloaded project has no metadata");
    }
}
