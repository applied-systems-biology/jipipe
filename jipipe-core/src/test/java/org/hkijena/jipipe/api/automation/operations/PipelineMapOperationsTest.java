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
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphDebugFunction;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
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

    /**
     * Test-only node type used as compartment content and as the plain
     * non-compartment vertex of the edge skip test. It extends the comment
     * node so the fixture inherits valid node plumbing (no workload, no
     * validation issues) and declares one output slot itself (slot annotations
     * are not inherited), so the skip test can wire it inside the compartment
     * graph and the cache tests have a real output slot name to store under.
     */
    @ConfigureJIPipeNode(function = GraphDebugFunction.class)
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
