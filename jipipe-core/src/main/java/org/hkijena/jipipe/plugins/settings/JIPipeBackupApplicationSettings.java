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

package org.hkijena.jipipe.plugins.settings;

import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
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
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class JIPipeBackupApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:backups";
    private final Timer backupTimer;
    private boolean enableBackups = true;
    private int backupDelay = 7;
    private OptionalPathParameter customBackupPath = new OptionalPathParameter();

    public JIPipeBackupApplicationSettings() {
        backupTimer = new Timer(backupDelay * 60 * 1000, e -> backupAll());
        backupTimer.setRepeats(true);
        backupTimer.start();
    }

    public static JIPipeBackupApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeBackupApplicationSettings.class);
    }

    private Path getDefaultSavePath() {
        Path targetDirectory = PathUtils.getJIPipeUserDir().resolve("backups");
        if (!Files.isDirectory(targetDirectory)) {
            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return targetDirectory;
    }

    public void backup(JIPipeDesktopProjectWindow window) {
        String name = "untitled";
        if (window.getProjectSavePath() != null) {
            name = window.getProjectSavePath().getFileName().toString();
        }
        window.getProjectUI().getBackupQueue().cancelAll();
        String finalName = name;
        JIPipeRunnable run = new AbstractJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return "Creating backup";
            }

            @Override
            public void run() {
                try {
                    Path directory = getCurrentBackupPath();
                    directory = directory.resolve(window.getSessionId().toString());
                    Files.createDirectories(directory);

                    String dateTimeFormatted = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    String baseName = finalName + "_" + dateTimeFormatted.replace(':', '-');
                    baseName = StringUtils.makeFilesystemCompatible(baseName);
                    Path targetFile = directory.resolve(baseName + ".jip");
                    window.getProject().saveProject(targetFile);

                    SwingUtilities.invokeLater(() -> window.getProjectUI().sendStatusBarText("Saved backup to " + targetFile));

                    // Write storage info
                    JIPipeProjectBackupSessionInfo info = new JIPipeProjectBackupSessionInfo();
                    info.setProjectStoragePath(window.getProjectSavePath() != null ? window.getProjectSavePath().toString() : "");
                    info.setProjectSessionId(window.getSessionId().toString());
                    info.setLastDateTimeInfo(dateTimeFormatted);
                    JsonUtils.saveToFile(info, directory.resolve("backup-info.json"));

                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> window.getProjectUI().sendStatusBarText("Failed to save backup: " + e.getMessage()));
                    IJ.handleException(e);
                    e.printStackTrace();
                }
            }
        };
        window.getProjectUI().getBackupQueue().enqueue(run);
    }

    public Path getCurrentBackupPath() {
        Path directory;
        if (customBackupPath.isEnabled()) {
            if (!Files.isDirectory(customBackupPath.getContent())) {
                directory = customBackupPath.getContent();
                try {
                    Files.createDirectories(customBackupPath.getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                directory = customBackupPath.getContent();
            }
        } else {
            directory = getDefaultSavePath();
        }
        return directory;
    }

    public void backupAll() {
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.isVisible()) {
                backup(window);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Enable", description = "If enabled, JIPipe will automatically save all projects into a separate folder for crash recovery.")
    @JIPipeParameter("enable-backups")
    public boolean isEnableBackups() {
        return enableBackups;
    }

    @JIPipeParameter("enable-backups")
    public void setEnableBackups(boolean enableBackups) {
        this.enableBackups = enableBackups;
        if (!enableBackups) {
            backupTimer.stop();
        } else {
            backupTimer.restart();
        }
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
        this.backupTimer.setDelay(autoSaveDelay * 60 * 1000);
        this.backupTimer.restart();
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
        return UIUtils.getIconFromResources("actions/filesave.png");
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
