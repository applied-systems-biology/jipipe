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
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.io.JIPipeLegacyProjectReader;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JIPipeXProjectReader}, the JIPX container reader.
 * <p>
 * Fixture: {@code /projects/5.3.0-taxonomy-fixtures/basic-images.jipipe}
 * (1 node, {@code ij1-blur-gaussian2d}). A throwaway {@code .jipx} container
 * is built from it with {@link JIPipeProjectParts} +
 * {@link JIPipeXManifest#defaultManifest(String)} + zip4j; the reader must
 * load it to a model identical to the legacy load of the same fixture.
 * <p>
 * <b>Scope: jipipe-launcher.</b> Only the launcher has all plugin modules on
 * its classpath, so the fixture node actually resolves to a node type (the
 * same reason as {@code FixtureProjectsLoadTest}).
 */
class JIPipeXProjectReaderTest {

    private static final String FIXTURE = "/projects/5.3.0-taxonomy-fixtures/basic-images.jipipe";

    @TempDir
    Path tmp;

    @BeforeAll
    static void init() {
        // The ImageJLegacyPatcherLauncherSessionListener registered in this
        // module preinits the ImageJ1 legacy patcher before any test class
        // loads, so ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();
    }

    @Test
    void readsJipxContainer() throws IOException, URISyntaxException {
        Path jipx = buildJipxFromFixture(tmp.resolve("basic-images.jipx"));

        JIPipeProject project = new JIPipeXProjectReader().read(jipx,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);

        assertEquals(jipx, project.getProjectFile());
        assertEquals(jipx.getParent(), project.getWorkDirectory());
        // Non-vacuity: the fixture's node must have resolved and loaded.
        assertEquals(1, project.getGraph().traverse().size());
        JIPipeGraphNode node = project.getGraph().traverse().iterator().next();
        assertEquals("ij1-blur-gaussian2d", node.getInfo().getId());
    }

    @Test
    void readJipxIsModelEquivalentToLegacyLoad() throws IOException, URISyntaxException {
        Path fixture = fixturePath();
        Path jipx = buildJipxFromFixture(tmp.resolve("equivalence.jipx"));

        JIPipeProject legacy = new JIPipeLegacyProjectReader().read(fixture,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        JIPipeProject jipxProject = new JIPipeXProjectReader().read(jipx,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);

        assertEquals(nodeIdMapping(legacy.getGraph()), nodeIdMapping(jipxProject.getGraph()),
                "JIPX load is not model-equivalent to the legacy load of the same fixture");
    }

    @Test
    void missingJipxManifestIsRejectedWithClearMessage() throws IOException, URISyntaxException {
        // A zip that contains project parts but no jipx.json is not a JIPipe
        // 6 project (this is also how non-JIPX zips, e.g. crate files, are
        // rejected during read).
        Path jipx = tmp.resolve("no-manifest.jipx");
        Map<String, JsonNode> parts = loadFixtureParts();
        Path staging = tmp.resolve("no-manifest-staging");
        writeParts(staging, parts);
        try (ZipFile zip = new ZipFile(jipx.toFile())) {
            for (Path partFile : listFiles(staging)) {
                zip.addFile(partFile.toFile(), fileNameInZipParams(staging, partFile));
            }
        }
        assertFalse(Files.exists(staging.resolve("jipx.json")));

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> new JIPipeXProjectReader().read(jipx,
                        new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                        new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT));
        assertTrue(e.getMessage().contains("jipx.json"),
                "Expected the exception to name the missing manifest, got: " + e.getMessage());
    }

    @Test
    void corruptContainerNamesTheMissingPart() throws IOException, URISyntaxException {
        // The manifest lists project/metadata.json, but the zip omits it:
        // the reader must fail naming the missing entry (corrupt container).
        Path jipx = tmp.resolve("missing-part.jipx");
        Map<String, JsonNode> parts = loadFixtureParts();
        Path staging = tmp.resolve("missing-part-staging");
        writeParts(staging, parts);
        JIPipeXManifest manifest = JIPipeXManifest.defaultManifest(JIPipe.getJIPipeVersion());
        writeJson(staging.resolve(JIPipeXManifest.MANIFEST_FILE_NAME), manifest.toJsonNode());
        Files.delete(staging.resolve("project/metadata.json"));
        try (ZipFile zip = new ZipFile(jipx.toFile())) {
            for (Path partFile : listFiles(staging)) {
                zip.addFile(partFile.toFile(), fileNameInZipParams(staging, partFile));
            }
        }

        // A part listed in the manifest but absent from the container is a
        // corrupt container: the workspace's IOException names the missing
        // entry and propagates unchecked-to-checked through read().
        IOException e = assertThrows(IOException.class,
                () -> new JIPipeXProjectReader().read(jipx,
                        new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                        new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT));
        assertTrue(e.getMessage().contains("project/metadata.json"),
                "Expected the exception to name the missing part entry, got: " + e.getMessage());
    }

    @Test
    void readerSupportsOnlyJipxZipFiles() throws IOException, URISyntaxException {
        JIPipeXProjectReader reader = new JIPipeXProjectReader();
        assertEquals("org.hkijena.jipipe.core:jipx", reader.getId());
        assertEquals(java.util.List.of("jipx"), reader.getFileExtensions());

        Path jipx = buildJipxFromFixture(tmp.resolve("supports.jipx"));
        assertTrue(reader.supports(jipx));

        Path fixture = fixturePath();
        assertFalse(reader.supports(fixture), "A plain .jip file must not be supported by the JIPX reader");

        Path nonZip = tmp.resolve("fake.jipx");
        Files.writeString(nonZip, "{ \"metadata\": {} }");
        assertFalse(reader.supports(nonZip), "A non-ZIP .jipx file must not be supported by the JIPX reader");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Path fixturePath() throws URISyntaxException {
        return Path.of(JIPipeXProjectReaderTest.class.getResource(FIXTURE).toURI());
    }

    private static Map<String, JsonNode> loadFixtureParts() throws IOException, URISyntaxException {
        UnspecifiedValidationReportContext ctx = new UnspecifiedValidationReportContext();
        JIPipeProject fixtureProject = JIPipeProject.loadProject(fixturePath(), ctx,
                new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        return JIPipeProjectParts.sliceParts(fixtureProject);
    }

    /**
     * Builds a throwaway JIPX container from the basic-images fixture:
     * slices the fixture's parts, writes them under {@code project/} at the
     * default manifest file names, writes {@code jipx.json} (the default
     * manifest) at the container root, and zips the result with NO wrapper
     * root directory. (Test-local — the production writer in Task 4 does
     * not reuse this helper.)
     */
    private static Path buildJipxFromFixture(Path jipxPath) throws IOException, URISyntaxException {
        Map<String, JsonNode> parts = loadFixtureParts();
        Path staging = jipxPath.getParent().resolve(jipxPath.getFileName() + "-staging");
        writeParts(staging, parts);
        JIPipeXManifest manifest = JIPipeXManifest.defaultManifest(JIPipe.getJIPipeVersion());
        // Only list the parts actually present in the fixture's slice.
        manifest.getParts().entrySet().removeIf(e -> !parts.containsKey(e.getValue()));
        writeJson(staging.resolve(JIPipeXManifest.MANIFEST_FILE_NAME), manifest.toJsonNode());
        try (ZipFile zip = new ZipFile(jipxPath.toFile())) {
            for (Path entryFile : listFiles(staging)) {
                zip.addFile(entryFile.toFile(), fileNameInZipParams(staging, entryFile));
            }
        }
        return jipxPath;
    }

    private static void writeParts(Path staging, Map<String, JsonNode> parts) throws IOException {
        Files.createDirectories(staging);
        for (Map.Entry<String, JsonNode> entry : parts.entrySet()) {
            Path partFile = staging.resolve("project").resolve(entry.getKey().replace(':', '-') + ".json");
            Files.createDirectories(partFile.getParent());
            writeJson(partFile, entry.getValue());
        }
    }

    private static List<Path> listFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).sorted().forEach(files::add);
        }
        return files;
    }

    /**
     * Zip parameters that store the file under its path relative to the
     * staging root — zip4j's plain {@code addFile} would strip the parent
     * directory from the entry name.
     */
    private static ZipParameters fileNameInZipParams(Path staging, Path file) {
        ZipParameters parameters = new ZipParameters();
        parameters.setFileNameInZip(
                staging.relativize(file).toString().replace(java.io.File.separatorChar, '/'));
        return parameters;
    }

    private static void writeJson(Path file, JsonNode node) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), node);
    }

    private static Map<UUID, String> nodeIdMapping(JIPipeGraph graph) {
        Map<UUID, String> mapping = new HashMap<>();
        for (JIPipeGraphNode n : graph.getGraphNodes()) {
            mapping.put(n.getUUIDInParentGraph(), n.getInfo().getId());
        }
        return mapping;
    }
}
