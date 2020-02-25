package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

@ACAQDocumentation(name = "File")
public class ACAQFileData extends ACAQData {

    private ACAQFolderData parent;
    private Path filePath;

    public ACAQFileData(ACAQFolderData parent, Path filePath) {
        this.parent = parent;
        this.filePath = filePath;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public Path getFilePath() {
        return filePath;
    }

    public ACAQFolderData getParent() {
        return parent;
    }
}
