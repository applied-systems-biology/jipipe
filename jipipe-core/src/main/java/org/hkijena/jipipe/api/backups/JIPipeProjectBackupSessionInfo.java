package org.hkijena.jipipe.api.backups;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Contains basic information about a project backup session
 * The serialized file should be contained within a backup-info.json file in a directory that contains all backup files of that session.
 */
public class JIPipeProjectBackupSessionInfo {
    private String projectSessionId;
    private String projectStoragePath;
    private String lastDateTimeInfo;

    @JsonGetter("project-session-id")
    public String getProjectSessionId() {
        return projectSessionId;
    }

    @JsonSetter("project-session-id")
    public void setProjectSessionId(String projectSessionId) {
        this.projectSessionId = projectSessionId;
    }

    @JsonGetter("project-storage-path")
    public String getProjectStoragePath() {
        return projectStoragePath;
    }

    @JsonSetter("project-storage-path")
    public void setProjectStoragePath(String projectStoragePath) {
        this.projectStoragePath = projectStoragePath;
    }

    @JsonGetter("last-backup-date-time")
    public String getLastDateTimeInfo() {
        return lastDateTimeInfo;
    }

    @JsonSetter("last-backup-date-time")
    public void setLastDateTimeInfo(String lastDateTimeInfo) {
        this.lastDateTimeInfo = lastDateTimeInfo;
    }
}
