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

package org.hkijena.jipipe.plugins.parameters.library.filesystem;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileChooserBookmark extends AbstractJIPipeParameterCollection {
    private String name;

    private Path path;

    public FileChooserBookmark() {

    }

    public FileChooserBookmark(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    public FileChooserBookmark(FileChooserBookmark other) {
        this.name = other.name;
        this.path = other.path;
    }

    @SetJIPipeDocumentation(name = "Name", description = "The name of the bookmark")
    @JsonGetter("name")
    @JIPipeParameter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Path", description = "The path")
    @JIPipeParameter("path")
    @PathParameterSettings(pathMode = PathType.DirectoriesOnly, ioMode = PathIOMode.Open)
    @JsonGetter("path")
    public Path getPath() {
        return path;
    }

    @JIPipeParameter("path")
    @JsonSetter("path")
    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isDirectory() {
        if (path == null)
            return false;
        return Files.isDirectory(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileChooserBookmark that = (FileChooserBookmark) o;
        return Objects.equals(name, that.name) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path);
    }
}
