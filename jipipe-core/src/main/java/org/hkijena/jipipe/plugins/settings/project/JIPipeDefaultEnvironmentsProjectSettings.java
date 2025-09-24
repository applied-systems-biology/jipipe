package org.hkijena.jipipe.plugins.settings.project;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDefaultEnvironmentsApplicationSettings;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class JIPipeDefaultEnvironmentsProjectSettings extends JIPipeDynamicParameterCollection implements JIPipeProjectSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:default-environments";

    @Override
    public void initialize(JIPipeProject project) {
        // At this point JIPipe is already available, and we can assume that the artifact repository is configured
        for (Map.Entry<String, JIPipeEnvironmentsServiceComponent.EnvironmentInfo> entry : JIPipe.getInstance().getEnvironments().getInfosById().entrySet()) {
            JIPipeEnvironmentsServiceComponent.EnvironmentInfo info = entry.getValue();
            if(info.getArchetype() == JIPipeEnvironmentArchetype.Managed) {
                JIPipeMutableParameterAccess access = addParameter(entry.getKey(), info.getOptionalEnvironmentClass(), info.getName(), info.getDescription() + ". " +
                        "Allows to override which environment is used if node overrides are disabled. " +
                        "If disabled, JIPipe will fall back to application-wide settings. " +
                        "Generally we recommend to keep this override enabled to ensure that projects are as reproducible as possible.");
                OptionalParameter<?> parameter = access.get(OptionalParameter.class);
                if(info.isArtifact() && info.hasArtifactQuery()) {
                    JIPipeArtifactEnvironment environment = (JIPipeArtifactEnvironment) parameter.getContent();
                    environment.trySetToLatestVersionPinnedArtifact();
                    parameter.setEnabled(true); // For new projects we always default to usage of the project-local environment (can be fixed later if broken)
                }
                else {
                    // If the application-wide equivalent is valid, disable this one (e.g. OMERO)
                    JIPipeDefaultEnvironmentsApplicationSettings applicationWideSettings = JIPipe.getSettings().getByType(JIPipeDefaultEnvironmentsApplicationSettings.class);
                    JIPipeParameterAccess applicationWideAccess = applicationWideSettings.get(info.getId());
                    if(applicationWideAccess != null) {
                        OptionalParameter<?> globalParameter = applicationWideAccess.get(OptionalParameter.class);
                        if(globalParameter != null && globalParameter.isEnabled()) {
                            JIPipeEnvironment globalEnvironment  = (JIPipeEnvironment) globalParameter.getContent();
                            parameter.setEnabled(!globalEnvironment.isValid());
                        }
                        else {
                            parameter.setEnabled(true);
                        }
                    }
                    else {
                        parameter.setEnabled(true);
                    }
                }
            }
        }
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/run-build-install.png");
    }

    @Override
    public String getName() {
        return "Environments";
    }

    @Override
    public String getCategory() {
        return JIPipeDefaultProjectSettingsSheetCategory.General.getCategory();
    }

    @Override
    public Icon getCategoryIcon() {
        return JIPipeDefaultProjectSettingsSheetCategory.General.getIcon();
    }

    @Override
    public String getDescription() {
        return "Allows to configure the project-wide environments";
    }
}
