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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class PythonPluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:python";
    private PythonEnvironment defaultPythonEnvironment = new PythonEnvironment();
    private PythonEnvironment.List presets = new PythonEnvironment.List();

    public PythonPluginApplicationSettings() {
        defaultPythonEnvironment.setLoadFromArtifact(true);
        defaultPythonEnvironment.setArtifactQuery(new JIPipeArtifactQueryParameter("org.python.python_prepackaged:*"));
    }

    public static PythonPluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, PythonPluginApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Presets", description = "List of presets stored for Python environments.")
    @JIPipeParameter("presets")
    public PythonEnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(PythonEnvironment.List presets) {
        this.presets = presets;
    }

    @SetJIPipeDocumentation(name = "Default Python environment", description = "The default Python environment that is associated to newly created projects. " +
            "Leave at default (<code>org.python.python_prepackaged:*</code>) to automatically select the best available Python environment from an artifact.")
    @JIPipeParameter("default-python-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = { "org.python.*" })
    public PythonEnvironment getDefaultPythonEnvironment() {
        return defaultPythonEnvironment;
    }

    @JIPipeParameter("default-python-environment")
    public void setDefaultPythonEnvironment(PythonEnvironment defaultPythonEnvironment) {
        this.defaultPythonEnvironment = defaultPythonEnvironment;
    }

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.presets.add((PythonEnvironment) preset);
        }
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Plugins;
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
