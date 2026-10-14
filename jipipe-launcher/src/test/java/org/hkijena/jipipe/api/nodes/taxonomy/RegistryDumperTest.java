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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registry dumper for the taxonomy research study (work item 1324, follow-up
 * research plan task 2).
 * <p>
 * Dumps the fully-booted node registry to
 * {@code utils/taxonomy-research/registry.json} — the registry-side twin of
 * the real-pipeline {@code corpus.json} produced by task 1. The later
 * analysis tasks consume the two files only; the emitted shape is a locked
 * interface:
 *
 * <pre>{@code
 * {
 *   "nodes": [
 *     {
 *       "id": "trackmate-create-spot-detector-dog-detector",
 *       "name": "DoG detector",
 *       "framework": "jipipe:framework:trackmate",
 *       "functions": ["…primary…", "…secondary…"],
 *       "tags": ["…"],
 *       "familySteps": [{"family": "jipipe:family:trackmate", "label": "Detect spots"}],
 *       "followUps": ["trackmate-tracking"],
 *       "inputs":  [{"class": "…FQN…", "dataType": "…data-type id or null…"}],
 *       "outputs": [{"class": "…FQN…", "dataType": "…data-type id or null…"}]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>Inputs/outputs carry the slot's data <em>class</em> FQN plus its
 * registered data-type id (the corpus side stores raw on-disk
 * {@code slot-data-type} tokens; the join corpus→registry happens on
 * {@code dataType}). An unregistered data class yields
 * {@code dataType: null}.
 * <p>
 * <b>Determinism.</b> The output is byte-stable across runs: nodes sorted
 * by id, and every set/array taken from the registry sorted lexicographically
 * ({@code functions} keeps the primary function first, its secondaries are
 * then sorted; {@code familySteps} sorted by family id, then label;
 * {@code followUps} sorted; input/output slot entries deduplicated by class
 * FQN and sorted by FQN). Written compact via the default Jackson mapper,
 * matching {@code corpus.json}.
 * <p>
 * Mirrors {@link TaxonomyFullValidationTest}'s bootstrap: this module's
 * registered ImageJ legacy-patcher {@code LauncherSessionListener} preinits
 * the patcher before any test class loads, so {@code JIPipe.ensureInstance()}
 * here is safe.
 */
class RegistryDumperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    void dumpTheRegistry() throws IOException {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        Map<String, JIPipeNodeInfo> nodeInfos = nodes.getRegisteredNodeInfos();
        FamilyAssembler assembler = nodes.getFamilyAssembler();
        Map<String, JIPipeNodeTaxonomyFamily> families = nodes.getFamilies();
        Set<String> familyIds = new HashSet<>(families.keySet());

        // node id -> its (familyId, label) steps, collected from every family.
        Map<String, List<String[]>> stepsByNode = new HashMap<>();
        // K: total number of resolved after-ids across all steps of all
        // families = the number of declared workflow edges in the family
        // graphs.
        long familyGraphEdges = 0;
        for (JIPipeNodeTaxonomyFamily family : families.values()) {
            for (JIPipeNodeTaxonomyFamilyStep step : assembler.steps(family)) {
                stepsByNode
                    .computeIfAbsent(step.nodeId(), k -> new ArrayList<>())
                    .add(new String[]{step.familyId(), step.label()});
                familyGraphEdges += step.afterIds(assembler).size();
            }
        }

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode nodesJson = root.putArray("nodes");
        List<String> nodeIds = new ArrayList<>(nodeInfos.keySet());
        Collections.sort(nodeIds);
        for (String nodeId : nodeIds) {
            JIPipeNodeInfo info = nodeInfos.get(nodeId);
            ObjectNode nodeJson = nodesJson.addObject();
            nodeJson.put("id", nodeId);
            nodeJson.put("name", info.getName());

            JIPipeNodeTaxonomyFramework framework = info.getFramework();
            if (framework == null) {
                nodeJson.putNull("framework");
            } else {
                nodeJson.put("framework", framework.getId());
            }

            // functions: primary first, secondaries sorted lexicographically
            ArrayNode functions = nodeJson.putArray("functions");
            JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
            if (primary != null) {
                functions.add(primary.getId());
            }
            List<String> secondaryIds = info.getSecondaryFunctions().stream()
                .map(JIPipeNodeTaxonomyFunction::getId)
                .sorted()
                .collect(Collectors.toList());
            secondaryIds.forEach(functions::add);

            // tags: merged tags, falling back to the node-level tags
            Set<String> tags = info.getMergedTags();
            if (tags == null || tags.isEmpty()) {
                tags = info.getTaxonomyTags();
            }
            ArrayNode tagsJson = nodeJson.putArray("tags");
            tags.stream().sorted().forEach(tagsJson::add);

            // familySteps sorted by (family id, label)
            ArrayNode familySteps = nodeJson.putArray("familySteps");
            List<String[]> mine = new ArrayList<>(stepsByNode.getOrDefault(nodeId, List.of()));
            mine.sort(Comparator.comparing((String[] s) -> s[0]).thenComparing(s -> s[1]));
            for (String[] step : mine) {
                ObjectNode stepJson = familySteps.addObject();
                stepJson.put("family", step[0]);
                stepJson.put("label", step[1]);
            }

            // followUps sorted lexicographically
            ArrayNode followUps = nodeJson.putArray("followUps");
            assembler.followUps(nodeId).stream().sorted().forEach(followUps::add);

            nodeJson.set("inputs",
                slotEntries(info.getInputSlots().stream().map(AddJIPipeInputSlot::value).collect(Collectors.toList())));
            nodeJson.set("outputs",
                slotEntries(info.getOutputSlots().stream().map(AddJIPipeOutputSlot::value).collect(Collectors.toList())));
        }

        // The test runs with working dir jipipe-launcher/; resolve to the
        // repo root and write (creating the directory if missing).
        Path out = Paths.get("..", "utils", "taxonomy-research", "registry.json").toAbsolutePath().normalize();
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        MAPPER.writeValue(out.toFile(), root);
        System.out.println("RegistryDumperTest: registry: " + nodeIds.size() + " nodes, "
            + families.size() + " families, " + familyGraphEdges + " family-graph edges -> " + out);

        // Assertions (d)-(h) against the file as written on disk.
        JsonNode parsed = MAPPER.readTree(out.toFile());
        assertNotNull(parsed, "registry.json is missing its root object");
        ArrayNode parsedNodes = (ArrayNode) parsed.get("nodes");
        assertNotNull(parsedNodes, "registry.json is missing the 'nodes' array");
        Map<String, JsonNode> parsedById = new HashMap<>();
        for (JsonNode n : parsedNodes) {
            parsedById.put(n.get("id").asText(), n);
        }

        // (d) the dump contains the DoG spot-detector node. The brief writes the
        // id as 'trackmate-create-spot-detector-dog', but the registry generates
        // it as "trackmate-create-spot-detector-" + factory.getKey() and DoG's
        // key is "dog-detector" ⇒ the real id has the trailing "-detector".
        // Adapted to the real id (the code wins over the brief).
        String dogDetectorId = "trackmate-create-spot-detector-dog-detector";
        assertTrue(parsedById.containsKey(dogDetectorId),
            "registry.json is missing node id '" + dogDetectorId + "'");

        // (e) that node's followUps contain trackmate-tracking (the round-9
        // fix is in place)
        assertFollowUpsContain(parsedById.get(dogDetectorId), "trackmate-tracking");

        // (f) trackmate-detector's followUps contain trackmate-tracking
        assertFollowUpsContain(parsedById.get("trackmate-detector"), "trackmate-tracking");

        // (g) every familySteps[].family value is present in the assembler's
        // family set (the registered family ids)
        for (JsonNode n : parsedNodes) {
            for (JsonNode step : n.get("familySteps")) {
                String family = step.get("family").asText();
                assertTrue(familyIds.contains(family),
                    "Node '" + n.get("id").asText() + "' references unknown family '" + family + "'");
            }
        }

        // (h) total node count matches the live registry
        assertEquals(nodeInfos.size(), parsedNodes.size(),
            "registry.json node count (" + parsedNodes.size()
                + ") does not match the live registry size (" + nodeInfos.size() + ")");
    }

    private static void assertFollowUpsContain(JsonNode node, String expectedFollowUp) {
        assertNotNull(node, "Node not found in the dump: " + expectedFollowUp);
        ArrayNode followUps = (ArrayNode) node.get("followUps");
        assertNotNull(followUps, "Node '" + node.get("id").asText() + "' has no followUps array");
        for (JsonNode followUp : followUps) {
            if (expectedFollowUp.equals(followUp.asText())) {
                return;
            }
        }
        fail("Node '" + node.get("id").asText()
            + "' does not follow up with '" + expectedFollowUp + "': " + followUps);
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    /**
     * Emits the locked input/output slot entries: one {@code {"class",
     * "dataType"}} object per distinct slot data class, deduplicated and
     * sorted by class FQN. {@code dataType} is the registered data-type id
     * (null if the class is not registered as a data type).
     */
    private static ArrayNode slotEntries(List<Class<? extends JIPipeData>> classes) {
        Map<String, Class<? extends JIPipeData>> byFqn = new TreeMap<>();
        for (Class<? extends JIPipeData> dataClass : classes) {
            byFqn.putIfAbsent(dataClass.getName(), dataClass);
        }
        ArrayNode array = MAPPER.createArrayNode();
        for (Class<? extends JIPipeData> dataClass : byFqn.values()) {
            ObjectNode entry = array.addObject();
            entry.put("class", dataClass.getName());
            String dataType = JIPipe.getDataTypes().getIdOf(dataClass);
            if (dataType == null) {
                entry.putNull("dataType");
            } else {
                entry.put("dataType", dataType);
            }
        }
        return array;
    }
}
