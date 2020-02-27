package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.util.List;

@ACAQDocumentation(name = "Folders")
public interface ACAQFoldersData extends ACAQFilesystemData {
    List<ACAQFolderData> getFolders();
}
