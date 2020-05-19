package org.hkijena.acaq5.extensions.filesystem.api.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;

/**
 * Data containing a file
 */
@ACAQDocumentation(name = "File")
public class FileData extends PathData {

    /**
     * Initializes file data from a file
     *
     * @param path File path
     */
    public FileData(Path path) {
        super(path);
    }

    private FileData() {
    }
}
