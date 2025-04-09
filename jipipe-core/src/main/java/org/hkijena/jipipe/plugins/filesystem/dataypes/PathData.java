/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.filesystem.dataypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeFastThumbnail;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeTextThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Encapsulates a {@link java.nio.file.Path}
 */
@SetJIPipeDocumentation(name = "Path", description = "A file or folder")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.json file. The JSON data has following structure: " +
        "<pre>" +
        "{\n" +
        "    \"jipipe:data-type\": \"[Data type ID]\",\n" +
        "    \"path\": \"[The path]\"\n" +
        "}" +
        "</pre>", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/path-data.schema.json")
@JIPipeFastThumbnail
public class PathData implements JIPipeData {
    private String path;

    /**
     * Initializes file data from a file
     *
     * @param path File path
     */
    public PathData(Path path) {
        this.path = path.toString();
    }

    public PathData(String path) {
        this.path = path;
    }

    protected PathData() {
    }

    public static PathData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(PathData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path jsonFile = storage.getFileSystemPath().resolve(name + ".json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new PathData(path);
    }

    @Override
    public JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        String name = "N/A";
        if (path != null) {
            try {
                name = Paths.get(path).getFileName().toString();
            } catch (InvalidPathException e) {
            }
        }
        return new JIPipeTextThumbnailData(name);
    }

    /**
     * Returns the data type name stored into the JSON
     *
     * @return data type id
     */
    @JsonGetter("jipipe:data-type")
    private String getDataTypeName() {
        return JIPipe.getDataTypes().getIdOf(getClass());
    }

    @JsonGetter("path")
    public String getPath() {
        return path;
    }

    @JsonSetter("path")
    public void setPath(String path) {
        this.path = path;
    }

    public void setPath(Path path) {
        this.path = path.toString();
    }

    @Override
    public String toString() {
        return path;
    }

    public Path toPath() {
        return Paths.get(path);
    }
}
