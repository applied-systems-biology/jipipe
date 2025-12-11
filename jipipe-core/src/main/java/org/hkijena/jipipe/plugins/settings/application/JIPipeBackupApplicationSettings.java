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
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;

import javax.swing.*;


public class JIPipeBackupApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:backups";
    private boolean enableBackups = true;
    private int backupDelay = 7;
    private OptionalPathParameter customBackupPath = new OptionalPathParameter();
    private final CleanupSettings cleanupSettings;

    public JIPipeBackupApplicationSettings() {
        this.cleanupSettings = new CleanupSettings();
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

    @SetJIPipeDocumentation(name = "Custom backup path", description = "Allows to change the path where the auto-saves are placed. By default, they are put into the profile directory.")
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

    @SetJIPipeDocumentation(name = "Cleanup", description = "Settings related to the automated cleanup of backups")
    @JIPipeParameter("cleanup-settings")
    public CleanupSettings getCleanupSettings() {
        return cleanupSettings;
    }

    public static class CleanupSettings extends AbstractJIPipeParameterCollection {
        private boolean enableAutoCleanupOnStartup = true;
        private OptionalIntegerParameter maxAgeDays = new OptionalIntegerParameter(true, 60);
        private OptionalIntegerParameter keepBackupsPerYear = new OptionalIntegerParameter(true, 12);
        private OptionalIntegerParameter keepBackupsPerMonth = new OptionalIntegerParameter(true, 4);
        private OptionalIntegerParameter keepBackupsPerWeek = new OptionalIntegerParameter(true, 4);
        private OptionalIntegerParameter keepBackupsPerDay = new OptionalIntegerParameter(true, 1);
        private OptionalIntegerParameter keepBackupsPerHour = new OptionalIntegerParameter(true, 1);

        public CleanupSettings() {

        }

        @SetJIPipeDocumentation(name = "Maximum age (days)", description = "If enabled, sets the maximum age of backups in days. Backups older than the given age are deleted. Please note that this age-based rule is overwritten by the number of retained backups (year/month/week/day). Fallback rule.")
        @JIPipeParameter(value = "max-age-days", uiOrder = 100)
        public OptionalIntegerParameter getMaxAgeDays() {
            if(maxAgeDays.getContent() <= 0) {
                maxAgeDays.setContent(1);
            }
            return maxAgeDays;
        }

        @JIPipeParameter("max-age-days")
        public void setMaxAgeDays(OptionalIntegerParameter maxAgeDays) {
            this.maxAgeDays = maxAgeDays;
        }

        @SetJIPipeDocumentation(name = "Automatically cleanup backups on JIPipe start", description = "If enabled, JIPipe will automatically cleanup backups during startup.")
        @JIPipeParameter(value = "enable-auto-cleanup", uiOrder = -100)
        public boolean isEnableAutoCleanupOnStartup() {
            return enableAutoCleanupOnStartup;
        }

        @JIPipeParameter("enable-auto-cleanup")
        public void setEnableAutoCleanupOnStartup(boolean enableAutoCleanupOnStartup) {
            this.enableAutoCleanupOnStartup = enableAutoCleanupOnStartup;
        }

        @SetJIPipeDocumentation(name = "Keep yearly backups", description = "If enabled, JIPipe will keep the given number of backups if they are older than one year. " +
                "If they are older than more than one year, JIPipe will keep backups based on the year number (e.g., keep N backups for 2023, 2024, and 2025 each).")
        @JIPipeParameter(value = "keep-backups-per-year", uiOrder = 90)
        public OptionalIntegerParameter getKeepBackupsPerYear() {
            if(keepBackupsPerYear.getContent() <= 0) {
                keepBackupsPerYear.setContent(1);
            }
            return keepBackupsPerYear;
        }

        @JIPipeParameter("keep-backups-per-year")
        public void setKeepBackupsPerYear(OptionalIntegerParameter keepBackupsPerYear) {
            this.keepBackupsPerYear = keepBackupsPerYear;
        }

        @SetJIPipeDocumentation(name = "Keep monthly backups", description = "If enabled, JIPipe will keep the given number of backups if they are older than one month but do not fall within the 'Keep yearly backups' rule.")
        @JIPipeParameter(value = "keep-backups-per-month", uiOrder = 80)
        public OptionalIntegerParameter getKeepBackupsPerMonth() {
            if(keepBackupsPerMonth.getContent() <= 0) {
                keepBackupsPerMonth.setContent(1);
            }
            return keepBackupsPerMonth;
        }

        @JIPipeParameter("keep-backups-per-month")
        public void setKeepBackupsPerMonth(OptionalIntegerParameter keepBackupsPerMonth) {
            this.keepBackupsPerMonth = keepBackupsPerMonth;
        }

        @SetJIPipeDocumentation(name = "Keep weekly backups", description = "If enabled, JIPipe will keep the given number of backups if they are older than one week but do not fall within the 'Keep yearly backups' or 'Keep monthly backups' rules.")
        @JIPipeParameter(value = "keep-backups-per-week", uiOrder = 70)
        public OptionalIntegerParameter getKeepBackupsPerWeek() {
            if(keepBackupsPerWeek.getContent() <= 0) {
                keepBackupsPerWeek.setContent(1);
            }
            return keepBackupsPerWeek;
        }

        @JIPipeParameter("keep-backups-per-week")
        public void setKeepBackupsPerWeek(OptionalIntegerParameter keepBackupsPerWeek) {
            this.keepBackupsPerWeek = keepBackupsPerWeek;
        }

        @SetJIPipeDocumentation(name = "Keep daily backups", description = "If enabled, JIPipe will keep the given number of backups if they are older than one day but do not fall within the 'Keep yearly backups', 'Keep monthly backups', or 'Keep weekly backups' rules.")
        @JIPipeParameter(value = "keep-backups-per-day", uiOrder = 60)
        public OptionalIntegerParameter getKeepBackupsPerDay() {
            if(keepBackupsPerDay.getContent() <= 0) {
                keepBackupsPerDay.setContent(1);
            }
            return keepBackupsPerDay;
        }

        @JIPipeParameter("keep-backups-per-day")
        public void setKeepBackupsPerDay(OptionalIntegerParameter keepBackupsPerDay) {
            this.keepBackupsPerDay = keepBackupsPerDay;
        }

        @SetJIPipeDocumentation(name = "Keep hourly backups", description = "If enabled, JIPipe will keep the given number of backups if they are older than one hour but do not fall within the 'Keep yearly backups', 'Keep monthly backups', 'Keep weekly backups', or 'Keep daily backups' rules.")
        @JIPipeParameter(value = "keep-backups-per-hour", uiOrder = 50)
        public OptionalIntegerParameter getKeepBackupsPerHour() {
            if(keepBackupsPerHour.getContent() <= 0) {
                keepBackupsPerHour.setContent(1);
            }
            return keepBackupsPerHour;
        }

        @JIPipeParameter("keep-backups-per-hour")
        public void setKeepBackupsPerHour(OptionalIntegerParameter keepBackupsPerHour) {
            this.keepBackupsPerHour = keepBackupsPerHour;
        }
    }
}
