package org.hkijena.acaq5.api.filesystem.dataypes;

import org.hkijena.acaq5.api.ACAQDocumentation;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ACAQDocumentation(name = "Folder")
public class ACAQFolderData implements ACAQFoldersData {

    private ACAQFolderData parent;
    private Path folderPath;
    private Map<String, Object> annotations = new HashMap<>();

    public ACAQFolderData(ACAQFolderData parent, Path folderPath) {
        this.parent = parent;
        this.folderPath = folderPath;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    public Path getFolderPath() {
        return folderPath;
    }

    public ACAQFolderData getParent() {
        return parent;
    }

    public ACAQFolderData resolveToFolder(Path path) {
        return new ACAQFolderData(this, folderPath.resolve(path));
    }

    public ACAQFileData resolveToFile(Path path) {
        return new ACAQFileData(this, folderPath.resolve(path));
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
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
    public List<ACAQFolderData> getFolders() {
        return Arrays.asList(this);
    }
}
