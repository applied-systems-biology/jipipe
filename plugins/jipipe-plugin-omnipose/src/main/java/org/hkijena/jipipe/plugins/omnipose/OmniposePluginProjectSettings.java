package org.hkijena.jipipe.plugins.omnipose;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
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
    private OptionalPythonEnvironment omnipose0Environment = new OptionalPythonEnvironment();

    public OmniposePluginProjectSettings() {
        autoConfigureDefaultEnvironment();
    }

    private void autoConfigureDefaultEnvironment() {
        if (OmniposePluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(OmniposePluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                PythonEnvironment environment = new PythonEnvironment();
                environment.setLoadFromArtifact(true);
                environment.setArtifactQuery(new JIPipeArtifactQueryParameter(target.getFullId()));

                omnipose0Environment.setEnabled(true);
                omnipose0Environment.setContent(environment);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Project default environment", description = "If enabled, overwrite the application-wide Omnipose environment and store them inside the project.")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Omnipose", allowArtifact = true, artifactFilters = {"com.github.kevinjohncutler.omnipose:*"})
    public OptionalPythonEnvironment getOmnipose0Environment() {
        return omnipose0Environment;
    }

    @JIPipeParameter("project-default-environment")
    public void setOmnipose0Environment(OptionalPythonEnvironment omnipose0Environment) {
        this.omnipose0Environment = omnipose0Environment;
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

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        if (omnipose0Environment.isEnabled()) {
            target.add(omnipose0Environment.getContent());
        }
    }
}
