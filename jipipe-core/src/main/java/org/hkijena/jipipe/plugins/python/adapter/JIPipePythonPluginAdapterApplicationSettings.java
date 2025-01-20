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
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class JIPipePythonPluginAdapterApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements JIPipeExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:python-adapter";
    private final JIPipePythonAdapterLibraryEnvironment standardEnvironment = new JIPipePythonAdapterLibraryEnvironment();
    private OptionalJIPipePythonAdapterLibraryEnvironment defaultEnvironment = new OptionalJIPipePythonAdapterLibraryEnvironment();
    private JIPipePythonAdapterLibraryEnvironment.List presets = new JIPipePythonAdapterLibraryEnvironment.List();

    public JIPipePythonPluginAdapterApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    public static JIPipePythonPluginAdapterApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipePythonPluginAdapterApplicationSettings.class);
    }

    private void preconfigureEnvironment(JIPipePythonAdapterLibraryEnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("org.hkijena.jipipe-python-adapter:*"));
    }

    public JIPipePythonAdapterLibraryEnvironment getReadOnlyDefaultEnvironment() {
        if (defaultEnvironment.isEnabled()) {
            return new JIPipePythonAdapterLibraryEnvironment(defaultEnvironment.getContent());
        } else {
            return new JIPipePythonAdapterLibraryEnvironment(standardEnvironment);
        }
    }

    @SetJIPipeDocumentation(name = "Python adapter presets", description = "List of presets stored for JIPipe Python adapters")
    @JIPipeParameter("python-adapter-presets")
    public JIPipePythonAdapterLibraryEnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("python-adapter-presets")
    public void setPresets(JIPipePythonAdapterLibraryEnvironment.List presets) {
        this.presets = presets;
    }

    @SetJIPipeDocumentation(name = "Default JIPipe Python adapter library", description = "This environment allows you to setup how the JIPipe Python adapter library is supplied. " +
            "Leave at the default (<code>org.hkijena.jipipe-python-adapter:*</code>) to setup the library automatically. " +
            "If disabled, falls back to <code>org.hkijena.jipipe-python-adapter:*</code>")
    @JIPipeParameter("default-python-adapter-library")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.hkijena.jipipe-python-adapter:*"})
    public OptionalJIPipePythonAdapterLibraryEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-python-adapter-library")
    public void setDefaultEnvironment(OptionalJIPipePythonAdapterLibraryEnvironment defaultEnvironment) {
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
            this.presets.add((JIPipePythonAdapterLibraryEnvironment) preset);
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
