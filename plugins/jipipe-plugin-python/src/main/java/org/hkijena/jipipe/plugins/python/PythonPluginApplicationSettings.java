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
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class PythonPluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements JIPipeExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:python";
    private final PythonEnvironment standardEnvironment = new PythonEnvironment();
    private OptionalPythonEnvironment defaultEnvironment = new OptionalPythonEnvironment();
    private PythonEnvironment.List presets = new PythonEnvironment.List();

    public PythonPluginApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    private void preconfigureEnvironment(PythonEnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("org.python.python_prepackaged:*"));
    }

    public static PythonPluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, PythonPluginApplicationSettings.class);
    }

    /**
     * Gets the (standard) default Python environment
     * @return the default
     */
    public PythonEnvironment getReadOnlyDefaultEnvironment() {
        if(defaultEnvironment.isEnabled()) {
            return new PythonEnvironment(defaultEnvironment.getContent());
        }
        else {
            return new PythonEnvironment(standardEnvironment);
        }
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
            "Leave at default (<code>org.python.python_prepackaged:*</code>) to automatically select the best available Python environment from an artifact. " +
            "If not enabled, will always use <code>org.python.python_prepackaged:*</code>.")
    @JIPipeParameter("default-python-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.python.*"})
    public OptionalPythonEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-python-environment")
    public void setDefaultEnvironment(OptionalPythonEnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
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
