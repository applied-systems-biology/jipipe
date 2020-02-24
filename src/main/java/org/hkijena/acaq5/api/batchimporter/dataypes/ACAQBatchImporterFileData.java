package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.io.IOException;
import java.nio.file.Path;

@ACAQDocumentation(name = "File")
public class ACAQBatchImporterFileData extends ACAQData {

    private Path fileName;

    public ACAQBatchImporterFileData(Path fileName) {

        this.fileName = fileName;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public Path getFileName() {
        return fileName;
    }
}
