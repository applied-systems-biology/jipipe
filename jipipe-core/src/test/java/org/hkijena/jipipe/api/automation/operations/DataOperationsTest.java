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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeTextThumbnailData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the data operations: describe_data and export_data, including the export path guard.
 */
public class DataOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Minimal fixture: a project with one compartment, one node, and one cached data entry in output slot "output".
     */
    private record Fixture(JIPipeProject project, JIPipeGraphNode node) {
    }

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    /**
     * Returns the info of any registered node type that can be instantiated, or null.
     */
    private static JIPipeNodeInfo findInstantiableNodeInfo() {
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            try {
                info.newInstance();
                return info;
            } catch (Exception ignored) {
                // Uninstantiable node types are skipped
            }
        }
        return null;
    }

    private static Fixture createFixture() {
        JIPipeNodeInfo info = findInstantiableNodeInfo();
        assumeTrue(info != null, "no instantiable node type found in this environment");
        JIPipeProject project = new JIPipeProject();
        var compartment = project.addCompartment("Data Test Compartment");
        JIPipeGraphNode node = info.newInstance();
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        JIPipeOutputDataSlot table = new JIPipeOutputDataSlot(
                new JIPipeDataSlotInfo(JIPipeTextThumbnailData.class, JIPipeSlotType.Output, "output", ""), node);
        table.addData(new JIPipeTextThumbnailData("hello mcp"), new JIPipeProgressInfo());
        project.getCache().store(node, node.getUUIDInParentGraph(), table, "output", new JIPipeProgressInfo());
        JIPipeOutputDataSlot emptyTable = new JIPipeOutputDataSlot(
                new JIPipeDataSlotInfo(JIPipeTextThumbnailData.class, JIPipeSlotType.Output, "empty-output", ""), node);
        project.getCache().store(node, node.getUUIDInParentGraph(), emptyTable, "empty-output", new JIPipeProgressInfo());
        return new Fixture(project, node);
    }

    private static ObjectNode describeParams(Fixture fixture, String compartment, String node, String slot) {
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment", compartment != null ? compartment : "Data Test Compartment");
        params.put("node", node != null ? node : fixture.project().getGraph().getAliasIdOf(fixture.node()));
        params.put("slot", slot != null ? slot : "output");
        return params;
    }

    private static ObjectNode exportParams(Fixture fixture, String format, String targetDir) {
        ObjectNode params = describeParams(fixture, null, null, null);
        params.put("format", format);
        if (targetDir != null) {
            params.put("target-dir", targetDir);
        }
        return params;
    }

    @Test
    void describeDataReturnsErrorForUnknownNode() {
        Fixture fixture = createFixture();
        ObjectNode params = describeParams(fixture, null, "no-such-node-alias", null);
        JsonNode result = new DataOperations.DescribeData().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown node must return an error field");
    }

    @Test
    void describeDataReturnsErrorForUnknownCompartment() {
        Fixture fixture = createFixture();
        ObjectNode params = describeParams(fixture, "No Such Compartment", null, null);
        JsonNode result = new DataOperations.DescribeData().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown compartment must return an error field");
    }

    @Test
    void describeDataReturnsErrorForUnknownSlot() {
        Fixture fixture = createFixture();
        ObjectNode params = describeParams(fixture, null, null, "no-such-slot");
        JsonNode result = new DataOperations.DescribeData().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown slot must return an error field");
    }

    @Test
    void describeDataReturnsErrorForMissingCache() {
        Fixture fixture = createFixture();
        ObjectNode params = describeParams(fixture, null, null, "empty-output");
        JsonNode result = new DataOperations.DescribeData().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "a cached but empty slot must return an error field");
    }

    @Test
    void describeDataReturnsErrorForEntryOutOfRange() {
        Fixture fixture = createFixture();
        ObjectNode params = describeParams(fixture, null, null, null);
        params.put("entry", 5);
        JsonNode result = new DataOperations.DescribeData().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an out-of-range entry index must return an error field");
        assertTrue(result.get("error").asText().contains("out of range"), "the error must state the range problem");
    }

    @Test
    void describeDataReturnsTypeInfoAndPreviewForCachedEntry() throws IOException {
        Fixture fixture = createFixture();
        JsonNode result = new DataOperations.DescribeData().execute(createContext(fixture.project()),
                describeParams(fixture, null, null, null));

        assertFalse(result.has("error"), "a cached entry must not produce an error");
        assertEquals("jipipe:thumbnail-text", result.get("dataType").asText());
        assertEquals("Text thumbnail", result.get("dataTypeDisplayName").asText());
        assertEquals(1, result.get("entryCount").asInt());
        assertEquals(0, result.get("entry").asInt());
        assertTrue(result.has("image-base64"), "a thumbnail-capable data type must produce a preview envelope");
        assertEquals("image/png", result.get("mime").asText());
        byte[] decoded = Base64.getDecoder().decode(result.get("image-base64").asText());
        assertTrue(decoded.length > 8, "the preview must be a non-empty PNG");
        assertEquals((byte) 0x89, decoded[0], "PNG magic bytes expected");
        assertEquals('P', decoded[1], "PNG magic bytes expected");
        assertEquals('N', decoded[2], "PNG magic bytes expected");
        assertEquals('G', decoded[3], "PNG magic bytes expected");
    }

    @Test
    void exportDataWritesFileInsideSystemExportRoot() throws IOException {
        Fixture fixture = createFixture();
        Path target = Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export", "export-out-test");
        JsonNode result = new DataOperations.ExportData().execute(createContext(fixture.project()),
                exportParams(fixture, "jipipe", target.toString()));

        assertFalse(result.has("error"), () -> "an in-root target must be accepted: " + result);
        assertEquals("jipipe:thumbnail-text", result.get("dataType").asText());
        assertTrue(Files.isDirectory(target), "the export target directory must exist");
        try (Stream<Path> files = Files.list(target)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().endsWith(".txt")),
                    "the exported data must be written as a file");
        }
        cleanup(target);
    }

    @Test
    void exportDataDefaultsToSystemExportDirectory() throws IOException {
        Fixture fixture = createFixture();
        Path defaultRoot = Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export");
        JsonNode result = new DataOperations.ExportData().execute(createContext(fixture.project()),
                exportParams(fixture, "jipipe", null));

        assertFalse(result.has("error"), () -> "the default target must be accepted: " + result);
        assertEquals(defaultRoot.toAbsolutePath().normalize().toString(), result.get("saved-to").asText());
        try (Stream<Path> files = Files.list(defaultRoot)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().endsWith(".txt")),
                    "the exported data must be written into the default directory");
        }
    }

    @Test
    void exportDataRefusesTargetOutsideAllowedRoots() throws IOException {
        Fixture fixture = createFixture();
        Path outside = Files.createTempDirectory("mcp-export-forbidden");
        JsonNode result = new DataOperations.ExportData().execute(createContext(fixture.project()),
                exportParams(fixture, "jipipe", outside.toString()));

        assertTrue(result.has("error"), "a target outside the allowed roots must be refused");
        assertTrue(result.get("error").asText().contains("allowed"),
                "the error must mention the allowed roots");
    }

    @Test
    void exportDataRefusesTraversalEscapeFromAllowedRoot() {
        Fixture fixture = createFixture();
        Path inside = Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export");
        Path traversal = inside.resolve("..").resolve("..").resolve("escaped-via-traversal");
        JsonNode result = new DataOperations.ExportData().execute(createContext(fixture.project()),
                exportParams(fixture, "jipipe", traversal.toString()));

        assertTrue(result.has("error"), "a '..' traversal out of an allowed root must be refused");
        assertTrue(result.get("error").asText().contains("allowed"),
                "the error must mention the allowed roots");
    }

    /**
     * Deletes a directory tree, ignoring failures (best-effort test cleanup).
     */
    private static void cleanup(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> entries = Files.walk(path)) {
            entries.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // Best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup
        }
    }

    @Test
    void exportDataResolvesFormatCaseInsensitively() throws IOException {
        Fixture fixture = createFixture();
        Path target = Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export", "export-case-insensitive");
        JsonNode result = new DataOperations.ExportData().execute(createContext(fixture.project()),
                exportParams(fixture, "TEXT THUMBNAIL", target.toString()));

        assertFalse(result.has("error"), () -> "format matching must be case-insensitive: " + result);
        assertTrue(Files.isDirectory(target));
        cleanup(target);
    }

    @Test
    void exportDataRejectsUnknownFormat() {
        Fixture fixture = createFixture();
        JsonNode result = new DataOperations.ExportData().execute(createContext(fixture.project()),
                exportParams(fixture, "no-such-format", null));

        assertTrue(result.has("error"), "an unmatched format must return an error field");
        assertTrue(result.get("error").asText().contains("jipipe"),
                "the error must list the accepted formats");
    }

    @Test
    void dataOperationsAreRegistered() {
        AutomationOperationRegistry registry = JIPipe.getInstance().getAutomation().getOperationRegistry();
        assertTrue(registry.has("describe_data"), "describe_data must be registered");
        assertTrue(registry.has("export_data"), "export_data must be registered");
    }
}
