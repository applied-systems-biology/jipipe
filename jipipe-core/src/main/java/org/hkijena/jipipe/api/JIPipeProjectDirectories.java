package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JIPipeProjectDirectories extends AbstractJIPipeParameterCollection {

    private ParameterCollectionList directories = ParameterCollectionList.containingCollection(DirectoryEntry.class);

    @JIPipeDocumentation(name = "User directories", description = "A list of directories that can be used in various nodes")
    @JIPipeParameter("user-directories")
    @JsonGetter("user-directories")
    @PathParameterSettings(pathMode = PathType.DirectoriesOnly, ioMode = PathIOMode.Open)
    public ParameterCollectionList getDirectories() {
        return directories;
    }

    @JIPipeParameter("user-directories")
    @JsonSetter("user-directories")
    public void setDirectories(ParameterCollectionList directories) {
        this.directories = directories;
    }

    /**
     * Gets a map of user-defined directories
     *
     * @return the directories
     */
    public Map<String, Path> getDirectoryMap(Path projectDir) {
        Map<String, Path> result = new HashMap<>();
        for (DirectoryEntry directoryEntry : directories.mapToCollection(DirectoryEntry.class)) {
            if (!StringUtils.isNullOrEmpty(directoryEntry.getKey())) {
                Path path = directoryEntry.path;
                if (path != null && !StringUtils.isNullOrEmpty(path.toString())) {
                    if (projectDir != null && projectDir.isAbsolute() && !path.isAbsolute()) {
                        path = projectDir.resolve(path);
                    }
                }
                result.put(directoryEntry.key, path);
            }
        }
        return result;
    }

    public void setUserDirectory(String key, Path value) {
        for (JIPipeDynamicParameterCollection parameterCollection : directories) {
            String currentKey = parameterCollection.get("key").get(String.class);
            if(Objects.equals(key, currentKey)) {
                parameterCollection.setParameter("path", value);
                return;
            }
        }

        JIPipeDynamicParameterCollection parameterCollection = directories.addNewInstance();
        parameterCollection.setParameter("key", key);
        parameterCollection.setParameter("path", value);
    }

    public static class DirectoryEntry extends AbstractJIPipeParameterCollection {
        private String key;
        private Path path;

        public DirectoryEntry() {
        }

        public DirectoryEntry(DirectoryEntry other) {
            this.key = other.key;
            this.path = other.path;
        }

        @JIPipeDocumentation(name = "Key", description = "The key that will be used to access the directory. Cannot be empty.")
        @JIPipeParameter(value = "key", uiOrder = -100)
        public String getKey() {
            return key;
        }

        @JIPipeParameter("key")
        public void setKey(String key) {
            this.key = key;
        }

        @JIPipeDocumentation(name = "Path", description = "The path that will be referenced")
        @JIPipeParameter(value = "path", uiOrder = -90)
        @PathParameterSettings(pathMode = PathType.DirectoriesOnly, ioMode = PathIOMode.Open)
        public Path getPath() {
            return path;
        }

        @JIPipeParameter("path")
        public void setPath(Path path) {
            this.path = path;
        }
    }
}
