package org.hkijena.jipipe.desktop.commons.components.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ai.JIPipeAIModelRunnerStatus;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.commons.components.ai.monitor.JIPipeDesktopAIMonitorWindow;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

/**
 * Status bar button that displays the current AI model status and provides controls
 * to load/unload the embedding model.
 * <p>
 * Subscribes to {@link JIPipeAIServiceComponent.StatusChangedEventEmitter} for event-driven
 * updates instead of polling.
 * <p>
 * The Busy→Idle transition is debounced to prevent UI flickering when
 * the model rapidly alternates between busy and idle states during batched
 * embedding requests.
 */
public class JIPipeDesktopAIStatusControl extends JButton implements JIPipeAIServiceComponent.StatusChangedEventListener {

    /**
     * Debounce delay for the Busy→Idle transition (milliseconds).
     */
    private static final long IDLE_DEBOUNCE_MS = 500;

    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final AIApplicationSettings settings;
    private final ImageIcon defaultIcon;
    private final SpinnerIcon busyIcon;

    /**
     * Tracks the last status that was applied to the UI, used to detect Busy→Idle transitions.
     */
    private JIPipeAIModelRunnerStatus displayedStatus = null;

    /**
     * Debouncer for the Busy→Idle transition. If the status changes back to Busy
     * before the debouncer fires, the pending Idle update is effectively cancelled
     * because the next {@link #onAIStatusChanged} call will apply Busy immediately.
     */
    private final StaticDebouncer idleDebouncer;

    public JIPipeDesktopAIStatusControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.settings = AIApplicationSettings.getInstance();
        this.defaultIcon = JIPipe.RESOURCES.getIcon16("actions/ai.png");
        this.busyIcon = new SpinnerIcon(this);
        this.idleDebouncer = new StaticDebouncer(IDLE_DEBOUNCE_MS, TimeUnit.MILLISECONDS, this::applyIdleStatus);
        initialize();
        updateStatus();

        // Subscribe to status change events (event-driven, no polling)
        JIPipe.getInstance().getAiService().getStatusChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setIcon(defaultIcon);
        setText("N/A");
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    @Override
    public void onAIStatusChanged(JIPipeAIServiceComponent.StatusChangedEvent event) {
        // Status changes arrive from the queue worker thread; must update UI on EDT
        SwingUtilities.invokeLater(() -> handleStatusChange(event.getNewStatus()));
    }

    /**
     * Handles a status change with debouncing for the Busy→Idle transition.
     * All other transitions are applied immediately.
     */
    private void handleStatusChange(JIPipeAIModelRunnerStatus newStatus) {
        if (newStatus == JIPipeAIModelRunnerStatus.Idle
                && displayedStatus == JIPipeAIModelRunnerStatus.Busy) {
            // Debounce the Busy→Idle transition to prevent flickering
            idleDebouncer.debounce();
        } else {
            // Apply all other transitions immediately
            updateStatus();
        }
    }

    /**
     * Applies the Idle status to the UI. Called by the debouncer after the
     * debounce delay, or directly if no debouncing is needed.
     */
    private void applyIdleStatus() {
        // Re-read the current status in case it changed during the debounce window
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        JIPipeAIModelRunnerStatus currentStatus = aiService.getEmbeddingModelStatus();
        if (currentStatus == JIPipeAIModelRunnerStatus.Idle) {
            updateStatus();
        }
        // If status is no longer Idle (e.g., went back to Busy), do nothing —
        // the next onAIStatusChanged call will handle it
    }

