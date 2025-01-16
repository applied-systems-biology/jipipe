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

package org.hkijena.jipipe.plugins.ijfilaments.settings;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsPlugin;
import org.hkijena.jipipe.plugins.ijfilaments.environments.OptionalTSOAXEnvironment;
import org.hkijena.jipipe.plugins.ijfilaments.environments.TSOAXEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

import javax.swing.*;
import java.util.List;

public class TSOAXApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements JIPipeExternalEnvironmentSettings {

    public static final String ID = "org.hkijena.jipipe:filaments-tsoax";
    private TSOAXEnvironment standardEnvironment = new TSOAXEnvironment();
    private OptionalTSOAXEnvironment defaultEnvironment = new OptionalTSOAXEnvironment();
    private TSOAXEnvironment.List presets = new TSOAXEnvironment.List();

    public TSOAXApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    public static TSOAXApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, TSOAXApplicationSettings.class);
    }

    private void preconfigureEnvironment(TSOAXEnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("com.github.tix209.tsoax:*"));
        environment.setArguments(new JIPipeExpressionParameter("cli_parameters"));
    }

    public TSOAXEnvironment getReadOnlyDefaultEnvironment() {
        if (defaultEnvironment.isEnabled()) {
            return new TSOAXEnvironment(defaultEnvironment.getContent());
        } else {
            return new TSOAXEnvironment(standardEnvironment);
        }
    }

    @SetJIPipeDocumentation(name = "TSOAX environment", description = "Contains information about the location of the TSOAX installation.")
    @JIPipeParameter("default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"com.github.tix209.tsoax:*"})
    public OptionalTSOAXEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-environment")
    public void setDefaultEnvironment(OptionalTSOAXEnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    @SetJIPipeDocumentation(name = "Presets", description = "List of TSOAX environment presets")
    @JIPipeParameter("presets")
    public TSOAXEnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(TSOAXEnvironment.List presets) {
        this.presets = presets;
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
        return FilamentsPlugin.RESOURCES.getIconFromResources("tsoax.png");
    }

    @Override
    public String getName() {
        return "TSOAX (Filaments)";
    }

    @Override
    public String getDescription() {
        return "Settings related to the TSOAX integration for filaments processing";
    }

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.presets.add((TSOAXEnvironment) preset);
        }
    }
}
