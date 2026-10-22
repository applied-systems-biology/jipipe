import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Standalone dumper for the JIPipe 6.0.0 real-pipeline taxonomy study (work item 1324).
 *
 * <p>Walks the given corpus root(s) for {@code *.jip} project files (recursively),
 * parses them with Jackson, and extracts the structure-only project graph:
 * compartments, nodes (uid / node type id / name / declared slots with their raw
 * on-disk {@code slot-data-type} tokens), and edges. Output is a single
 * {@code corpus.json} with the locked shape:
 *
 * <pre>{@code
 * {
 *   "projects": [
 *     {
 *       "file": "Example workflows/AdrianFeile_VibrioOnChip_04.jip",
 *       "loadable": true,
 *       "loadError": null,
 *       "compartments": [
 *         {
 *           "name": "Analysis",
 *           "nodes": [
 *             {"uid": "...", "id": "...", "name": "...",
 *              "slots": {"input": [{"name": "...", "dataClass": "..."}],
 *                        "output": [{"name": "...", "dataClass": "..."}]}}
 *           ],
 *           "edges": [
 *             {"fromUid": "...", "fromSlot": "...", "dataClass": "...", "toUid": "...", "toSlot": "..."}
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <h2>On-disk format (JIPipe 6.0.0, flat-graph project format)</h2>
 * <p>A {@code .jip} file is a JSON object with two graphs:
 * <ul>
 *   <li>{@code graph} — the <b>flat main graph</b>: {@code nodes} (a map keyed by node
 *       UUID) and {@code edges} (a list of
 *       {@code source-node}/{@code source-slot}/{@code target-node}/{@code target-slot}).
 *       Every main-graph node carries {@code jipipe:graph-compartment} = the UUID of
 *       the compartment it belongs to (empty/absent for root-level nodes).</li>
 *   <li>{@code compartments.compartment-graph} — a separate graph whose nodes are the
 *       <b>compartment container</b> nodes ({@code jipipe:node-info-id} =
 *       {@code jipipe:project-compartment}). The container nodes themselves are not
 *       duplicated into the output; they provide the compartment display names.</li>
 * </ul>
 * <p>Main-graph nodes additionally include per-compartment
 * {@code jipipe:compartment-output} <i>proxy</i> nodes (one per compartment) that
 * declare the compartment's input/output slot interface. Edges are re-homed into
 * compartments by the <b>target</b> node's compartment (every node is in exactly one
 * compartment), so an edge whose source is another compartment's output proxy
 * appears in the target's compartment; its {@code fromUid} then references a node
 * of the source compartment. (Inter-compartment wiring at the container level is
 * redundant with these proxy edges and is not emitted separately.)
 *
 * <p>Node slots live under {@code jipipe:slot-configuration} =
 * {@code {"input": {slotName: {...}}, "output": {slotName: {...}}}}. The raw
 * on-disk data class token is the slot's {@code slot-data-type} field, recorded
 * verbatim (normalization happens later, Task 3). The edge's {@code dataClass} is
 * the same token of the source node's output slot, since the on-disk format
 * carries no data class on the edge itself.
 *
 * <p>Structure only: a node's {@code data} field (embedded data payloads) and all
 * other non-structural fields are never copied into the output.
 *
 * <h2>Usage</h2>
 * <pre>
 *   ProjectCorpusDumper selftest                        # assertions, exit 0/1
 *   ProjectCorpusDumper &lt;root1&gt; [root2 ...] &lt;out.json&gt;  # dump the corpus
 * </pre>
 *
 * <p>Not a Maven module: compile with {@code javac} against the Jackson jars in
 * {@code ~/.m2} (databind + core + annotations).
 */
public final class ProjectCorpusDumper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProjectCorpusDumper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "selftest".equals(args[0])) {
            int failures = selftest();
            System.out.println(failures == 0 ? "SELFTEST: PASS" : "SELFTEST: FAIL (" + failures + " check(s) failed)");
            System.exit(failures == 0 ? 0 : 1);
        }
        if (args.length < 2) {
            System.err.println("usage: ProjectCorpusDumper selftest | ProjectCorpusDumper <root1> [root2 ...] <out.json>");
            System.exit(2);
        }
        List<Path> roots = new ArrayList<>();
        for (int i = 0; i < args.length - 1; i++) {
            roots.add(Paths.get(args[i]));
        }
        int failures = dumpCorpus(roots, Paths.get(args[args.length - 1]));
        System.exit(failures);
    }

    // ------------------------------------------------------------------
    // Selftest
    // ------------------------------------------------------------------

    private static final Path SELFTEST_1 =
            Paths.get("/data/JIPipe/AIAgentTests/01-BasicWorkshop/lecture_02.jip");
    private static final Path SELFTEST_2 =
            Paths.get("/data/JIPipe/AIAgentTests/05-Homework-JSC2025/Homework_JSC_2025/Alternative1/hyphae_single.jip");

    /**
     * Asserts against the two smallest corpus files that (d) {@code loadable == true},
     * (e) node count &gt; 0, (f) every edge references existing uids and slot names,
     * and (g) a deliberately bogus path yields {@code loadable == false} with a
     * non-null {@code loadError} instead of an exception. Returns the number of
     * failed checks (exit code).
     */
    static int selftest() {
        int failures = 0;
        ProjectRecord lecture = loadProject(SELFTEST_1, null);
        failures += check("(d) lecture_02 loadable == true", lecture.loadable, () -> "loadError=" + lecture.loadError);
        failures += check("(e) lecture_02 node count > 0", totalNodes(lecture) > 0, () -> "nodes=" + totalNodes(lecture));
        failures += checkEdges("(f) lecture_02 edge uids/slots", lecture);
        System.out.println("  selftest file: " + lecture.file + " nodes=" + totalNodes(lecture) + " edges=" + totalEdges(lecture)
                + " compartments=" + lecture.compartments.size());

        ProjectRecord hyphae = loadProject(SELFTEST_2, null);
        failures += check("(d) hyphae_single loadable == true", hyphae.loadable, () -> "loadError=" + hyphae.loadError);
        failures += check("(e) hyphae_single node count > 0", totalNodes(hyphae) > 0, () -> "nodes=" + totalNodes(hyphae));
        failures += checkEdges("(f) hyphae_single edge uids/slots", hyphae);
        System.out.println("  selftest file: " + hyphae.file + " nodes=" + totalNodes(hyphae) + " edges=" + totalEdges(hyphae)
                + " compartments=" + hyphae.compartments.size());

        ProjectRecord bogus = loadProject(Paths.get("/data/JIPipe/__selftest_does_not_exist__.jip"), null);
        failures += check("(g) bogus path loadable == false", !bogus.loadable, () -> "loadable was true");
        failures += check("(g) bogus path loadError != null",
                bogus.loadError != null && !bogus.loadError.isBlank(), () -> "loadError=" + bogus.loadError);
        return failures;
    }

    /**
     * (f) Every edge references existing uids and slot names: the source uid must
     * exist (any compartment of the project, since source proxies of other
     * compartments appear in the target's edge list) and expose the source slot in
     * its output slots; the target uid must exist in the edge's compartment and
     * expose the target slot in its input slots.
     */
    private static int checkEdges(String label, ProjectRecord record) {
        Map<String, NodeRecord> byUid = new HashMap<>();
        for (CompartmentRecord compartment : record.compartments) {
            for (NodeRecord node : compartment.nodes) {
                byUid.put(node.uid, node);
            }
        }
        int failures = 0;
        for (CompartmentRecord compartment : record.compartments) {
            Set<String> localUids = new LinkedHashSet<>();
            for (NodeRecord node : compartment.nodes) {
                localUids.add(node.uid);
            }
            for (int i = 0; i < compartment.edges.size(); i++) {
                EdgeRecord edge = compartment.edges.get(i);
                NodeRecord from = byUid.get(edge.fromUid);
                NodeRecord to = byUid.get(edge.toUid);
                boolean fromOk = from != null && hasSlot(from.output, edge.fromSlot);
                boolean toOk = to != null && localUids.contains(edge.toUid) && hasSlot(to.input, edge.toSlot);
                if (!fromOk || !toOk) {
                    failures++;
                    System.out.println("  FAILED: " + label + " compartment '" + compartment.name + "' edge[" + i + "]"
                            + " fromUid=" + edge.fromUid + "(ok=" + fromOk + ")"
                            + " toUid=" + edge.toUid + "(ok=" + toOk + ")");
                }
            }
        }
        if (failures == 0) {
            System.out.println("  ok: " + label);
        }
        return failures;
    }

    private static boolean hasSlot(List<SlotRecord> slots, String slotName) {
        for (SlotRecord slot : slots) {
            if (slot.name.equals(slotName)) {
                return true;
            }
        }
        return false;
    }

    private static int totalNodes(ProjectRecord record) {
        int total = 0;
        for (CompartmentRecord compartment : record.compartments) {
            total += compartment.nodes.size();
        }
        return total;
    }

    private static int totalEdges(ProjectRecord record) {
        int total = 0;
        for (CompartmentRecord compartment : record.compartments) {
            total += compartment.edges.size();
        }
        return total;
    }

    private static int check(String label, boolean ok, java.util.function.Supplier<String> diag) {
        if (!ok) {
            System.out.println("  FAILED: " + label + " (" + diag.get() + ")");
            return 1;
        }
        System.out.println("  ok: " + label);
        return 0;
    }

    // ------------------------------------------------------------------
    // Corpus walk + extraction
    // ------------------------------------------------------------------

    /**
     * Dumps every {@code *.jip} under the given roots (recursive, duplicates
     * removed) into the locked {@code corpus.json} shape at {@code out}.
     * Returns the number of failed files (used as exit code).
     */
    static int dumpCorpus(List<Path> roots, Path out) throws IOException {
        List<Path> files = new ArrayList<>();
        Set<Path> seen = new LinkedHashSet<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                System.err.println("warning: root does not exist or is not a directory, skipping: " + root);
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".jip"))
                        .sorted()::iterator) {
                    if (seen.add(file.toRealPath())) {
                        files.add(file);
                    }
                }
            }
        }
        System.out.println("found " + files.size() + " .jip file(s) under " + roots.size() + " root(s)");

        ObjectNode rootJson = MAPPER.createObjectNode();
        ArrayNode projects = rootJson.putArray("projects");
        int loaded = 0;
        for (Path file : files) {
            ProjectRecord record = loadProject(file, null);
            if (record.loadable) {
                loaded++;
            }
            projects.add(record.toJson());
        }

        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        MAPPER.writeValue(out.toFile(), rootJson);
        System.out.println("wrote " + out + " (" + Files.size(out) + " bytes)");
        System.out.println("coverage: " + loaded + " loaded / " + files.size() + " total, "
                + (files.size() - loaded) + " failed (see loadError)");
        return files.size() - loaded;
    }

    /**
     * Loads one project file. Never throws for I/O or parse problems: they are
     * captured as {@code loadable=false} + {@code loadError} (Review Focus #1).
     */
    static ProjectRecord loadProject(Path file, String fileFieldOverride) {
        String fileField = fileFieldOverride != null ? fileFieldOverride
                : relativeToFileField(file);
        ProjectRecord record = new ProjectRecord(fileField);
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            if (root == null || !root.isObject()) {
                record.fail("not a JSON object");
                return record;
            }

            JsonNode mainNodes = root.path("graph").path("nodes");
            JsonNode mainEdges = root.path("graph").path("edges");
            JsonNode containerNodes = root.path("compartments").path("compartment-graph").path("nodes");

            // Compartment display names (container nodes), keyed by compartment UUID.
            Map<String, String> compNameByUid = new LinkedHashMap<>();
            if (containerNodes.isObject()) {
                for (Map.Entry<String, JsonNode> e : containerNodes.properties()) {
                    compNameByUid.put(e.getKey(), textOrEmpty(e.getValue().path("jipipe:node:name")));
                }
            }

            // Group main-graph nodes (uid, node) by their owning compartment.
            Map<String, List<Map.Entry<String, JsonNode>>> nodesByCompartment = new LinkedHashMap<>();
            List<Map.Entry<String, JsonNode>> rootNodes = new ArrayList<>();
            if (mainNodes.isObject()) {
                for (Map.Entry<String, JsonNode> e : mainNodes.properties()) {
                    String compartment = textOrEmpty(e.getValue().path("jipipe:graph-compartment"));
                    if (compartment.isEmpty()) {
                        rootNodes.add(e);
                    } else {
                        nodesByCompartment.computeIfAbsent(compartment, k -> new ArrayList<>()).add(e);
                    }
                }
            }

            // Deterministic compartment order: all referenced UIDs, sorted.
            Set<String> compartmentOrder = new LinkedHashSet<>(nodesByCompartment.keySet());
            compartmentOrder.addAll(compNameByUid.keySet());
            List<String> orderedCompartments = new ArrayList<>(compartmentOrder);
            orderedCompartments.sort(Comparator.naturalOrder());

            for (String compartmentUid : orderedCompartments) {
                record.compartments.add(buildCompartment(
                        compNameByUid.getOrDefault(compartmentUid, ""),
                        nodesByCompartment.getOrDefault(compartmentUid, Collections.emptyList()),
                        mainEdges, mainNodes));
            }
            if (!rootNodes.isEmpty()) {
                // Root-level nodes (no owning compartment); kept so nothing is lost.
                record.compartments.add(buildCompartment("(root)", rootNodes, mainEdges, mainNodes));
            }
        } catch (Exception e) {
            record.fail(shortError(e));
        }
        return record;
    }

    /**
     * Builds one compartment: its nodes (in on-disk order) and all main-graph edges
     * whose target node is inside it (edges are re-homed by target compartment).
     */
    private static CompartmentRecord buildCompartment(String name, List<Map.Entry<String, JsonNode>> nodes,
                                                      JsonNode mainEdges, JsonNode mainNodes) {
        CompartmentRecord compartment = new CompartmentRecord(name);
        Map<String, NodeRecord> localByUid = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : nodes) {
            NodeRecord node = toNodeRecord(entry.getKey(), entry.getValue());
            compartment.nodes.add(node);
            localByUid.put(node.uid, node);
        }
        if (mainEdges.isArray()) {
            for (JsonNode edge : mainEdges) {
                String toUid = textOrEmpty(edge.path("target-node"));
                if (!localByUid.containsKey(toUid)) {
                    continue; // edge belongs to the target's own compartment
                }
                String fromUid = textOrEmpty(edge.path("source-node"));
                String fromSlot = textOrEmpty(edge.path("source-slot"));
                compartment.edges.add(new EdgeRecord(
                        fromUid, fromSlot,
                        outputSlotDataClass(mainNodes, fromUid, fromSlot),
                        toUid, textOrEmpty(edge.path("target-slot"))));
            }
        }
        return compartment;
    }

    private static NodeRecord toNodeRecord(String uid, JsonNode node) {
        JsonNode slotConfig = node.path("jipipe:slot-configuration");
        return new NodeRecord(
                uid,
                textOrEmpty(node.path("jipipe:node-info-id")),
                textOrEmpty(node.path("jipipe:node:name")),
                readSlots(slotConfig.path("input")),
                readSlots(slotConfig.path("output")));
    }

    /**
     * Raw on-disk {@code slot-data-type} token of the given node's output slot,
     * or null if the node or slot cannot be found (cross-compartment sources are
     * looked up in the flat main graph, so they are always found in practice).
     */
    private static String outputSlotDataClass(JsonNode mainNodes, String uid, String slotName) {
        JsonNode slot = mainNodes.path(uid).path("jipipe:slot-configuration").path("output").path(slotName);
        String dataClass = textOrEmpty(slot.path("slot-data-type"));
        return dataClass.isEmpty() ? null : dataClass;
    }

    private static List<SlotRecord> readSlots(JsonNode slotMap) {
        List<SlotRecord> result = new ArrayList<>();
        if (slotMap.isObject()) {
            for (Map.Entry<String, JsonNode> e : slotMap.properties()) {
                String dataClass = textOrEmpty(e.getValue().path("slot-data-type"));
                result.add(new SlotRecord(e.getKey(), dataClass.isEmpty() ? null : dataClass));
            }
        }
        return result;
    }

    private static String relativeToFileField(Path file) {
        // Files are recorded relative to the corpus parent (/data/JIPipe), e.g.
        // "Example workflows/AdrianFeile_VibrioOnChip_04.jip".
        Path corpusRoot = Paths.get("/data/JIPipe").toAbsolutePath().normalize();
        Path absolute = file.toAbsolutePath().normalize();
        if (absolute.startsWith(corpusRoot)) {
            return corpusRoot.relativize(absolute).toString();
        }
        return file.toString();
    }

    private static String textOrEmpty(JsonNode node) {
        return node != null && node.isValueNode() ? node.asText() : "";
    }

    private static String shortError(Exception e) {
        String message = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    // ------------------------------------------------------------------
    // Record types (in-memory; the emitted JSON shape is the locked interface)
    // ------------------------------------------------------------------

    static final class ProjectRecord {
        final String file;
        boolean loadable = true;
        String loadError;
        final List<CompartmentRecord> compartments = new ArrayList<>();

        ProjectRecord(String file) {
            this.file = file;
        }

        void fail(String error) {
            this.loadable = false;
            this.loadError = error;
        }

        ObjectNode toJson() {
            ObjectNode json = MAPPER.createObjectNode();
            json.put("file", file);
            json.put("loadable", loadable);
            if (loadError == null) {
                json.putNull("loadError");
            } else {
                json.put("loadError", loadError);
            }
            ArrayNode comps = json.putArray("compartments");
            for (CompartmentRecord compartment : compartments) {
                comps.add(compartment.toJson());
            }
            return json;
        }
    }

    static final class CompartmentRecord {
        final String name;
        final List<NodeRecord> nodes = new ArrayList<>();
        final List<EdgeRecord> edges = new ArrayList<>();

        CompartmentRecord(String name) {
            this.name = name;
        }

        ObjectNode toJson() {
            ObjectNode json = MAPPER.createObjectNode();
            json.put("name", name);
            ArrayNode nodesJson = json.putArray("nodes");
            for (NodeRecord node : nodes) {
                nodesJson.add(node.toJson());
            }
            ArrayNode edgesJson = json.putArray("edges");
            for (EdgeRecord edge : edges) {
                edgesJson.add(edge.toJson());
            }
            return json;
        }
    }

    static final class NodeRecord {
        final String uid;
        final String id;
        final String name;
        final List<SlotRecord> input;
        final List<SlotRecord> output;

        NodeRecord(String uid, String id, String name, List<SlotRecord> input, List<SlotRecord> output) {
            this.uid = uid;
            this.id = id;
            this.name = name;
            this.input = input;
            this.output = output;
        }

        ObjectNode toJson() {
            ObjectNode json = MAPPER.createObjectNode();
            json.put("uid", uid);
            json.put("id", id);
            json.put("name", name);
            ObjectNode slots = json.putObject("slots");
            ArrayNode inputJson = slots.putArray("input");
            for (SlotRecord slot : input) {
                inputJson.add(slot.toJson());
            }
            ArrayNode outputJson = slots.putArray("output");
            for (SlotRecord slot : output) {
                outputJson.add(slot.toJson());
            }
            return json;
        }
    }

    static final class SlotRecord {
        final String name;
        final String dataClass; // raw on-disk slot-data-type token (null if absent)

        SlotRecord(String name, String dataClass) {
            this.name = name;
            this.dataClass = dataClass;
        }

        ObjectNode toJson() {
            ObjectNode json = MAPPER.createObjectNode();
            json.put("name", name);
            if (dataClass == null) {
                json.putNull("dataClass");
            } else {
                json.put("dataClass", dataClass);
            }
            return json;
        }
    }

    static final class EdgeRecord {
        final String fromUid;
        final String fromSlot;
        final String dataClass; // raw on-disk slot-data-type token of the source output slot (null if absent)
        final String toUid;
        final String toSlot;

        EdgeRecord(String fromUid, String fromSlot, String dataClass, String toUid, String toSlot) {
            this.fromUid = fromUid;
            this.fromSlot = fromSlot;
            this.dataClass = dataClass;
            this.toUid = toUid;
            this.toSlot = toSlot;
        }

        ObjectNode toJson() {
            ObjectNode json = MAPPER.createObjectNode();
            json.put("fromUid", fromUid);
            json.put("fromSlot", fromSlot);
            if (dataClass == null) {
                json.putNull("dataClass");
            } else {
                json.put("dataClass", dataClass);
            }
            json.put("toUid", toUid);
            json.put("toSlot", toSlot);
            return json;
        }
    }
}
