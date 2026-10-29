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
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
 *   ],
 *   "functions": [
 *     {"id": "jipipe:function:…", "name": "…", "parent": "jipipe:function:…"},
 *     {"id": "jipipe:function:images", "name": "Images", "parent": null}
 *   ],
 *   "families": [
 *     {"id": "jipipe:family:trackmate",
 *      "steps": [
 *        {"nodeId": "trackmate-detector", "label": "Detect spots", "after": []},
 *        {"nodeId": "trackmate-tracking", "label": "Track spots",
 *         "after": ["trackmate-create-spot-detector-dog-detector", …, "trackmate-detector"]}
 *      ]}
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
 * The top-level {@code functions} array is the taxonomy function tree
 * (added for the workflow-chain / branch analyses): one entry per
 * registered function with {@code parent} = the parent function's id, or
 * {@code null} for top-level branches ({@code getParent() == null}, exactly
 * the {@link FunctionRegistry#topLevel()} definition). It lets a consumer
 * resolve which top-level branch every node's leaf function id lives under
 * without re-deriving the tree.
 * <p>
 * The top-level {@code families} array carries the <em>declared after-edges</em>
 * of each registered family (added for the family-coverage-vs-usage
 * analysis): per family, {@code steps[]} lists every step with its
 * {@code nodeId}, {@code label}, and {@code after} = the step's resolved
 * after <em>node</em> ids — {@link JIPipeNodeTaxonomyFamilyStep#afterIds(FamilyAssembler)}
 * returns step ids ({@code familyId + "#" + nodeId}); the family prefix is
 * stripped because steps are per-family. The resolution itself is the lazy
 * class→node-id mapping only the live assembler can do; a shared instance
 * class behind programmatically generated entries fans out to all of their
 * node ids. The total number of emitted after-ids is
 * exactly the reported family-graph edge count K.
 * <p>
 * <b>Determinism.</b> The output is byte-stable across runs: nodes sorted
 * by id, and every set/array taken from the registry sorted lexicographically
 * ({@code functions} keeps the primary function first, its secondaries are
 * then sorted; {@code familySteps} sorted by family id, then label;
 * {@code followUps} sorted; input/output slot entries deduplicated by class
 * FQN and sorted by FQN; the top-level {@code functions} array sorted by
 * function id; the top-level {@code families} array sorted by family id, its
 * {@code steps} sorted by node id, and each {@code after} array sorted).
 * Written compact via the default Jackson mapper,
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
     * Emits the per-slot entries WITH the runtime slot name: one
     * {@code {"class", "dataType", "name"}} object per slot (in declaration
     * order, no deduplication — the same data class may back several named
     * slots). {@code name} is the slot's declared name (empty string when
     * the annotation has none, which happens for single-slot nodes that
     * derive the name from the class).
     */
    private static ArrayNode slotNameEntries(List<Class<? extends JIPipeData>> classes, List<String> names) {
        ArrayNode array = MAPPER.createArrayNode();
        for (int i = 0; i < classes.size(); i++) {
            ObjectNode entry = array.addObject();
            entry.put("class", classes.get(i).getName());
            String dataType = JIPipe.getDataTypes().getIdOf(classes.get(i));
            if (dataType == null) {
                entry.putNull("dataType");
            } else {
                entry.put("dataType", dataType);
            }
            entry.put("name", names.get(i));
        }
        return array;
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

            // Per-slot entries WITH names (additive, 2026-10-19): the runtime
            // slot name is authoritative for dynamically-registered nodes
            // (cLij2/clijx kernels — no @AddJIPipe*Slot source annotation to
            // parse), and a code-classpath dump cannot see those nodes at all.
            nodeJson.set("inputSlots",
                    slotNameEntries(info.getInputSlots().stream().map(AddJIPipeInputSlot::value).collect(Collectors.toList()),
                            info.getInputSlots().stream().map(AddJIPipeInputSlot::name).collect(Collectors.toList())));
            nodeJson.set("outputSlots",
                    slotNameEntries(info.getOutputSlots().stream().map(AddJIPipeOutputSlot::value).collect(Collectors.toList()),
                            info.getOutputSlots().stream().map(AddJIPipeOutputSlot::name).collect(Collectors.toList())));
        }

        // Top-level function tree block (R6): one entry per registered
        // function, sorted by id. parent = the parent function's id, null for
        // top-level branches (getParent() == null, the FunctionRegistry
        // .topLevel() definition). all() returns a fresh set per call, but
        // we copy it anyway before sorting (the registry's sets may be
        // immutable; in-place sort must never touch registry-owned state).
        FunctionRegistry functionRegistry = nodes.getFunctions();
        List<JIPipeNodeTaxonomyFunction> functions = new ArrayList<>(functionRegistry.all());
        functions.sort(Comparator.comparing(JIPipeNodeTaxonomyFunction::getId));
        ArrayNode functionsJson = root.putArray("functions");
        for (JIPipeNodeTaxonomyFunction function : functions) {
            ObjectNode functionJson = functionsJson.addObject();
            functionJson.put("id", function.getId());
            functionJson.put("name", function.getName());
            JIPipeNodeTaxonomyFunction parent = function.getParent();
            if (parent == null) {
                functionJson.putNull("parent");
            } else {
                functionJson.put("parent", parent.getId());
            }
        }

        // Top-level family block (R7): the declared after-edges of every
        // registered family. id = the family id; steps = every step the
        // assembler knows for the family (sorted by nodeId), each carrying
        // nodeId, label, and after = the step's resolved after NODE ids
        // (step.afterIds(assembler) returns step ids "familyId#nodeId" —
        // the prefix is stripped, injective within a family; the lazy
        // class→node-id resolution itself is only the live assembler can
        // perform, and a shared instance class behind programmatically
        // generated entries fans out to all of their node ids). after is
        // sorted lexicographically. families sorted by id. Immutable
        // collections are copied before any in-place sort (R6 lesson:
        // never sort registry-owned state).
        long emittedAfterEdges = 0;
        long emittedSteps = 0;
        List<String> familyIdOrder = new ArrayList<>(families.keySet());
        familyIdOrder.sort(Comparator.naturalOrder());
        ArrayNode familiesJson = root.putArray("families");
        for (String familyId : familyIdOrder) {
            JIPipeNodeTaxonomyFamily family = families.get(familyId);
            ObjectNode familyJson = familiesJson.addObject();
            familyJson.put("id", familyId);
            List<JIPipeNodeTaxonomyFamilyStep> familySteps =
                    new ArrayList<>(assembler.steps(family));
            familySteps.sort(Comparator.comparing(JIPipeNodeTaxonomyFamilyStep::nodeId));
            emittedSteps += familySteps.size();
            ArrayNode stepsJson = familyJson.putArray("steps");
            // afterIds() returns STEP ids ("familyId + '#' + nodeId", see
            // JIPipeNodeTaxonomyFamilyStep#afterIds); the locked block shape
            // carries NODE ids, so the family prefix is stripped (injective
            // within a family — no duplicates can arise).
            String stepPrefix = FamilyAssembler.stepId(familyId, "");
            for (JIPipeNodeTaxonomyFamilyStep step : familySteps) {
                ObjectNode stepJson = stepsJson.addObject();
                stepJson.put("nodeId", step.nodeId());
                stepJson.put("label", step.label());
                List<String> after = new ArrayList<>();
                for (String stepId : step.afterIds(assembler)) {
                    after.add(stepId.substring(stepPrefix.length()));
                }
                after.sort(Comparator.naturalOrder());
                emittedAfterEdges += after.size();
                ArrayNode afterJson = stepJson.putArray("after");
                after.forEach(afterJson::add);
            }
        }

        // The test runs with working dir jipipe-launcher/; resolve to the
        // repo root and write (creating the directory if missing).
        Path out = Paths.get("..", "utils", "taxonomy-research", "registry.json").toAbsolutePath().normalize();
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        MAPPER.writeValue(out.toFile(), root);
        System.out.println("RegistryDumperTest: registry: " + nodeIds.size() + " nodes, "
                + functions.size() + " functions, "
                + families.size() + " families (" + emittedSteps + " steps, "
                + emittedAfterEdges + " after-edges), " + familyGraphEdges
                + " family-graph edges -> " + out);

        // Assertions (d)-(h) against the file as written on disk.
        JsonNode parsed = MAPPER.readTree(out.toFile());
        assertNotNull(parsed, "registry.json is missing its root object");
        ArrayNode parsedNodes = (ArrayNode) parsed.get("nodes");
        assertNotNull(parsedNodes, "registry.json is missing the 'nodes' array");
        ArrayNode parsedFunctions = (ArrayNode) parsed.get("functions");
        assertNotNull(parsedFunctions, "registry.json is missing the 'functions' array");
        // (i) per-slot name entries: present on every node, consistent in
        //     size with the registered slots, and at least one node must
        //     carry a non-empty slot name (guards the cLij2 dynamic-slot
        //     provenance the (C') derivation sweep relies on).
        long nodesWithNamedSlots = 0;
        for (JsonNode n : parsedNodes) {
            String nodeId = n.get("id").asText();
            JIPipeNodeInfo info = nodeInfos.get(nodeId);
            assertNotNull(info, "no registered node info for emitted id " + nodeId);
            ArrayNode inSlots = (ArrayNode) n.get("inputSlots");
            ArrayNode outSlots = (ArrayNode) n.get("outputSlots");
            assertNotNull(inSlots, "node " + nodeId + " is missing 'inputSlots'");
            assertNotNull(outSlots, "node " + nodeId + " is missing 'outputSlots'");
            assertEquals(info.getInputSlots().size(), inSlots.size(),
                    "input slot count mismatch for " + nodeId);
            assertEquals(info.getOutputSlots().size(), outSlots.size(),
                    "output slot count mismatch for " + nodeId);
            boolean named = false;
            for (JsonNode s2 : inSlots) {
                assertNotNull(s2.get("name"), "input slot of " + nodeId + " has no name field");
                if (!s2.get("name").asText().isEmpty()) {
                    named = true;
                }
            }
            for (JsonNode s2 : outSlots) {
                assertNotNull(s2.get("name"), "output slot of " + nodeId + " has no name field");
                if (!s2.get("name").asText().isEmpty()) {
                    named = true;
                }
            }
            if (named) {
                nodesWithNamedSlots++;
            }
        }
        assertTrue(nodesWithNamedSlots > 0,
                "no node carries a named slot - the per-slot name entries are dead");

        Map<String, JsonNode> parsedById = new HashMap<>();
        for (JsonNode n : parsedNodes) {
            parsedById.put(n.get("id").asText(), n);
        }
        // function id -> entry, for the new-block self-consistency checks
        Map<String, JsonNode> functionById = new HashMap<>();
        for (JsonNode f : parsedFunctions) {
            functionById.put(f.get("id").asText(), f);
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

        // (i) every node's functions[] entry (primary + secondary ids)
        // resolves to an entry in the top-level functions array
        for (JsonNode n : parsedNodes) {
            for (JsonNode functionId : n.get("functions")) {
                String id = functionId.asText();
                assertTrue(functionById.containsKey(id),
                        "Node '" + n.get("id").asText() + "' references unknown function '" + id + "'");
            }
        }

        // (ii) every non-null parent resolves to another entry in the array
        for (JsonNode f : parsedFunctions) {
            JsonNode parent = f.get("parent");
            if (!parent.isNull()) {
                String parentId = parent.asText();
                assertTrue(functionById.containsKey(parentId),
                        "Function '" + f.get("id").asText() + "' references unknown parent '" + parentId + "'");
            }
        }

        // (iii) at least one top-level entry (parent == null); count not
        // asserted (the tree shape is not a locked expectation)
        boolean hasTopLevel = false;
        for (JsonNode f : parsedFunctions) {
            if (f.get("parent").isNull()) {
                hasTopLevel = true;
                break;
            }
        }
        assertTrue(hasTopLevel, "registry.json functions array has no top-level entry (parent == null)");

        // (iv) the emitted families block is consistent with the reported
        // family-graph edge count K: sum of len(after) over all emitted
        // steps == K (both computed from step.afterIds(assembler))
        ArrayNode parsedFamilies = (ArrayNode) parsed.get("families");
        assertNotNull(parsedFamilies, "registry.json is missing the 'families' array");
        long emittedEdgeCount = 0;
        for (JsonNode family : parsedFamilies) {
            for (JsonNode step : family.get("steps")) {
                emittedEdgeCount += step.get("after").size();
            }
        }
        assertEquals(familyGraphEdges, emittedEdgeCount,
                "Emitted families block carries " + emittedEdgeCount
                        + " after-edges but the reported family-graph edge count K is "
                        + familyGraphEdges);

        // (v) every after id resolves to an existing registry node id
        // (from the emitted nodes array)
        for (JsonNode family : parsedFamilies) {
            for (JsonNode step : family.get("steps")) {
                for (JsonNode afterId : step.get("after")) {
                    String nodeId = afterId.asText();
                    assertTrue(parsedById.containsKey(nodeId),
                            "Family '" + family.get("id").asText() + "' step '"
                                    + step.get("nodeId").asText()
                                    + "' references unknown node id '" + nodeId + "'");
                }
            }
        }
    }
}
