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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.plugins.ilastik.IlastikPlugin;
import org.hkijena.jipipe.plugins.ilastik.environments.IlastikEnvironment;
import org.hkijena.jipipe.plugins.ilastik.environments.OptionalIlastikEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

import javax.swing.*;
import java.util.List;

public class IlastikPluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:ilastik";

    private OptionalIlastikEnvironment projectDefaultEnvironment = new OptionalIlastikEnvironment();

    public IlastikPluginProjectSettings() {
        autoConfigureEnvironment();
    }

    private void autoConfigureEnvironment() {
        if (IlastikPluginApplicationSettings.getInstance().getDefaultEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(IlastikPluginApplicationSettings.getInstance().getDefaultEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                IlastikEnvironment environment = new IlastikEnvironment();
                environment.setName("");
                environment.setLoadFromArtifact(true);
                environment.setArtifactQuery(new JIPipeArtifactQueryParameter(target.getFullId()));

                projectDefaultEnvironment.setEnabled(true);
                projectDefaultEnvironment.setContent(environment);
            }
        }
    }


    @SetJIPipeDocumentation(name = "Project default environment", description = "If enabled, overwrite the application-wide Ilastik environment and store them inside the project. ")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"org.embl.ilastik:*"})
    public OptionalIlastikEnvironment getProjectDefaultEnvironment() {
        return projectDefaultEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultEnvironment(OptionalIlastikEnvironment projectDefaultEnvironment) {
        this.projectDefaultEnvironment = projectDefaultEnvironment;
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
}
