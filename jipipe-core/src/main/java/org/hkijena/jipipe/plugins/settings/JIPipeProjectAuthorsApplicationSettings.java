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
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeProjectAuthorsApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public final static String ID = "org.hkijena.jipipe:project-authors";

    private JIPipeAuthorMetadata.List projectAuthors = new JIPipeAuthorMetadata.List();
    private boolean automaticallyAddToProjects = true;
    private boolean warnNoAuthors = true;

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

    @SetJIPipeDocumentation(name = "Authors", description = "The list of authors that should be added to edited projects")
    @JIPipeParameter("project-authors")
    public JIPipeAuthorMetadata.List getProjectAuthors() {
        return projectAuthors;
    }

    @JIPipeParameter("project-authors")
    public void setProjectAuthors(JIPipeAuthorMetadata.List projectAuthors) {
        this.projectAuthors = projectAuthors;
    }

    public static JIPipeProjectAuthorsApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeProjectAuthorsApplicationSettings.class);
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
}
