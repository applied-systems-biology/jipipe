package org.hkijena.jipipe.api.data.storage;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Storage on a file system
 */
public class JIPipeFileSystemReadDataStorage implements JIPipeReadDataStorage {
    private final JIPipeProgressInfo progressInfo;
    private final Path fileSystemPath;
    private final Path internalPath;

    public JIPipeFileSystemReadDataStorage(JIPipeProgressInfo progressInfo, Path fileSystemPath) {
        this.progressInfo = progressInfo;
        this.fileSystemPath = fileSystemPath;
        this.internalPath = Paths.get("");
    }

    public JIPipeFileSystemReadDataStorage(JIPipeProgressInfo progressInfo, Path fileSystemPath, Path internalPath) {
        this.progressInfo = progressInfo;
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
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public JIPipeReadDataStorage resolve(Path path) {
        return new JIPipeFileSystemReadDataStorage(progressInfo, getFileSystemPath().resolve(path), getInternalPath().resolve(path));
    }

    @Override
    public boolean isFile(Path path) {
        return Files.isRegularFile(getFileSystemPath().resolve(path));
    }


    @Override
    public boolean exists(Path path) {
        return Files.exists(getFileSystemPath().resolve(path));
    }

    @Override
    public boolean isDirectory(Path path) {
        return Files.isDirectory(getFileSystemPath().resolve(path));
    }

    @Override
    public Collection<Path> list() {
        try {
            return Files.list(getFileSystemPath()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream open(Path path) {
        try {
            return new FileInputStream(getFileSystemPath().resolve(path).toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "{Filesystem Read} /" + getInternalPath() + " @ " + getFileSystemPath();
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }
}
