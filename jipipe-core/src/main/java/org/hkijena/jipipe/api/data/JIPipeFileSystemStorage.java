package org.hkijena.jipipe.api.data;

import java.nio.file.Path;

/**
 * Storage on a file system
 */
public class JIPipeFileSystemStorage implements JIPipeFileStorage {
    private final Path fileSystemPath;

    public JIPipeFileSystemStorage(Path fileSystemPath) {
        this.fileSystemPath = fileSystemPath;
    }

    @Override
    public Path getFileSystemPath() {
        return fileSystemPath;
    }
}
