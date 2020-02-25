package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ACAQDocumentation(name = "Folders")
public class ACAQFoldersData extends ACAQData {

    private List<ACAQFolderData> folders;

    public ACAQFoldersData(List<ACAQFolderData> folders) {
        this.folders = folders;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public List<ACAQFolderData> getFolders() {
        return Collections.unmodifiableList(folders);
    }
}
