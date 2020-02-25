package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ACAQDocumentation(name = "Folders")
public class ACAQFoldersData extends ACAQData {

    private List<Path> folderPaths;

    public ACAQFoldersData(List<Path> folderPaths) {
        this.folderPaths = folderPaths;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public List<Path> getFolderPaths() {
        return Collections.unmodifiableList(folderPaths);
    }
}
