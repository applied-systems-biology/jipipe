package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ACAQDocumentation(name = "Files")
public class ACAQFilesData extends ACAQData {

    private List<Path> fileNames;

    public ACAQFilesData(List<Path> fileNames) {
        this.fileNames = fileNames;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public List<Path> getFileNames() {
        return Collections.unmodifiableList(fileNames);
    }
}
