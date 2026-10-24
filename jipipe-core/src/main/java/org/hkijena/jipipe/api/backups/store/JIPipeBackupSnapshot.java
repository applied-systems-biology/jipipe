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

package org.hkijena.jipipe.api.backups.store;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

/**
 * The manifest of one content-addressed backup snapshot, serialized as
 * {@code <snapshot-id>.json} in the store's {@code snapshots/} directory:
 * <pre>
 * {
 *   "snapshot-id": "&lt;uuid&gt;",
 *   "created": "&lt;ISO-8601 local date-time&gt;",
 *   "project-format": "jip | jipx",
 *   "project-name": "&lt;original project file name&gt;",
 *   "entries": [
 *     { "path": "project/graph.json", "size": 12345, "hash": "&lt;sha256&gt;" },
 *     ...
 *   ]
 * }
 * </pre>
 * A {@code .jip} backup has exactly one entry ({@code path} = {@code "project.jip"});
 * a {@code .jipx} backup has one entry per workspace file.
 * <p>
 * This is a pure data holder: entry hashes are validated by the store
 * ({@link JIPipeBackupObjectStore}) at use time, not here. The order of
 * {@code entries} is whatever the creator stored (the creator sorts by path
 * for deterministic output); serialization preserves list order.
 */
public class JIPipeBackupSnapshot {
    private String snapshotId;
    private String created;
    private String projectFormat;
    private String projectName;
    private List<SnapshotEntry> entries = new ArrayList<>();

    @JsonGetter("snapshot-id")
    public String getSnapshotId() {
        return snapshotId;
    }

    @JsonSetter("snapshot-id")
    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    /**
     * @return the creation time as an ISO_LOCAL_DATE_TIME string, or null
     */
    @JsonGetter("created")
    public String getCreated() {
        return created;
    }

    @JsonSetter("created")
    public void setCreated(String created) {
        this.created = created;
    }

    /**
     * @return the project format this snapshot was created from: {@code "jip"} or {@code "jipx"}
     */
    @JsonGetter("project-format")
    public String getProjectFormat() {
        return projectFormat;
    }

    @JsonSetter("project-format")
    public void setProjectFormat(String projectFormat) {
        this.projectFormat = projectFormat;
    }

    /**
     * @return the original project file name (e.g. {@code "my-project.jipx"})
     */
    @JsonGetter("project-name")
    public String getProjectName() {
        return projectName;
    }

    @JsonSetter("project-name")
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return the snapshot entries, in stored order (never null)
     */
    @JsonGetter("entries")
    public List<SnapshotEntry> getEntries() {
        if (entries == null)
            entries = new ArrayList<>();
        return entries;
    }

    @JsonSetter("entries")
    public void setEntries(List<SnapshotEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    /**
     * @return the sum of the sizes of all entries (0 if there are none)
     */
    public long getTotalSize() {
        long total = 0;
        for (SnapshotEntry entry : getEntries())
            total += entry.getSize();
        return total;
    }

    /**
     * One file of a snapshot: its workspace-relative path (for {@code .jip}
     * snapshots, {@code "project.jip"} by convention), its size in bytes, and
     * the SHA-256 of its exact content (content-addressed object identity).
     */
    public static class SnapshotEntry {
        private String path;
        private long size;
        private String hash;

        @JsonGetter("path")
        public String getPath() {
            return path;
        }

        @JsonSetter("path")
        public void setPath(String path) {
            this.path = path;
        }

        @JsonGetter("size")
        public long getSize() {
            return size;
        }

        @JsonSetter("size")
        public void setSize(long size) {
            this.size = size;
        }

        @JsonGetter("hash")
        public String getHash() {
            return hash;
        }

        @JsonSetter("hash")
        public void setHash(String hash) {
            this.hash = hash;
        }
    }
}
