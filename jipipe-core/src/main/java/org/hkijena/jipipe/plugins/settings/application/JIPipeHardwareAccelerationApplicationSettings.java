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
import org.hkijena.jipipe.api.acceleration.JIPipeHardwareAccelerationMode;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;

import javax.swing.*;

/**
 * Settings related to downloads
 */
public class JIPipeHardwareAccelerationApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:hardware-acceleration";


    private JIPipeHardwareAccelerationMode accelerationPreference = JIPipeHardwareAccelerationMode.CPU;
    private Vector2iParameter accelerationPreferenceVersions = new Vector2iParameter();
    private boolean autoConfigureAccelerationOnNextStartup = true;

    /**
     * Creates a new instance
     */
    public JIPipeHardwareAccelerationApplicationSettings() {
    }

    public static JIPipeHardwareAccelerationApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeHardwareAccelerationApplicationSettings.class);
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
        return JIPipe.RESOURCES.getIcon16("actions/speedometer.png");
    }

    @Override
    public String getName() {
        return "Hardware acceleration";
    }

    @Override
    public String getDescription() {
        return "Allows to configure preferred hardware-acceleration modes";
    }

    @SetJIPipeDocumentation(name = "Auto-configure acceleration on next startup", description = "Attempts to automatically determine the acceleration during the next JIPipe startup.")
    @JIPipeParameter("auto-configure-acceleration-on-next-startup-v2")
    public boolean isAutoConfigureAccelerationOnNextStartup() {
        return autoConfigureAccelerationOnNextStartup;
    }

    @JIPipeParameter("auto-configure-acceleration-on-next-startup-v2")
    public void setAutoConfigureAccelerationOnNextStartup(boolean autoConfigureAccelerationOnNextStartup) {
        this.autoConfigureAccelerationOnNextStartup = autoConfigureAccelerationOnNextStartup;
    }

    @SetJIPipeDocumentation(name = "Acceleration mode", description = "Determines if JIPipe should prefer artifacts with a specific acceleration type. " +
            "For maximum compatibility, choose CPU (will run slowest). For Nvidia GPUs, select CUDA. For AMD GPUs select ROCm. " +
            "If no compatible artifact is found, CPU will be automatically selected.")
    @JIPipeParameter("acceleration-mode")
    public JIPipeHardwareAccelerationMode getAccelerationPreference() {
        return accelerationPreference;
    }

    @JIPipeParameter("acceleration-mode")
    public void setAccelerationPreference(JIPipeHardwareAccelerationMode accelerationPreference) {
        this.accelerationPreference = accelerationPreference;
    }

    @SetJIPipeDocumentation(name = "Acceleration version limits (GPU only)", description = "Determines version limits for GPU acceleration modes. " +
            "This is important for certain GPUs that will not run GPU accelerated code from older versions. " +
            "If a value is set to zero, no limit is assigned to the minimum or maximum. " +
            "Input the three-digit numeric version, for example 112 for CUDA version 11.2")
    @JIPipeParameter("acceleration-mode-version-limit")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2iParameter getAccelerationPreferenceVersions() {
        return accelerationPreferenceVersions;
    }

    @JIPipeParameter("acceleration-mode-version-limit")
    public void setAccelerationPreferenceVersions(Vector2iParameter accelerationPreferenceVersions) {
        this.accelerationPreferenceVersions = accelerationPreferenceVersions;
    }
}
