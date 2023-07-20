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

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.filesystem.PathParameterSettings;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata for a {@link JIPipeProject}
 */
public class JIPipeProjectMetadata extends JIPipeMetadata {
    private JIPipeImageJUpdateSiteDependency.List updateSiteDependencies = new JIPipeImageJUpdateSiteDependency.List();
    private String templateDescription = "";
    private JIPipeProjectPermissions permissions = new JIPipeProjectPermissions();
    private JIPipeNodeTemplate.List nodeTemplates = new JIPipeNodeTemplate.List();
    private boolean restoreTabs = true;
    private ParameterCollectionList directories = ParameterCollectionList.containingCollection(DirectoryEntry.class);

    @JIPipeDocumentation(name = "Restore tabs", description = "If enabled, all tabs are restored on loading the project. Otherwise, the Project overview and Compartments " +
            "tab are opened.")
    @JIPipeParameter(value = "restore-tabs", uiOrder = -1)
    @JsonGetter("restore-tabs")
    public boolean isRestoreTabs() {
        return restoreTabs;
    }

    @JIPipeParameter("restore-tabs")
    @JsonSetter("restore-tabs")
    public void setRestoreTabs(boolean restoreTabs) {
        this.restoreTabs = restoreTabs;
    }

    @JIPipeDocumentation(name = "ImageJ update site dependencies", description = "ImageJ update sites that should be enabled for the project to work. Use this if you rely on " +
            "third-party methods that are not referenced in a JIPipe extension (e.g. within a script or macro node). " +
            "Users will get a notification if a site is not activated or found. Both name and URL should be set. The URL is only used if a site with the same name " +
            "does not already exist in the user's repository.")
    @JIPipeParameter(value = "update-site-dependencies", uiOrder = 10)
    @JsonGetter("update-site-dependencies")
    public JIPipeImageJUpdateSiteDependency.List getUpdateSiteDependencies() {
        return updateSiteDependencies;
    }

    @JIPipeParameter("update-site-dependencies")
    @JsonSetter("update-site-dependencies")
    public void setUpdateSiteDependencies(JIPipeImageJUpdateSiteDependency.List updateSiteDependencies) {
        this.updateSiteDependencies = updateSiteDependencies;
    }

    @JIPipeDocumentation(name = "Template description", description = "Description used in the 'New from template' list if this project is used as custom project template.")
    @JIPipeParameter("template-description")
    @JsonGetter("template-description")
    public String getTemplateDescription() {
        return templateDescription;
    }

    @JIPipeParameter("template-description")
    @JsonSetter("template-description")
    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    @JIPipeParameter("permissions")
    @JIPipeDocumentation(name = "Permissions", description = "Here you can set various permissions that affect what parts of " +
            "each project users can change.")
    @JsonGetter("permissions")
    public JIPipeProjectPermissions getPermissions() {
        return permissions;
    }

    @JsonSetter("permissions")
    public void setPermissions(JIPipeProjectPermissions permissions) {
        this.permissions = permissions;
    }

    @JIPipeDocumentation(name = "Node templates", description = "A list of node templates that will be available for users who edit the project.")
    @JIPipeParameter("node-templates")
    @JsonGetter("node-templates")
    public JIPipeNodeTemplate.List getNodeTemplates() {
        return nodeTemplates;
    }

    @JIPipeParameter("node-templates")
    @JsonSetter("node-templates")
    public void setNodeTemplates(JIPipeNodeTemplate.List nodeTemplates) {
        this.nodeTemplates = nodeTemplates;
    }

    @JIPipeDocumentation(name = "Directories", description = "A list of directories that can be used in various nodes")
    @JIPipeParameter("directories")
    public ParameterCollectionList getDirectories() {
        return directories;
    }
    @JIPipeParameter("directories")
    public void setDirectories(ParameterCollectionList directories) {
        this.directories = directories;
    }

    /**
     * Gets a map of user-defined directories
     * @return the directories
     */
    public Map<String, Path> getDirectoryMap(Path projectDir) {
        Map<String, Path> result = new HashMap<>();
        for (DirectoryEntry directoryEntry : directories.mapToCollection(DirectoryEntry.class)) {
            if(!StringUtils.isNullOrEmpty(directoryEntry.getKey())) {
                Path path = directoryEntry.path;
                if(path != null && !StringUtils.isNullOrEmpty(path.toString())) {
                    if(projectDir != null && projectDir.isAbsolute() && !path.isAbsolute()) {
                        path = projectDir.resolve(path);
                    }
                }
                result.put(directoryEntry.key, path);
            }
        }
        return result;
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
