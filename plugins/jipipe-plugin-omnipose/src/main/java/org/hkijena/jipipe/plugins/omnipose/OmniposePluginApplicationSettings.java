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

package org.hkijena.jipipe.plugins.omnipose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;

import javax.swing.*;

public class OmniposePluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:omnipose";

    private final PythonEnvironment standardEnvironment = new PythonEnvironment();
    private OptionalPythonEnvironment defaultEnvironment = new OptionalPythonEnvironment();

    public OmniposePluginApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    public static OmniposePluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, OmniposePluginApplicationSettings.class);
    }

    private void preconfigureEnvironment(PythonEnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("com.github.kevinjohncutler.omnipose:*"));
    }

    public PythonEnvironment getReadOnlyDefaultEnvironment() {
        if (defaultEnvironment.isEnabled()) {
            return new PythonEnvironment(defaultEnvironment.getContent());
        } else {
            return new PythonEnvironment(standardEnvironment);
        }
    }

    @SetJIPipeDocumentation(name = "Omnipose Python environment", description = "The default Omnipose environment that is associated to newly created projects. " +
            "Leave at default (<code>com.github.kevinjohncutler.omnipose:*</code>) to automatically select the best available environment from an artifact. " +
            "If disabled, falls back to <code>com.github.kevinjohncutler.omnipose:*</code>.")
    @JIPipeParameter("default-omnipose-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Omnipose", allowArtifact = true, artifactFilters = {"com.github.kevinjohncutler.omnipose:*"})
    public OptionalPythonEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-omnipose-environment")
    public void setDefaultEnvironment(OptionalPythonEnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
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
        return OmniposePlugin.RESOURCES.getIconFromResources("omnipose.png");
    }

    @Override
    public String getName() {
        return "Omnipose";
    }

    @Override
    public String getDescription() {
        return "Settings related to the Omnipose integration";
    }
}
