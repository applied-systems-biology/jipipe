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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The directory-based v1 implementation of {@link JIPipeProjectWorkspace}.
 * <p>
 * The workspace lives in a plain directory: the part files at their
 * default manifest paths (see {@link JIPipeXManifest#defaultManifest}),
 * the manifest at {@code jipx.json}, and the workspace state (origin
 * information + save/dirty flags) at {@code workspace.json}. Materializing
 * re-derives the part files from the project model and is idempotent —
 * unknown/payload entries (e.g. {@code data/foo.tiff}) are never deleted
 * or rewritten (spec §5.4, "payload never regenerated" contract).
 */
public class JIPipeLocalProjectWorkspace implements JIPipeProjectWorkspace {

    /**
     * The file name of the workspace state document.
     */
    public static final String WORKSPACE_STATE_FILE_NAME = "workspace.json";

    private final Path baseDirectory;
    private final JIPipeProject project;
    private JIPipeXManifest manifest;

    /**
     * @param baseDirectory the directory this workspace lives in
     * @param project       the project this workspace materializes
     *                      (kept for origin-format derivation from the
     *                      project file)
     */
    public JIPipeLocalProjectWorkspace(Path baseDirectory, JIPipeProject project) {
        this.baseDirectory = baseDirectory;
        this.project = project;
    }

    @Override
    public Path getBaseDirectory() {
        return baseDirectory;
    }

    @Override
    public void materializeFromModel(JIPipeProgressInfo progressInfo) throws IOException {
        PathUtils.createDirectories(baseDirectory);
        ObjectMapper mapper = JsonUtils.getObjectMapper();

        // Write the part files (idempotent: only the listed part files are
        // touched — unknown/payload entries are never deleted or rewritten).
        // The manifest lists exactly the parts present in the slice: a
        // project without optional parts (e.g. no additional metadata)
        // materializes fewer files, and a listed part is always present —
        // a missing listed file means a corrupt container.
        Map<String, JsonNode> parts = JIPipeProjectParts.sliceParts(project);
        progressInfo.log("Materializing workspace parts ...");
        JIPipeXManifest newManifest = JIPipeXManifest.defaultManifest(JIPipe.getJIPipeVersion());
        newManifest.getParts().entrySet().removeIf(e -> !parts.containsKey(e.getValue()));
        for (Map.Entry<String, JsonNode> entry : parts.entrySet()) {
            Path partFile = baseDirectory.resolve(defaultPartPath(entry.getKey()));
            PathUtils.createDirectories(partFile.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(partFile.toFile(), entry.getValue());
        }

        // Write the manifest
        writeJson(baseDirectory.resolve(JIPipeXManifest.MANIFEST_FILE_NAME), newManifest.toJsonNode());
        this.manifest = newManifest;

        // Refresh the workspace state
        updateWorkspaceState(state -> {
            state.put("origin-path", "");
            state.put("origin-format", deriveOriginFormat());
            state.put("last-save", nowIso8601());
            state.put("dirty", false);
        });
    }

    @Override
    public void extractFromZip(Path zipFile, JIPipeProgressInfo progressInfo) throws IOException {
        PathUtils.createDirectories(baseDirectory);
        ArchiveUtils.decompressZipFile(zipFile, baseDirectory, progressInfo);
        this.manifest = null; // re-read the manifest from the extracted jipx.json
    }

    @Override
    public Map<String, JsonNode> readParts() throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        Map<String, JsonNode> parts = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : getManifest().getParts().entrySet()) {
            String entryPath = entry.getKey();
            String partKey = entry.getValue();
            Path partFile = baseDirectory.resolve(entryPath);
            if (!Files.isRegularFile(partFile)) {
                throw new IOException("Corrupt JIPX container: listed part file '" + entryPath
                    + "' (part '" + partKey + "') is missing in " + baseDirectory);
            }
            parts.put(partKey, mapper.readTree(partFile.toFile()));
        }
        return parts;
    }

    @Override
    public void markSaved(Path originPath) throws IOException {
        updateWorkspaceState(state -> {
            state.put("last-save", nowIso8601());
            state.put("dirty", false);
            state.put("origin-path", originPath == null ? "" : originPath.toString());
        });
    }

    @Override
    public void markDirty() throws IOException {
        updateWorkspaceState(state -> state.put("dirty", true));
    }

    @Override
    public JIPipeXManifest getManifest() throws IOException {
        if (manifest == null) {
            Path manifestFile = baseDirectory.resolve(JIPipeXManifest.MANIFEST_FILE_NAME);
            if (!Files.isRegularFile(manifestFile)) {
                throw new IOException("Corrupt JIPX container: manifest '" + manifestFile + "' is missing");
            }
            manifest = JIPipeXManifest.fromJsonNode(JsonUtils.getObjectMapper().readTree(manifestFile.toFile()));
        }
        return manifest;
    }

    /**
     * The default container entry path of a part: {@code project/} + the
     * part key with {@code ':'} replaced by {@code '-'} + {@code .json}.
     */
    private static String defaultPartPath(String partKey) {
        return "project/" + partKey.replace(':', '-') + ".json";
    }

    /**
     * Reads (or initializes) the {@code workspace.json} state, applies the
     * given update, and writes it back.
     */
    private void updateWorkspaceState(Consumer<ObjectNode> update) throws IOException {
        Path stateFile = baseDirectory.resolve(WORKSPACE_STATE_FILE_NAME);
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        JsonNode existing = Files.isRegularFile(stateFile)
            ? mapper.readTree(stateFile.toFile())
            : mapper.createObjectNode();
        ObjectNode state = existing.isObject() ? (ObjectNode) existing : mapper.createObjectNode();
        if (!state.has("origin-path")) {
            state.put("origin-path", "");
        }
        if (!state.has("origin-format")) {
            state.put("origin-format", deriveOriginFormat());
        }
        if (!state.has("last-save")) {
            state.put("last-save", nowIso8601());
        }
        if (!state.has("dirty")) {
            state.put("dirty", false);
        }
        update.accept(state);
        writeJson(stateFile, state);
    }

    /**
     * The origin format of this workspace, derived from the project file
     * extension: {@code .jipx} → {@code "jipx"}, {@code .jip} →
     * {@code "jip"}, and — for projects without a project file yet (e.g.
     * freshly loaded legacy projects before their first save) — {@code
     * "jip"} as the fallback.
     */
    private String deriveOriginFormat() {
        Path projectFile = project == null ? null : project.getProjectFile();
        if (projectFile != null) {
            if (PathUtils.isJipxPath(projectFile)) {
                return "jipx";
            }
            if (PathUtils.isJipPath(projectFile)) {
                return "jip";
            }
        }
        return "jip";
    }

    private static void writeJson(Path file, JsonNode node) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), node);
    }

    private static String nowIso8601() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
