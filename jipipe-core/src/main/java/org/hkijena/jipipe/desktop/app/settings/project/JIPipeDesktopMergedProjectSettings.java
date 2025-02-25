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

package org.hkijena.jipipe.desktop.app.settings.project;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.Collections;
import java.util.Map;

public class JIPipeDesktopMergedProjectSettings extends AbstractJIPipeParameterCollection implements JIPipeCustomParameterCollection {

    private final JIPipeProject project;
    private final JIPipeDesktopMergedProjectSettingsUserDirectories userDirectoriesSettings;

    public JIPipeDesktopMergedProjectSettings(JIPipeProject project) {
        this.project = project;
        this.userDirectoriesSettings = new JIPipeDesktopMergedProjectSettingsUserDirectories(project);
        registerSubParameter(userDirectoriesSettings);
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        return Collections.emptyMap();
    }

    @Override
    public boolean getIncludeReflectionParameters() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Project-wide directories", description = "Project-wide directories that can be access through a variety of nodes and expressions. " +
            "The set of directories can be modified in the project settings.")
    @JIPipeParameter(value = "user-directories",
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/places/folder-blue.png")
    public JIPipeDesktopMergedProjectSettingsUserDirectories getUserDirectoriesSettings() {
        return userDirectoriesSettings;
    }

    public JIPipeProject getProject() {
        return project;
    }
}
