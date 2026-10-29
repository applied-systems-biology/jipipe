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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JIPipeXManifest} (JIPX container manifest) and the
 * local project workspace's "payload never regenerated" idempotency
 * contract (spec §5.4).
 * <p>
 * <b>Scope: jipipe-launcher.</b> Same reason as
 * {@code FixtureProjectsLoadTest}: only the launcher has all plugin modules
 * on its classpath, so the fixture node actually resolves to a node type.
 */
class JIPipeXManifestTest {

    private static final String FIXTURE = "/projects/5.3.0-taxonomy-fixtures/tables.jipipe";

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe.
        JIPipe.ensureInstance();
    }

    @Test
    void defaultManifestRoundTrips() throws IOException {
        JIPipeXManifest manifest = JIPipeXManifest.defaultManifest("test-version");

        // (1) Fields
        assertEquals(JIPipeXManifest.FORMAT_VERSION, manifest.getFormatVersion(),
                "Default manifest must use format version " + JIPipeXManifest.FORMAT_VERSION);
        assertEquals(1, JIPipeXManifest.FORMAT_VERSION, "FORMAT_VERSION must be 1");
        assertEquals("test-version", manifest.getJipipeVersion());
        assertEquals(JIPipeXManifest.MAIN_GRAPH, manifest.getMainGraph(),
                "Default manifest must use the main-graph constant");
        assertEquals("project/graph.json", JIPipeXManifest.MAIN_GRAPH, "MAIN_GRAPH constant value");
        assertTrue(manifest.getUnknown().isEmpty(), "Default manifest has no unknown parts");

        // (2) The 9 default parts: every part key maps to project/<key>.json
        // with ':' replaced by '-' in the FILE name only (the value keeps
        // the real part key).
        assertEquals(JIPipeProjectParts.PART_KEYS.size(), manifest.getParts().size(),
                "Default manifest must list all 9 parts");
        for (String key : JIPipeProjectParts.PART_KEYS) {
            String expectedPath = "project/" + key.replace(':', '-') + ".json";
            assertEquals(key, manifest.getParts().get(expectedPath),
                    "Part file " + expectedPath + " must map to key '" + key + "'");
        }
        assertEquals("project/jipipe-doc-parameters.json",
                manifest.getParts().keySet().stream().filter(k -> k.contains("jipipe-doc-parameters")).findFirst().orElse(""),
                "Part file naming: jipipe:doc:parameters -> project/jipipe-doc-parameters.json");
        assertEquals("jipipe:doc:parameters", manifest.getParts().get("project/jipipe-doc-parameters.json"),
                "The manifest value keeps the real part key");

        // (3) Round-trip through the JSON tree: the manifest is identical
        // after toJsonNode() -> fromJsonNode().
        JsonNode jsonNode = manifest.toJsonNode();
        JIPipeXManifest roundTripped = JIPipeXManifest.fromJsonNode(jsonNode);
        assertEquals(manifest.getFormatVersion(), roundTripped.getFormatVersion());
        assertEquals(manifest.getJipipeVersion(), roundTripped.getJipipeVersion());
        assertEquals(manifest.getMainGraph(), roundTripped.getMainGraph());
        assertEquals(manifest.getParts(), roundTripped.getParts(),
                "Parts table must survive the JSON round-trip");
        assertEquals(manifest.getUnknown(), roundTripped.getUnknown(),
                "Unknown map must survive the JSON round-trip");
    }

    @Test
    void unknownPartsArePreservedOnRoundTrip() throws IOException {
        JIPipeXManifest manifest = JIPipeXManifest.defaultManifest("test-version");
        manifest.getUnknown().put("data/foo.tiff", "data/foo.tiff");

        JIPipeXManifest roundTripped = JIPipeXManifest.fromJsonNode(manifest.toJsonNode());
        assertEquals("data/foo.tiff", roundTripped.getUnknown().get("data/foo.tiff"),
                "Unknown parts must be preserved verbatim on round-trip");
        assertEquals(1, roundTripped.getUnknown().size());
    }

    @Test
    void materializationIsIdempotentAndNeverDeletesPayloads(@TempDir Path workDir) throws IOException, URISyntaxException {
        // Load the fixture as a project with a work directory (the temp
        // base - and thus the workspace - must live under it, per the
        // getTemporaryBaseDirectory() memoization ruling).
        UnspecifiedValidationReportContext ctx = new UnspecifiedValidationReportContext();
        Path path = Path.of(JIPipeXManifestTest.class.getResource(FIXTURE).toURI());
        JIPipeProject project = JIPipeProject.loadProject(path, ctx, new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        project.setWorkDirectory(workDir);

        // Non-vacuity: the loaded graph must have 1+ nodes.
        Set<UUID> nodeUuids = project.getGraph().getGraphNodeUUIDs();
        assertFalse(nodeUuids.isEmpty(),
                "Fixture loaded 0 nodes - table-add-columns-generate not resolved; test would be vacuous");

        JIPipeProjectWorkspace workspace = project.getWorkspace();
        assertTrue(Files.exists(workspace.getBaseDirectory()), "getWorkspace() must create the base directory");

        // First materialization: jipx.json + every listed part file exist.
        // The manifest lists exactly the parts present in the project's
        // slice - a bare project without optional parts (this fixture has
        // no additional metadata) materializes fewer files than the full
        // 9-part default.
        workspace.materializeFromModel(JIPipeProgressInfo.SILENT);
        JIPipeXManifest manifest = workspace.getManifest();
        assertEquals(JIPipeXManifest.FORMAT_VERSION, manifest.getFormatVersion());
        assertEquals(JIPipe.getJIPipeVersion(), manifest.getJipipeVersion(),
                "Materialized manifest must carry the current JIPipe version");
        Map<String, JsonNode> slice = JIPipeProjectParts.sliceParts(project);
        assertEquals(slice.size(), manifest.getParts().size(),
                "The manifest must list exactly the parts present in the slice");
        for (String entryPath : manifest.getParts().keySet()) {
            assertTrue(Files.isRegularFile(workspace.getBaseDirectory().resolve(entryPath)),
                    "Part file " + entryPath + " must exist after materialization");
        }

        // The workspace.json state: origin-format derived from the project
        // file extension (.jipipe fixture -> "jip"), not dirty after
        // materialization, no origin-path.
        ObjectNode state = (ObjectNode) JsonUtils.getObjectMapper()
                .readValue(workspace.getBaseDirectory().resolve("workspace.json").toFile(), JsonNode.class);
        assertEquals("jip", state.path("origin-format").asText(),
                "Legacy (non-.jipx) project file must yield origin-format 'jip'");
        assertEquals("", state.path("origin-path").asText());
        assertFalse(state.path("dirty").asBoolean(),
                "Workspace must not be dirty right after materialization");
        assertFalse(state.path("last-save").asText().isEmpty(), "last-save must be set");

        // Idempotency: a payload file placed in the workspace survives
        // re-materialization (the "payload never regenerated" contract).
        Path payloadDir = workspace.getBaseDirectory().resolve("data");
        PathUtils.createDirectories(payloadDir);
        Path payload = payloadDir.resolve("foo.tiff");
        Files.writeString(payload, "payload content v1");

        workspace.materializeFromModel(JIPipeProgressInfo.SILENT);
        assertTrue(Files.isRegularFile(payload),
                "Re-materialization must never delete unknown/payload files");
        assertEquals("payload content v1", Files.readString(payload),
                "Re-materialization must never rewrite payload files");
        for (String entryPath : manifest.getParts().keySet()) {
            assertTrue(Files.isRegularFile(workspace.getBaseDirectory().resolve(entryPath)),
                    "Part file " + entryPath + " must still exist after re-materialization");
        }

        // State transitions: markDirty() -> dirty=true; markSaved(path)
        // -> dirty=false, origin-path set, origin-format retained.
        workspace.markDirty();
        state = (ObjectNode) JsonUtils.getObjectMapper()
                .readValue(workspace.getBaseDirectory().resolve("workspace.json").toFile(), JsonNode.class);
        assertTrue(state.path("dirty").asBoolean(), "markDirty() must set dirty=true");

        workspace.markSaved(path);
        state = (ObjectNode) JsonUtils.getObjectMapper()
                .readValue(workspace.getBaseDirectory().resolve("workspace.json").toFile(), JsonNode.class);
        assertFalse(state.path("dirty").asBoolean(), "markSaved() must clear dirty");
        assertEquals(path.toString(), state.path("origin-path").asText(),
                "markSaved() must record the origin path");
        assertEquals("jip", state.path("origin-format").asText(),
                "markSaved() must not touch the origin format");
    }

    @Test
    void unknownManifestEntriesSurviveMaterializeOverExtractedWorkspace(@TempDir Path workDir) throws IOException, URISyntaxException {
        // The spec §4 contract: "Unknown parts are preserved verbatim on
        // round-trip." A foreign/future .jipx carrying custom entries is
        // extracted into the workspace; a re-save re-materializes the
        // model. The payload files survive (only-compress), but their
        // manifest registration must survive too - otherwise they become
        // unlisted duplicates in the re-saved container.
        UnspecifiedValidationReportContext ctx = new UnspecifiedValidationReportContext();
        Path path = Path.of(JIPipeXManifestTest.class.getResource(FIXTURE).toURI());
        JIPipeProject project = JIPipeProject.loadProject(path, ctx, new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        project.setWorkDirectory(workDir);

        JIPipeProjectWorkspace workspace = project.getWorkspace();
        Path baseDir = workspace.getBaseDirectory();

        // (1) Fresh materialization (no unknown entries yet).
        workspace.materializeFromModel(JIPipeProgressInfo.SILENT);
        assertTrue(workspace.getManifest().getUnknown().isEmpty(),
                "Fresh materialization must have no unknown entries");

        // (2) Simulate the EXTRACTION of a FOREIGN .jipx into this
        // workspace: a container with a manifest carrying an unknown
        // entry plus the payload file (the real reader path:
        // extractFromZip resets the manifest cache).
        Path foreignZip = workDir.resolve("foreign.jipx");
        try (java.util.zip.ZipOutputStream zos =
                     new java.util.zip.ZipOutputStream(Files.newOutputStream(foreignZip))) {
            JIPipeXManifest foreignManifest = JIPipeXManifest.defaultManifest("foreign-version");
            foreignManifest.getUnknown().put("data/foo.tiff", "data/foo.tiff");
            zos.putNextEntry(new java.util.zip.ZipEntry(JIPipeXManifest.MANIFEST_FILE_NAME));
            zos.write(foreignManifest.toJsonNode().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new java.util.zip.ZipEntry("data/foo.tiff"));
            zos.write("foreign payload".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        workspace.extractFromZip(foreignZip, JIPipeProgressInfo.SILENT);
        assertEquals("data/foo.tiff", workspace.getManifest().getUnknown().get("data/foo.tiff"),
                "The extracted foreign manifest must carry the unknown entry");

        // (3) Re-save = re-materialize from the model over the extracted
        // workspace.
        workspace.materializeFromModel(JIPipeProgressInfo.SILENT);

        // (4) The unknown entry must still be registered in the manifest
        // AND the payload file must be intact.
        Path payload = baseDir.resolve("data/foo.tiff");
        JIPipeXManifest result = workspace.getManifest();
        assertEquals("data/foo.tiff", result.getUnknown().get("data/foo.tiff"),
                "Unknown manifest entries from the extracted workspace must survive re-materialization");
        assertTrue(Files.isRegularFile(payload),
                "The foreign payload file must survive re-materialization");
        assertEquals("foreign payload", Files.readString(payload),
                "The foreign payload file must be rewritten verbatim (only-compress)");
    }

    @Test
    void readPartsReturnsEveryListedPart(@TempDir Path workDir) throws IOException, URISyntaxException {
        UnspecifiedValidationReportContext ctx = new UnspecifiedValidationReportContext();
        Path path = Path.of(JIPipeXManifestTest.class.getResource(FIXTURE).toURI());
        JIPipeProject project = JIPipeProject.loadProject(path, ctx, new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        project.setWorkDirectory(workDir);

        JIPipeProjectWorkspace workspace = project.getWorkspace();
        workspace.materializeFromModel(JIPipeProgressInfo.SILENT);

        Map<String, JsonNode> parts = workspace.readParts();
        Map<String, JsonNode> expected = JIPipeProjectParts.sliceParts(project);
        assertEquals(expected.keySet(), parts.keySet(),
                "readParts() must return exactly the listed part keys");
        for (Map.Entry<String, JsonNode> entry : expected.entrySet()) {
            if ("dependencies".equals(entry.getKey())) {
                continue; // HashSet-serialized dependencies: order-insensitive (see JIPipeProjectPartsTest)
            }
            assertEquals(entry.getValue(), parts.get(entry.getKey()),
                    "Part '" + entry.getKey() + "' must be byte-for-byte the monolith field");
        }

        // A listed part file that is missing (corrupt container) must throw
        // a clear IOException.
        Path graphPart = workspace.getBaseDirectory().resolve("project/graph.json");
        assertTrue(Files.deleteIfExists(graphPart));
        try {
            workspace.readParts();
            assertFalse(true, "readParts() must throw when a listed part file is missing");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("graph.json") || e.getMessage().contains("part"),
                    "The error must name the missing part: " + e.getMessage());
        }
    }

    @Test
    void getWorkspaceReturnsNullForDisposedProject(@TempDir Path workDir) throws IOException, URISyntaxException {
        UnspecifiedValidationReportContext ctx = new UnspecifiedValidationReportContext();
        Path path = Path.of(JIPipeXManifestTest.class.getResource(FIXTURE).toURI());
        JIPipeProject project = JIPipeProject.loadProject(path, ctx, new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        project.setWorkDirectory(workDir);
        project.close(JIPipeProgressInfo.SILENT);
        assertEquals(null, project.getWorkspace(),
                "getWorkspace() must return null for disposed projects");
    }
}
