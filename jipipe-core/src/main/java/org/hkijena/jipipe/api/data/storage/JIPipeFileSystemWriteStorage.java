package org.hkijena.jipipe.api.data.storage;

import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Storage on a file system. This is the simplest {@link JIPipeWriteDataStorage}.
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
    public boolean isFileSystemPathInitialized() {
        return true;
    }

    @Override
    public Path getFileSystemPath() {
        return fileSystemPath;
    }

    @Override
    public JIPipeWriteDataStorage resolve(Path path) {
        Path newPath = getFileSystemPath().resolve(path);
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to create directory '" + newPath + "'!",
                    toString(), "The path might be invalid, or you might not have the permissions to write in a parent folder.",
                    "Check if the path is valid, and you have write-access.");
        }
        return new JIPipeFileSystemWriteStorage(newPath, getInternalPath().resolve(path));
    }

    @Override
    public OutputStream write(Path path) {
        try {
            return new FileOutputStream(getFileSystemPath().resolve(path).toFile(), false);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "{Filesystem Write} /" + getInternalPath() + " @ " + getFileSystemPath();
    }

}
