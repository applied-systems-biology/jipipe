package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ACAQDocumentation(name = "File")
public class ACAQFileData implements ACAQData {

    private Path filePath;

    public ACAQFileData(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
    }

    public Path getFilePath() {
        return filePath;
    }
}
