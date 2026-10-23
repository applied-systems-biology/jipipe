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
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Static format dispatcher for JIPipe project files.
 * <p>
 * Dispatch policy (the contract the .jipx format builds on in Phase 1):
 * <ul>
 * <li><b>Reads are content-first.</b> The file's bytes decide the reader:
 * a file with ZIP magic is rejected as a ZIP-based project format (not yet
 * implemented), and every non-ZIP file is handed to the
 * {@link JIPipeLegacyProjectReader}, regardless of its extension (the legacy
 * format is plain JSON, and its parser surfaces content errors).
 * {@link JIPipeProjectIO#supports(Path)} is <i>not</i> used for read
 * dispatch.</li>
 * <li><b>Writes are extension-driven.</b> The writer is selected purely
 * from the file extension: {@code .jip} →
 * {@link JIPipeLegacyProjectWriter}, {@code .jipx} → not yet supported,
 * anything else → rejected.</li>
 * </ul>
 */
public final class JIPipeProjectIOs {

    private JIPipeProjectIOs() {
    }

    /**
     * Resolves the project reader for the given file.
     * <p>
     * Only dispatches — the file is not read by this method (the returned
     * reader's {@link JIPipeProjectReader#read} performs the actual load).
     * ZIP-based project formats are rejected:
     *
     * @param path the project file
     * @return the reader for the file's content
     * @throws IOException                       triggered by the ZIP magic-byte inspection
     * @throws UnsupportedOperationException     if the file is ZIP-based (.jipx arrives in Phase 1)
     */
    public static JIPipeProjectReader readerFromPath(Path path) throws IOException {
        if (ArchiveUtils.isZipFile(path)) {
            throw new UnsupportedOperationException(
                    "ZIP-based project formats are not yet supported (the .jipx format arrives with JIPipe 6.0 phase 1): " + path);
        }
        return new JIPipeLegacyProjectReader();
    }

    /**
     * Resolves the project writer for the given file, selected by extension.
     *
     * @param path the target file
     * @return the writer for the target's extension
     * @throws UnsupportedOperationException     if the target is {@code .jipx} (arrives in Phase 1)
     * @throws IllegalArgumentException          if the extension cannot be mapped to a writer
     */
    public static JIPipeProjectWriter writerFromPath(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".jip")) {
            return new JIPipeLegacyProjectWriter();
        } else if (fileName.endsWith(".jipx")) {
            throw new UnsupportedOperationException(".jipx is not yet supported: " + path);
        } else {
            throw new IllegalArgumentException("Cannot determine project writer for: " + path);
        }
    }

    /**
     * Peeks at a project file without loading it: parses only the JSON tree
     * and extracts the metadata and the dependency set.
     *
     * @param path the project file
     * @return the extracted project dependencies and metadata
     * @throws IOException                       triggered by the JSON parser
     * @throws UnsupportedOperationException     if the file is ZIP-based (the .jipx format arrives in Phase 1)
     */
    public static JIPipeProjectPeek peek(Path path) throws IOException {
        if (ArchiveUtils.isZipFile(path)) {
            throw new UnsupportedOperationException(
                    "ZIP-based project formats are not yet supported (the .jipx format arrives with JIPipe 6.0 phase 1): " + path);
        }
        JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
        return new JIPipeProjectPeek(
                JIPipeProject.loadDependenciesFromJson(jsonData),
                JIPipeProject.loadMetadataFromJson(jsonData));
    }
}
