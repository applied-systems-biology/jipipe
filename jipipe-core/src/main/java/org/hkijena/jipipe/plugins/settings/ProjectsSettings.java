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

package org.hkijena.jipipe.plugins.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.filesystem.PathList;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Remembers the last projects
 */
public class ProjectsSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "projects";
    private PathList recentProjects = new PathList();
    private PathList recentJsonExtensionProjects = new PathList();
    private ProjectTemplateEnum projectTemplate = new ProjectTemplateEnum();

    private StringList projectTemplateDownloadRepositories = new StringList();
    private boolean restoreTabs = true;

    public ProjectsSettings() {
        projectTemplate.setValue(JIPipeProjectTemplate.getFallbackTemplateId());
        projectTemplateDownloadRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/project-templates/project-templates.json");
    }

    public static ProjectsSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ProjectsSettings.class);
    }

    @SetJIPipeDocumentation(name = "Recent projects", description = "List of recent projects")
    @JIPipeParameter("recent-projects")
    public PathList getRecentProjects() {
        return recentProjects;
    }

    @JIPipeParameter("recent-projects")
    public void setRecentProjects(PathList recentProjects) {
        this.recentProjects = recentProjects;

    }

    @SetJIPipeDocumentation(name = "Recent JSON extension projects", description = "List of recent JSON extension projects")
    @JIPipeParameter("recent-json-extension-projects")
    public PathList getRecentJsonExtensionProjects() {
        return recentJsonExtensionProjects;
    }

    @JIPipeParameter("recent-json-extension-projects")
    public void setRecentJsonExtensionProjects(PathList recentJsonExtensionProjects) {
        this.recentJsonExtensionProjects = recentJsonExtensionProjects;

    }

    /**
     * Adds a project file to the list of recent projects
     *
     * @param fileName Project file
     */
    public void addRecentProject(Path fileName) {
        int index = recentProjects.indexOf(fileName);
        if (index == -1) {
            recentProjects.add(0, fileName);
            emitParameterChangedEvent("recent-projects");
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        } else if (index != 0) {
            recentProjects.remove(index);
            recentProjects.add(0, fileName);
            emitParameterChangedEvent("recent-projects");
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
        }
    }

    /**
     * Adds a JSON extension file to the list of recent JSON extensions
     *
     * @param fileName JSON extension file
     */
    public void addRecentJsonExtension(Path fileName) {
        int index = recentJsonExtensionProjects.indexOf(fileName);
        if (index == -1) {
            recentJsonExtensionProjects.add(0, fileName);
            emitParameterChangedEvent("recent-json-extension-projects");
            JIPipe.getSettings().save();
        } else if (index != 0) {
            recentJsonExtensionProjects.remove(index);
            recentJsonExtensionProjects.add(0, fileName);
            emitParameterChangedEvent("recent-json-extension-projects");
            JIPipe.getSettings().save();
        }
    }

    @SetJIPipeDocumentation(name = "New project template", description = "Template used for creating new projects")
    @JIPipeParameter("new-project-template-2")
    public ProjectTemplateEnum getProjectTemplate() {
        return projectTemplate;
    }

    @JIPipeParameter("new-project-template-2")
    public void setProjectTemplate(ProjectTemplateEnum projectTemplate) {
        this.projectTemplate = projectTemplate;
    }

    @SetJIPipeDocumentation(name = "Restore tabs on opening projects", description = "If enabled, JIPipe attempts to restore the tabs when opening a project.")
    @JIPipeParameter("restore-tabs")
    public boolean isRestoreTabs() {
        return restoreTabs;
    }

    @JIPipeParameter("restore-tabs")
    public void setRestoreTabs(boolean restoreTabs) {
        this.restoreTabs = restoreTabs;
    }

    @SetJIPipeDocumentation(name = "Template downloader repositories", description = "List of repositories for the 'Get more templates' feature")
    @JIPipeParameter("template-download-repositories")
    public StringList getProjectTemplateDownloadRepositories() {
        return projectTemplateDownloadRepositories;
    }

    @JIPipeParameter("template-download-repositories")
    public void setProjectTemplateDownloadRepositories(StringList projectTemplateDownloadRepositories) {
        this.projectTemplateDownloadRepositories = projectTemplateDownloadRepositories;
    }

    /**
     * An enum of strings that point to {@link JIPipeProjectTemplate}
     */
    public static class ProjectTemplateEnum extends DynamicEnumParameter<String> {
        public ProjectTemplateEnum() {
            initialize();
        }

        public ProjectTemplateEnum(ProjectTemplateEnum other) {
            this.setAllowedValues(other.getAllowedValues());
            this.setValue(other.getValue());
        }

        private void initialize() {
            if (JIPipe.getInstance() != null && JIPipe.getInstance().getProjectTemplateRegistry() != null) {
                setAllowedValues(new ArrayList<>(JIPipe.getInstance().getProjectTemplateRegistry().getRegisteredTemplates().keySet()));
            }
        }

        @Override
        public String renderLabel(String value) {
            if (JIPipe.getInstance() != null && JIPipe.getInstance().getProjectTemplateRegistry() != null) {
                JIPipeProjectTemplate template = JIPipe.getInstance().getProjectTemplateRegistry().getRegisteredTemplates().getOrDefault(value, null);
                if (template != null) {
                    return template.getMetadata().getName();
                } else {
                    return "<Invalid>";
                }
            } else {
                return value;
            }
        }

        @Override
        public Icon renderIcon(String value) {
            return UIUtils.getIconFromResources("mimetypes/application-jipipe.png");
        }
    }
}
