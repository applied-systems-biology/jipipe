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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * A project workspace: the materialized, per-project working copy of a
 * project's parts plus its {@code workspace.json} state (origin
 * information and save/dirty flags, spec §5.5 recovery hooks).
 * <p>
 * This is the seam for remote workspaces: future implementations (e.g.
 * container- or server-backed) implement the same interface. The
 * directory-based v1 implementation is
 * {@link JIPipeLocalProjectWorkspace}.
 * <p>
 * <b>Idempotency contract:</b> materializing a workspace is a pure
 * derivation from the project model — it (re)writes the listed part
 * files, the manifest ({@code jipx.json}), and the workspace state file
 * ({@code workspace.json}), but never deletes or rewrites any other
 * entry of the workspace (payload files such as {@code data/foo.tiff}
 * survive re-materialization).
 * <p>
 * <b>Concurrency contract:</b> the workspace instance is the lock object
 * for the JIPX save sequence. Concurrent saves of the SAME project (e.g.
 * the EDT save and the backup-queue save) must synchronize the whole
 * materialize → crate/README/manifest write → zip → self-verify → atomic
 * move → markSaved sequence on the workspace
 * ({@code synchronized (workspace) { ... }}) — otherwise a torn container
 * can pass the self-verify (each part parses individually) and silently
 * replace the target with a chimera.
 */
public interface JIPipeProjectWorkspace {

    /**
     * @return the base directory of this workspace
     */
    Path getBaseDirectory();

    /**
     * Materializes this workspace from the current project model: writes
     * each part (see {@link JIPipeProjectParts#sliceParts}) as
     * pretty-printed JSON at its default manifest path, writes the
     * manifest ({@code jipx.json}), and refreshes the workspace state
     * ({@code workspace.json}).
     * <p>
     * Idempotent: existing unknown/payload files in the base directory
     * are never deleted or overwritten — only the listed part files,
     * the manifest, and the state file are written. The manifest's
     * {@code unknown} entries from a pre-existing manifest are merged
     * into the new one (spec §4: unknown parts are preserved verbatim on
     * round-trip).
     *
     * @param progressInfo the progress info
     * @throws IOException triggered by file or JSON I/O
     */
    void materializeFromModel(JIPipeProgressInfo progressInfo) throws IOException;

    /**
     * Extracts the given JIPX container zip into this workspace's base
     * directory (with a zip-slip guard).
     *
     * @param zipFile      the container zip
     * @param progressInfo the progress info
     * @throws IOException triggered by file or zip I/O
     */
    void extractFromZip(Path zipFile, JIPipeProgressInfo progressInfo) throws IOException;

    /**
     * Reads the manifest ({@code jipx.json}) and every part listed in
     * its parts table from this workspace.
     *
     * @return part key → part JSON node (in the manifest's table order)
     * @throws IOException if the manifest cannot be read or a listed
     *                     part file is missing (corrupt container)
     */
    Map<String, JsonNode> readParts() throws IOException;

    /**
     * Marks this workspace as saved from the given origin path:
     * records {@code last-save} (now, ISO-8601), clears the dirty flag,
     * and stores {@code origin-path} (empty when the origin is null).
     *
     * @param originPath the path the workspace was saved to, or null
     * @throws IOException triggered by file I/O
     */
    void markSaved(Path originPath) throws IOException;

    /**
     * Marks this workspace as dirty (unsaved changes).
     *
     * @throws IOException triggered by file I/O
     */
    void markDirty() throws IOException;

    /**
     * Gets this workspace's manifest, reading {@code jipx.json} lazily
     * (cached after the first read).
     *
     * @return the manifest
     * @throws IOException if {@code jipx.json} is missing or unreadable
     */
    JIPipeXManifest getManifest() throws IOException;
}