    private void reloadMenu() {
        popupMenu.removeAll();
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        JIPipeAIModelRunnerStatus status = aiService.getEmbeddingModelStatus();

        // Load embedding model (available when unloaded or failed)
        if (status == JIPipeAIModelRunnerStatus.Unloaded || status == JIPipeAIModelRunnerStatus.Failed) {
            popupMenu.add(UIUtils.createMenuItem("Load embedding model", "Loads the embedding model",
                    JIPipe.RESOURCES.getIcon16("actions/circle-play.png"), this::startEmbeddingModel));
        }

        // Unload embedding model (available when idle or busy; busy defers unload until task completes)
        if (status == JIPipeAIModelRunnerStatus.Idle || status == JIPipeAIModelRunnerStatus.Busy) {
            String text = status == JIPipeAIModelRunnerStatus.Busy
                    ? "Unload embedding model (after current task)"
                    : "Unload embedding model";
            String tooltip = status == JIPipeAIModelRunnerStatus.Busy
                    ? "Unloads the embedding model after the current task completes"
                    : "Unloads the current embedding model";
            popupMenu.add(UIUtils.createMenuItem(text, tooltip,
                    JIPipe.RESOURCES.getIcon16("actions/circle-stop.png"), this::stopEmbeddingModel));
        }

        // Show error details if failed
        if (status == JIPipeAIModelRunnerStatus.Failed) {
            String error = aiService.getEmbeddingModelError();
            if (error != null && !error.isEmpty()) {
                popupMenu.add(UIUtils.createMenuItem("Show error details", "Shows the error that occurred",
                        JIPipe.RESOURCES.getIcon16("actions/help.png"), this::showErrorDetails));
            }
        }

        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Open AI monitor", "Opens the AI monitor window",
                JIPipe.RESOURCES.getIcon16("actions/ai.png"), this::openAIMonitor));
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Configure ...", "Opens the settings page for AI",
                JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openApplicationSettings));
    }

    private void updateStatus() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        JIPipeAIModelRunnerStatus status = aiService.getEmbeddingModelStatus();
        displayedStatus = status;

        switch (status) {
            case Unloaded:
                setText("AI offline");
                setIcon(defaultIcon);
                busyIcon.stop();
                break;
            case Loading:
                setText("AI loading...");
                setIcon(busyIcon);
                busyIcon.start();
                break;
            case Idle:
                setText("AI ready");
                setIcon(defaultIcon);
                busyIcon.stop();
                break;
            case Busy:
                setText("AI busy");
                setIcon(busyIcon);
                busyIcon.start();
                break;
            case Unloading:
                setText("AI shutting down...");
                setIcon(busyIcon);
                busyIcon.start();
                break;
            case Failed:
                String error = aiService.getEmbeddingModelError();
                setText("AI error" + (error != null ? ": " + truncate(error, 30) : ""));
                setIcon(defaultIcon);
                busyIcon.stop();
                break;
        }

        setToolTipText(getToolTipTextForStatus(status, aiService));
    }

    private String getToolTipTextForStatus(JIPipeAIModelRunnerStatus status, JIPipeAIServiceComponent aiService) {
        switch (status) {
            case Unloaded:
                return "AI model is not loaded. Click to load.";
            case Loading:
                return "AI model is loading...";
            case Idle:
                return "AI model is ready";
            case Busy:
                return "AI model is processing a task";
            case Unloading:
                return "AI model is shutting down...";
            case Failed:
                String error = aiService.getEmbeddingModelError();
                return "AI model error" + (error != null ? ": " + error : "");
            default:
                return "AI status: " + status;
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private void openApplicationSettings() {
        workbench.openApplicationSettings("/General/AI");
    }

    private void startEmbeddingModel() {
        if (!JIPipeDesktopAISetupDialog.checkFirstTimeSetup(workbench)) {
            return;
        }
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        aiService.tryStartEmbeddingModel();
    }

    private void stopEmbeddingModel() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        aiService.tryStopEmbeddingModel();
    }

    private void showErrorDetails() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        String error = aiService.getEmbeddingModelError();
        JOptionPane.showMessageDialog(this,
                "AI model error:\n" + (error != null ? error : "Unknown error"),
                "AI Error", JOptionPane.ERROR_MESSAGE);
    }

    private void openAIMonitor() {
        JIPipeDesktopAIMonitorWindow window = new JIPipeDesktopAIMonitorWindow(workbench);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
    }
}
