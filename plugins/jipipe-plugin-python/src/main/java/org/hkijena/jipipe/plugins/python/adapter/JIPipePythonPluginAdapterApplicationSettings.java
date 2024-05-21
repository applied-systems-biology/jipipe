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

package org.hkijena.jipipe.plugins.python.adapter;

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
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class JIPipePythonPluginAdapterApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements ExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:python-adapter";
    private JIPipePythonAdapterLibraryEnvironment defaultPythonAdapterLibraryEnvironment = new JIPipePythonAdapterLibraryEnvironment();
    private JIPipePythonAdapterLibraryEnvironment.List pythonAdapterPresets = new JIPipePythonAdapterLibraryEnvironment.List();

    public JIPipePythonPluginAdapterApplicationSettings() {
        defaultPythonAdapterLibraryEnvironment.setLoadFromArtifact(true);
        defaultPythonAdapterLibraryEnvironment.setArtifactQuery(new JIPipeArtifactQueryParameter("org.hkijena.jipipe-python-adapter:*"));
    }

    public static JIPipePythonPluginAdapterApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipePythonPluginAdapterApplicationSettings.class);
    }

    /**
     * Checks the Python settings
     *
     * @return if the settings are correct
     */
    public static boolean pythonSettingsAreValid() {
        if (JIPipe.getInstance() != null) {
            JIPipePythonPluginAdapterApplicationSettings instance = getInstance();
            JIPipeValidationReport report = new JIPipeValidationReport();
            instance.getDefaultPythonAdapterLibraryEnvironment().reportValidity(new UnspecifiedValidationReportContext(), report);
            return report.isValid();
        }
        return false;
    }

    @SetJIPipeDocumentation(name = "Python adapter presets", description = "List of presets stored for JIPipe Python adapters")
    @JIPipeParameter("python-adapter-presets")
    public JIPipePythonAdapterLibraryEnvironment.List getPythonAdapterPresets() {
        return pythonAdapterPresets;
    }

    @JIPipeParameter("python-adapter-presets")
    public void setPythonAdapterPresets(JIPipePythonAdapterLibraryEnvironment.List pythonAdapterPresets) {
        this.pythonAdapterPresets = pythonAdapterPresets;
    }

    @SetJIPipeDocumentation(name = "Default JIPipe Python adapter library", description = "This environment allows you to setup how the JIPipe Python adapter library is supplied. " +
            "Leave at the default (<code>org.hkijena.jipipe-python-adapter:*</code>) to setup the library automatically.")
    @JIPipeParameter("default-python-adapter-library")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.hkijena.jipipe-python-adapter:*"})
    public JIPipePythonAdapterLibraryEnvironment getDefaultPythonAdapterLibraryEnvironment() {
        return defaultPythonAdapterLibraryEnvironment;
    }

    @JIPipeParameter("default-python-adapter-library")
    public void setDefaultPythonAdapterLibraryEnvironment(JIPipePythonAdapterLibraryEnvironment defaultPythonAdapterLibraryEnvironment) {
        this.defaultPythonAdapterLibraryEnvironment = defaultPythonAdapterLibraryEnvironment;
    }

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(pythonAdapterPresets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.pythonAdapterPresets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.pythonAdapterPresets.add((JIPipePythonAdapterLibraryEnvironment) preset);
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
        return "Python integration (adapter)";
    }

    @Override
    public String getDescription() {
        return "Settings for the Python adapter library that is utilized by JIPipe";
    }
}
