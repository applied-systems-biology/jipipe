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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.jipx.JIPipeXProjectReader;
import org.hkijena.jipipe.api.project.jipx.JIPipeXProjectWriter;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Static format dispatcher for JIPipe project files.
 * <p>
 * Dispatch policy:
 * <ul>
 * <li><b>Reads are content-first.</b> The file's bytes decide the reader:
 * a file with ZIP magic is handed to the {@link JIPipeXProjectReader}
 * (the JIPX container format, which rejects non-JIPX ZIP contents during
 * {@link JIPipeProjectReader#read} via its {@code jipx.json} manifest
 * check), and every non-ZIP file is handed to the
 * {@link JIPipeLegacyProjectReader}, regardless of its extension (the
 * legacy format is plain JSON, and its parser surfaces content errors).
 * {@link JIPipeProjectIO#supports(Path)} is <i>not</i> used for read
 * dispatch. The {@code .crate.zip} import flow is handled by the GUI
 * layer and does not go through this dispatcher.</li>
 * <li><b>Writes are extension-driven.</b> The writer is selected purely
 * from the file extension: {@code .jip} →
 * {@link JIPipeLegacyProjectWriter}, {@code .jipx} →
 * {@link JIPipeXProjectWriter}, anything else → rejected. (Unlike
 * reads, the target's content plays no role — a save target may not
 * exist yet.)</li>
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
     * Reads are content-first: a file with ZIP magic is handed to the
     * {@link JIPipeXProjectReader} (which rejects non-JIPX ZIP contents
     * during read via its {@code jipx.json} manifest check), everything
     * else to the {@link JIPipeLegacyProjectReader}.
     *
     * @param path the project file
     * @return the reader for the file's content
     * @throws IOException triggered by the ZIP magic-byte inspection
     */
    public static JIPipeProjectReader readerFromPath(Path path) throws IOException {
        if (ArchiveUtils.isZipFile(path)) {
            return new JIPipeXProjectReader();
        }
        return new JIPipeLegacyProjectReader();
    }

    /**
     * Resolves the project writer for the given file, selected by extension.
     *
     * @param path the target file
     * @return the writer for the target's extension
     * @throws IllegalArgumentException if the extension cannot be mapped to a writer
     */
    public static JIPipeProjectWriter writerFromPath(Path path) {
        if (PathUtils.isJipPath(path)) {
            return new JIPipeLegacyProjectWriter();
        } else if (PathUtils.isJipxPath(path)) {
            return new JIPipeXProjectWriter();
        } else {
            throw new IllegalArgumentException("Cannot determine project writer for: " + path);
        }
    }

    /**
     * Peeks at a project file without loading it: extracts only the
     * metadata and the dependency set.
     * <p>
     * For the legacy JSON format this parses only the JSON tree. For a
     * JIPX container this streams the {@code jipx.json},
     * {@code project/metadata.json}, and {@code project/dependencies.json}
     * entries directly out of the ZIP (no extraction, no temporary
     * directory).
     *
     * @param path the project file
     * @return the extracted project dependencies and metadata
     * @throws IOException                 triggered by the JSON parser
     * @throws UnsupportedOperationException if the file is a ZIP that is not
     *                                       a JIPX container (no
     *                                       {@code jipx.json} entry) — the
     *                                       {@code .crate.zip} import path
     *                                       is handled by the GUI layer and
     *                                       never reaches {@code peek}
     */
    public static JIPipeProjectPeek peek(Path path) throws IOException {
        if (ArchiveUtils.isZipFile(path)) {
            return peekZip(path);
        }
        JsonNode jsonData = JsonUtils.getObjectMapper().readValue(path.toFile(), JsonNode.class);
        return new JIPipeProjectPeek(
                JIPipeProject.loadDependenciesFromJson(jsonData),
                JIPipeProject.loadMetadataFromJson(jsonData));
    }

    /**
     * Peeks at a JIPX container by streaming its entries out of the ZIP
     * without extracting it.
     *
     * @param path the JIPX container
     * @return the extracted project dependencies and metadata
     * @throws IOException                       triggered by the JSON parser
     * @throws UnsupportedOperationException     if the ZIP has no
     *                                           {@code jipx.json} entry
     */
    private static JIPipeProjectPeek peekZip(Path path) throws IOException {
        try (ZipFile zip = openZipForPeek(path)) {
            ZipEntry manifestEntry = zip.getEntry("jipx.json");
            if (manifestEntry == null) {
                throw new UnsupportedOperationException(
                        "not a JIPipe project crate (missing jipx.json): " + path);
            }
            JsonNode metadataPart = readZipEntryAsJson(zip, "project/metadata.json");
            JsonNode dependenciesPart = readZipEntryAsJson(zip, "project/dependencies.json");
            // The container parts are the monolith's top-level fields
            // themselves (the JIPipeProjectParts fidelity contract), while
            // the loaders below expect the monolith root — re-wrap the
            // parts under their part keys. Absent parts yield the loaders'
            // defaults (empty dependency set / empty metadata).
            ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
            if (metadataPart != null) {
                root.set("metadata", metadataPart);
            }
            if (dependenciesPart != null) {
                root.set("dependencies", dependenciesPart);
            }
            return new JIPipeProjectPeek(
                    JIPipeProject.loadDependenciesFromJson(root),
                    JIPipeProject.loadMetadataFromJson(root));
        }
    }

    /**
     * Reads a single ZIP entry as a JSON tree, returning {@code null} when
     * the entry is absent.
     */
    private static JsonNode readZipEntryAsJson(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            return null;
        }
        try (InputStream is = zip.getInputStream(entry)) {
            return JsonUtils.getObjectMapper().readValue(is, JsonNode.class);
        }
    }

    /**
     * Opens a ZIP for peeking; a ZIP that cannot be opened at all (invalid
     * or truncated archive — including files that only carry the ZIP magic
     * bytes) is reported as not a JIPipe 6 project.
     */
    private static ZipFile openZipForPeek(Path path) throws IOException {
        try {
            return new ZipFile(path.toFile());
        } catch (ZipException e) {
            throw new UnsupportedOperationException("not a JIPipe project crate (missing jipx.json): " + path, e);
        }
    }
}
