package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ACAQDocumentation(name = "File")
public class ACAQFileData implements ACAQFilesData {

    private ACAQFolderData parent;
    private Path filePath;
    private Map<String, Object> annotations = new HashMap<>();

    public ACAQFileData(ACAQFolderData parent, Path filePath) {
        this.parent = parent;
        this.filePath = filePath;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
    }

    public Path getFilePath() {
        return filePath;
    }

    public ACAQFolderData getParent() {
        return parent;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return null;
    }

    @Override
    public Object findAnnotation(String key) {
        Object existing = annotations.get(key);
        if(existing == null && parent != null)
            existing = parent.findAnnotation(key);
        return existing;
    }

    @Override
    public void annotate(String key, Object value) {
        annotations.put(key, value);
    }

    @Override
    public List<ACAQFileData> getFiles() {
        return Arrays.asList(this);
    }
}
