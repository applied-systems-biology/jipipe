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
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.io.JIPipeProjectIOs;
import org.hkijena.jipipe.api.project.io.JIPipeProjectWriter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectIOApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JIPipeXProjectWriter}, the JIPX container writer.
 * <p>
 * Fixture: {@code /projects/5.3.0-taxonomy-fixtures/basic-images.jipipe}
 * (1 node, {@code ij1-blur-gaussian2d}), loaded via
 * {@link JIPipeProject#loadProject} — the same content-first read dispatch
 * the whole code base uses. The writer is exercised through both its own
 * seam ({@link JIPipeXProjectWriter#write} / {@code writeWithViolations})
 * and the legacy API ({@link JIPipeProject#saveProject(Path, boolean)} →
 * {@link JIPipeProjectIOs#writerFromPath} dispatch).
 * <p>
 * <b>Scope: jipipe-launcher.</b> Only the launcher has all plugin modules on
 * its classpath, so the fixture node actually resolves to a node type (the
 * same reason as {@code FixtureProjectsLoadTest}).
 * <p>
 * <b>ZIP contract under test (spec §6):</b> the production writer uses
 * {@code java.util.zip} only — the first entry is
 * {@code ro-crate-metadata.json} (Workflow RO-Crate root convention),
 * then {@code jipx.json}, then {@code README.md}, then the part files in
 * manifest parts-table order, then any unknown/payload entries sorted by
 * path. Entry names are the workspace-relative paths (no wrapper root).
 */
class JIPipeXProjectWriterTest {

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
    // 1. Happy path + round-trip
    // ---------------------------------------------------------------------

    @Test
    void writesContainerWithCorrectEntriesAndRoundTrips() throws IOException, URISyntaxException {
        Path jipx = tmp.resolve("p.jipx");
        JIPipeProject project = loadFixture();

        new JIPipeXProjectWriter().write(project, jipx, JIPipeProgressInfo.SILENT);

        assertTrue(Files.isRegularFile(jipx), "The written container must exist: " + jipx);
        List<String> entries = zipEntryNames(jipx);
        assertTrue(entries.contains("jipx.json"), "jipx.json missing; entries: " + entries);
        assertTrue(entries.contains("project/graph.json"), "project/graph.json missing; entries: " + entries);
        assertTrue(entries.contains("ro-crate-metadata.json"), "ro-crate-metadata.json missing; entries: " + entries);
        assertTrue(entries.contains("README.md"), "README.md missing; entries: " + entries);
        // Dropped-in-JIPX fields must not be present (spec §4).
        assertFalse(entries.contains("external-environments.json"),
                "external-environments.json must not be in a JIPX container: " + entries);
        for (String entry : entries) {
            assertFalse(entry.startsWith("project/external-"),
                    "external-* entries must not be in a JIPX container: " + entry);
        }
        // Workflow RO-Crate root convention: the metadata file comes first.
        assertEquals("ro-crate-metadata.json", entries.get(0),
                "The first zip entry must be ro-crate-metadata.json; got: " + entries);
        // No wrapper root: every entry is a workspace-relative path.
        for (String entry : entries) {
            assertFalse(entry.startsWith("/"), "Entry must not start with '/': " + entry);
        }

        // Round-trip: the container loads to the same model as the fixture.
        JIPipeProject reloaded = JIPipeProject.loadProject(jipx, new UnspecifiedValidationReportContext(),
                new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        assertEquals(jipx, reloaded.getProjectFile());
        // Non-vacuity: the fixture's node must have resolved and loaded.
        assertEquals(1, reloaded.getGraph().traverse().size());
        JIPipeGraphNode node = reloaded.getGraph().traverse().iterator().next();
        assertEquals("ij1-blur-gaussian2d", node.getInfo().getId());
        assertEquals(nodeIdMapping(project.getGraph()), nodeIdMapping(reloaded.getGraph()),
                "Round-tripped node id mapping differs from the original fixture load");
    }

    // ---------------------------------------------------------------------
    // 2. Crate conformance
    // ---------------------------------------------------------------------

    @Test
    void crateConformsToWorkflowRoCrateProfile() throws IOException, URISyntaxException {
        Path jipx = tmp.resolve("crate.jipx");
        JIPipeProject project = loadFixture();
        // A non-empty category exercises the keywords = JIPipe + categories
        // contract (the fixture itself has none).
        project.getMetadata().addCategory("Analysis");

        List<String> violations = new ArrayList<>();
        new JIPipeXProjectWriter().writeWithViolations(project, jipx, JIPipeProgressInfo.SILENT, violations);
        assertTrue(violations.isEmpty(), "A well-formed save must yield zero compliance violations: " + violations);

        JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
        assertNotNull(metadata, "ro-crate-metadata.json must parse as JSON");
        JsonNode graph = metadata.get("@graph");
        assertTrue(graph != null && graph.isArray(), "The crate metadata must have an @graph array");

        JsonNode root = entityWithId(graph, "./");
        assertNotNull(root, "The root './' entity must be present");
        // conformsTo lives on the JsonDescriptor entity (the
        // ro-crate-metadata.json node), not on the root data entity —
        // it may be a single object or an array of objects.
        JsonNode descriptor = entityWithId(graph, "ro-crate-metadata.json");
        assertNotNull(descriptor, "The JsonDescriptor entity ro-crate-metadata.json must be present");
        assertTrue(conformsToContains(descriptor.get("conformsTo"), "https://w3id.org/workflowhub/workflow-ro-crate/1.0"),
                "The crate must conform to the Workflow RO-Crate profile; got: " + descriptor.get("conformsTo"));
        // mainEntity points at the graph part.
        assertEquals("project/graph.json", root.get("mainEntity").path("@id").asText(),
                "The root mainEntity must be project/graph.json; got: " + root.get("mainEntity"));
        // License: the fixture has no license and no settings default → MIT fallback.
        assertNotNull(root.get("license"), "The root entity must carry a license");
        assertEquals("https://spdx.org/licenses/MIT", root.get("license").path("@id").asText(),
                "Empty metadata license must fall back to MIT; got: " + root.get("license"));
        // keywords = "JIPipe" + the metadata's categories (the raw
        // category list, one repeated keywords property per category).
        assertTrue(propertyContains(root.get("keywords"), "JIPipe"),
                "The root entity must carry the JIPipe keyword; got: " + root.get("keywords"));
        for (var category : project.getMetadata().getCategories()) {
            String value = StringUtils.nullToEmpty(category.getValue()).trim();
            if (!value.isEmpty()) {
                assertTrue(propertyContains(root.get("keywords"), value),
                        "The root entity must carry the metadata category keyword '" + value + "'; got: " + root.get("keywords"));
            }
        }

        // The main entity is a ComputationalWorkflow written in JIPipe.
        JsonNode mainEntity = entityWithId(graph, "project/graph.json");
        assertNotNull(mainEntity, "The mainEntity data entity project/graph.json must be present");
        assertTrue(typesContain(mainEntity.get("@type"), "ComputationalWorkflow"),
                "The main entity must be typed ComputationalWorkflow; got: " + mainEntity.get("@type"));
        JsonNode programmingLanguage = mainEntity.get("programmingLanguage");
        assertNotNull(programmingLanguage, "The main entity must declare programmingLanguage");
        assertEquals("https://jipipe.org/jipipe", programmingLanguage.path("@id").asText(),
                "The main entity programmingLanguage must reference the JIPipe contextual entity; got: " + programmingLanguage);
        JsonNode jipipeLanguage = entityWithId(graph, "https://jipipe.org/jipipe");
        assertNotNull(jipipeLanguage, "The JIPipe ComputerLanguage contextual entity must be present");
        assertTrue(typesContain(jipipeLanguage.get("@type"), "ComputerLanguage"),
                "The JIPipe contextual entity must be typed ComputerLanguage; got: " + jipipeLanguage.get("@type"));
        assertEquals(JIPipe.getJIPipeVersion(), jipipeLanguage.get("version").asText(),
                "The JIPipe contextual entity must carry the JIPipe version");

        // The README data entity.
        JsonNode readme = entityWithId(graph, "README.md");
        assertNotNull(readme, "The README.md data entity must be present");
        assertEquals("text/markdown", readme.get("encodingFormat").asText(),
                "The README entity must be markdown; got: " + readme.get("encodingFormat"));

        // The README.md payload in the zip must be non-empty markdown.
        String readmeContent = readZipEntryAsString(jipx, "README.md");
        assertTrue(readmeContent.startsWith("# "), "README.md must be a markdown document; got: " + readmeContent);
        assertTrue(readmeContent.contains("Created with JIPipe " + JIPipe.getJIPipeVersion() + "."),
                "README.md must carry the generator line; got: " + readmeContent);
    }

    @Test
    void settingsDefaultReplacesMitFallbackForEmptyMetadataLicense() throws IOException, URISyntaxException {
        JIPipeProjectIOApplicationSettings settings = JIPipeProjectIOApplicationSettings.getInstance();
        String original = settings.getDefaultRoCrateLicense();
        try {
            settings.setDefaultRoCrateLicense("Apache-2.0");

            Path jipx = tmp.resolve("license.jipx");
            JIPipeProject project = loadFixture();
            List<String> violations = new ArrayList<>();
            new JIPipeXProjectWriter().writeWithViolations(project, jipx, JIPipeProgressInfo.SILENT, violations);
            assertTrue(violations.isEmpty(), "A well-formed save must yield zero compliance violations: " + violations);

            JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
            JsonNode root = entityWithId(metadata.get("@graph"), "./");
            // Empty metadata license → the settings default wins over the MIT fallback.
            assertEquals("https://spdx.org/licenses/Apache-2.0", root.get("license").path("@id").asText(),
                    "The settings default license must replace the MIT fallback; got: " + root.get("license"));
        } finally {
            settings.setDefaultRoCrateLicense(original);
        }
    }

    @Test
    void metadataLicenseAlwaysWins() throws IOException, URISyntaxException {
        JIPipeProjectIOApplicationSettings settings = JIPipeProjectIOApplicationSettings.getInstance();
        String original = settings.getDefaultRoCrateLicense();
        try {
            settings.setDefaultRoCrateLicense("Apache-2.0");

            Path jipx = tmp.resolve("license2.jipx");
            JIPipeProject project = loadFixture();
            project.getMetadata().setLicense("GPL-3.0"); // a non-empty metadata license always wins

            List<String> violations = new ArrayList<>();
            new JIPipeXProjectWriter().writeWithViolations(project, jipx, JIPipeProgressInfo.SILENT, violations);
            assertTrue(violations.isEmpty(), "A well-formed save must yield zero compliance violations: " + violations);

            JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
            JsonNode root = entityWithId(metadata.get("@graph"), "./");
            assertEquals("https://spdx.org/licenses/GPL-3.0", root.get("license").path("@id").asText(),
                    "A non-empty metadata license must win over the settings default; got: " + root.get("license"));
        } finally {
            settings.setDefaultRoCrateLicense(original);
        }
    }

    @Test
    void authorsWithoutOrcidProduceStablePersonIds() throws IOException, URISyntaxException {
        Path jipx = tmp.resolve("authors.jipx");
        JIPipeProject project = loadFixture();
        JIPipeAuthorMetadata authorNoOrcid = new JIPipeAuthorMetadata();
        authorNoOrcid.setFirstName("Ada");
        authorNoOrcid.setLastName("Lovelace");
        JIPipeOrganizationMetadata.List affiliations = new JIPipeOrganizationMetadata.List();
        affiliations.add(new JIPipeOrganizationMetadata("Analytical Engines Ltd", "https://ror.org/0000-0001", ""));
        authorNoOrcid.setAffiliations(affiliations);
        JIPipeAuthorMetadata authorWithOrcid = new JIPipeAuthorMetadata();
        authorWithOrcid.setFirstName("Grace");
        authorWithOrcid.setLastName("Hopper");
        authorWithOrcid.setOrcid("0000-0002-1825-0097");
        project.getMetadata().setAuthors(
                new JIPipeAuthorMetadata.List(authorNoOrcid, authorWithOrcid));

        List<String> violations = new ArrayList<>();
        new JIPipeXProjectWriter().writeWithViolations(project, jipx, JIPipeProgressInfo.SILENT, violations);
        assertTrue(violations.isEmpty(), "A well-formed save must yield zero compliance violations: " + violations);

        JsonNode metadata = readZipEntryAsJson(jipx, "ro-crate-metadata.json");
        JsonNode graph = metadata.get("@graph");

        // The author with an ORCID gets the ORCID URL as id (CreateROCrateRun convention).
        JsonNode orcidPerson = entityWithId(graph, "https://orcid.org/0000-0002-1825-0097");
        assertNotNull(orcidPerson, "The ORCID author must be a Person entity with the ORCID URL id");
        assertTrue(typesContain(orcidPerson.get("@type"), "Person"),
                "The ORCID author must be typed Person; got: " + orcidPerson.get("@type"));
        assertEquals("Grace", orcidPerson.get("givenName").asText());

        // The author without an ORCID gets a stable non-URL id (a URN),
        // not an ORCID URL and not an empty/invalid id.
        JsonNode plainPerson = entityWithId(graph, "urn:person:0");
        assertNotNull(plainPerson, "The ORCID-less author must be a Person entity with a stable id (got: " + graph + ")");
        assertTrue(typesContain(plainPerson.get("@type"), "Person"),
                "The ORCID-less author must be typed Person; got: " + plainPerson.get("@type"));
        assertEquals("Ada", plainPerson.get("givenName").asText());
        assertFalse(entityWithId(graph, "https://orcid.org/") != null,
                "An ORCID-less author must not get the bare orcid.org URL as id");

        // The affiliation is referenced by the person and present as an entity.
        JsonNode affiliation = entityWithId(graph, "https://ror.org/0000-0001");
        assertNotNull(affiliation, "The organization affiliation must be present");
        assertEquals("Analytical Engines Ltd", affiliation.get("name").asText());
        assertNotNull(plainPerson.get("affiliation"), "The person must reference the affiliation");
        assertEquals("https://ror.org/0000-0001", plainPerson.get("affiliation").path("@id").asText());
    }

    // ---------------------------------------------------------------------
    // 3. Atomicity / failure
    // ---------------------------------------------------------------------

    @Test
    void selfVerifyZipRejectsMissingListedPartNamingTheEntry() throws IOException {
        // A hand-built bad zip: the jipx.json manifest lists
        // project/graph.json, but the entry is absent from the container.
        Path badZip = tmp.resolve("bad.jipx");
        JIPipeXManifest manifest = JIPipeXManifest.defaultManifest(JIPipe.getJIPipeVersion());
        manifest.getParts().clear();
        manifest.getParts().put(JIPipeXManifest.MAIN_GRAPH, "graph");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(badZip))) {
            putZipEntry(zos, "jipx.json", manifest.toJsonNode());
        }

        JIPipeValidationRuntimeException e = assertThrows(JIPipeValidationRuntimeException.class,
                () -> JIPipeXProjectWriter.selfVerifyZip(badZip, JIPipeProgressInfo.SILENT));
        assertTrue(e.getMessage().contains("project/graph.json"),
                "The self-verify failure must name the first missing listed part entry; got: " + e.getMessage());
    }

    @Test
    void failedWriteLeavesNoPartAndTargetUnchanged() throws IOException, URISyntaxException {
        // Deterministic failure simulation: the target path is a
        // NON-EMPTY EXISTING DIRECTORY. A POSIX rename() onto an empty
        // directory would silently succeed (replacing it), but onto a
        // non-empty one it fails after the self-verify passed — exactly
        // the atomic-replace failure path under test. (The brief's
        // read-only-directory variant is skipped: this suite runs as
        // root, where the read-only bit is ignored.)
        JIPipeProject project = loadFixture();
        Path targetDir = tmp.resolve("collision");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("keep.txt"), "sentinel");

        JIPipeXProjectWriter writer = new JIPipeXProjectWriter();
        assertThrows(IOException.class,
                () -> writer.write(project, targetDir, JIPipeProgressInfo.SILENT));

        // The .part must have been cleaned up; the target dir must be intact.
        try (Stream<Path> listing = Files.list(tmp)) {
            assertFalse(listing.anyMatch(p -> p.getFileName().toString().endsWith(".jipx.part")),
                    "A failed write must not leave a .jipx.part file behind: " + tmp);
        }
        assertTrue(Files.isDirectory(targetDir), "The colliding target directory must be untouched");
        assertTrue(Files.readString(targetDir.resolve("keep.txt")).equals("sentinel"),
                "The colliding target directory's content must be untouched");
    }

    // ---------------------------------------------------------------------
    // 4. Dispatcher + legacy API seam
    // ---------------------------------------------------------------------

    @Test
    void writerDispatcherSelectsJipxWriterByExtension() {
        JIPipeProjectWriter dispatcher = JIPipeProjectIOs.writerFromPath(tmp.resolve("x.jipx"));
        assertTrue(dispatcher instanceof JIPipeXProjectWriter,
                "writerFromPath(.jipx) must return a JIPipeXProjectWriter, got: " + dispatcher);
        assertEquals("org.hkijena.jipipe.core:jipx", dispatcher.getId());
    }

    @Test
    void saveProjectDispatchesByExtensionAndUpdatesProjectFile() throws IOException, URISyntaxException {
        JIPipeProject project = loadFixture();
        Path original = project.getProjectFile();

        Path jipxPath = tmp.resolve("dispatched.jipx");
        project.saveProject(jipxPath, true);
        assertTrue(Files.isRegularFile(jipxPath), "saveProject must create the .jipx container");
        assertEquals(jipxPath, project.getProjectFile(),
                "updateSavePath=true must point the project file at the new container");
        // The dispatch must have picked the JIPX writer (a container, not a monolith).
        List<String> entries = zipEntryNames(jipxPath);
        assertTrue(entries.contains("jipx.json"), "The dispatched save must write a JIPX container: " + entries);

        // The legacy API contract: updateSavePath=false leaves the path alone.
        Path jipxPath2 = tmp.resolve("dispatched2.jipx");
        project.saveProject(jipxPath2, false);
        assertTrue(Files.isRegularFile(jipxPath2));
        assertEquals(jipxPath, project.getProjectFile(),
                "updateSavePath=false must leave the project file at the previous container");

        // Regression guard: a .jip target still saves the legacy monolith
        // through JIPipeLegacyProjectWriter (behavior frozen by Task 3).
        Path jipPath = tmp.resolve("legacy.jip");
        project.saveProject(jipPath, false);
        assertTrue(Files.isRegularFile(jipPath));
        String monolith = Files.readString(jipPath);
        assertTrue(monolith.contains("\"metadata\""), "A .jip save must remain a monolithic JSON document");
        assertFalse(monolith.startsWith("PK"), "A .jip save must not be a ZIP container");
        assertEquals(jipxPath, project.getProjectFile(),
                "updateSavePath=false must leave the project file at the previous container");
        // The fixture's original path was tracked through the whole sequence.
        assertNotNull(original, "The fixture load must have recorded its project file");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static JIPipeProject loadFixture() throws IOException, URISyntaxException {
        Path fixture = Path.of(JIPipeXProjectWriterTest.class.getResource(FIXTURE).toURI());
        return JIPipeProject.loadProject(fixture, new UnspecifiedValidationReportContext(),
                new JIPipeValidationReport(), new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
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

    private static String readZipEntryAsString(Path zipPath, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            assertNotNull(entry, "The zip must contain the entry '" + entryName + "'");
            try (InputStream is = zip.getInputStream(entry)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static void putZipEntry(ZipOutputStream zos, String entryName, JsonNode node) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(node.toString().getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
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

    private static boolean propertyContains(JsonNode propertyNode, String value) {
        if (propertyNode == null || propertyNode.isMissingNode() || propertyNode.isNull()) {
            return false;
        }
        if (propertyNode.isArray()) {
            for (JsonNode element : propertyNode) {
                if (value.equals(element.asText())) {
                    return true;
                }
            }
            return false;
        }
        return value.equals(propertyNode.asText());
    }

    private static Map<UUID, String> nodeIdMapping(JIPipeGraph graph) {
        Map<UUID, String> mapping = new HashMap<>();
        for (JIPipeGraphNode n : graph.getGraphNodes()) {
            mapping.put(n.getUUIDInParentGraph(), n.getInfo().getId());
        }
        return mapping;
    }
}
