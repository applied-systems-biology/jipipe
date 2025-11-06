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

package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.api.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JIPipeProjectUserPaths extends AbstractJIPipeParameterCollection {

    private JIPipeParameterCollectionList paths = JIPipeParameterCollectionList.containingCollection(UserPathEntry.class);

    @SetJIPipeDocumentation(name = "User paths", description = "A list of paths that can be used in various nodes")
    @JIPipeParameter("user-directories")
    @JsonGetter("user-directories")
    @PathParameterSettings(pathMode = PathType.DirectoriesOnly, ioMode = PathIOMode.Open)
    @ParameterCollectionListTemplate(UserPathEntry.class)
    public JIPipeParameterCollectionList getPaths() {
        return paths;
    }

    @JIPipeParameter("user-directories")
    @JsonSetter("user-directories")
    public void setPaths(JIPipeParameterCollectionList paths) {
        this.paths = paths;
    }

    public List<UserPathEntry> getUserPathsAsInstance() {
        return paths.mapToCollection(UserPathEntry.class);
    }

    /**
     * Gets a map of user-defined directories
     *
     * @return the directories
     */
    public Map<String, Path> getDirectoryMap(Path projectDir) {
        Map<String, Path> result = new HashMap<>();
        for (UserPathEntry userPathEntry : paths.mapToCollection(UserPathEntry.class)) {
            if (!StringUtils.isNullOrEmpty(userPathEntry.getKey())) {
                Path path = userPathEntry.path;
                if (path != null && !StringUtils.isNullOrEmpty(path.toString())) {
                    if (projectDir != null && projectDir.isAbsolute() && !path.isAbsolute()) {
                        path = projectDir.resolve(path);
                    }
                }
                result.put(userPathEntry.key, path);
            }
        }
        return result;
    }

    /**
     * Gets a map of user-defined directories that must exist
     *
     * @return the directories
     */
    public Map<String, Path> getMandatoryUserPathsMap(Path projectDir) {
        Map<String, Path> result = new HashMap<>();
        for (UserPathEntry userPathEntry : paths.mapToCollection(UserPathEntry.class)) {
            if (userPathEntry.getRole() == Role.Input && !StringUtils.isNullOrEmpty(userPathEntry.getKey())) {
                Path path = userPathEntry.path;
                if (path != null && !StringUtils.isNullOrEmpty(path.toString())) {
                    if (projectDir != null && projectDir.isAbsolute() && !path.isAbsolute()) {
                        path = projectDir.resolve(path);
                    }
                }
                result.put(userPathEntry.key, path);
            }
        }
        return result;
    }

    public void setUserPath(String key, Path value) {
        for (JIPipeDynamicParameterCollection parameterCollection : paths) {
            String currentKey = parameterCollection.get("key").get(String.class);
            if (Objects.equals(key, currentKey)) {
                parameterCollection.setParameter("path", value);
                return;
            }
        }

        JIPipeDynamicParameterCollection parameterCollection = paths.addNewInstance();
        parameterCollection.setParameter("key", key);
        parameterCollection.setParameter("path", value);
    }

    public enum Role {
        Input,
        Output,
        Unspecified,
        Ignored
    }

    public static class UserPathEntry extends AbstractJIPipeParameterCollection {
        private String name;
        private String description;
        private String key;
        private Path path;
        private Role role = Role.Unspecified;

        public UserPathEntry() {
        }

        public UserPathEntry(UserPathEntry other) {
            this.key = other.key;
            this.path = other.path;
            this.role = other.role;
            this.name = other.name;
            this.description = other.description;
        }

        @SetJIPipeDocumentation(name = "Name", description = "Name as displayed in the UI")
        @JIPipeParameter(value = "name", uiOrder = -95)
        public String getName() {
            return name;
        }

        @JIPipeParameter("name")
        public void setName(String name) {
            this.name = name;
        }

        @SetJIPipeDocumentation(name = "Description", description = "Description as displayed in the UI")
        @JIPipeParameter(value = "description", uiOrder = -92)
        public String getDescription() {
            return description;
        }

        @JIPipeParameter("description")
        public void setDescription(String description) {
            this.description = description;
        }

        @SetJIPipeDocumentation(name = "Key", description = "The key that will be used to access the path. Cannot be empty.")
        @JIPipeParameter(value = "key", uiOrder = -100)
        public String getKey() {
            return key;
        }

        @JIPipeParameter("key")
        public void setKey(String key) {
            this.key = key;
        }

        @SetJIPipeDocumentation(name = "Path", description = "The path that will be referenced")
        @JIPipeParameter(value = "path", uiOrder = -90)
        @PathParameterSettings(pathMode = PathType.FilesAndDirectories, ioMode = PathIOMode.Open)
        public Path getPath() {
            return path;
        }

        @JIPipeParameter("path")
        public void setPath(Path path) {
            this.path = path;
        }

        @SetJIPipeDocumentation(name = "Role", description = "The role of this path (input/output/unspecified). If the role is set to 'Input', JIPipe will check if the path exists.")
        @JIPipeParameter("role")
        public Role getRole() {
            return role;
        }

        @JIPipeParameter("role")
        public void setRole(Role role) {
            this.role = role;
        }
    }
}
