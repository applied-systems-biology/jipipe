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
