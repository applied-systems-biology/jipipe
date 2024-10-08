package org.hkijena.jipipe.plugins.cellpose;

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
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class CellposePluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {
    public static String ID = "org.hkijena.jipipe:cellpose";
    private OptionalPythonEnvironment projectDefaultEnvironment = new OptionalPythonEnvironment();

    public CellposePluginProjectSettings() {
        autoConfigureDefaultEnvironment();
    }

    private void autoConfigureDefaultEnvironment() {
        if (CellposePluginApplicationSettings.getInstance().getDefaultCellposeEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(CellposePluginApplicationSettings.getInstance().getDefaultCellposeEnvironment().getArtifactQuery().getQuery());
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

    @SetJIPipeDocumentation(name = "Project default environment", description = "If enabled, overwrite the application-wide Cellpose environment and store them inside the project.")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Cellpose", allowArtifact = true, artifactFilters = {"com.github.mouseland.cellpose:*"})
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
        return UIUtils.getIconFromResources("apps/cellpose.png");
    }

    @Override
    public String getName() {
        return "Cellpose";
    }

    @Override
    public String getDescription() {
        return "Settings related to the Cellpose integration";
    }
}
