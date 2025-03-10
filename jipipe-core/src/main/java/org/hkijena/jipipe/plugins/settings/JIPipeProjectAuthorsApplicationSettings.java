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
import org.hkijena.jipipe.api.OptionalJIPipeAuthorMetadata;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeProjectAuthorsApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public final static String ID = "org.hkijena.jipipe:project-authors";

    private OptionalJIPipeAuthorMetadata.List projectAuthors = new OptionalJIPipeAuthorMetadata.List();
    private boolean automaticallyAddToProjects = true;
    private boolean warnNoAuthors = true;

    public static JIPipeProjectAuthorsApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeProjectAuthorsApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Warn if no authors are configured", description = "Show a tooltip if no authors are configured")
    @JIPipeParameter("warn-no-authors")
    public boolean isWarnNoAuthors() {
        return warnNoAuthors;
    }

    @JIPipeParameter("warn-no-authors")
    public void setWarnNoAuthors(boolean warnNoAuthors) {
        this.warnNoAuthors = warnNoAuthors;
    }

    @SetJIPipeDocumentation(name = "Automatically add to projects", description = "If enabled, automatically add the authors to projects")
    @JIPipeParameter("auto-add-to-projects")
    public boolean isAutomaticallyAddToProjects() {
        return automaticallyAddToProjects;
    }

    @JIPipeParameter("auto-add-to-projects")
    public void setAutomaticallyAddToProjects(boolean automaticallyAddToProjects) {
        this.automaticallyAddToProjects = automaticallyAddToProjects;
    }

    @SetJIPipeDocumentation(name = "Authors", description = "The list of authors that should be added to edited projects. You can enable/disable authors to exclude them from being added to projects.")
    @JIPipeParameter("project-authors")
    public OptionalJIPipeAuthorMetadata.List getProjectAuthors() {
        return projectAuthors;
    }

    @JIPipeParameter("project-authors")
    public void setProjectAuthors(OptionalJIPipeAuthorMetadata.List projectAuthors) {
        this.projectAuthors = projectAuthors;
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
        return UIUtils.getIconFromResources("actions/icon_user.png");
    }

    @Override
    public String getName() {
        return "Project authors";
    }

    @Override
    public String getDescription() {
        return "Authors that are automatically added to the project";
    }

    public boolean isConfigured() {
        for (OptionalJIPipeAuthorMetadata projectAuthor : projectAuthors) {
            if (projectAuthor.isEnabled()) {
                return true;
            }
        }
        return false;
    }
}
