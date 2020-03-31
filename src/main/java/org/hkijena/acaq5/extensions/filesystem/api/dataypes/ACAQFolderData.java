package org.hkijena.acaq5.extensions.filesystem.api.dataypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Data that stores a folder
 */
@ACAQDocumentation(name = "Folder")
public class ACAQFolderData implements ACAQData {

    private Path folderPath;

    /**
     * Instantiates the data from a folder path
     * @param folderPath Folder path
     */
    public ACAQFolderData(Path folderPath) {
        this.folderPath = folderPath;
    }

    /**
     * Creates a new instance
     */
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

    /**
     * Gets the data type name
     * @return Data type name
     */
    @JsonGetter("acaq:data-type")
    private String getDataTypeName() {
        return ACAQDatatypeRegistry.getInstance().getIdOf(getClass());
    }

    /**
     * @return The folder path
     */
    @JsonGetter("folder-path")
    public Path getFolderPath() {
        return folderPath;
    }

    /**
     * Sets the folder path
     * @param path Folder path
     */
    @JsonSetter("folder-path")
    private void setFolderPath(Path path) {
        this.folderPath = path;
    }
}
