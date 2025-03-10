package org.hkijena.jipipe.plugins.cellpose;

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
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class CellposePluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {
    public static String ID = "org.hkijena.jipipe:cellpose";
    private OptionalPythonEnvironment cellpose2Environment = new OptionalPythonEnvironment();
    private OptionalPythonEnvironment cellpose3Environment = new OptionalPythonEnvironment();

    public CellposePluginProjectSettings() {
        autoConfigureDefaultEnvironment();
    }

    private void autoConfigureDefaultEnvironment() {
        if (Cellpose2PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(Cellpose2PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                PythonEnvironment environment = new PythonEnvironment();
                environment.setLoadFromArtifact(true);
                environment.setArtifactQuery(new JIPipeArtifactQueryParameter(target.getFullId()));

                cellpose2Environment.setEnabled(true);
                cellpose2Environment.setContent(environment);
            }
        }
        if (Cellpose3PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(Cellpose3PluginApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                PythonEnvironment environment = new PythonEnvironment();
                environment.setLoadFromArtifact(true);
                environment.setArtifactQuery(new JIPipeArtifactQueryParameter(target.getFullId()));

                cellpose3Environment.setEnabled(true);
                cellpose3Environment.setContent(environment);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Cellpose 2.x environment", description = "If enabled, overwrite the application-wide Cellpose 2.x environment and store them inside the project.")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Cellpose", allowArtifact = true, artifactFilters = {"com.github.mouseland.cellpose:*"})
    public OptionalPythonEnvironment getCellpose2Environment() {
        return cellpose2Environment;
    }

    @JIPipeParameter("project-default-environment")
    public void setCellpose2Environment(OptionalPythonEnvironment cellpose2Environment) {
        this.cellpose2Environment = cellpose2Environment;
    }

    @SetJIPipeDocumentation(name = "Cellpose 3.x environment", description = "If enabled, overwrite the application-wide Cellpose 3.x environment and store them inside the project.")
    @JIPipeParameter("project-cellpose3-environment")
    @ExternalEnvironmentParameterSettings(showCategory = "Cellpose", allowArtifact = true, artifactFilters = {"com.github.mouseland.cellpose3:*"})
    public OptionalPythonEnvironment getCellpose3Environment() {
        return cellpose3Environment;
    }

    @JIPipeParameter("project-cellpose3-environment")
    public void setCellpose3Environment(OptionalPythonEnvironment cellpose3Environment) {
        this.cellpose3Environment = cellpose3Environment;
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

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        if (cellpose2Environment.isEnabled()) {
            target.add(cellpose2Environment.getContent());
        }
        if (cellpose3Environment.isEnabled()) {
            target.add(cellpose3Environment.getContent());
        }
    }
}
