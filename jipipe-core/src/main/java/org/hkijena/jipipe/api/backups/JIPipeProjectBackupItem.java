package org.hkijena.jipipe.api.backups;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * A single backup item
 */
public class JIPipeProjectBackupItem {
    private Path projectPath;

    private String originalProjectPath;
    private LocalDateTime backupTime;
    private String sessionId;

    public String getOriginalProjectPath() {
        return originalProjectPath;
    }

    public void setOriginalProjectPath(String originalProjectPath) {
        this.originalProjectPath = originalProjectPath;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(Path projectPath) {
        this.projectPath = projectPath;
    }

    public LocalDateTime getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(LocalDateTime backupTime) {
        this.backupTime = backupTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
