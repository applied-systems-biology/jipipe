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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.io.JIPipeProjectReader;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.settings.application.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The JIPX (JIPipe 6) project container reader.
 * <p>
 * Reads a {@code .jipx} file - a ZIP container holding the project parts
 * as individual JSON documents plus a {@code jipx.json} manifest
 * (see {@link JIPipeXManifest} and {@link JIPipeProjectParts}):
 * <ol>
 * <li>The project's work directory is set to the container's parent
 * folder <i>before</i> the workspace is touched: the temporary base
 * directory (and thus the workspace) is memoized from the work
 * directory, so accessing the workspace first would silently pin it to
 * the global temporary base.</li>
 * <li>The container is extracted into the project workspace
 * ({@link JIPipeProjectWorkspace#extractFromZip(Path, JIPipeProgressInfo)}).</li>
 * <li>The manifest ({@code jipx.json}) is read. A container without a
 * manifest is not a JIPipe 6 project and is rejected with a
 * {@link JIPipeValidationRuntimeException} naming the missing
 * {@code jipx.json} entry - this is also how non-JIPX ZIPs (e.g.
 * {@code .crate.zip} files) are rejected during read.</li>
 * <li>The parts listed in the manifest are read
 * ({@link JIPipeProjectWorkspace#readParts()}); a listed part that is
 * absent from the container is a corrupt container and fails naming the
 * missing entry.</li>
 * <li>The parts are reassembled into the exact monolith tree
 * ({@link JIPipeProjectParts#assembleParts}) and loaded via
 * {@link JIPipeProject#fromJson} - the full upgrade machinery applies.</li>
 * <li>Same reader contract as the legacy reader: user path validation,
 * settings-gated broken-environment fixing, and the project file path is
 * recorded.</li>
 * </ol>
 */
public class JIPipeXProjectReader implements JIPipeProjectReader {

    /**
     * The stable id of the JIPX project format.
     * Persisted in saved projects - never derive from code.
     */
    public static final String FORMAT_ID = "org.hkijena.jipipe.core:jipx";

    @Override
    public String getId() {
        return FORMAT_ID;
    }

    @Override
    public List<String> getFileExtensions() {
        return List.of("jipx");
    }

    @Override
    public boolean supports(Path path) {
        return Files.isRegularFile(path)
                && PathUtils.isJipxPath(path)
                && ArchiveUtils.isZipFile(path);
    }

    @Override
    public JIPipeProject read(Path path, JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException {
        JIPipeProject project = new JIPipeProject();
        // (a) Work directory BEFORE the workspace: the temporary base
        // directory (and thus the workspace) is memoized from the work
        // directory - see the class Javadoc.
        project.setWorkDirectory(path.getParent());
        // (b) Extract the container into the workspace.
        project.getWorkspace().extractFromZip(path, progressInfo.resolve("Extract JIPX container"));
        // (c) Read the manifest; reject containers without jipx.json.
        JIPipeXManifest manifest;
        try {
            manifest = project.getWorkspace().getManifest();
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e, "Not a JIPipe project crate",
                    "The ZIP container '" + path + "' does not contain a valid " + JIPipeXManifest.MANIFEST_FILE_NAME + " manifest: " + e.getMessage(),
                    "JIPX project files are ZIP containers with a " + JIPipeXManifest.MANIFEST_FILE_NAME + " manifest at their root. " +
                            "If this file is not a JIPipe project crate, it cannot be opened by the JIPX reader.");
        }
        // (d) Read the parts listed in the manifest; a missing listed part
        // is a corrupt container (the workspace's IOException names the
        // missing entry and propagates).
        Map<String, JsonNode> parts = project.getWorkspace().readParts();
        // (d2) The main project parts are MANDATORY (spec §4): both
        // "compartments" and "graph" must be present. A manifest that
        // lacks them would otherwise reach JIPipeProject.fromJson with a
        // NullPointerException (fromJson tolerates a missing graph but
        // not a missing compartments object) or silently load an empty
        // project while the crate's mainEntity points at a missing file.
        for (String requiredPart : new String[]{"compartments", "graph"}) {
            if (!parts.containsKey(requiredPart)) {
                throw new JIPipeValidationRuntimeException(new IllegalStateException(
                        "missing required part '" + requiredPart + "'"),
                        "Invalid JIPipe project crate",
                        "The " + JIPipeXManifest.MANIFEST_FILE_NAME + " manifest of '" + path + "' does not list the required part '"
                                + requiredPart + "'.",
                        "The container is not a valid JIPipe project crate - it may be corrupt or was written by an incompatible tool. " +
                                "Please check the file or re-save the project.");
            }
        }
        // (e) Reassemble the monolith tree and load it (full upgrade
        // machinery applies).
        project.fromJson(JIPipeProjectParts.assembleParts(parts, manifest.getJipipeVersion()),
                context, report, notifications, progressInfo);
        // (f) Same reader contract as the legacy reader.
        project.validateUserPaths(notifications);
        if (JIPipeProjectDefaultsApplicationSettings.getInstance().isAutoFixProjectEnvironments()) {
            project.fixBrokenEnvironments(progressInfo.resolve("Fix environments"));
        }
        project.setProjectFile(path);
        return project;
    }
}
