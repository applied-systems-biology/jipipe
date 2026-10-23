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

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void zipNamedJipIsRejectedAsReader() throws IOException {
        Path zip = writeZipMagicFile(tmp, "x.jip");
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> JIPipeProjectIOs.readerFromPath(zip));
        assertTrue(e.getMessage().contains("not yet supported"));
    }

    @Test
    void writesDispatchByExtension() throws IOException {
        Path jip = tmp.resolve("a.jip");
        Files.writeString(jip, MINIMAL_PROJECT_JSON);
        assertTrue(JIPipeProjectIOs.writerFromPath(jip) instanceof JIPipeLegacyProjectWriter);

        Path jipx = tmp.resolve("a.jipx");
        Files.writeString(jipx, MINIMAL_PROJECT_JSON);
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> JIPipeProjectIOs.writerFromPath(jipx));
        assertTrue(e.getMessage().contains("not yet supported"));

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
        // Skipped where the local /data/JIPipe corpus is absent (e.g. CI) —
        // the hand-written-fixture assertions above are the real contract.
        Path corpus = Path.of("/data/JIPipe/06072020a.jip");
        Assumptions.assumeTrue(Files.exists(corpus), "local /data/JIPipe corpus absent; skipping corpus peek check");
        JIPipeProjectPeek corpusPeek = JIPipeProjectIOs.peek(corpus);
        assertTrue(7 == corpusPeek.getDependencies().size(),
                "Expected 7 dependencies, got " + corpusPeek.getDependencies().size());
    }

    @Test
    void peekRejectsZipFiles() throws IOException {
        Path zip = writeZipMagicFile(tmp, "peek.zip");
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> JIPipeProjectIOs.peek(zip));
        assertTrue(e.getMessage().contains("not yet supported"));
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
}
