package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.util.List;

@ACAQDocumentation(name = "Files")
public interface ACAQFilesData extends ACAQFilesystemData {
    List<ACAQFileData> getFiles();
}
