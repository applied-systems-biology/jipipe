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

package org.hkijena.jipipe.plugins.nodetemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseBuilderRun;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.relations.NodeRelations;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.service.components.JIPipeNodesServiceComponent;
import org.hkijena.jipipe.api.service.components.JIPipeNodeTemplatesServiceComponent;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration gate for shipping the corpus-mined workflow patterns as bundled
 * System node templates (task 8, spec §11 + the revised R8 rulings).
 * <p>
 * The 64 curated, deduplicated patterns are registered by
 * {@code TaxonomyPlugin.registerSystemTemplates()} from the bundled resources
 * under {@code org/hkijena/jipipe/plugins/nodetemplate/patterns/} (64 raw
 * {@code JIPipeGraph} JSONs + the committed {@code system-templates.json}
 * metadata sidecar). They arrive through the plugin-template path
 * ({@link JIPipeNodeTemplatesServiceComponent#addFromPlugin(JIPipeNodeTemplate)}),
 * so {@link JIPipeNodeTemplatesServiceComponent#isPluginTemplate(JIPipeNodeTemplate)}
 * is {@code true} for each (System scope).
 * <p>
 * The shipped set is two families, both reviewed by the 1324 manual review:
 * <ul>
 *   <li><b>24 linear (node-sequence) patterns</b> — the 185 research
 *       artifacts cut to 64 with derivable names (fan shapes + every
 *       {@code jipipe:compartment-output} sink excluded, shape-label
 *       duplicates deduped), then Option C (ruling 2026-19) to the 24
 *       genuinely linear-only workflows: the 32 node+edge sub-paths of
 *       already-shipped DAG patterns and the 8 plumbing (comment/group)
 *       patterns are dropped. Their names are OUTCOME names ("what the
 *       workflow does") — no node chain, no chain separator of any kind —
 *       the mechanism stays in the description ({@code Sequence:}/
 *       {@code Wiring:}).</li>
 *   <li><b>400 DAG patterns</b> — 422 mined/derived (262 corpus-mined [3
 *       stale-id remaps, 3 excludes] + 160 derived [26 twins + 134
 *       what-next, incl. the 6 cLij2 re-sweep chains]), cleaned at
 *       generation (group I/O wrapper nodes stripped, the {Parameters}
 *       external slot enabled where wired), then the item-3c ruling drops
 *       16: 5 patterns involving the (to-be-redesigned) JIPipe scripting
 *       system, 2 loop-plumbing tunnel patterns, and 9 degenerate
 *       multi-component harvest artifacts (34 further multi-component
 *       patterns keep their largest connected component), then the B2
 *       ruling drops 6 patterns containing an io-interface node (5 mined
 *       + 1 derived). Decisions:
 *       {@code utils/taxonomy-research/dag-pattern-decisions.json}.</li>
 * </ul>
 * A pattern ships only if it has a proper derivable name of what it does.
 * <p>
 * Pins:
 * <ol>
 *   <li><b>exact count 424</b> — the 24 linear keep-set + the cleaned DAG
 *       set (422 mined/derived − 16 item-3c drops − 6 io-interface drops
 *       = 400) is pinned to a number; not "≥424".</li>
 *   <li><b>per-template deserialization guard</b> — each shipped graph must
 *       deserialize through the exact {@code JIPipeNodeTemplate.getGraph()}
 *       path with the same node set as its bundled resource, and every node
 *       id must resolve in the registry (the silent-empty / stale-id guard,
 *       generalized from {@link
 *       org.hkijena.jipipe.api.nodes.taxonomy.TemplateDataDeserializationTest}
 *       to all 424). Identity is (name, node-info-id multiset): 13 DAG names
 *       are shared by structurally different graphs, so name alone is no
 *       longer a unique key — (name, id multiset) is (verified at
 *       generation).</li>
 *   <li><b>top-level function</b> — every template's {@code taxonomyFunction}
 *       resolves and its assigned function is a top-level branch
 *       ({@code getParent() == null}).</li>
 *   <li><b>flagship mined next-step</b> — the flagship pattern
 *       {@code folder-list-files → path-to-annotation-simple →
 *       import-imagej-imgplus-from-file} flows into the relation index: the
 *       {@code next} relations of {@code path-to-annotation-simple} include an
 *       entry named "Import image".</li>
 *   <li><b>consumer-side dedup</b> — no two shipped templates share
 *       (name, graph data); the deduped shape-label duplicates must not
 *       reappear, and same-name patterns that ship are kept distinct
 *       (different slot wiring → different graph data).</li>
 *   <li><b>curation rules</b> — no shipped pattern contains a
 *       {@code jipipe:compartment-output} node and no shipped name is a
 *       statistical "fans in/out from N ids" name.</li>
 *   <li><b>function tie-break</b> — a surviving 3-way tie (annotations/
 *       images/rois at max count) resolves to the lexicographically-first id.</li>
 * </ol>
 * <p>
 * Mirrors {@link org.hkijena.jipipe.api.nodes.taxonomy.RegistryDumperTest}'s
 * bootstrap: this module's registered ImageJ legacy-patcher
 * {@code LauncherSessionListener} preinits the patcher before any test class
 * loads, so {@code JIPipe.ensureInstance()} is safe; no new listeners or
 * {@code --add-opens} are needed. jipipe-launcher has no AssertJ, so JUnit
 * {@code Assertions} are used throughout.
 */
class SystemPatternTemplatesTest {

    /** The shipped patterns are registered under this bundled resource dir. */
    static final String PATTERN_RESOURCE_DIR =
        "/org/hkijena/jipipe/plugins/nodetemplate/patterns/";
    static final String SIDECAR_RESOURCE = PATTERN_RESOURCE_DIR + "system-templates.json";

    /** Exactly 424 distinct patterns ship: 24 linear (node-sequence, from
     * the 185 research artifacts, curation + 25 collapsed shape-label
     * duplicates, then Option C (ruling 2026-19) drops the 32 node+edge
     * sub-paths of already-shipped DAG patterns and the 8 plumbing
     * (comment/group) patterns) + 400 DAG (422 mined/derived minus the 16
     * item-3c drops — 5 JIPipe-scripting patterns, 2 tunnel patterns, 9
     * degenerate multi-component harvest artifacts — and the 6 io-interface
     * drops from the B2 ruling: 5 mined + 1 derived). */
    static final int EXPECTED_SYSTEM_TEMPLATES = 424;

    /** The flagship mined pattern (a Data Sources chain): list a folder's
     * files and import them as 2D ROIs (one of the 24 Option-C linear
     * patterns; the old flagship "… -> Import image" is a sub-path of a
     * shipped DAG and was dropped by Option C). */
    static final String FLAGSHIP = "chain-3-folder-list-files-path-to-annotation-simple-import-imagej-roi-from-file";
    static final String FLAGSHIP_NODE = "path-to-annotation-simple";
    static final String FLAGSHIP_NEXT_NAME = "Import ROI";

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent: reuses the
        // JIPipe singleton if another test class in the same JVM already
        // initialized it.
        JIPipe.ensureInstance();
    }

    /** All plugin-registered (System-scope) templates. */
    private static List<JIPipeNodeTemplate> systemTemplates() {
        JIPipeNodeTemplatesServiceComponent service = JIPipe.getNodeTemplates();
        List<JIPipeNodeTemplate> result = new ArrayList<>();
        for (JIPipeNodeTemplate t : service.getAllTemplates(null)) {
            if (service.isPluginTemplate(t)) {
                result.add(t);
            }
        }
        return result;
    }

    /** Reads a bundled resource from the classpath as a UTF-8 string. */
    private static String readResource(String path) throws Exception {
        try (InputStream in = SystemPatternTemplatesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "resource not found on the classpath: " + path);
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @Test
    void allSystemPatternsAreRegistered() {
        List<JIPipeNodeTemplate> templates = systemTemplates();
        assertEquals(EXPECTED_SYSTEM_TEMPLATES, templates.size(),
            "the curation + dedup from the 185 research artifacts is pinned "
                + "to an exact count (fan shapes + compartment sinks excluded, "
                + "shape-label duplicates collapsed); got "
                + templates.size() + " System (plugin) templates");

        // The flagship must be among them (it is one of the shipped patterns).
        boolean flagshipPresent = false;
        for (JIPipeNodeTemplate t : templates) {
            if (t.getData() != null && t.getData().contains("folder-list-files")
                && t.getGraph() != null
                && t.getGraph().getNodeCount() == 3) {
                // Identify the flagship by its node set (stable identity).
                Set<String> ids = nodeIds(t.getGraph());
                if (ids.equals(Set.of("folder-list-files", "path-to-annotation-simple",
                    "import-imagej-roi-from-file"))) {
                    flagshipPresent = true;
                    break;
                }
            }
        }
        assertTrue(flagshipPresent,
            "the flagship pattern " + FLAGSHIP + " must be registered as a System template");

        // --- functionId pins (sidecar-driven; end-to-end for the rule) ---
        // Flagship: complete node set is folder-list-files -> data,
        // path-to-annotation-simple -> annotations, import-imagej-roi-from-file
        // -> data = {data:2, annotations:1}, so the most-common branch is DATA
        // (NOT a 3-way tie, and no rois node). This pins that the sidecar's
        // flagship functionId is data and that the rule ran over the complete node
        // set (a truncated/example-subgraph source would give a different set).
        assertEquals("jipipe:function:data",
            templateByName("Load 2D ROIs from a folder")
                .getTaxonomyFunction().getId(),
            "the flagship pattern's function must be jipipe:function:data "
                + "(complete node set {data:2, annotations:1} -> most-common data)");

        // Tie-break pin: this 3-node pattern is a GENUINE 3-way tie
        // (annotations/data/rois all at max count), so the winner is the
        // lexicographically-first id = jipipe:function:annotations. This pins
        // the "ties -> lexicographically first" rule end-to-end.
        assertEquals("jipipe:function:annotations",
            templateByName("Import 2D ROIs and convert them to masks")
                .getTaxonomyFunction().getId(),
            "the 3-way-tie pattern must resolve to the lexicographically-first "
                + "branch jipipe:function:annotations");
    }

    /** Finds the registered System template by its (unique) display name. */
    private static JIPipeNodeTemplate templateByName(String name) {
        for (JIPipeNodeTemplate t : systemTemplates()) {
            if (name.equals(t.getName())) {
                return t;
            }
        }
        throw new AssertionError("no registered System template named: " + name);
    }

    @Test
    void everyPatternGraphDeserializes() throws Exception {
        JIPipeNodesServiceComponent nodes = JIPipe.getNodes();
        List<JIPipeNodeTemplate> templates = systemTemplates();
        assertEquals(EXPECTED_SYSTEM_TEMPLATES, templates.size());

        // Identity key: (name, RESOLVABLE node-info-id multiset). Matching by
        // (name, node-ids) — NOT name alone — is the required key: 13 DAG
        // pattern names are shared by structurally different graphs, so a
        // name-only key would false-conflict; (name, node-info-id multiset) is
        // unique across all 446 (verified at generation; multiset, not set, so
        // repeated node types still disambiguate).
        //
        // "Resolvable" = present in the RUNNING test JVM's registry (hasNodeInfo-
        // WithId). The test JVM boots on the Maven-classpath cLij2 2.3.0.6, while
        // the SHIPPING dist bundles cLij2 2.5.3.5 + clijx 0.32.2.0 (a Fiji-only
        // build, not on Maven Central / imagej.net — so the pom cannot be
        // bumped to match it in CI). 10 cLij2 patterns therefore reference
        // dynamically-registered clij:plugin:* / clijx:* kernels that the
        // older test-JVM cLij2 does not register; fromJson drops exactly those
        // (they load fine in the dist). The authoritative stale-id oracle is the
        // 2072-node full-launcher registry, enforced at generation
        // (generate-dag-system-template-metadata.py fails closed on any
        // unregistered id) — per the 1324 (D) ruling that the (D) id oracle is
        // the full-launcher dump, never the test-classpath registry. Filtering
        // to resolvable ids here makes the guard tolerant of ONLY that
        // version-gated cLij2 gap while still catching every real defect:
        //   * a silent fromJson drop of a resolvable node -> the loaded
        //     id-multiset is a proper subset -> no matching identity -> fail;
        //   * a genuinely stale id -> already rejected at generation against
        //     the full-launcher registry.
        Map<String, Integer> nodeCountByIdentity = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : sidecarEntries()) {
            String name = entry.getValue().get("name").asText();
            String raw = readResource(PATTERN_RESOURCE_DIR + entry.getValue().get("file").asText());
            String resolvable = resolvableNodeInfoIds(raw, nodes);
            String identity = name + " || " + resolvable;
            if (nodeCountByIdentity.containsKey(identity)) {
                throw new IllegalStateException("identity '" + identity
                    + "' is shared by two bundled resources; (name, resolvable-"
                    + "node-ids) is no longer a unique key");
            }
            nodeCountByIdentity.put(identity, countNodeInfoIds(resolvable));
        }

        for (JIPipeNodeTemplate t : templates) {
            // (i) the exact JIPipeNodeTemplate.getGraph() path must not throw /
            //     must not return null.
            JIPipeGraph graph = t.getGraph();
            assertNotNull(graph, "template '" + t.getName() + "' deserialized to a null graph");

            // The loaded graph's node-info-id multiset IS the set of bundled
            // ids the test JVM could resolve (fromJson keeps registered nodes,
            // drops unregistered ones). Matching on (name, loaded-ids) therefore
            // pins the template to exactly one bundled resource and verifies
            // that every resolvable node loaded.
            String identity = t.getName() + " || " + nodeInfoIds(graph);
            Integer expected = nodeCountByIdentity.get(identity);
            assertNotNull(expected,
                "template '" + t.getName() + "' (loaded node ids "
                    + nodeInfoIds(graph) + ") is not one of the shipped patterns "
                    + "— a bundled node the test JVM can resolve was dropped by fromJson");

            // (ii) silent-empty guard: the loaded node count equals the number
            //      of bundled nodes the test JVM can resolve. (A fromJson that
            //      silently empties the graph yields 0, which cannot equal a
            //      non-empty resolvable count.)
            assertEquals(expected, graph.getNodeCount(),
                "template '" + t.getName() + "' must load all " + expected
                    + " resolvable nodes (an empty/short graph means fromJson dropped nodes silently)");

            // (iii) every loaded node id is a registry id in the test JVM.
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                String id = node.getInfo().getId();
                assertTrue(nodes.hasNodeInfoWithId(id),
                    "template '" + t.getName() + "' references unregistered node id: " + id);
            }
        }
    }

    @Test
    void functionsAreTopLevelBranches() {
        for (JIPipeNodeTemplate t : systemTemplates()) {
            var ref = t.getTaxonomyFunction();
            assertNotNull(ref, "template '" + t.getName() + "' has no taxonomy function");
            assertTrue(ref.isResolved(),
                "template '" + t.getName() + "' taxonomy function id '" + ref.getId()
                    + "' does not resolve to a registered function");
            JIPipeNodeTaxonomyFunction fn = ref.getFunction();
            assertNotNull(fn, "template '" + t.getName() + "' resolved to a null function");
            assertNull(fn.getParent(),
                "template '" + t.getName() + "' function '" + fn.getId()
                    + "' is not a top-level branch (parent " + (fn.getParent() != null ? fn.getParent().getId() : "null") + ")");
        }
    }

    @Test
    void flagshipMinesNextSteps() {
        // A database that runs its builder run synchronously on this thread (the
        // default enqueue needs an EDT the headless JUnit JVM does not reliably
        // provide). The real builder run populates the entries (so the relation
        // node-ids map back to entries) and mines the template graphs — see
        // TemplateRelationSourceTest for the same override.
        JIPipeNodeDatabase database = new SyncNodeDatabase(null);

        JIPipeNodeDatabaseEntry entry = entryByOriginNodeId(database, FLAGSHIP_NODE);
        assertNotNull(entry, "database must hold an entry for " + FLAGSHIP_NODE);

        NodeRelations relations = database.relations(entry, 6);
        boolean found = false;
        for (JIPipeNodeDatabaseEntry next : relations.next()) {
            if (FLAGSHIP_NEXT_NAME.equals(next.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found,
            "next(" + FLAGSHIP_NODE + ") must contain an entry named '" + FLAGSHIP_NEXT_NAME
                + "' (mined from the flagship pattern); got: " + names(relations.next()));
    }

    @Test
    void noDuplicateNamesWithSameGraph() {
        // The consumer-side dedup pin: no two shipped System templates share
        // the identity (name, graph data). This is the graph signature the
        // generator deduped on, viewed from the registered side.
        // <p>
        // The graph data (not just the node id set) is REQUIRED: same-name
        // patterns that differ only in which slot they connect through would
        // false-positive under a (name, node-ids) key. The deduped
        // shape-label duplicates must not reappear as two identical
        // (name, data) entries.
        Set<String> seen = new HashSet<>();
        List<String> dupes = new ArrayList<>();
        for (JIPipeNodeTemplate t : systemTemplates()) {
            String key = t.getName() + " || " + t.getData();
            if (!seen.add(key)) {
                dupes.add(t.getName());
            }
        }
        assertTrue(dupes.isEmpty(),
            "no two shipped System templates may share (name, graph data); duplicates: " + dupes);
    }

    @Test
    void curationRulesHold() {
        // The manual-review curation: a pattern ships only if it has a proper
        // derivable name of what it does. That means NO compartment sink
        // (workflow-specific plumbing) and NO statistical "fans in/out from N
        // ids" name — both were the shipped noise.
        for (JIPipeNodeTemplate t : systemTemplates()) {
            boolean hasCompartmentSink = false;
            for (JIPipeGraphNode node : t.getGraph().getGraphNodes()) {
                if ("jipipe:compartment-output".equals(node.getInfo().getId())) {
                    hasCompartmentSink = true;
                    break;
                }
            }
            assertFalse(hasCompartmentSink,
                "shipped pattern '" + t.getName() + "' contains a compartment-output sink");
            assertFalse(t.getName().matches(".*fans (in from|out to) \\d+.*"),
                "shipped pattern name is a statistical fan name: " + t.getName());
        }
    }

    @Test
    void everyBundledPatternNodeHasAResolvableLocation() throws Exception {
        // Contract pin (task 1, RED until the data fix ships): every node in
        // every bundled pattern must have a UI location resolvable in its own
        // compartment — the exact key the editor looks up. This is a true
        // load-path contract: we deserialize through
        // {@code JsonUtils.readFromString(json, JIPipeGraph.class)}, the very
        // call {@code JIPipeNodeTemplate.getGraph()} makes, so a location
        // resolvable here is one resolvable in the app.
        // <p>
        // Resolvability ONLY, NOT spacing/overlap: 36 of the 251 mined files
        // carry user layouts with nodes closer than any fixed footprint, so a
        // spacing assertion would false-fail. Spacing of the *generated* files
        // is guaranteed by construction — the same longest-path layering the
        // app's own Sugiyama impl uses — and is eyeballed in-app.
        for (Map.Entry<String, JsonNode> entry : sidecarEntries()) {
            String f = entry.getValue().get("file").asText();
            String raw = readResource(PATTERN_RESOURCE_DIR + f);
            JIPipeGraph graph = JsonUtils.readFromString(raw, JIPipeGraph.class);
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                String key = StringUtils.nullToEmpty(graph.getCompartmentUUIDOf(node));
                assertNotNull(node.getNodeUILocationWithin(key),
                    f + ": node " + node.getName() + " has no resolvable location (compartment key " + key + ")");
            }
        }
    }

    @Test
    void everyBundledPatternEdgeSurvivesDeserialization() throws Exception {
        // Contract pin (task 1, RED until the data fix ships): the number of
        // cross-node edges the deserialized graph holds must equal the number
        // of edges the bundled resource declares. A mismatch means at least
        // one edge was dropped on load because its endpoint slot/node did not
        // resolve — the slot configurations + slot-driving parameters
        // stripped from the minimal format (the B1 dynamic-slot rule, and the
        // parameter-driven named slots such as ij1-color-split-channels-2's
        // Blue/Green/… outputs) — leaving nodes floating.
        // <p>
        // The metric is {@code getSlotEdges()} (cross-node edges only) — NOT
        // {@code getGraph().edgeSet().size()}: repairGraph() (run on every
        // node insert during deserialization) adds per-node internal
        // input→output edges to the JGraphT graph, so edgeSet().size() =
        // cross + internal and can never equal the raw cross-edge count.
        // getSlotEdges() skips same-node edges and is the metric the graph's
        // own serializer uses.
        for (Map.Entry<String, JsonNode> entry : sidecarEntries()) {
            String f = entry.getValue().get("file").asText();
            String raw = readResource(PATTERN_RESOURCE_DIR + f);
            JsonNode tree = JsonUtils.getObjectMapper().readTree(raw);
            JIPipeGraph graph = JsonUtils.readFromString(raw, JIPipeGraph.class);
            int rawEdges = tree.get("edges").size();
            int loadedEdges = graph.getSlotEdges().size();
            assertEquals(rawEdges, loadedEdges,
                f + ": " + (rawEdges - loadedEdges) + " edge(s) dropped on load (missing slot / missing node)");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Set<String> nodeIds(JIPipeGraph graph) {
        Set<String> ids = new HashSet<>();
        if (graph != null) {
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                ids.add(node.getInfo().getId());
            }
        }
        return ids;
    }

    /** Reads the bundled sidecar and returns its entries (pattern id → object). */
    private static List<Map.Entry<String, JsonNode>> sidecarEntries() throws Exception {
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry :
            new ObjectMapper().readTree(readResource(SIDECAR_RESOURCE)).properties()) {
            entries.add(entry);
        }
        return entries;
    }

    /**
     * Canonical RESOLVABLE node-info-id multiset (sorted, comma-joined) of a
     * bundled raw graph resource: the node ids that the running test JVM's
     * registry actually resolves (see {@code everyPatternGraphDeserializes}
     * for why version-gated cLij2 kernels are filtered). Repeated node types
     * are preserved (multiset, not set) so they still disambiguate same-name
     * patterns.
     */
    private static String resolvableNodeInfoIds(String raw, JIPipeNodesServiceComponent nodes) throws Exception {
        JsonNode nodesJson = new ObjectMapper().readTree(raw).get("nodes");
        List<String> ids = new ArrayList<>();
        nodesJson.forEach(n -> ids.add(n.get("jipipe:node-info-id").asText()));
        ids.removeIf(id -> !nodes.hasNodeInfoWithId(id));
        Collections.sort(ids);
        return String.join(",", ids);
    }

    /** Element count of a comma-joined id list ("" counts as 0). */
    private static int countNodeInfoIds(String commaJoined) {
        return commaJoined.isEmpty() ? 0 : commaJoined.split(",", -1).length;
    }

    /** Canonical node-info-id multiset (sorted, comma-joined) of a loaded graph. */
    private static String nodeInfoIds(JIPipeGraph graph) {
        List<String> ids = new ArrayList<>();
        if (graph != null) {
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                ids.add(node.getInfo().getId());
            }
        }
        Collections.sort(ids);
        return String.join(",", ids);
    }

    private static JIPipeNodeDatabaseEntry entryByOriginNodeId(JIPipeNodeDatabase database, String nodeId) {
        for (JIPipeNodeDatabaseEntry entry : database.getEntries()) {
            if (nodeId.equals(entry.getOriginNodeId())) {
                return entry;
            }
        }
        return null;
    }

    private static String names(List<JIPipeNodeDatabaseEntry> entries) {
        return entries.stream().map(JIPipeNodeDatabaseEntry::getName).toList().toString();
    }

    /**
     * A node database that runs its {@link JIPipeNodeDatabaseBuilderRun}
     * synchronously on the calling thread instead of enqueueing it on the
     * async {@code SwingWorker} executor — so after construction the entries
     * are set and the relation index is current, with no dependence on an EDT.
     * (Same pattern as {@code TemplateRelationSourceTest}.)
     */
    private static class SyncNodeDatabase extends JIPipeNodeDatabase {
        private SyncNodeDatabase(org.hkijena.jipipe.api.project.JIPipeProject project) {
            super(project);
        }

        @Override
        public void rebuildImmediately() {
            new JIPipeNodeDatabaseBuilderRun(this).run();
        }
    }
}
