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

package org.hkijena.jipipe.plugins.ilastik.settings;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.ilastik.IlastikPlugin;
import org.hkijena.jipipe.plugins.ilastik.environments.IlastikEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

import javax.swing.*;
import java.util.List;

public class IlastikPluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements ExternalEnvironmentSettings {

    public static final String ID = "org.hkijena.jipipe:ilastik";
    private IlastikEnvironment defaultEnvironment = new IlastikEnvironment();
    private IlastikEnvironment.List presets = new IlastikEnvironment.List();
    private int maxThreads = -1;
    private int maxMemory = 4096;

    public IlastikPluginApplicationSettings() {
        defaultEnvironment.setLoadFromArtifact(true);
        defaultEnvironment.setArtifactQuery(new JIPipeArtifactQueryParameter("org.embl.ilastik:*"));
        defaultEnvironment.setArguments(new JIPipeExpressionParameter("cli_parameters"));
    }

    public static IlastikPluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, IlastikPluginApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Ilastik environment", description = "Contains information about the location of the Ilastik installation.")
    @JIPipeParameter("default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.embl.ilastik:*"})
    public IlastikEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-environment")
    public void setDefaultEnvironment(IlastikEnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    @SetJIPipeDocumentation(name = "Presets", description = "List of Ilastik environment presets")
    @JIPipeParameter("presets")
    public IlastikEnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(IlastikEnvironment.List presets) {
        this.presets = presets;
    }

    @SetJIPipeDocumentation(name = "Maximum number of threads", description = "The maximum number of threads Ilastik will utilize. Negative or zero values indicate no limitation.")
    @JIPipeParameter("max-threads")
    public int getMaxThreads() {
        return maxThreads;
    }

    @JIPipeParameter("max-threads")
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    @SetJIPipeDocumentation(name = "Maximum RAM allocation (MB)", description = "The maximum RAM that Ilastik will utilize. Must be at least 256 (values below that limit will be automatically increased)")
    @JIPipeParameter("max-memory")
    public int getMaxMemory() {
        return maxMemory;
    }

    @JIPipeParameter("max-memory")
    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
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

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.presets.add((IlastikEnvironment) preset);
        }
    }
}
