/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.Timer;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class AutoSaveSettings implements JIPipeParameterCollection {
    public static final String ID = "auto-save";
    private final EventBus eventBus = new EventBus();
    private final Timer autoSaveTimer;
    private boolean enableAutoSave = true;
    private int autoSaveDelay = 3;
    private OptionalPathParameter customSavePath = new OptionalPathParameter();
    private Set<String> savedBackupHashes = new HashSet<>();

    public AutoSaveSettings() {
        autoSaveTimer = new Timer(autoSaveDelay * 60 * 1000, e -> autoSaveAll());
        autoSaveTimer.setRepeats(true);
        autoSaveTimer.start();
    }

    public static AutoSaveSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, AutoSaveSettings.class);
    }

    private Path getDefaultSavePath() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        Path targetDirectory = imageJDir.resolve("jipipe").resolve("backups");
        if (!Files.isDirectory(targetDirectory)) {
            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return targetDirectory;
    }

    public void autoSave(JIPipeProjectWindow window) {
        String name = "untitled";
        if (window.getProjectSavePath() != null) {
            name = window.getProjectSavePath().getFileName().toString();
        }
        Path directory = getCurrentSavePath();
        try {
            Set<String> existing = Files.list(directory).map(p -> p.getFileName().toString().replace(".jip", "")).collect(Collectors.toSet());
            String baseName = name + "_" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(':', '-');
            baseName = StringUtils.makeFilesystemCompatible(StringUtils.makeUniqueString(baseName, "_", existing));
            Path targetFile = directory.resolve(baseName + ".jip");
            window.getProject().saveProject(targetFile);

            // Calculate the SHA1
            String sha1 = PathUtils.computeFileSHA1(targetFile.toFile());
            if (this.savedBackupHashes.contains(sha1)) {
                Files.delete(targetFile);
                return;
            }

            savedBackupHashes.add(sha1);
            window.getProjectUI().sendStatusBarText("Saved backup to " + targetFile);
            getEventBus().post(new ParameterChangedEvent(this, "last-saves"));
        } catch (IOException e) {
            window.getProjectUI().sendStatusBarText("Failed to save backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Path getCurrentSavePath() {
        Path directory;
        if (customSavePath.isEnabled()) {
            if (!Files.isDirectory(customSavePath.getContent())) {
                directory = customSavePath.getContent();
                try {
                    Files.createDirectories(customSavePath.getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                directory = customSavePath.getContent();
            }
        } else {
            directory = getDefaultSavePath();
        }
        return directory;
    }

    public void autoSaveAll() {
        for (JIPipeProjectWindow window : JIPipeProjectWindow.getOpenWindows()) {
            autoSave(window);
        }
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Enable", description = "If enabled, JIPipe will automatically save all projects into a separate folder for crash recovery.")
    @JIPipeParameter("enable-auto-save")
    public boolean isEnableAutoSave() {
        return enableAutoSave;
    }

    @JIPipeParameter("enable-auto-save")
    public void setEnableAutoSave(boolean enableAutoSave) {
        this.enableAutoSave = enableAutoSave;
        if (!enableAutoSave) {
            autoSaveTimer.stop();
        } else {
            autoSaveTimer.restart();
        }
    }

    @JIPipeDocumentation(name = "Save interval (minutes)", description = "Determines the interval between auto-saves")
    @JIPipeParameter("auto-save-delay")
    public int getAutoSaveDelay() {
        return autoSaveDelay;
    }

    @JIPipeParameter("auto-save-delay")
    public boolean setAutoSaveDelay(int autoSaveDelay) {
        if (autoSaveDelay <= 0)
            return false;
        this.autoSaveDelay = autoSaveDelay;
        this.autoSaveTimer.setDelay(autoSaveDelay * 60 * 1000);
        this.autoSaveTimer.restart();
        return true;
    }

    @JIPipeDocumentation(name = "Custom auto-save path", description = "Allows to change the path where the auto-saves are placed. By default, they are put into a temporary directory.")
    @JIPipeParameter("custom-save-path")
    public OptionalPathParameter getCustomSavePath() {
        return customSavePath;
    }

    @JIPipeParameter("custom-save-path")
    public void setCustomSavePath(OptionalPathParameter customSavePath) {
        this.customSavePath = customSavePath;
    }

    @JIPipeDocumentation(name = "Remove duplicate backups", description = "Removes all duplicate backups")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/clear-brush.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/clear-brush.png")
    public void removeDuplicateBackups(JIPipeWorkbench workbench) {
        if (JOptionPane.showConfirmDialog(workbench.getWindow(),
                "Do you really want to remove duplicate backups?\nThis will only affect backups and not your actual project files.",
                "Remove duplicate backups",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeRunExecuterUI.runInDialog(workbench.getWindow(), new CleanBackupsRun(workbench));
        }
    }

    public List<Path> getLastBackups() {
        try {
            List<Path> paths = Files.list(getCurrentSavePath()).filter(path -> path.getFileName().toString().endsWith(".jip")).sorted(Comparator.comparing((Path path) -> {
                try {
                    return Files.getLastModifiedTime(path);
                } catch (IOException e) {
                    return FileTime.from(Instant.now());
                }
            }).reversed()).collect(Collectors.toList());
            return paths;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @JIPipeDocumentation(name = "Restore backup", description = "Restores a backup from the list below")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/reload.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/reload.png")
    public void openRestoreMenu(JIPipeWorkbench workbench) {
        JList<Path> listControl = new JList<>();
        DefaultListModel<Path> model = new DefaultListModel<>();
        for (Path lastSave : getLastBackups()) {
            model.addElement(lastSave);
        }
        listControl.setModel(model);
        JLabel message = new JLabel("Please select the snapshot that should be restored:");
        JScrollPane scrollPane = new JScrollPane(listControl);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        int result = JOptionPane.showOptionDialog(
                workbench.getWindow(),
                new Object[]{message, scrollPane},
                "Restore backup",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            if (listControl.getSelectedValue() != null) {
                JIPipeProjectWindow window = (JIPipeProjectWindow) workbench.getWindow();
                window.openProject(listControl.getSelectedValue());
            }
        }
    }

}
