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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conformance of generated JIPX crate metadata against Workflow RO-Crate 1.0
 * and the JIPipe Workflow RO-Crate profile 1.0 (work item #1360).
 * <p>
 * Fixture: {@code /projects/5.3.0-taxonomy-fixtures/basic-images.jipipe}
 * (1 node, {@code ij1-blur-gaussian2d}), loaded via
 * {@link JIPipeProject#loadProject} and written with
 * {@link JIPipeXProjectWriter} - the same seam every save uses.
 * <p>
 * <b>Scope: jipipe-launcher.</b> Only the launcher has all plugin modules on
 * its classpath, so the fixture node actually resolves to a node type.
 * <p>
 * Every {@code conformsTo} assertion is order-independent: the contrib
 * library accumulates extra profile values in a {@code HashSet}, so the
 * serialized array order is not deterministic.
 */
class JIPipeProjectCrateConformanceTest {

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

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static JIPipeProject loadFixture() throws IOException, URISyntaxException {
        Path fixture = Path.of(JIPipeProjectCrateConformanceTest.class.getResource(FIXTURE).toURI());
        return JIPipeProject.loadProject(fixture, new UnspecifiedValidationReportContext(),
                new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
    }

    private Path writeContainer(String fileName) throws IOException, URISyntaxException {
        Path target = tmp.resolve(fileName);
        JIPipeProject project = loadFixture();
        List<String> violations = new ArrayList<>();
        new JIPipeXProjectWriter().writeWithViolations(project, target, JIPipeProgressInfo.SILENT, violations);
        assertTrue(violations.isEmpty(), "A well-formed save must yield zero compliance violations: " + violations);
        return target;
    }

    private JsonNode writeAndReadMetadata(String fileName) throws IOException, URISyntaxException {
        return readZipEntryAsJson(writeContainer(fileName), "ro-crate-metadata.json");
    }

    private static List<String> zipEntryNames(Path zipPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements(); ) {
                entries.add(it.nextElement().getName());
            }
        }
        return entries;
    }

    private static JsonNode readZipEntryAsJson(Path zipPath, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            assertNotNull(entry, "The zip must contain the entry '" + entryName + "'");
            try (InputStream is = zip.getInputStream(entry)) {
                return JsonUtils.getObjectMapper().readValue(is, JsonNode.class);
            }
        }
    }

    /**
     * Finds the graph entity with the given @id, or null.
     */
    private static JsonNode entityWithId(JsonNode graph, String id) {
        assertNotNull(graph, "The crate metadata must have an @graph");
        for (JsonNode entity : graph) {
            if (id.equals(entity.path("@id").asText())) {
                return entity;
            }
        }
        return null;
    }

    /**
     * True when a conformsTo value (single object or array of objects)
     * carries the given @id.
     */
    private static boolean conformsToContains(JsonNode conformsTo, String uri) {
        if (conformsTo == null || conformsTo.isMissingNode() || conformsTo.isNull()) {
            return false;
        }
        if (conformsTo.isArray()) {
            for (JsonNode element : conformsTo) {
                if (uri.equals(element.path("@id").asText())) {
                    return true;
                }
            }
            return false;
        }
        return uri.equals(conformsTo.path("@id").asText());
    }

    private static boolean typesContain(JsonNode typeNode, String type) {
        if (typeNode == null || typeNode.isMissingNode()) {
            return false;
        }
        if (typeNode.isArray()) {
            for (JsonNode element : typeNode) {
                if (type.equals(element.asText())) {
                    return true;
                }
            }
            return false;
        }
        return type.equals(typeNode.asText());
    }

    /**
     * True when the hasPart array (or single @id object) contains the
     * given entity id.
     */
    private static boolean idArrayContains(JsonNode arrayNode, String id) {
        if (arrayNode == null || arrayNode.isMissingNode() || arrayNode.isNull()) {
            return false;
        }
        if (arrayNode.isArray()) {
            for (JsonNode element : arrayNode) {
                if (id.equals(element.path("@id").asText())) {
                    return true;
                }
            }
            return false;
        }
        return id.equals(arrayNode.path("@id").asText());
    }

    // ---------------------------------------------------------------------
    // 1. Profile declaration (profile 1.0 requirement 2)
    // ---------------------------------------------------------------------

    @Test
    void descriptorDeclaresAllThreeProfiles() throws IOException, URISyntaxException {
        JsonNode metadata = writeAndReadMetadata("conformance-descriptor.jipx");
        JsonNode descriptor = entityWithId(metadata.get("@graph"), "ro-crate-metadata.json");
        assertNotNull(descriptor);
        JsonNode conformsTo = descriptor.get("conformsTo");
        assertTrue(conformsToContains(conformsTo, "https://w3id.org/ro/crate/1.1"),
                "descriptor must declare RO-Crate 1.1: " + conformsTo);
        assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.WORKFLOW_RO_CRATE_PROFILE_URI),
                "descriptor must declare the Workflow RO-Crate profile: " + conformsTo);
        assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI),
                "descriptor must declare the JIPipe Workflow RO-Crate profile: " + conformsTo);
    }

    @Test
    void rootDeclaresWorkflowProfiles() throws IOException, URISyntaxException {
        JsonNode metadata = writeAndReadMetadata("conformance-root.jipx");
        JsonNode root = entityWithId(metadata.get("@graph"), "./");
        assertNotNull(root);
        JsonNode conformsTo = root.get("conformsTo");
        assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.WORKFLOW_RO_CRATE_PROFILE_URI),
                "root must declare the Workflow RO-Crate profile: " + conformsTo);
        assertTrue(conformsToContains(conformsTo, JIPipeProjectCrate.JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI),
                "root must declare the JIPipe Workflow RO-Crate profile: " + conformsTo);
    }

    // ---------------------------------------------------------------------
    // 2. Workflow language (profile 1.0 requirement 4)
    // ---------------------------------------------------------------------

    @Test
    void languageEntityCarriesNameVersionAndUrl() throws IOException, URISyntaxException {
        JsonNode metadata = writeAndReadMetadata("conformance-language.jipx");
        JsonNode graph = metadata.get("@graph");
        JsonNode language = entityWithId(graph, "https://jipipe.org/jipipe");
        assertNotNull(language, "the JIPipe ComputerLanguage entity must be present");
        assertEquals("JIPipe", language.get("name").asText());
        assertEquals(JIPipe.getJIPipeVersion(), language.get("version").asText());
        assertEquals("https://jipipe.hki-jena.de/jipipe", language.path("url").path("@id").asText(),
                "the language entity must carry the hosted language page as url");
    }

    // ---------------------------------------------------------------------
    // 3. Main workflow (Workflow RO-Crate 1.0 MUSTs + profile requirement 3)
    // ---------------------------------------------------------------------

    @Test
    void mainWorkflowHasFullWorkflowTypeSet() throws IOException, URISyntaxException {
        JsonNode metadata = writeAndReadMetadata("conformance-main.jipx");
        JsonNode graph = metadata.get("@graph");
        JsonNode mainEntity = entityWithId(graph, "project/graph.json");
        assertNotNull(mainEntity);
        assertTrue(typesContain(mainEntity.get("@type"), "File"));
        assertTrue(typesContain(mainEntity.get("@type"), "SoftwareSourceCode"));
        assertTrue(typesContain(mainEntity.get("@type"), "ComputationalWorkflow"));
        assertEquals("project/graph.json",
                entityWithId(graph, "./").get("mainEntity").path("@id").asText());
        assertNotNull(entityWithId(graph, "./").get("license"), "the crate MUST specify a license");
        assertNotNull(mainEntity.get("programmingLanguage"), "the main workflow MUST have programmingLanguage");
    }

    // ---------------------------------------------------------------------
    // 4. Container structure (profile 1.0 requirements 1, 5, 6)
    // ---------------------------------------------------------------------

    @Test
    void containerCarriesMandatoryRootEntries() throws IOException, URISyntaxException {
        Path jipx = writeContainer("conformance-entries.jipx");
        List<String> entries = zipEntryNames(jipx);
        assertEquals("ro-crate-metadata.json", entries.get(0), "metadata file must be the first zip entry");
        assertTrue(entries.contains("jipx.json"), "the manifest entry must exist");
        assertTrue(entries.contains("README.md"), "the README entry must exist");
        JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
        assertNotNull(entityWithId(metadata.get("@graph"), "README.md"), "the README data entity must be present");

        // Manifest fields (profile 1.0 requirement 5)
        JsonNode manifest = readZipEntryAsJson(jipx, "jipx.json");
        assertEquals(1, manifest.get("format-version").asInt(), "manifest format-version must be 1");
        assertEquals(JIPipe.getJIPipeVersion(), manifest.get("jipipe-version").asText(),
                "the manifest must record the writing JIPipe version");
        assertEquals("project/graph.json", manifest.get("main-graph").asText(),
                "the manifest main-graph must be the main entity path");
        assertEquals("graph", manifest.get("parts").get("project/graph.json").asText(),
                "the parts table must map the main graph path to its part key");
    }

    // ---------------------------------------------------------------------
    // 5. Container inventory (profile 1.0 requirement 7)
    // ---------------------------------------------------------------------

    @Test
    void hasPartInventoriesManifestAndAllParts() throws IOException, URISyntaxException {
        Path jipx = tmp.resolve("conformance-inventory.jipx");
        JIPipeProject project = loadFixture();
        List<String> violations = new ArrayList<>();
        new JIPipeXProjectWriter().writeWithViolations(project, jipx, JIPipeProgressInfo.SILENT, violations);
        assertTrue(violations.isEmpty(), violations::toString);

        JsonNode manifest = readZipEntryAsJson(jipx, "jipx.json");
        JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
        JsonNode root = entityWithId(metadata.get("@graph"), "./");
        JsonNode hasPart = root.get("hasPart");
        assertNotNull(hasPart, "the root entity must carry hasPart");

        assertTrue(idArrayContains(hasPart, "jipx.json"), "the manifest must be listed in hasPart");
        // parts is a JSON object (entry path -> part key); iterate its FIELD NAMES
        manifest.get("parts").fieldNames().forEachRemaining(partPath ->
                assertTrue(idArrayContains(hasPart, partPath),
                        "part " + partPath + " must be listed in hasPart"));
        assertTrue(idArrayContains(hasPart, "README.md"));

        JsonNode manifestEntity = entityWithId(metadata.get("@graph"), "jipx.json");
        assertNotNull(manifestEntity, "the manifest data entity must be present");
        assertEquals("application/json", manifestEntity.get("encodingFormat").asText());
        assertEquals("JIPipe project manifest", manifestEntity.get("name").asText());
    }
}
