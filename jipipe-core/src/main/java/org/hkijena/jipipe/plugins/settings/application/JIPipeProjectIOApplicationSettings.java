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

package org.hkijena.jipipe.plugins.settings.application;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;

/**
 * Settings for the JIPipe project file formats (the .jipx container and
 * its Workflow RO-Crate metadata).
 */
public class JIPipeProjectIOApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    /**
     * The stable id of this settings sheet.
     * Persisted in saved settings — never derive from code.
     */
    public static final String ID = "org.hkijena.jipipe:project-io";

    /**
     * The default RO-Crate license for .jipx saves of projects whose
     * metadata has no license: an SPDX identifier (e.g. {@code MIT} or
     * {@code Apache-2.0}) or a license URL. Empty → the crate falls back
     * to MIT.
     */
    private String defaultRoCrateLicense = "";

    public static JIPipeProjectIOApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeProjectIOApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Default RO-Crate license", description =
            "The license of the Workflow RO-Crate metadata written for .jipx project saves when the project itself has no license. " +
                    "An SPDX identifier (e.g. MIT, Apache-2.0) or a license URL. Leave empty to fall back to MIT.")
    @JIPipeParameter("default-ro-crate-license")
    public String getDefaultRoCrateLicense() {
        return StringUtils.nullToEmpty(defaultRoCrateLicense);
    }

    @JIPipeParameter("default-ro-crate-license")
    public void setDefaultRoCrateLicense(String defaultRoCrateLicense) {
        this.defaultRoCrateLicense = defaultRoCrateLicense;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("mimetypes/application-jipipe.png");
    }

    @Override
    public String getName() {
        return "Project file format";
    }

    @Override
    public String getDescription() {
        return "Settings for the .jipx project container format (default RO-Crate license)";
    }
}
