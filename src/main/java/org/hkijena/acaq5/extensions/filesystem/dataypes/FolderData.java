package org.hkijena.acaq5.extensions.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;

/**
 * Data that stores a folder
 */
@ACAQDocumentation(name = "Folder")
public class FolderData extends PathData {

    /**
     * Initializes file data from a file
     *
     * @param path File path
     */
    public FolderData(Path path) {
        super(path);
    }

    private FolderData() {
    }
}
