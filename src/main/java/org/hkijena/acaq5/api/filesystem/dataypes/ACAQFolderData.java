package org.hkijena.acaq5.api.filesystem.dataypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ACAQDocumentation(name = "Folder")
public class ACAQFolderData implements ACAQData {

    private Path folderPath;

    public ACAQFolderData(Path folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        Path jsonFile = storageFilePath.resolve(name + ".json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonGetter("acaq:data-class")
    private String getDataTypeName() {
        return getClass().getCanonicalName();
    }

    @JsonGetter("folder-path")
    public Path getFolderPath() {
        return folderPath;
    }

    @JsonSetter("folder-path")
    private void setFolderPath(Path path) {
        this.folderPath = path;
    }
}
