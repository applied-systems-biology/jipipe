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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsPlugin;
import org.hkijena.jipipe.plugins.ijfilaments.environments.OptionalTSOAXEnvironment;
import org.hkijena.jipipe.plugins.ijfilaments.environments.TSOAXEnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;

import javax.swing.*;
import java.util.List;

public class FilamentsPluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:filaments";

    private OptionalTSOAXEnvironment projectDefaultTSOAXEnvironment = new OptionalTSOAXEnvironment();

    public FilamentsPluginProjectSettings() {
        autoConfigureTSOAXEnvironment();
    }

    private void autoConfigureTSOAXEnvironment() {
        if (TSOAXApplicationSettings.getInstance().getDefaultEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(TSOAXApplicationSettings.getInstance().getDefaultEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                TSOAXEnvironment environment = new TSOAXEnvironment();
                environment.setName("");
                environment.setLoadFromArtifact(true);
                environment.setArtifactQuery(new JIPipeArtifactQueryParameter(target.getFullId()));

                projectDefaultTSOAXEnvironment.setEnabled(true);
                projectDefaultTSOAXEnvironment.setContent(environment);
            }
        }
    }


    @SetJIPipeDocumentation(name = "Project default TSOAX environment", description = "If enabled, overwrite the application-wide TSOAX environment and store them inside the project. ")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"com.github.tix209.tsoax:*"})
    public OptionalTSOAXEnvironment getProjectDefaultTSOAXEnvironment() {
        return projectDefaultTSOAXEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultTSOAXEnvironment(OptionalTSOAXEnvironment projectDefaultTSOAXEnvironment) {
        this.projectDefaultTSOAXEnvironment = projectDefaultTSOAXEnvironment;
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
        return FilamentsPlugin.RESOURCES.getIconFromResources("data-type-filaments.png");
    }

    @Override
    public String getName() {
        return "Filaments";
    }

    @Override
    public String getDescription() {
        return "Settings related to the filaments plugin";
    }
}
