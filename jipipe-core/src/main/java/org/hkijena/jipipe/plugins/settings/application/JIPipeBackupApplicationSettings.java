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

import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupSessionInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class JIPipeBackupApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:backups";
    private boolean enableBackups = true;
    private int backupDelay = 7;
    private OptionalPathParameter customBackupPath = new OptionalPathParameter();

    public JIPipeBackupApplicationSettings() {
    }

    public static JIPipeBackupApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeBackupApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Enable", description = "If enabled, JIPipe will automatically save all projects into a separate folder for crash recovery.")
    @JIPipeParameter("enable-backups")
    public boolean isEnableBackups() {
        return enableBackups;
    }

    @JIPipeParameter("enable-backups")
    public void setEnableBackups(boolean enableBackups) {
        this.enableBackups = enableBackups;
    }

    @SetJIPipeDocumentation(name = "Backup interval (minutes)", description = "Determines the interval between auto-saves")
    @JIPipeParameter("backup-delay")
    public int getBackupDelay() {
        return backupDelay;
    }

    @JIPipeParameter("backup-delay")
    public boolean setAutoSaveDelay(int autoSaveDelay) {
        if (autoSaveDelay <= 0)
            return false;
        this.backupDelay = autoSaveDelay;
        return true;
    }

    @SetJIPipeDocumentation(name = "Custom backup path", description = "Allows to change the path where the auto-saves are placed. By default, they are put into a temporary directory.")
    @JIPipeParameter("custom-backup-path")
    public OptionalPathParameter getCustomBackupPath() {
        return customBackupPath;
    }

    @JIPipeParameter("custom-backup-path")
    public void setCustomBackupPath(OptionalPathParameter customBackupPath) {
        this.customBackupPath = customBackupPath;
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
        return "Backups";
    }

    @Override
    public String getDescription() {
        return "Determine the behavior of the automated backup functionality";
    }
}
