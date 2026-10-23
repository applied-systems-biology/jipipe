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
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.io.JIPipeProjectWriter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.contrib.ro_crate.RoCrate;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The JIPX (JIPipe 6) project container writer.
 * <p>
 * Writes a {@code .jipx} file — a ZIP container holding the project parts
 * as individual JSON documents plus a {@code jipx.json} manifest
 * (see {@link JIPipeXManifest} and {@link JIPipeProjectParts}) and the
 * Workflow RO-Crate metadata (see {@link JIPipeProjectCrate}):
 * <ol>
 * <li>The authors merge (same gate as the legacy Path variant:
 * {@code auto-add-authors} → {@link JIPipeProject#addAuthorsFromGlobalList()})
 * — the writer is self-contained; the {@code saveProject(Path, boolean)}
 * delegate stays merge-free.</li>
 * <li>The project workspace is materialized from the model
 * ({@link JIPipeProjectWorkspace#materializeFromModel(JIPipeProgressInfo)}) —
 * this updates the part files in place, so unknown/payload entries
 * (e.g. {@code data/foo.tiff}) survive and are zipped verbatim.</li>
 * <li>The crate metadata ({@code ro-crate-metadata.json}) and a generated
 * {@code README.md} are written into the workspace.</li>
 * <li>The workspace is zipped into a {@code .part} sibling of the target
 * with {@link ZipOutputStream} (production ZIP writing uses
 * {@code java.util.zip} only — never zip4j). Entry order:
 * {@code ro-crate-metadata.json} first (Workflow RO-Crate root
 * convention), then {@code jipx.json}, then {@code README.md}, then the
 * {@code project/*.json} part files in manifest parts-table order, then
 * any unknown/payload entries sorted by path (determinism). Entry names
 * are the workspace-relative paths — no leading slash, no wrapper root.</li>
 * <li>{@link #selfVerifyZip(Path, JIPipeProgressInfo)} re-opens the
 * {@code .part} and asserts the container's internal consistency; on
 * failure the part is deleted and the exception carries the first
 * problem.</li>
 * <li>The verified part atomically replaces the target (a single OS-level
 * rename — see {@link #writeWithViolations}), and the workspace is marked
 * saved. The target is never half-written.</li>
 * </ol>
 * <p>
 * <b>Compliance seam:</b> the interface {@link #write} logs RO-Crate
 * compliance violations via the progress info. The package-visible
 * {@link #writeWithViolations(JIPipeProject, Path, JIPipeProgressInfo, List)}
 * overload does the same work but fills the caller's list instead of
 * (in addition to) logging — the GUI save flow (Task 5) uses it to show
 * the warning dialog.
 * <p>
 * The {@code Writer}/{@code OutputStream} variants throw
 * {@link UnsupportedOperationException} — the JIPX format is a ZIP
 * container and stream writes are not supported (the frozen asymmetry:
 * only the legacy format supports stream writes).
 */
public class JIPipeXProjectWriter implements JIPipeProjectWriter {

    /**
     * The RO-Crate metadata file name within the container (the library's
     * writers copy files by this name — we write the ZIP ourselves, but
     * the name is the Workflow RO-Crate root convention).
     */
    public static final String CRATE_METADATA_FILE_NAME = "ro-crate-metadata.json";

    /**
     * The generated README file name within the container.
     */
    public static final String README_FILE_NAME = "README.md";

    /**
     * The suffix of the temporary part file the container is built in
     * before the atomic replace.
     */
    public static final String PART_SUFFIX = ".jipx.part";

    @Override
    public String getId() {
        return JIPipeXProjectReader.FORMAT_ID;
    }

    @Override
    public List<String> getFileExtensions() {
        return List.of("jipx");
    }

    /**
     * Unlike the reader, this does NOT check the ZIP magic bytes: a save
     * TARGET may not exist yet (and if it does, its content is about to
     * be replaced). Write dispatch is extension-driven — see
     * {@link org.hkijena.jipipe.api.project.io.JIPipeProjectIOs#writerFromPath(Path)}.
     */
    @Override
    public boolean supports(Path path) {
        return path.getFileName() != null && PathUtils.isJipxPath(path);
    }

    @Override
    public void write(JIPipeProject project, Path target, JIPipeProgressInfo progressInfo) throws IOException {
        // Log mode: compliance violations go to the progress info.
        writeWithViolations(project, target, progressInfo, null);
    }

    /**
     * The full write path with a compliance-violations seam.
     * <p>
     * Behaves exactly like {@link #write(JIPipeProject, Path, JIPipeProgressInfo)}
     * except that every RO-Crate compliance violation is additionally
     * reported into {@code violationsOut} (when non-null, the violations
     * are NOT logged via the progress info — the caller presents them).
     *
     * @param project      the project to write
     * @param target       the target file (replaced atomically)
     * @param progressInfo the progress info
     * @param violationsOut the list that receives the compliance
     *                      violations, or null for log mode
     * @throws IOException triggered by file or zip I/O
     */
    public void writeWithViolations(JIPipeProject project, Path target, JIPipeProgressInfo progressInfo, List<String> violationsOut) throws IOException {
        // (1) Authors merge — same gate as the legacy Path variant; the
        // writer is self-contained, the saveProject delegate is merge-free.
        if (project.getMetadata().isAutoAddAuthors()) {
            project.addAuthorsFromGlobalList();
        }

        // (2) Materialize the workspace from the model (idempotent;
        // unknown/payload entries survive). A disposed project has no
        // workspace.
        JIPipeProjectWorkspace workspace = project.getWorkspace();
        if (workspace == null) {
            throw new JIPipeValidationRuntimeException(new IllegalStateException("Project is closed"),
                    "Project is closed",
                    "The project's workspace is unavailable (the project has been disposed) — it cannot be saved as a JIPX container.",
                    "Re-open the project and try again.");
        }

        workspace.materializeFromModel(progressInfo.resolve("Materialize JIPX workspace"));

        // (3) Build the crate and write its metadata + the generated
        // README into the workspace.
        List<String> violations = violationsOut != null ? violationsOut : new ArrayList<>();
        RoCrate crate = JIPipeProjectCrate.buildCrate(project, violations);
        if (violationsOut == null) {
            for (String violation : violations) {
                progressInfo.log("RO-Crate compliance warning: " + violation);
            }
        }
        Files.writeString(workspace.getBaseDirectory().resolve(CRATE_METADATA_FILE_NAME),
                crate.getJsonMetadata(), StandardCharsets.UTF_8);
        Files.writeString(workspace.getBaseDirectory().resolve(README_FILE_NAME),
                generateReadme(project), StandardCharsets.UTF_8);

        // (4) The manifest (jipx.json) was written by materializeFromModel
        // and lists exactly the parts present — nothing to refresh here;
        // markSaved runs AFTER the zip succeeds (step 8).

        // (5) + (6) + (7): zip the workspace into a .part sibling of the
        // target, self-verify it, and atomically replace the target.
        Path part = target.getParent() == null
                ? Path.of("." + target.getFileName() + PART_SUFFIX)
                : target.getParent().resolve(target.getFileName() + PART_SUFFIX);
        try {
            zipWorkspace(workspace, part, progressInfo);
            selfVerifyZip(part, progressInfo);
            // Single atomic replace: Files.move with REPLACE_EXISTING is an
            // OS-level rename — it atomically replaces an existing regular
            // file (same parent directory → same filesystem) and FAILS
            // when the target is a non-empty directory
            // (DirectoryNotEmptyException),
            // so the target can never end up half-written. A delete-first
            // would remove an empty target directory instead of failing.
            Files.move(part, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException deleteFailed) {
                e.addSuppressed(deleteFailed);
            }
            throw e;
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException deleteFailed) {
                e.addSuppressed(deleteFailed);
            }
            throw e;
        }

        // (8) Mark the workspace saved from the new origin.
        workspace.markSaved(project.getProjectFile() != null ? project.getProjectFile() : target);
    }

    /**
     * Writes a generated {@code README.md} for the container: the project
     * name as a markdown heading, the project summary/description as
     * plain text, and the generator line.
     */
    static String generateReadme(JIPipeProject project) {
        StringBuilder readme = new StringBuilder();
        readme.append("# ").append(StringUtils.nullToEmpty(project.getMetadata().getName())).append("\n\n");
        String summary = project.getMetadata().getSummary() == null ? "" : project.getMetadata().getSummary().toPlainText();
        String description = project.getMetadata().getDescription() == null ? "" : project.getMetadata().getDescription().toPlainText();
        String body = StringUtils.isNullOrEmpty(summary) ? description : summary;
        readme.append(StringUtils.nullToEmpty(body).trim()).append("\n\n");
        readme.append("Created with JIPipe ").append(JIPipe.getJIPipeVersion()).append(".\n");
        return readme.toString();
    }

    /**
     * Zips the workspace directory into a JIPX container with
     * {@link ZipOutputStream} (no wrapper root). Entry order:
     * {@code ro-crate-metadata.json}, {@code jipx.json}, {@code README.md},
     * the part files in manifest parts-table order, then every remaining
     * file sorted by path (unknown/payload entries — deterministic).
     *
     * @param workspace the materialized workspace to zip
     * @param target    the part file to write
     * @param progressInfo the progress info
     * @throws IOException triggered by zip I/O
     */
    private static void zipWorkspace(JIPipeProjectWorkspace workspace, Path target, JIPipeProgressInfo progressInfo) throws IOException {
        Path baseDirectory = workspace.getBaseDirectory();
        progressInfo.log("Zipping JIPX workspace " + baseDirectory + " into " + target + " ...");
        JIPipeXManifest manifest = workspace.getManifest();

        // Workspace-relative entry paths in the spec'd order.
        Map<String, Path> orderedEntries = new LinkedHashMap<>();
        orderedEntries.put(CRATE_METADATA_FILE_NAME, baseDirectory.resolve(CRATE_METADATA_FILE_NAME));
        orderedEntries.put(JIPipeXManifest.MANIFEST_FILE_NAME, baseDirectory.resolve(JIPipeXManifest.MANIFEST_FILE_NAME));
        orderedEntries.put(README_FILE_NAME, baseDirectory.resolve(README_FILE_NAME));
        // The part files in manifest parts-table order.
        for (String partPath : manifest.getParts().keySet()) {
            orderedEntries.put(partPath, baseDirectory.resolve(partPath));
        }
        // Every remaining file (unknown/payload entries), sorted by path.
        List<Path> remaining = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(baseDirectory)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String relative = baseDirectory.relativize(file).toString().replace(java.io.File.separatorChar, '/');
                if (!orderedEntries.containsKey(relative)) {
                    remaining.add(file);
                }
            });
        }
        remaining.sort(Comparator.comparing(file -> baseDirectory.relativize(file).toString().replace(java.io.File.separatorChar, '/')));
        for (Path file : remaining) {
            orderedEntries.put(baseDirectory.relativize(file).toString().replace(java.io.File.separatorChar, '/'), file);
        }

        try (OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, Path> entry : orderedEntries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                Files.copy(entry.getValue(), zip);
                zip.closeEntry();
            }
            zip.finish();
        }
    }

    /**
     * The self-verification seam of the write path: re-opens the
     * container with {@link ZipFile} and asserts its internal
     * consistency —
     * <ul>
     * <li>the {@code jipx.json} entry exists and parses as JSON,</li>
     * <li>every part listed in the manifest's parts table exists in the
     * container and parses as JSON (a missing listed part is a corrupt
     * container),</li>
     * <li>the {@code ro-crate-metadata.json} entry exists and parses as
     * JSON.</li>
     * </ul>
     * Any failure throws a {@link JIPipeValidationRuntimeException}
     * naming the FIRST problem.
     *
     * @param zipPath the container to verify
     * @param progressInfo the progress info
     * @throws IOException triggered by zip I/O
     */
    public static void selfVerifyZip(Path zipPath, JIPipeProgressInfo progressInfo) throws IOException {
        progressInfo.log("Self-verifying JIPX container " + zipPath + " ...");
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            // (1) The manifest.
            ZipEntry manifestEntry = zip.getEntry(JIPipeXManifest.MANIFEST_FILE_NAME);
            if (manifestEntry == null) {
                throw new JIPipeValidationRuntimeException(new IllegalStateException(
                        "missing " + JIPipeXManifest.MANIFEST_FILE_NAME + " entry"),
                        "Invalid JIPX container",
                        "The container " + zipPath + " has no " + JIPipeXManifest.MANIFEST_FILE_NAME + " entry.",
                        "The container was written by JIPipe and is internally inconsistent — delete it and save the project again.");
            }
            JsonNode manifestJson;
            try (InputStream is = zip.getInputStream(manifestEntry)) {
                manifestJson = JsonUtils.getObjectMapper().readTree(is);
            } catch (Exception e) {
                throw new JIPipeValidationRuntimeException(e, "Invalid JIPX container",
                        "The " + JIPipeXManifest.MANIFEST_FILE_NAME + " entry of " + zipPath + " does not parse as JSON: " + e.getMessage(),
                        "The container was written by JIPipe and is internally inconsistent — delete it and save the project again.");
            }
            JIPipeXManifest manifest = JIPipeXManifest.fromJsonNode(manifestJson);

            // (2) Every listed part exists and parses as JSON.
            for (String partPath : manifest.getParts().keySet()) {
                ZipEntry partEntry = zip.getEntry(partPath);
                if (partEntry == null) {
                    throw new JIPipeValidationRuntimeException(new IllegalStateException(
                            "missing listed part entry " + partPath),
                            "Invalid JIPX container",
                            "The " + JIPipeXManifest.MANIFEST_FILE_NAME + " manifest of " + zipPath + " lists the part entry '"
                                    + partPath + "', but it is missing from the container.",
                            "The container was written by JIPipe and is internally inconsistent — delete it and save the project again.");
                }
                try (InputStream is = zip.getInputStream(partEntry)) {
                    JsonUtils.getObjectMapper().readTree(is);
                } catch (Exception e) {
                    throw new JIPipeValidationRuntimeException(e, "Invalid JIPX container",
                            "The part entry '" + partPath + "' of " + zipPath + " does not parse as JSON: " + e.getMessage(),
                            "The container was written by JIPipe and is internally inconsistent — delete it and save the project again.");
                }
            }

            // (3) The crate metadata.
            ZipEntry crateEntry = zip.getEntry(CRATE_METADATA_FILE_NAME);
            if (crateEntry == null) {
                throw new JIPipeValidationRuntimeException(new IllegalStateException(
                        "missing " + CRATE_METADATA_FILE_NAME + " entry"),
                        "Invalid JIPX container",
                        "The container " + zipPath + " has no " + CRATE_METADATA_FILE_NAME + " entry.",
                        "The container was written by JIPipe and is internally inconsistent — delete it and save the project again.");
            }
            try (InputStream is = zip.getInputStream(crateEntry)) {
                JsonUtils.getObjectMapper().readTree(is);
            } catch (Exception e) {
                throw new JIPipeValidationRuntimeException(e, "Invalid JIPX container",
                        "The " + CRATE_METADATA_FILE_NAME + " entry of " + zipPath + " does not parse as JSON: " + e.getMessage(),
                        "The container was written by JIPipe and is internally inconsistent — delete it and save the project again.");
            }
        }
    }

    @Override
    public void write(JIPipeProject project, Writer writer) {
        throw new UnsupportedOperationException(".jipx is a ZIP container; stream writes are not supported");
    }

    @Override
    public void write(JIPipeProject project, OutputStream outputStream) {
        throw new UnsupportedOperationException(".jipx is a ZIP container; stream writes are not supported");
    }
}
