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

package org.hkijena.jipipe.plugins.statistics.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

import javax.swing.*;

/**
 * Settings related to usage statistics collection and reporting
 */
public class JIPipeStatisticsApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:statistics";

    private boolean enabled = true;
    private StatisticsPrivacyLevel privacyLevel = StatisticsPrivacyLevel.Everything;
    private String serverUrl = "https://jipipe.hki-jena.de/statistics/api/v1/statistics";
    private boolean showFirstTimePrompt = true;

    /**
     * Creates a new instance
     */
    public JIPipeStatisticsApplicationSettings() {
    }

    public static JIPipeStatisticsApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeStatisticsApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Enable statistics", description = "If enabled, JIPipe collects and sends usage statistics according to the selected privacy level.")
    @JIPipeParameter("statistics-enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("statistics-enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @SetJIPipeDocumentation(name = "Privacy level", description = "Controls what usage statistics JIPipe collects and sends. Higher levels include all information from lower levels.")
    @JIPipeParameter("statistics-privacy-level")
    public StatisticsPrivacyLevel getPrivacyLevel() {
        return privacyLevel;
    }

    @JIPipeParameter("statistics-privacy-level")
    public void setPrivacyLevel(StatisticsPrivacyLevel privacyLevel) {
        this.privacyLevel = privacyLevel;
    }

    @SetJIPipeDocumentation(name = "Server URL", description = "The URL where statistics are sent.")
    @JIPipeParameter("statistics-server-url")
    public String getServerUrl() {
        return serverUrl;
    }

    @JIPipeParameter("statistics-server-url")
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @SetJIPipeDocumentation(name = "Show first-time prompt", description = "If enabled, JIPipe shows a first-time prompt about statistics collection on startup.")
    @JIPipeParameter("statistics-show-first-time-prompt")
    public boolean isShowFirstTimePrompt() {
        return showFirstTimePrompt;
    }

    @JIPipeParameter("statistics-show-first-time-prompt")
    public void setShowFirstTimePrompt(boolean showFirstTimePrompt) {
        this.showFirstTimePrompt = showFirstTimePrompt;
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
        return JIPipe.RESOURCES.getIcon16("actions/chart-bar.png");
    }

    @Override
    public String getName() {
        return "Statistics";
    }

    @Override
    public String getDescription() {
        return "Settings for usage statistics collection and reporting";
    }
}
