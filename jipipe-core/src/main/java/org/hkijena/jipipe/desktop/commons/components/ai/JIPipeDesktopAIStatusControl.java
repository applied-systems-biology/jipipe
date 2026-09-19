package org.hkijena.jipipe.desktop.commons.components.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEvent;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeListener;
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
 * Subscribes to {@link org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEventEmitter} on the
 * {@link JIPipeAIServiceComponent.EmbeddingModelService} for event-driven updates instead of polling.
 * <p>
 * The Busy→Idle transition (within the Ready state) is debounced to prevent UI flickering when
 * the model rapidly alternates between busy and idle states during batched
 * embedding requests.
 */
public class JIPipeDesktopAIStatusControl extends JButton implements MicroserviceStateChangeListener {

    /**
     * Debounce delay for the Ready(busy)→Ready(idle) transition (milliseconds).
     */
    private static final long IDLE_DEBOUNCE_MS = 500;

    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final AIApplicationSettings settings;
    private final ImageIcon defaultIcon;
    private final SpinnerIcon busyIcon;

    /**
     * Tracks the last status that was applied to the UI, used to detect busy→idle transitions.
     */
    private MicroserviceState displayedStatus = null;
    private String displayedDetail = null;

    /**
     * Debouncer for the busy→idle transition. If the state detail changes back to "Busy"
     * before the debouncer fires, the pending idle update is effectively cancelled
     * because the next state-change call will apply busy immediately.
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

        // Subscribe to state change events (event-driven, no polling)
        JIPipe.getInstance().getAiService().getEmbeddingModelService().getStateChangeEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setIcon(defaultIcon);
        setText("N/A");
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        // State changes arrive from the service thread; must update UI on EDT
        SwingUtilities.invokeLater(() -> handleStatusChange(event.getNewState()));
    }

    /**
     * Handles a status change with debouncing for the busy→idle transition.
     * All other transitions are applied immediately.
     */
    private void handleStatusChange(MicroserviceState newStatus) {
        String detail = JIPipe.getInstance().getAiService().getEmbeddingModelService().getStateDetail();
        if (newStatus == MicroserviceState.Ready
                && "Idle".equals(detail)
                && displayedStatus == MicroserviceState.Ready
                && "Busy".equals(displayedDetail)) {
            // Debounce the busy→idle transition to prevent flickering
            idleDebouncer.debounce();
        } else {
            // Apply all other transitions immediately
            updateStatus();
        }
    }

    /**
     * Applies the idle status to the UI. Called by the debouncer after the
     * debounce delay, or directly if no debouncing is needed.
     */
    private void applyIdleStatus() {
        // Re-read the current status in case it changed during the debounce window
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        MicroserviceState currentStatus = aiService.getEmbeddingModelStatus();
        String detail = aiService.getEmbeddingModelService().getStateDetail();
        if (currentStatus == MicroserviceState.Ready && "Idle".equals(detail)) {
            updateStatus();
        }
    }

    private void reloadMenu() {
        popupMenu.removeAll();
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        MicroserviceState status = aiService.getEmbeddingModelStatus();
        String detail = aiService.getEmbeddingModelService().getStateDetail();
        boolean isBusy = status == MicroserviceState.Ready && "Busy".equals(detail);

        // Load embedding model (available when stopped or failed)
        if (status == MicroserviceState.Stopped || status == MicroserviceState.Failed) {
            popupMenu.add(UIUtils.createMenuItem("Load embedding model", "Loads the embedding model",
                    JIPipe.RESOURCES.getIcon16("actions/circle-play.png"), this::startEmbeddingModel));
        }

        // Unload embedding model (available when ready; busy defers unload until task completes)
        if (status == MicroserviceState.Ready) {
            String text = isBusy
                    ? "Unload embedding model (after current task)"
                    : "Unload embedding model";
            String tooltip = isBusy
                    ? "Unloads the embedding model after the current task completes"
                    : "Unloads the current embedding model";
            popupMenu.add(UIUtils.createMenuItem(text, tooltip,
                    JIPipe.RESOURCES.getIcon16("actions/circle-stop.png"), this::stopEmbeddingModel));
        }

        // Show error details if failed
        if (status == MicroserviceState.Failed) {
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
        MicroserviceState status = aiService.getEmbeddingModelStatus();
        String detail = aiService.getEmbeddingModelService().getStateDetail();
        displayedStatus = status;
        displayedDetail = detail;

        switch (status) {
            case Stopped:
                setText("AI offline");
                setIcon(defaultIcon);
                busyIcon.stop();
                break;
            case Starting:
                setText("AI loading...");
                setIcon(busyIcon);
                busyIcon.start();
                break;
            case Ready:
                if ("Busy".equals(detail)) {
                    setText("AI busy");
                    setIcon(busyIcon);
                    busyIcon.start();
                } else {
                    setText("AI ready");
                    setIcon(defaultIcon);
                    busyIcon.stop();
                }
                break;
            case Stopping:
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

    private String getToolTipTextForStatus(MicroserviceState status, JIPipeAIServiceComponent aiService) {
        switch (status) {
            case Stopped:
                return "AI model is not loaded. Click to load.";
            case Starting:
                return "AI model is loading...";
            case Ready:
                String detail = aiService.getEmbeddingModelService().getStateDetail();
                return "Busy".equals(detail) ? "AI model is processing a task" : "AI model is ready";
            case Stopping:
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
