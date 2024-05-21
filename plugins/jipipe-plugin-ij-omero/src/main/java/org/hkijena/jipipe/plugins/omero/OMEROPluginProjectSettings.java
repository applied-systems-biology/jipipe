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

package org.hkijena.jipipe.plugins.omero;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class OMEROPluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:omero";
    private OptionalOMEROCredentialsEnvironment projectDefaultEnvironment = new OptionalOMEROCredentialsEnvironment();

    public OMEROPluginProjectSettings() {

    }

    public OMEROPluginProjectSettings(OMEROPluginProjectSettings other) {
        this.projectDefaultEnvironment = new OptionalOMEROCredentialsEnvironment(other.projectDefaultEnvironment);
    }

    @SetJIPipeDocumentation(name = "Project default credentials", description = "If enabled, overwrite the application-wide OMERO default credentials and store them inside the project. " +
            "Will be used to access the OMERO server.")
    @JIPipeParameter("project-default-environment")
    public OptionalOMEROCredentialsEnvironment getProjectDefaultEnvironment() {
        return projectDefaultEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultEnvironment(OptionalOMEROCredentialsEnvironment projectDefaultEnvironment) {
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
        return UIUtils.getIconFromResources("apps/omero.png");
    }

    @Override
    public String getName() {
        return "OMERO";
    }

    @Override
    public String getDescription() {
        return "Settings for the OMERO integration";
    }
}
