package org.hkijena.acaq5.api.events;

import java.nio.file.Path;

/**
 * Triggered when the work directory of a project or algorithm was changed
 */
public class WorkDirectoryChangedEvent {
    private Path workDirectory;

    public WorkDirectoryChangedEvent(Path workDirectory) {
        this.workDirectory = workDirectory;
    }

    public Path getWorkDirectory() {
        return workDirectory;
    }
}
