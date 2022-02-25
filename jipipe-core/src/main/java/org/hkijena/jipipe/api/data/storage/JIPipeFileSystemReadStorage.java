package org.hkijena.jipipe.api.data.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Storage on a file system
 */
public class JIPipeFileSystemReadStorage implements JIPipeReadDataStorage {
    private final Path fileSystemPath;
    private final Path internalPath;

    public JIPipeFileSystemReadStorage(Path fileSystemPath) {
        this.fileSystemPath = fileSystemPath;
        this.internalPath = Paths.get("");
    }

    public JIPipeFileSystemReadStorage(Path fileSystemPath, Path internalPath) {
        this.fileSystemPath = fileSystemPath;
        this.internalPath = internalPath;
    }

    @Override
    public Path getInternalPath() {
        return internalPath;
    }

    @Override
    public Path getFileSystemPath() {
        return fileSystemPath;
    }

    @Override
    public JIPipeReadDataStorage resolve(String name) {
        return new JIPipeFileSystemReadStorage(getFileSystemPath().resolve(name), getInternalPath().resolve(name));
    }

    @Override
    public String toString() {
        return "{Filesystem Read} /" + getInternalPath() + " @ " + getFileSystemPath();
    }
}
