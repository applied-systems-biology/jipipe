package org.hkijena.jipipe.api.data.storage;

import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Storage on a file system
 */
public class JIPipeFileSystemWriteStorage implements JIPipeWriteDataStorage {
    private final Path fileSystemPath;
    private final Path internalPath;

    public JIPipeFileSystemWriteStorage(Path fileSystemPath) {
        this.fileSystemPath = fileSystemPath;
        this.internalPath = Paths.get("");
    }

    public JIPipeFileSystemWriteStorage(Path fileSystemPath, Path internalPath) {
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
    public JIPipeFileSystemWriteStorage resolve(String name) {
        Path path = getFileSystemPath().resolve(name);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to create directory '" + path + "'!",
                    toString(), "The path might be invalid, or you might not have the permissions to write in a parent folder.",
                    "Check if the path is valid, and you have write-access.");
        }
        return new JIPipeFileSystemWriteStorage(path, getInternalPath().resolve(name));
    }

    @Override
    public String toString() {
        return "{Filesystem Write} /" + getInternalPath() + " @ " + getFileSystemPath();
    }
}
