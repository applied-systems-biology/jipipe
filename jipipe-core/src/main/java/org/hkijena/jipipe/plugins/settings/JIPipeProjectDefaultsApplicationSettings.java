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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Remembers the last projects
 */
public class JIPipeProjectDefaultsApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:projects";
    private ProjectTemplateEnum projectTemplate = new ProjectTemplateEnum();

    private StringList projectTemplateDownloadRepositories = new StringList();
    private boolean restoreTabs = true;

    public JIPipeProjectDefaultsApplicationSettings() {
        projectTemplate.setValue(JIPipeProjectTemplate.getFallbackTemplateId());
        projectTemplateDownloadRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/project-templates/project-templates.json");
    }

    public static JIPipeProjectDefaultsApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeProjectDefaultsApplicationSettings.class);
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

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/project-development.png");
    }

    @Override
    public String getName() {
        return "Projects";
    }

    @Override
    public String getDescription() {
        return "Project-related settings, including the default template and list of recent projects";
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
