package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ACAQDocumentation(name = "Folders")
public class ACAQFolderListData implements ACAQFoldersData {

    private List<ACAQFolderData> entries;
    private Map<String, Object> annotations = new HashMap<>();

    public ACAQFolderListData(List<ACAQFolderData> entries) {
        this.entries = entries;
    }

    @Override
    public List<ACAQFolderData> getFolders() {
        return entries;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public Object findAnnotation(String key) {
        Object existing = annotations.get(key);
        return existing;
    }

    @Override
    public void annotate(String key, Object value) {
        annotations.put(key, value);
    }

    @Override
    public void saveTo(Path storageFolderPath, String name) {

    }
}
