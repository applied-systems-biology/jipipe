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

package org.hkijena.jipipe.plugins.imagejdatatypes.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.plugins.dataenvironment.JIPipeDataDirectoryEnvironment;
import org.hkijena.jipipe.plugins.dataenvironment.OptionalJIPipeDataDirectoryEnvironment;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class ImageSamplesProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static String ID = "org.hkijena.jipipe:samples-images";

    private OptionalJIPipeDataDirectoryEnvironment projectDefaultEnvironment = new OptionalJIPipeDataDirectoryEnvironment();

    public ImageSamplesProjectSettings() {
        autoConfigureEnvironment();
    }

    private void autoConfigureEnvironment() {
        if (ImageSamplesApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(ImageSamplesApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                JIPipeDataDirectoryEnvironment environment = new JIPipeDataDirectoryEnvironment();
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
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"sc.fiji.sample_images:*"})
    public OptionalJIPipeDataDirectoryEnvironment getProjectDefaultEnvironment() {
        return projectDefaultEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultEnvironment(OptionalJIPipeDataDirectoryEnvironment projectDefaultEnvironment) {
        this.projectDefaultEnvironment = projectDefaultEnvironment;
    }

    @Override
    public JIPipeDefaultProjectSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultProjectSettingsSheetCategory.Samples;
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        if (projectDefaultEnvironment.isEnabled()) {
            target.add(projectDefaultEnvironment.getContent());
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/image.png");
    }

    @Override
    public String getName() {
        return "Images (ImageJ)";
    }

    @Override
    public String getDescription() {
        return "Determines where to download ImageJ sample images";
    }
}
