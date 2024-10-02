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

package org.hkijena.jipipe.plugins.r;

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

public class RPluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements JIPipeExternalEnvironmentSettings {

    public static String ID = "org.hkijena.jipipe:r";
    private final REnvironment standardEnvironment = new REnvironment();
    private OptionalREnvironment defaultEnvironment = new OptionalREnvironment();
    private REnvironment.List presets = new REnvironment.List();

    public RPluginApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    private void preconfigureEnvironment(REnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("org.r.r_prepackaged:*"));
    }

    public static RPluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, RPluginApplicationSettings.class);
    }

    public REnvironment getReadOnlyEnvironment() {
        if(defaultEnvironment.isEnabled()) {
            return new REnvironment(defaultEnvironment.getContent());
        }
        else {
            return new REnvironment(standardEnvironment);
        }
    }

    @SetJIPipeDocumentation(name = "R environment", description = "Describes the R environment to use. If not enabled, falls back to <code>org.r.*</code>.")
    @JIPipeParameter("default-r-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.r.*"})
    public OptionalREnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-r-environment")
    public void setDefaultEnvironment(OptionalREnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    @SetJIPipeDocumentation(name = "Presets", description = "List of presets stored for R environments.")
    @JIPipeParameter("presets")
    public REnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(REnvironment.List presets) {
        this.presets = presets;
    }

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.presets.add((REnvironment) preset);
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
        return UIUtils.getIconFromResources("apps/rlogo_icon.png");
    }

    @Override
    public String getName() {
        return "R integration";
    }

    @Override
    public String getDescription() {
        return "Settings related to the R integration";
    }
}
