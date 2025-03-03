package org.hkijena.jipipe.desktop.commons.components.filechoosernext;

import java.nio.file.Path;

public class CommonDirectory {

    public enum DirectoryType {
        HOME, ROOT, DRIVE, GENERIC
    }

    private final Path path;
    private final DirectoryType type;

    public CommonDirectory(Path path, DirectoryType type) {
        this.path = path;
        this.type = type;
    }

    public Path getPath() {
        return path;
    }

    public DirectoryType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + path;
    }
}

