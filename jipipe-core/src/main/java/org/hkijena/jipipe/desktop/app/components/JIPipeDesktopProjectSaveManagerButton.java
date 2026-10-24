package org.hkijena.jipipe.desktop.app.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.commons.components.SplitButton;
import org.hkijena.jipipe.plugins.settings.application.JIPipeAutoSaveApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * The menubar save control. Besides the usual Unsaved / Save / Auto save /
 * Saved states it now shows (feature #753):
 * <ul>
 *   <li>SAVING — while a NON-MODAL JIPX save runs: the main button is
 *       disabled, a spinner icon is shown, and the text is
 *       {@code Saving… <stage>} (stage ∈ writing / compressing / verifying,
 *       from the save run's progress). Indeterminate — no % bar.</li>
 *   <li>RESULT FLASH — when the save finishes: {@code "Saved ✓"} (green) or
 *       {@code "Save failed ✗"} (red) shown for a few seconds by a single-shot
 *       timer, then {@link #updateStatus()} reverts to the normal state.</li>
 * </ul>
 * The window signals the button through the workbench's
 * {@link JIPipeDesktopProjectWorkbench.ProjectSavingEventEmitter}.
 */
public class JIPipeDesktopProjectSaveManagerButton extends SplitButton implements JIPipeDesktopProjectWorkbench.ProjectSavedEventListener, JIPipeDesktopProjectWorkbench.ProjectSavingEventListener {
    private static final int RESULT_FLASH_MILLIS = 4000;
    private final JIPipeDesktopProjectWorkbench workbench;
    private final JIPipeAutoSaveApplicationSettings settings;
    private LocalDateTime lastProjectSaveTime;
    private long lastAutoSaveTime = 0;
    private final JCheckBoxMenuItem autoSaveMenuItem = new JCheckBoxMenuItem("Auto save");
    private final Timer checkTimer;

    /** Whether a save is currently running (SAVING state). */
    private boolean saving = false;
    /** The current save stage ("writing" / "compressing" / "verifying"). */
    private String savingStage = "";

    /** Transient result-flash state (single-shot timer reverts it). */
    private boolean flashActive = false;
    private Icon flashIcon;
    private String flashText;
    private final Timer flashTimer = new Timer(RESULT_FLASH_MILLIS, e -> {
        flashActive = false;
        updateStatus();
    });

    public JIPipeDesktopProjectSaveManagerButton(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.settings = JIPipeAutoSaveApplicationSettings.getInstance();
        this.checkTimer = new Timer(Math.max(1, settings.getAutoSaveWatcherInterval()) * 1000, e -> doUpdate());

        checkTimer.setRepeats(true);
        checkTimer.start();
        // Single-shot: fire once to revert the flash.
        flashTimer.setRepeats(false);

        initialize();
        updateStatus();

        workbench.getProjectSavedEventEmitter().subscribe(this);
        workbench.getProjectSavingEventEmitter().subscribe(this);
        autoSaveMenuItem.setState(settings.isDefaultEnableAutoSave());
    }

    private void doUpdate() {

        if (workbench.getProject().isDisposed()) {
            checkTimer.stop();
            // Leak-free: drop the event subscriptions once the project goes
            // away (the workbench and its emitters share the button's
            // lifecycle, but unsubscribe explicitly on dispose).
            workbench.getProjectSavingEventEmitter().unsubscribe(this);
            workbench.getProjectSavedEventEmitter().unsubscribe(this);
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
        getMainButton().setEnabled(!saving);
        if (saving) {
            setIcon(JIPipe.RESOURCES.getIcon16("actions/spinner.png"));
            setText(savingStage.isEmpty() ? "Saving…" : "Saving… " + savingStage);
            return;
        }
        if (flashActive) {
            setIcon(flashIcon);
            setText(flashText);
            return;
        }
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

    /**
     * Starts the transient result flash (single-shot timer reverts it).
     *
     * @param icon  the flash icon
     * @param text  the flash text
     */
    private void showResultFlash(Icon icon, String text) {
        flashIcon = icon;
        flashText = text;
        flashActive = true;
        flashTimer.restart();
        updateStatus();
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

    @Override
    public void onProjectSaving(JIPipeDesktopProjectWorkbench.ProjectSavingEvent event) {
        // The live-stage emission can come from the save worker thread; hop
        // to the EDT before touching the button's components.
        SwingUtilities.invokeLater(() -> {
            if (event.isSaving()) {
                saving = true;
                savingStage = event.getStage() == null ? "" : event.getStage();
                updateStatus();
            } else {
                saving = false;
                savingStage = "";
                String stage = event.getStage();
                if (stage != null) {
                    if (stage.startsWith("Saved")) {
                        showResultFlash(JIPipe.RESOURCES.getIcon16("actions/save-green.png"), "Saved ✓");
                        return;
                    }
                    if (stage.startsWith("Save failed")) {
                        showResultFlash(JIPipe.RESOURCES.getIcon16("actions/save-red.png"), "Save failed ✗");
                        return;
                    }
                    // "Save cancelled" and other results: no flash, just
                    // revert to the normal (modified) state.
                }
                updateStatus();
            }
        });
    }
}
