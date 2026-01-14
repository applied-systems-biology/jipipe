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

import javax.swing.*;


public class JIPipeAutoSaveApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:auto-save";
    private boolean defaultEnableAutoSave = false;
    private int autoSaveWatcherInterval = 10;
    private int autoSaveInterval = 3 * 60;

    public JIPipeAutoSaveApplicationSettings() {
    }

    public static JIPipeAutoSaveApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeAutoSaveApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Enabled by default", description = "If enabled, the project auto save feature is enabled by default.")
    @JIPipeParameter("enable-backups")
    public boolean isDefaultEnableAutoSave() {
        return defaultEnableAutoSave;
    }

    @JIPipeParameter("enable-backups")
    public void setDefaultEnableAutoSave(boolean defaultEnableAutoSave) {
        this.defaultEnableAutoSave = defaultEnableAutoSave;
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
        return JIPipe.RESOURCES.getIcon16("actions/filesave.png");
    }

    @Override
    public String getName() {
        return "Auto save";
    }

    @Override
    public String getDescription() {
        return "Allows to setup the project auto save feature";
    }

    @SetJIPipeDocumentation(name = "Watcher interval (s)", description = "The interval of the timer that handles the auto-save functionality. Does not control the actual auto-save interval. " +
            "Requires reloading the project(s) for changes to be applied. Lower values may impact the performance.")
    @JIPipeParameter("auto-save-watcher-interval")
    public int getAutoSaveWatcherInterval() {
        return autoSaveWatcherInterval;
    }

    @JIPipeParameter("auto-save-watcher-interval")
    public void setAutoSaveWatcherInterval(int autoSaveWatcherInterval) {
        this.autoSaveWatcherInterval = autoSaveWatcherInterval;
    }

    @SetJIPipeDocumentation(name = "Save interval (s)", description = "The autosave interval. The precision is controlled by the watcher interval. Lower values may impact the performance.")
    @JIPipeParameter("auto-save-interval")
    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    @JIPipeParameter("auto-save-interval")
    public void setAutoSaveInterval(int autoSaveInterval) {
        this.autoSaveInterval = autoSaveInterval;
    }
}
