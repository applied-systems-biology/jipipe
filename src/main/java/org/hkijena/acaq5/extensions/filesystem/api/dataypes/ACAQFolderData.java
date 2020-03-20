package org.hkijena.acaq5.extensions.filesystem.api.dataypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;

@ACAQDocumentation(name = "Folder")
public class ACAQFolderData implements ACAQData {

    private Path folderPath;

    public ACAQFolderData(Path folderPath) {
        this.folderPath = folderPath;
    }

    private ACAQFolderData() {
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

    @JsonGetter("acaq:data-type")
    private String getDataTypeName() {
        return ACAQDatatypeRegistry.getInstance().getIdOf(getClass());
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
