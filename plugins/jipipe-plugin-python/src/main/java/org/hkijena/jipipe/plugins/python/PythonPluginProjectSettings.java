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

package org.hkijena.jipipe.plugins.python;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.plugins.python.adapter.OptionalJIPipePythonAdapterLibraryEnvironment;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class PythonPluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:python";

    private OptionalPythonEnvironment projectDefaultEnvironment = new OptionalPythonEnvironment();
    private OptionalJIPipePythonAdapterLibraryEnvironment projectPythonAdapterLibraryEnvironment = new OptionalJIPipePythonAdapterLibraryEnvironment();

    public PythonPluginProjectSettings() {

    }

    @SetJIPipeDocumentation(name = "Project default environment", description = "If enabled, overwrite the application-wide Python environment and store them inside the project.")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = { "org.python.*" })
    public OptionalPythonEnvironment getProjectDefaultEnvironment() {
        return projectDefaultEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultEnvironment(OptionalPythonEnvironment projectDefaultEnvironment) {
        this.projectDefaultEnvironment = projectDefaultEnvironment;
    }

    @SetJIPipeDocumentation(name = "Project default Python adapter", description = "If enabled, overwrite the application-wide JIPipe Python adapter and store them inside the project.")
    @JIPipeParameter("project-default-jipipe-adapter-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.hkijena.jipipe-python-adapter:*"})
    public OptionalJIPipePythonAdapterLibraryEnvironment getProjectPythonAdapterLibraryEnvironment() {
        return projectPythonAdapterLibraryEnvironment;
    }

    @JIPipeParameter("project-default-jipipe-adapter-environment")
    public void setProjectPythonAdapterLibraryEnvironment(OptionalJIPipePythonAdapterLibraryEnvironment projectPythonAdapterLibraryEnvironment) {
        this.projectPythonAdapterLibraryEnvironment = projectPythonAdapterLibraryEnvironment;
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
        return UIUtils.getIconFromResources("apps/python.png");
    }

    @Override
    public String getName() {
        return "Python integration";
    }

    @Override
    public String getDescription() {
        return "Settings related to the Python integration";
    }
}
