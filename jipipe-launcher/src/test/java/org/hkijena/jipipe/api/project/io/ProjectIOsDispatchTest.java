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

package org.hkijena.jipipe.api.project.io;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.project.jipx.JIPipeXManifest;
import org.hkijena.jipipe.api.project.jipx.JIPipeXProjectReader;
import org.hkijena.jipipe.api.project.jipx.JIPipeXProjectWriter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decision-table test for the {@link JIPipeProjectIOs} format dispatcher.
 * <p>
 * Reads are content-first (ZIP magic checked before any extension logic),
 * writes are extension-driven, and {@code peek} parses only the JSON tree
 * without loading the project.
 */
class ProjectIOsDispatchTest {

    private static final String MINIMAL_PROJECT_JSON =
            "{\n" +
                    "  \"metadata\": {\n" +
                    "    \"name\": \"Roundtrip\"\n" +
                    "  },\n" +
                    "  \"dependencies\": []\n" +
                    "}\n";

    @TempDir
    Path tmp;

    @BeforeAll
    static void init() {
        // Required by peek's loadDependenciesFromJson path, which may touch
        // the dependency registry. The ImageJ legacy patcher launcher
        // session listener preinits the patcher for the whole module, so
        // ensureInstance() here is safe. Idempotent.
        JIPipe.ensureInstance();
    }

