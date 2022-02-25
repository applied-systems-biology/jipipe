package org.hkijena.jipipe.api.data.storage;

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
    public boolean isFileSystemPathInitialized() {
        return true;
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
    public JIPipeReadDataStorage resolve(Path path) {
        return new JIPipeFileSystemReadStorage(getFileSystemPath().resolve(path), getInternalPath().resolve(path));
    }

    @Override
    public boolean isFile(String name) {
        return Files.isRegularFile(getFileSystemPath().resolve(name));
    }

    @Override
    public boolean isFile(Path path) {
        return Files.isRegularFile(getFileSystemPath().resolve(path));
    }

    @Override
    public boolean exists(String name) {
        return Files.exists(getFileSystemPath().resolve(name));
    }

    @Override
    public boolean exists(Path path) {
        return Files.exists(getFileSystemPath().resolve(path));
    }

    @Override
    public boolean isDirectory(String name) {
        return Files.isDirectory(getFileSystemPath().resolve(name));
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
    public InputStream open(String name) {
        try {
            return new FileInputStream(getFileSystemPath().resolve(name).toFile());
        } catch (FileNotFoundException e) {
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
}
