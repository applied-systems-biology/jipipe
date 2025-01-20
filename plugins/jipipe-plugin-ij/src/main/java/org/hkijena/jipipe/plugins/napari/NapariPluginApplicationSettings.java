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

package org.hkijena.jipipe.plugins.napari;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class NapariPluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:napari";

    private final PythonEnvironment standardEnvironment = new PythonEnvironment();
    private OptionalPythonEnvironment defaultEnvironment = new OptionalPythonEnvironment();

    public NapariPluginApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    public static NapariPluginApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, NapariPluginApplicationSettings.class);
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
        return UIUtils.getIconFromResources("apps/napari.png");
    }

    @Override
    public String getName() {
        return "Napari";
    }

    @Override
    public String getDescription() {
        return "Settings related to Napari";
    }

    private void preconfigureEnvironment(PythonEnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("org.napari.napari:*"));
    }

    public PythonEnvironment getReadOnlyDefaultEnvironment() {
        if (defaultEnvironment.isEnabled()) {
            return new PythonEnvironment(defaultEnvironment.getContent());
        } else {
            return new PythonEnvironment(standardEnvironment);
        }
    }

    @SetJIPipeDocumentation(name = "Default Napari environment", description = "The default Napari environment. " +
            "Leave at default (<code>org.napari.napari:*</code>) to automatically select the best available environment from an artifact. " +
            "If disabled, falls back to <code>org.napari.napari:*</code>.")
    @JIPipeParameter("default-napari-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Napari", allowArtifact = true, artifactFilters = {"org.napari.napari:*"})
    public OptionalPythonEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-napari-environment")
    public void setDefaultEnvironment(OptionalPythonEnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

}
