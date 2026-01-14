package org.hkijena.jipipe.desktop.app.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.commons.components.SplitButton;
import org.hkijena.jipipe.plugins.settings.application.JIPipeAutoSaveApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class JIPipeDesktopProjectSaveManagerButton extends SplitButton implements JIPipeDesktopProjectWorkbench.ProjectSavedEventListener {
    private final JIPipeDesktopProjectWorkbench workbench;
    private final JIPipeAutoSaveApplicationSettings settings;
    private LocalDateTime lastProjectSaveTime;
    private long lastAutoSaveTime = 0;
    private final JCheckBoxMenuItem autoSaveMenuItem = new JCheckBoxMenuItem("Auto save");
    private final Timer checkTimer;

    public JIPipeDesktopProjectSaveManagerButton(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.settings = JIPipeAutoSaveApplicationSettings.getInstance();
        this.checkTimer = new Timer(Math.max(1, settings.getAutoSaveWatcherInterval()) * 1000, e -> doUpdate());

        checkTimer.setRepeats(true);
        checkTimer.start();

        initialize();
        updateStatus();

        workbench.getProjectSavedEventEmitter().subscribe(this);
        autoSaveMenuItem.setState(settings.isDefaultEnableAutoSave());
    }

    private void doUpdate() {

        if (workbench.getProject().isDisposed()) {
            checkTimer.stop();
            return;
        }

        final long currentTime = System.currentTimeMillis();

        if ((currentTime - lastAutoSaveTime) > 1000L * Math.max(1, settings.getAutoSaveInterval())) {

            // Update the autosave time
            lastAutoSaveTime = currentTime;

            // Auto save interval triggered
            if (autoSaveMenuItem.isSelected()) {
                if (workbench.getProject().getProjectFile() == null) {
                    return;
                }
                workbench.getProjectWindow().saveProjectAs(true, false);
            }
        }

        // Always update the control's text
        updateStatus();
    }

    private void initialize() {
        setOpaque(false);
        addActionListener(e -> {
            workbench.getProjectWindow().saveProjectAs(true, true);
        });
        autoSaveMenuItem.addActionListener(e -> updateStatus());

        getPopupMenu().add(autoSaveMenuItem);
        getPopupMenu().add(UIUtils.createMenuItem("Configure ...", "Opens the settings page for configuring the auto save feature", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openAutoSaveSettings));
    }

    private void openAutoSaveSettings() {
        workbench.openApplicationSettings("/General/Auto save");
    }

    private void updateStatus() {
        if (workbench.getProject().getProjectFile() == null) {
            setText("Unsaved");
            setIcon(JIPipe.RESOURCES.getIcon16("actions/save-red.png"));
        } else {
            boolean withAutoSave = autoSaveMenuItem.getState();
            if (workbench.isProjectModified()) {
                setText(withAutoSave ? "Auto save" : "Save");
                setIcon(JIPipe.RESOURCES.getIcon16(withAutoSave ? "actions/autosave.png" : "actions/save-green.png"));
            } else if (lastProjectSaveTime != null) {
                setText((withAutoSave ? "Auto saved " : "Saved ") + formatTimeAgo());
                setIcon(JIPipe.RESOURCES.getIcon16(withAutoSave ? "actions/autosave.png" : "actions/save.png"));
            } else {
                setText(withAutoSave ? "Auto save" : "Save");
                setIcon(JIPipe.RESOURCES.getIcon16(withAutoSave ? "actions/autosave.png" : "actions/save.png"));
            }
        }
    }

    private String formatTimeAgo() {
        if (lastProjectSaveTime == null) {
            return "never";
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(lastProjectSaveTime, now);

        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "< 1 min ago";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " min ago";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " h ago";
        }

        long days = hours / 24;
        return days + " day" + (days != 1 ? "s" : "") + " ago";
    }


    @Override
    public void onProjectSaved(JIPipeDesktopProjectWorkbench.ProjectSavedEvent event) {
        lastProjectSaveTime = LocalDateTime.now();
        updateStatus();
    }
}
