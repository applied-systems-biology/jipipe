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

package org.hkijena.jipipe.plugins.ijocr.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeArtifactsRegistry;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultProjectSettingsSheetCategory;
import org.hkijena.jipipe.plugins.ijocr.environments.OptionalTesseractOCREnvironment;
import org.hkijena.jipipe.plugins.ijocr.environments.TesseractOCREnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class OCRPluginProjectSettings extends JIPipeDefaultProjectSettingsSheet {

    public static final String ID = "org.hkijena.jipipe:ij-ocr";

    private OptionalTesseractOCREnvironment projectDefaultEnvironment = new OptionalTesseractOCREnvironment();

    public OCRPluginProjectSettings() {
        autoConfigureTSOAXEnvironment();
    }

    private void autoConfigureTSOAXEnvironment() {
        if (TesseractOCRApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().isLoadFromArtifact()) {
            List<JIPipeArtifact> artifacts = JIPipe.getArtifacts().queryCachedArtifacts(TesseractOCRApplicationSettings.getInstance().getReadOnlyDefaultEnvironment().getArtifactQuery().getQuery());
            artifacts.removeIf(artifact -> !artifact.isCompatible());
            if (!artifacts.isEmpty()) {
                JIPipeArtifact target = JIPipeArtifactsRegistry.selectPreferredArtifactByClassifier(artifacts);
                TesseractOCREnvironment environment = new TesseractOCREnvironment();
                environment.setName("");
                environment.setLoadFromArtifact(true);
                environment.setArtifactQuery(new JIPipeArtifactQueryParameter(target.getFullId()));

                projectDefaultEnvironment.setEnabled(true);
                projectDefaultEnvironment.setContent(environment);
            }
        }
    }


    @SetJIPipeDocumentation(name = "Project default Tesseract OCR environment", description = "If enabled, overwrite the application-wide Tesseract OCR environment and store them inside the project. ")
    @JIPipeParameter("project-default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"com.github.tesseractocr.tesseract:*"})
    public OptionalTesseractOCREnvironment getProjectDefaultEnvironment() {
        return projectDefaultEnvironment;
    }

    @JIPipeParameter("project-default-environment")
    public void setProjectDefaultEnvironment(OptionalTesseractOCREnvironment projectDefaultEnvironment) {
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
        return UIUtils.getIconFromResources("actions/text_outer_style.png");
    }

    @Override
    public String getName() {
        return "OCR";
    }

    @Override
    public String getDescription() {
        return "Settings related to the Optical Character Recognition (OCR)";
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        if (projectDefaultEnvironment.isEnabled()) {
            target.add(projectDefaultEnvironment.getContent());
        }
    }
}
