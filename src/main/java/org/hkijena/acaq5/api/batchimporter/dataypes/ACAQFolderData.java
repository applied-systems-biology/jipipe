package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

@ACAQDocumentation(name = "Folder")
public class ACAQFolderData extends ACAQData {

    private Path folderPath;

    public ACAQFolderData(Path folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public Path getFolderPath() {
        return folderPath;
    }
}
