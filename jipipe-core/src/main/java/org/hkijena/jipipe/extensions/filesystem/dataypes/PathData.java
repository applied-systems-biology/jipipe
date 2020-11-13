/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.filesystem.dataypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Encapsulates a {@link java.nio.file.Path}
 */
@JIPipeDocumentation(name = "Path", description = "A file or folder")
public class PathData implements JIPipeData {
    private Path path;

    /**
     * Initializes file data from a file
     *
     * @param path File path
     */
    public PathData(Path path) {
        this.path = path;
    }

    protected PathData() {
    }

    public static PathData importFrom(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(PathData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
        Path jsonFile = storageFilePath.resolve(name + ".json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate() {
        return new FileData(path);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        try {
            Desktop.getDesktop().open(getPath().toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    /**
     * @return The path
     */
    @JsonGetter("path")
    public Path getPath() {
        return path;
    }

    /**
     * Sets the file
     *
     * @param path The path
     */
    @JsonSetter("path")
    private void setPath(Path path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "" + path;
    }
}
