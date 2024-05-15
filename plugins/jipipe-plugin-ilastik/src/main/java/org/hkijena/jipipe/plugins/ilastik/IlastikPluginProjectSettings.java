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

package org.hkijena.jipipe.plugins.ilastik;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;

import javax.swing.*;

public class IlastikPluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:ilastik";

    private OptionalIlastikEnvironment projectDefaultEnvironment = new OptionalIlastikEnvironment();

    public IlastikPluginProjectSettings() {

    }

    @SetJIPipeDocumentation(name = "Project default environment", description = "If enabled, overwrite the application-wide Ilastik environment and store them inside the project. ")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = { "org.embl.ilastik:*" })
    public OptionalIlastikEnvironment getProjectDefaultEnvironment() {
        return projectDefaultEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultEnvironment(OptionalIlastikEnvironment projectDefaultEnvironment) {
        this.projectDefaultEnvironment = projectDefaultEnvironment;
    }

    @Override
    public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultProjectSettingsSheetCategory.Plugins;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return IlastikPlugin.RESOURCES.getIconFromResources("ilastik.png");
    }

    @Override
    public String getName() {
        return "Ilastik";
    }

    @Override
    public String getDescription() {
        return "Settings related to the Ilastik integration";
    }
}
