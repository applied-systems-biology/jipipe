package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ACAQDocumentation(name = "Files")
public class ACAQFileListData implements ACAQFilesData {

    private List<ACAQFileData> entries;
    private Map<String, Object> annotations = new HashMap<>();

    public ACAQFileListData(List<ACAQFileData> entries) {
        this.entries = entries;
    }

    @Override
    public List<ACAQFileData> getFiles() {
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
    public void saveTo(Path storageFilePath, String name) {

    }
}