    /**
     * Builds a minimal JIPX container (no wrapper root): {@code jipx.json}
     * (the default manifest, pruned to the parts written), {@code
     * project/metadata.json} ({@code {"name": "Peek"}}), and - when
     * {@code withDependencies} - {@code project/dependencies.json}
     * ({@code []}).
     */
    private static Path writeJipxZip(Path zipPath, boolean withDependencies) throws IOException {
        JsonNode metadata = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree("{\"name\": \"Peek\"}");
        JsonNode dependencies = new com.fasterxml.jackson.databind.ObjectMapper()
                .createArrayNode();
        JIPipeXManifest manifest = JIPipeXManifest.defaultManifest(JIPipe.getJIPipeVersion());
        manifest.getParts().clear();
        manifest.getParts().put("project/metadata.json", "metadata");
        if (withDependencies) {
            manifest.getParts().put("project/dependencies.json", "dependencies");
        }
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            putZipEntry(zos, "jipx.json", manifest.toJsonNode());
            putZipEntry(zos, "project/metadata.json", metadata);
            if (withDependencies) {
                putZipEntry(zos, "project/dependencies.json", dependencies);
            }
        }
        return zipPath;
    }

    private static void putZipEntry(ZipOutputStream zos, String entryName, JsonNode node) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(node.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static Path writeZipMagicFile(Path dir, String fileName) throws IOException {
        Path path = dir.resolve(fileName);
        List<Integer> bytes = List.of(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05);
        byte[] zipMagic = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            zipMagic[i] = (byte) bytes.get(i).intValue();
        }
        Files.write(path, zipMagic);
        return path;
    }

    @Test
    void readsDispatchByContent() throws IOException {
        Path jip = tmp.resolve("project.jip");
        Files.writeString(jip, MINIMAL_PROJECT_JSON);
        assertTrue(JIPipeProjectIOs.readerFromPath(jip) instanceof JIPipeLegacyProjectReader);

        // Same content, different extension: content wins over extension.
        Path json = tmp.resolve("project.json");
        Files.writeString(json, MINIMAL_PROJECT_JSON);
        assertTrue(JIPipeProjectIOs.readerFromPath(json) instanceof JIPipeLegacyProjectReader);
    }

    @Test
    void zipContentDispatchesToJipxReader() throws IOException {
        // Reads are content-first: a ZIP-magic file goes to the JIPX reader
        // regardless of its extension (the reader's read() rejects
        // non-JIPX contents via the jipx.json check).
        Path zip = writeZipMagicFile(tmp, "x.jip");
        assertTrue(JIPipeProjectIOs.readerFromPath(zip) instanceof JIPipeXProjectReader);
    }

    @Test
    void writesDispatchByExtension() throws IOException {
        Path jip = tmp.resolve("a.jip");
        Files.writeString(jip, MINIMAL_PROJECT_JSON);
        assertTrue(JIPipeProjectIOs.writerFromPath(jip) instanceof JIPipeLegacyProjectWriter);

        // Writes are extension-driven: .jipx selects the JIPX container
        // writer (Task 4) - the save target may not exist yet, so the
        // writer's supports() is not consulted here.
        Path jipx = tmp.resolve("a.jipx");
        Files.writeString(jipx, MINIMAL_PROJECT_JSON);
        assertTrue(JIPipeProjectIOs.writerFromPath(jipx) instanceof JIPipeXProjectWriter);

        Path unknown = tmp.resolve("a.txt");
        Files.writeString(unknown, MINIMAL_PROJECT_JSON);
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> JIPipeProjectIOs.writerFromPath(unknown));
        assertTrue(iae.getMessage().contains("Cannot determine project writer for: " + unknown));
    }

    @Test
    void peekParsesMetadataAndDependencies() throws IOException {
        Path jip = tmp.resolve("peek.jip");
        Files.writeString(jip, MINIMAL_PROJECT_JSON);

        JIPipeProjectPeek peek = JIPipeProjectIOs.peek(jip);
        assertTrue(peek.getDependencies().isEmpty());
        assertTrue("Roundtrip".equals(peek.getMetadata().getName()));

        // A real corpus file with exactly 7 dependencies (module fixtures
        // have zero, so the size assertion is the non-vacuity guard).
        // Skipped where the local /data/JIPipe corpus is absent (e.g. CI) -
        // the hand-written-fixture assertions above are the real contract.
        Path corpus = Path.of("/data/JIPipe/06072020a.jip");
        Assumptions.assumeTrue(Files.exists(corpus), "local /data/JIPipe corpus absent; skipping corpus peek check");
        JIPipeProjectPeek corpusPeek = JIPipeProjectIOs.peek(corpus);
        assertTrue(7 == corpusPeek.getDependencies().size(),
                "Expected 7 dependencies, got " + corpusPeek.getDependencies().size());
    }

    @Test
    void peekRejectsZipsWithoutJipxManifest() throws IOException {
        // A ZIP-magic file without a jipx.json entry is not a JIPipe 6
        // project - the GUI's .crate.zip import path is handled by the GUI
        // layer and never reaches peek.
        Path zip = writeZipMagicFile(tmp, "peek.zip");
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> JIPipeProjectIOs.peek(zip));
        assertTrue(e.getMessage().contains("not a JIPipe project crate"));
        assertTrue(e.getMessage().contains("jipx.json"));
    }

    @Test
    void peekStreamsJipxWithoutExtraction() throws IOException {
        // The content-stream peek contract: a proper JIPX container is peeked
        // from its zip entries (no extraction, no temp dir) - metadata from
        // project/metadata.json, dependencies from project/dependencies.json.
        Path jipx = tmp.resolve("peek.jipx");
        writeJipxZip(jipx, true);

        JIPipeProjectPeek peek = JIPipeProjectIOs.peek(jipx);
        assertTrue("Peek".equals(peek.getMetadata().getName()));
        assertTrue(peek.getDependencies().isEmpty());
    }

    @Test
    void peekJipxWithoutDependenciesEntryYieldsEmptySet() throws IOException {
        // project/dependencies.json is optional in the container - a missing
        // entry yields an empty dependency set (peek is best-effort: absent
        // parts fall back to the loaders' defaults; it is the READER's
        // corrupt-container contract that enforces listed-part presence).
        Path jipx = tmp.resolve("peek-nodir.jipx");
        writeJipxZip(jipx, false);

        JIPipeProjectPeek peek = JIPipeProjectIOs.peek(jipx);
        assertTrue(peek.getDependencies().isEmpty());
        assertTrue("Peek".equals(peek.getMetadata().getName()));
    }
}
