package org.hkijena.acaq5.api.batchimporter.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@ACAQDocumentation(name = "Files")
public interface ACAQFilesData extends ACAQFilesystemData {
    List<ACAQFileData> getFiles();
}
