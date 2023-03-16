package org.hkijena.jipipe.extensions.parameters.library.filesystem;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;

import java.nio.file.Files;
import java.nio.file.Path;

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

    @JIPipeDocumentation(name = "Name", description = "The name of the bookmark")
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

    @JIPipeDocumentation(name = "Path", description = "The path")
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
}
