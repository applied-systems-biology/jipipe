package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

@ACAQDocumentation(name = "Folder")
public class ACAQFolderData extends ACAQData {

    private ACAQFolderData parent;
    private Path folderPath;

    public ACAQFolderData(ACAQFolderData parent, Path folderPath) {
        this.parent = parent;
        this.folderPath = folderPath;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public Path getFolderPath() {
        return folderPath;
    }

    public ACAQFolderData getParent() {
        return parent;
    }

    public ACAQFolderData resolveToFolder(Path path) {
        return new ACAQFolderData(this, folderPath.resolve(path));
    }

    public ACAQFileData resolveToFile(Path path) {
        return new ACAQFileData(this, folderPath.resolve(path));
    }
}
