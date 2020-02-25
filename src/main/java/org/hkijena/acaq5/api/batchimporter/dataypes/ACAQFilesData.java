package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ACAQDocumentation(name = "Files")
public class ACAQFilesData extends ACAQData {

    private List<ACAQFileData> files;

    public ACAQFilesData(List<ACAQFileData> files) {
        this.files = files;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public List<ACAQFileData> getFiles() {
        return Collections.unmodifiableList(files);
    }
}
