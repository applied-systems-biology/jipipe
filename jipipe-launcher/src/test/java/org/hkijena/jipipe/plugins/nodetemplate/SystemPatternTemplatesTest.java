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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
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
 * Curation (the 1324 manual review): a pattern ships only if it has a
 * proper <b>derivable name of what it does</b>. The 185 research artifacts
 * are therefore cut down: the statistical fan shapes ("X fans in from N
 * source ids") and every pattern containing a {@code jipipe:compartment-output}
 * sink (workflow-specific plumbing) are excluded, and the survivors' shape-
 * label duplicates are deduped to the shipped 64.
 * <p>
 * Pins:
 * <ol>
 *   <li><b>exact count 64</b> — the curation + dedup from the 185 research
 *       artifacts is pinned to a number; not "≥64".</li>
 *   <li><b>per-template deserialization guard</b> — each shipped graph must
 *       deserialize through the exact {@code JIPipeNodeTemplate.getGraph()}
 *       path with the same node count as its bundled resource, and every node
 *       id must resolve in the registry (the silent-empty / stale-id guard,
 *       generalized from {@link
 *       org.hkijena.jipipe.api.nodes.taxonomy.TemplateDataDeserializationTest}
 *       to all 64).</li>
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

    /** Exactly 64 distinct patterns ship (185 research artifacts, curation
     *  + 25 collapsed shape-label duplicates). */
    static final int EXPECTED_SYSTEM_TEMPLATES = 64;

    /** The flagship mined pattern (a Data Sources chain). */
    static final String FLAGSHIP = "chain-3-folder-list-files-path-to-annotation-simple-import-imagej-imgplus-from-file";
    static final String FLAGSHIP_NODE = "path-to-annotation-simple";
    static final String FLAGSHIP_NEXT_NAME = "Import image";

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
                    "import-imagej-imgplus-from-file"))) {
                    flagshipPresent = true;
                    break;
                }
            }
        }
        assertTrue(flagshipPresent,
            "the flagship pattern " + FLAGSHIP + " must be registered as a System template");

        // --- functionId pins (sidecar-driven; end-to-end for the rule) ---
        // Flagship: complete node set is folder-list-files -> data,
        // path-to-annotation-simple -> annotations, import-imagej-imgplus-from-file
        // -> data = {data:2, annotations:1}, so the most-common branch is DATA
        // (NOT a 3-way tie, and no images node). This pins that the sidecar's
        // flagship functionId is data and that the rule ran over the complete node
        // set (a truncated/example-subgraph source would give a different set).
        assertEquals("jipipe:function:data",
            templateByName("List files -> Annotate with file/directory name -> Import image")
                .getTaxonomyFunction().getId(),
            "the flagship pattern's function must be jipipe:function:data "
                + "(complete node set {data:2, annotations:1} -> most-common data)");

        // Tie-break pin: this surviving 3-node pattern is a GENUINE 3-way tie
        // (annotations/images/rois all at max count), so the winner is the
        // lexicographically-first id = jipipe:function:annotations. This pins
        // the "ties -> lexicographically first" rule end-to-end.
        assertEquals("jipipe:function:annotations",
            templateByName("Convert 2D ROI to mask -> Morphological operation 2D -> Annotate with data")
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

        // Map each bundled resource's NAME to its node count. Matching by name
        // is deterministic because no name is shared by more than one shipped
        // resource (verified at generation; see the generator's name-unique
        // output). This sidesteps the getData() re-serialization (which, once
        // getGraph() has cached the graph, pretty-prints the data and no longer
        // byte-matches the bundled raw — e.g. trailing newline).
        Map<String, Integer> nodeCountByName = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : sidecarEntries()) {
            String name = entry.getValue().get("name").asText();
            String raw = readResource(PATTERN_RESOURCE_DIR + entry.getValue().get("file").asText());
            int count = countJsonNodes(raw);
            Integer prev = nodeCountByName.get(name);
            if (prev != null && prev != count) {
                throw new IllegalStateException("name '" + name
                    + "' is shared by resources with different node counts ("
                    + prev + " vs " + count + "); the name-key is no longer safe");
            }
            nodeCountByName.put(name, count);
        }

        for (JIPipeNodeTemplate t : templates) {
            Integer expected = nodeCountByName.get(t.getName());
            assertNotNull(expected,
                "template '" + t.getName() + "' is not one of the shipped patterns");

            // (i) the exact JIPipeNodeTemplate.getGraph() path must not throw /
            //     must not return null.
            JIPipeGraph graph = t.getGraph();
            assertNotNull(graph, "template '" + t.getName() + "' deserialized to a null graph");

            // (ii) silent-empty guard: the loaded node count equals the
            //      resource's node count (fromJson returns an EMPTY graph when
            //      a node id or the required compartment key is dropped).
            assertEquals(expected, graph.getNodeCount(),
                "template '" + t.getName() + "' must load all " + expected
                    + " nodes (an empty/short graph means fromJson dropped nodes silently)");

            // (iii) every loaded node id is a registry id (stale-id guard).
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

    private static int countJsonNodes(String data) throws Exception {
        return new ObjectMapper().readTree(data).get("nodes").size();
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
