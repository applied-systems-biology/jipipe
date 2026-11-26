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

package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphStructuralFunction;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the enriched get_pipeline_map response: one segment per compartment with
 * real node counts and cache coverage, the compartment-to-compartment wiring as
 * edges derived from the compartment graph (label = the source compartment's
 * output slot name), and the map version. The status per compartment is the
 * SegmentStatusLogic contract over validation issue counts and cache coverage.
 * Also tests the describe_project composite: the metadata extraction pin on
 * get_project_info, the summary sections (including the store-to-disk x
 * skip-export file-writer matrix), and the full detail's per-compartment node
 * lists.
 */
public class PipelineMapOperationsTest {

    private static final String TEST_NODE_ID = "pipeline-map-test:comment";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("pipeline-map-operations-test", "1.0.0",
                    "pipeline-map-operations-test");

    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean registeredTestNodeHere;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
        // The fixture node is a test-only type: it extends the comment node so
        // the fixture inherits valid node plumbing (one optional input, one
        // output, no workload, no validation issues); the id cannot collide
        // with a real registration. Same wall as the parameter introspection
        // tests: the core test JVM loads only the core plugins, so the type is
        // registered here when absent and unregistered afterwards.
        if (!JIPipe.getNodes().hasNodeInfoWithId(TEST_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(TEST_NODE_ID, MapTestNode.class), TEST_DEPENDENCY);
            registeredTestNodeHere = true;
        }
    }

    @AfterAll
    static void cleanup() {
        if (registeredTestNodeHere) {
            JIPipe.getNodes().unregister(TEST_NODE_ID);
            registeredTestNodeHere = false;
        }
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    private static JsonNode getPipelineMap(JIPipeProject project) {
        return new PipelineMapOperations.GetPipelineMap().execute(createContext(project), mapper.createObjectNode());
    }

    /**
     * Adds an output slot to the compartment. Must happen before connectCompartments:
     * a fresh compartment has no output slot, and connectCompartments wires the first one.
     */
    private static void addOutputSlot(JIPipeProjectCompartment compartment, String name) {
        var slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) compartment.getSlotConfiguration();
        slotConfiguration.addOutputSlot(name, "Test output", JIPipeCompartmentOutputData.class, true);
    }

    private static JIPipeGraphNode insertTestNode(JIPipeProject project, JIPipeProjectCompartment compartment) {
        JIPipeGraphNode node = JIPipe.createNode(TEST_NODE_ID);
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        return node;
    }

    private static void storeCache(JIPipeProject project, JIPipeGraphNode node) {
        project.getCache().store(node, project.getGraph().getUUIDOf(node), new JIPipeDataTable(),
                "Comment", new JIPipeProgressInfo());
    }

    /**
     * Returns the map segment of the compartment (matched by compartmentId), or null.
     */
    private static JsonNode compartmentSegment(JsonNode map, JIPipeProjectCompartment compartment) {
        for (JsonNode segment : map.get("root").get("children")) {
            if (compartment.getProjectCompartmentUUID().toString().equals(segment.get("compartmentId").asText())) {
                return segment;
            }
        }
        return null;
    }

    @Test
    void unconnectedProjectHasEmptyEdgesAndVersionOne() {
        JIPipeProject project = new JIPipeProject();
        project.addCompartment("A");
        project.addCompartment("B");

        JsonNode map = getPipelineMap(project);

        assertEquals(1, map.get("version").asInt(), "the enriched map is version 1");
        assertTrue(map.get("edges").isArray(), "edges is an array");
        assertEquals(0, map.get("edges").size(), "unconnected compartments produce no edges");
        assertEquals(2, map.get("root").get("children").size(), "one segment per compartment");
        assertFalse(map.get("root").has("totalNodes") || map.get("root").has("cachedNodes"),
                "the root segment keeps no count fields: " + map.get("root"));
        for (JsonNode segment : map.get("root").get("children")) {
            assertTrue(segment.has("totalNodes") && segment.has("cachedNodes"),
                    "every compartment segment carries the count fields: " + segment);
            assertEquals(0, segment.get("totalNodes").asInt());
            assertEquals(0, segment.get("cachedNodes").asInt());
            assertEquals("UNKNOWN", segment.get("status").asText());
        }
    }

    @Test
    void connectedCompartmentsProduceEdges() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");
        JIPipeProjectCompartment b = project.addCompartment("B");
        addOutputSlot(a, "Out");
        project.connectCompartments(a, b);

        JsonNode map = getPipelineMap(project);

        assertEquals(1, map.get("edges").size(), "exactly one wiring edge: " + map.get("edges"));
        JsonNode edge = map.get("edges").get(0);
        assertEquals(a.getProjectCompartmentUUID().toString(), edge.get("source").asText(),
                "the edge source is the source compartment UUID");
        assertEquals(b.getProjectCompartmentUUID().toString(), edge.get("target").asText(),
                "the edge target is the target compartment UUID");
        assertEquals("Out", edge.get("label").asText(),
                "the edge label is the source compartment's output slot name");
    }

    @Test
    void twoOutputSlotsProduceTwoEdges() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");
        JIPipeProjectCompartment b = project.addCompartment("B");
        addOutputSlot(a, "Out1");
        addOutputSlot(a, "Out2");
        project.connectCompartments(a, b);
        // connectCompartments always wires the FIRST output slot (a second call
        // would reuse it and no-op), so the second output is wired directly at
        // the slot level, mirroring what the graph editors do
        project.getCompartmentGraph().connect(a.getOutputSlot("Out2"), b.getFirstInputSlot());

        JsonNode map = getPipelineMap(project);

        assertEquals(2, map.get("edges").size(), "both wired outputs produce an edge: " + map.get("edges"));
        Set<String> labels = new HashSet<>();
        for (JsonNode edge : map.get("edges")) {
            assertEquals(a.getProjectCompartmentUUID().toString(), edge.get("source").asText(),
                    "both edges start at compartment A: " + edge);
            assertEquals(b.getProjectCompartmentUUID().toString(), edge.get("target").asText(),
                    "both edges end at compartment B: " + edge);
            labels.add(edge.get("label").asText());
        }
        assertEquals(Set.of("Out1", "Out2"), labels, "the labels are the two output slot names");
    }

    @Test
    void edgesSkipNonCompartmentVertices() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");
        JIPipeProjectCompartment b = project.addCompartment("B");
        addOutputSlot(a, "Out");
        project.connectCompartments(a, b);
        // An edge whose endpoint is a plain node (not a compartment) inside the
        // compartment graph must be skipped without failing the map
        JIPipeGraphNode intruder = JIPipe.createNode(TEST_NODE_ID);
        project.getCompartmentGraph().insertNode(intruder);
        project.getCompartmentGraph().connect(intruder.getFirstOutputSlot(), b.getFirstInputSlot());

        JsonNode map = getPipelineMap(project);

        assertEquals(1, map.get("edges").size(),
                "only the compartment-to-compartment edge survives: " + map.get("edges"));
        assertEquals("Out", map.get("edges").get(0).get("label").asText(),
                "the surviving edge is the A to B wiring");
    }

    @Test
    void cachedCompartmentIsOkAndCountsAreRight() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");
        JIPipeGraphNode node1 = insertTestNode(project, a);
        JIPipeGraphNode node2 = insertTestNode(project, a);
        storeCache(project, node1);
        storeCache(project, node2);

        JsonNode map = getPipelineMap(project);

        JsonNode segment = compartmentSegment(map, a);
        assertNotNull(segment, "the compartment has a segment");
        assertEquals("OK", segment.get("status").asText(),
                "a fully cached compartment without issues is OK: " + segment);
        assertEquals(2, segment.get("cachedNodes").asInt());
        assertEquals(2, segment.get("totalNodes").asInt());
        assertTrue(segment.get("summary").asText().contains("2 nodes, 2 cached"),
                "the summary reports the cache coverage: " + segment);
    }

    @Test
    void partiallyCachedCompartmentIsWarning() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");
        insertTestNode(project, a);
        JIPipeGraphNode cachedNode = insertTestNode(project, a);
        storeCache(project, cachedNode);

        JsonNode map = getPipelineMap(project);

        JsonNode segment = compartmentSegment(map, a);
        assertNotNull(segment, "the compartment has a segment");
        assertEquals("WARNING", segment.get("status").asText(),
                "a partially cached compartment is WARNING: " + segment);
        assertEquals(1, segment.get("cachedNodes").asInt());
        assertEquals(2, segment.get("totalNodes").asInt());
    }

    @Test
    void emptyCompartmentIsUnknown() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");

        JsonNode map = getPipelineMap(project);

        JsonNode segment = compartmentSegment(map, a);
        assertNotNull(segment, "the compartment has a segment");
        assertEquals("UNKNOWN", segment.get("status").asText(),
                "an empty compartment is UNKNOWN: " + segment);
        assertEquals(0, segment.get("cachedNodes").asInt());
        assertEquals(0, segment.get("totalNodes").asInt());
    }

    private static JsonNode describeProject(JIPipeProject project, String detail) {
        ObjectNode params = mapper.createObjectNode();
        if (detail != null) {
            params.put("detail", detail);
        }
        return new PipelineMapOperations.DescribeProject().execute(createContext(project), params);
    }

    /**
     * Returns the describe_project compartments entry of the compartment (matched by id), or null.
     */
    private static JsonNode compartmentEntry(JsonNode compartments, JIPipeProjectCompartment compartment) {
        for (JsonNode entry : compartments) {
            if (compartment.getProjectCompartmentUUID().toString().equals(entry.get("id").asText())) {
                return entry;
            }
        }
        return null;
    }

    @Test
    void getProjectInfoResponseIsUnchangedByTheExtraction() {
        JIPipeProject project = new JIPipeProject();
        project.getMetadata().setName("Metadata Pin Project");
        project.getMetadata().setDescription(new HTMLText("<p>Pin description</p>"));
        project.getMetadata().setLicense("MIT");
        project.getMetadata().setSummary(new HTMLText("Pin summary"));
        project.getMetadata().setWebsite("https://www.jipipe.org");
        project.getMetadata().setCitation("Pin citation 2026");
        JIPipeAuthorMetadata author = new JIPipeAuthorMetadata();
        author.setFirstName("Jane");
        author.setLastName("Doe");
        project.getMetadata().setAuthors(new JIPipeAuthorMetadata.List(author));
        JIPipeAuthorMetadata acknowledgement = new JIPipeAuthorMetadata();
        acknowledgement.setFirstName("Ack");
        acknowledgement.setLastName("Nowledgement");
        project.getMetadata().setAcknowledgements(new JIPipeAuthorMetadata.List(acknowledgement));

        JsonNode data = new InfoOperations.GetProjectInfo().execute(createContext(project),
                mapper.createObjectNode());

        JsonNode metadata = data.get("metadata");
        assertEquals("Pin description", metadata.get("description").asText());
        assertEquals("MIT", metadata.get("license").asText());
        assertEquals("Pin summary", metadata.get("summary").asText());
        assertEquals("https://www.jipipe.org", metadata.get("website").asText());
        assertEquals("Pin citation 2026", metadata.get("citation").asText());
        JsonNode authors = metadata.get("authors");
        assertEquals(1, authors.size(), "one author: " + authors);
        assertEquals("Jane", authors.get(0).get("firstName").asText());
        assertEquals("Doe", authors.get(0).get("lastName").asText());
        assertFalse(authors.get(0).get("firstAuthor").asBoolean());
        assertFalse(authors.get(0).get("correspondingAuthor").asBoolean());
        assertTrue(authors.get(0).get("affiliations").isArray(), "the author shape stays stable");
        JsonNode acknowledgements = metadata.get("acknowledgements");
        assertEquals(1, acknowledgements.size(), "one acknowledgement: " + acknowledgements);
        assertEquals("Ack", acknowledgements.get(0).get("firstName").asText());
        assertTrue(metadata.get("globalParameters").isObject(),
                "the metadata block ends with the globalParameters object");
    }

    @Test
    void describeProjectSummaryHasAllSections() {
        JIPipeProject project = new JIPipeProject();
        project.getMetadata().setName("Describe Project");
        project.getMetadata().setLicense("MIT");
        JIPipeProjectCompartment a = project.addCompartment("A");
        JIPipeProjectCompartment b = project.addCompartment("B");
        addOutputSlot(a, "Out");
        project.connectCompartments(a, b);
        JIPipeGraphNode nodeA = insertTestNode(project, a);
        JIPipeGraphNode nodeB = insertTestNode(project, a);
        JIPipeGraphNode nodeC = insertTestNode(project, a);
        // The store-to-disk x skip-export matrix through the composite: only
        // nodeA's output slot qualifies (defaults), nodeB skips the export and
        // nodeC has store-to-disk disabled on the slot info
        nodeB.getOutputSlot("Comment").setSkipExport(true);
        nodeC.getOutputSlot("Comment").getInfo().setStoreToDisk(false);

        AutomationContext ctx = createContext(project);
        JsonNode summary = new PipelineMapOperations.DescribeProject().execute(ctx, mapper.createObjectNode());
        JsonNode projectInfo = new InfoOperations.GetProjectInfo().execute(ctx, mapper.createObjectNode());
        JsonNode map = new PipelineMapOperations.GetPipelineMap().execute(ctx, mapper.createObjectNode());

        assertEquals("Describe Project", summary.get("name").asText(),
                "the name uses the GetProjectInfo resolution");
        assertTrue(summary.has("path"), "the path field is present (null for an unsaved project)");

        JsonNode metadata = summary.get("metadata");
        assertEquals("MIT", metadata.get("license").asText(),
                "the metadata block is the shared projectMetadata static: " + metadata);
        assertTrue(metadata.has("authors"), "the metadata block carries the authors array");

        JsonNode compartments = summary.get("compartments");
        assertEquals(2, compartments.size(), "one compartments entry per project compartment");
        JsonNode compartmentA = compartmentEntry(compartments, a);
        JsonNode compartmentB = compartmentEntry(compartments, b);
        assertNotNull(compartmentA);
        assertNotNull(compartmentB);
        for (JsonNode compartment : compartments) {
            assertTrue(compartment.has("id") && compartment.has("name") && compartment.has("nodeCount")
                            && compartment.has("cachedNodes") && compartment.has("totalNodes")
                            && compartment.has("status") && compartment.has("fileWriterCount"),
                    "every compartments entry carries the full summary shape: " + compartment);
            assertFalse(compartment.has("nodes"),
                    "the summary does not carry per-compartment node lists: " + compartment);
        }
        assertEquals(project.getGraph().getNodesWithinCompartment(a.getProjectCompartmentUUID()).size(),
                compartmentA.get("totalNodes").asInt(),
                "totalNodes is the compartment's project-graph node count (the three test nodes "
                        + "plus the compartment output node that the graph maintains for the Out slot)");
        assertEquals(0, compartmentB.get("totalNodes").asInt(), "compartment B is empty");
        // nodeCount agrees with get_project_info's compartments
        assertEquals(projectInfo.get("compartments").size(), compartments.size());
        for (JsonNode infoCompartment : projectInfo.get("compartments")) {
            JsonNode compartment = null;
            for (JsonNode candidate : compartments) {
                if (infoCompartment.get("id").asText().equals(candidate.get("id").asText())) {
                    compartment = candidate;
                    break;
                }
            }
            assertNotNull(compartment, "the compartments entry of " + infoCompartment.get("id").asText());
            assertEquals(infoCompartment.get("nodeCount").asInt(), compartment.get("nodeCount").asInt(),
                    "the nodeCount matches get_project_info for " + infoCompartment.get("id").asText());
        }

        // The wiring is the map's edge list, element for element
        JsonNode wiring = summary.get("wiring");
        assertTrue(wiring.isArray(), "the wiring is an array");
        assertEquals(map.get("edges").size(), wiring.size(),
                "the wiring carries the map's edges: " + wiring);
        for (int i = 0; i < map.get("edges").size(); i++) {
            JsonNode edge = map.get("edges").get(i);
            JsonNode wire = wiring.get(i);
            assertEquals(edge.get("source").asText(), wire.get("source").asText(), "edge " + i + " source");
            assertEquals(edge.get("target").asText(), wire.get("target").asText(), "edge " + i + " target");
            assertEquals(edge.get("label").asText(), wire.get("label").asText(), "edge " + i + " label");
        }

        // The annotations array is the queryAnnotations pass-through: the test
        // nodes are comment nodes, so they must appear
        JsonNode annotations = summary.get("annotations");
        assertTrue(annotations.isArray(), "the annotations section is an array");
        Set<String> annotationIds = new HashSet<>();
        for (JsonNode annotation : annotations) {
            annotationIds.add(annotation.get("id").asText());
        }
        assertTrue(annotationIds.contains(project.getGraph().getAliasIdOf(nodeA)),
                "the annotations carry the comment nodes: " + annotations);

        // The file-writer matrix: only nodeA qualifies
        JsonNode fileWritingNodes = summary.get("fileWritingNodes");
        assertEquals(1, fileWritingNodes.size(), "only nodeA is a file writer: " + fileWritingNodes);
        JsonNode writer = fileWritingNodes.get(0);
        assertEquals(project.getGraph().getAliasIdOf(nodeA), writer.get("id").asText(),
                "the writer id is nodeA's alias: " + writer);
        assertEquals(nodeA.getName(), writer.get("name").asText());
        assertEquals(TEST_NODE_ID, writer.get("typeId").asText());
        assertEquals(a.getProjectCompartmentUUID().toString(), writer.get("compartmentId").asText());
        assertEquals(1, writer.get("outputSlots").size(), "nodeA's qualifying slots: " + writer);
        assertEquals("Comment", writer.get("outputSlots").get(0).asText());
        Set<String> writerIds = new HashSet<>();
        for (JsonNode entry : fileWritingNodes) {
            writerIds.add(entry.get("id").asText());
        }
        assertFalse(writerIds.contains(project.getGraph().getAliasIdOf(nodeB)),
                "the skipExport slot does not qualify");
        assertFalse(writerIds.contains(project.getGraph().getAliasIdOf(nodeC)),
                "the store-to-disk disabled slot does not qualify");

        // The per-compartment fileWriterCount sums to the project-wide list
        int fileWriterCountSum = 0;
        for (JsonNode compartment : compartments) {
            fileWriterCountSum += compartment.get("fileWriterCount").asInt();
        }
        assertEquals(fileWritingNodes.size(), fileWriterCountSum,
                "the fileWriterCount sums match the fileWritingNodes list");
    }

    @Test
    void describeProjectFullAddsNodeLists() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");
        JIPipeGraphNode nodeA = insertTestNode(project, a);
        JIPipeGraphNode nodeB = insertTestNode(project, a);

        JsonNode full = describeProject(project, "full");

        JsonNode compartmentA = compartmentEntry(full.get("compartments"), a);
        assertNotNull(compartmentA);
        JsonNode nodes = compartmentA.get("nodes");
        assertTrue(nodes.isArray(), "the full detail adds the node list: " + compartmentA);
        assertEquals(2, nodes.size(), "the node list covers every node of the compartment: " + nodes);
        Set<String> ids = new HashSet<>();
        for (JsonNode node : nodes) {
            assertTrue(node.has("id") && node.has("name") && node.has("typeId"),
                    "the node entries carry the get_segment_detail shape: " + node);
            assertEquals(TEST_NODE_ID, node.get("typeId").asText());
            ids.add(node.get("id").asText());
        }
        Set<String> expectedIds = new HashSet<>();
        for (JIPipeGraphNode node : project.getGraph().getNodesWithinCompartment(a.getProjectCompartmentUUID())) {
            expectedIds.add(project.getGraph().getAliasIdOf(node));
        }
        assertEquals(expectedIds, ids, "the node list matches the compartment's project-graph nodes");
    }

    @Test
    void describeProjectDetailDefaultsToSummary() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment a = project.addCompartment("A");
        insertTestNode(project, a);

        JsonNode summary = describeProject(project, null);

        assertTrue(summary.get("compartments").size() > 0, "the compartments are present");
        for (JsonNode compartment : summary.get("compartments")) {
            assertFalse(compartment.has("nodes"),
                    "without the detail parameter the node lists are omitted: " + compartment);
        }
    }

    /**
     * Test-only node type used as compartment content and as the plain
     * non-compartment vertex of the edge skip test. It extends the comment
     * node so the fixture inherits valid node plumbing (no workload, no
     * validation issues) and declares one output slot itself (slot annotations
     * are not inherited), so the skip test can wire it inside the compartment
     * graph and the cache tests have a real output slot name to store under.
     */
    @ConfigureJIPipeNode(function = GraphStructuralFunction.class)
    @AddJIPipeOutputSlot(value = JIPipeData.class, name = "Comment", create = true)
    public static class MapTestNode extends JIPipeCommentNode {

        public MapTestNode(JIPipeNodeInfo info) {
            super(info);
        }

        public MapTestNode(MapTestNode other) {
            super(other);
        }
    }
}
