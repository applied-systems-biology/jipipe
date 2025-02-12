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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.ijocr.OCRPlugin;
import org.hkijena.jipipe.plugins.ijocr.environments.OptionalTesseractOCREnvironment;
import org.hkijena.jipipe.plugins.ijocr.environments.TesseractOCREnvironment;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class TesseractOCRApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet implements JIPipeExternalEnvironmentSettings {

    public static final String ID = "org.hkijena.jipipe:ij-ocr-tesseract";
    private TesseractOCREnvironment standardEnvironment = new TesseractOCREnvironment();
    private OptionalTesseractOCREnvironment defaultEnvironment = new OptionalTesseractOCREnvironment();
    private TesseractOCREnvironment.List presets = new TesseractOCREnvironment.List();

    public TesseractOCRApplicationSettings() {
        preconfigureEnvironment(standardEnvironment);
        preconfigureEnvironment(defaultEnvironment.getContent());
    }

    public static TesseractOCRApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, TesseractOCRApplicationSettings.class);
    }

    private void preconfigureEnvironment(TesseractOCREnvironment environment) {
        environment.setLoadFromArtifact(true);
        environment.setArtifactQuery(new JIPipeArtifactQueryParameter("com.github.tesseractocr.tesseract:*"));
        environment.setArguments(new JIPipeExpressionParameter("cli_parameters"));
    }

    public TesseractOCREnvironment getReadOnlyDefaultEnvironment() {
        if (defaultEnvironment.isEnabled()) {
            return new TesseractOCREnvironment(defaultEnvironment.getContent());
        } else {
            return new TesseractOCREnvironment(standardEnvironment);
        }
    }

    @SetJIPipeDocumentation(name = "Tesseract OCR environment", description = "Contains information about the location of the Tesseract OCR installation.")
    @JIPipeParameter("default-environment")
    @ExternalEnvironmentParameterSettings(allowArtifact = true, artifactFilters = {"com.github.tesseractocr.tesseract:*"})
    public OptionalTesseractOCREnvironment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    @JIPipeParameter("default-environment")
    public void setDefaultEnvironment(OptionalTesseractOCREnvironment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    @SetJIPipeDocumentation(name = "Presets", description = "List of Tesseract OCR environment presets")
    @JIPipeParameter("presets")
    public TesseractOCREnvironment.List getPresets() {
        return presets;
    }

    @JIPipeParameter("presets")
    public void setPresets(TesseractOCREnvironment.List presets) {
        this.presets = presets;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Plugins;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return  UIUtils.getIconFromResources("actions/text_outer_style.png");
    }

    @Override
    public String getName() {
        return "Tesseract OCR";
    }

    @Override
    public String getDescription() {
        return "Settings related to the Tesseract OCR";
    }

    @Override
    public List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass) {
        return ImmutableList.copyOf(presets);
    }

    @Override
    public void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass) {
        this.presets.clear();
        for (JIPipeEnvironment preset : presets) {
            this.presets.add((TesseractOCREnvironment) preset);
        }
    }
}
