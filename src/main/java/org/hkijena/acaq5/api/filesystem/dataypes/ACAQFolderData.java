package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ACAQDocumentation(name = "Folder")
public class ACAQFolderData implements ACAQData {

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
