package org.hkijena.jipipe.plugins.omnipose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.python.OptionalPythonEnvironment;
import org.hkijena.jipipe.plugins.python.PythonEnvironment;

import javax.swing.*;
import java.util.List;

public class OmniposePluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static String ID = "org.hkijena.jipipe:omnipose";
    private OptionalPythonEnvironment projectDefaultEnvironment = new OptionalPythonEnvironment();

    public OmniposePluginProjectSettings() {
        autoConfigureDefaultEnvironment();
    }

    private void autoConfigureDefaultEnvironment() {
        if (OmniposePluginApplicationSettings.getInstance().getDefaultOmniposeEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(OmniposePluginApplicationSettings.getInstance().getDefaultOmniposeEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                PythonEnvironment environment = new PythonEnvironment();
                environment.setLoadFromArtifact(true);
                environment.setArtifactQuery(new JIPipeArtifactQueryParameter(target.getFullId()));

                projectDefaultEnvironment.setEnabled(true);
                projectDefaultEnvironment.setContent(environment);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Project default environment", description = "If enabled, overwrite the application-wide Omnipose environment and store them inside the project.")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Omnipose", allowArtifact = true, artifactFilters = {"com.github.kevinjohncutler.omnipose:*"})
    public OptionalPythonEnvironment getProjectDefaultEnvironment() {
        return projectDefaultEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultEnvironment(OptionalPythonEnvironment projectDefaultEnvironment) {
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
