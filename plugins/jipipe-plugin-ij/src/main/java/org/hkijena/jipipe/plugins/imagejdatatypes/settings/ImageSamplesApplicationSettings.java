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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.dataenvironment.JIPipeDataDirectoryEnvironment;
import org.hkijena.jipipe.plugins.dataenvironment.OptionalJIPipeDataDirectoryEnvironment;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class ImageSamplesApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:samples-images";

    private final JIPipeDataDirectoryEnvironment standardEnvironment = new JIPipeDataDirectoryEnvironment();
    private OptionalJIPipeDataDirectoryEnvironment defaultEnvironment = new OptionalJIPipeDataDirectoryEnvironment();

    public ImageSamplesApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    private void preconfigureEnvironment(JIPipeDataDirectoryEnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("sc.fiji.sample_images:*"));
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Samples;
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

    public JIPipeDataDirectoryEnvironment getReadOnlyDefaultEnvironment() {
        if (defaultEnvironment.isEnabled()) {
            return new JIPipeDataDirectoryEnvironment(defaultEnvironment.getContent());
        } else {
            return new JIPipeDataDirectoryEnvironment(standardEnvironment);
        }
    }

    @SetJIPipeDocumentation(name = "Environment", description = "Contains information about the location of the sample data. If disabled, falls back to <code>sc.fiji.sample_images:*</code>")
    @JIPipeParameter("default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"sc.fiji.sample_images:*"})
    public OptionalJIPipeDataDirectoryEnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-environment")
    public void setDefaultEnvironment(OptionalJIPipeDataDirectoryEnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    public static ImageSamplesApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ImageSamplesApplicationSettings.class);
    }
}
