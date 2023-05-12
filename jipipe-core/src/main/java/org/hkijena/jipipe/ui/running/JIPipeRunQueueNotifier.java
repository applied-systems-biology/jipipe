package org.hkijena.jipipe.ui.running;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.settings.NotificationUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

/**
 * Generates notifications based on the the runner
 */
public class JIPipeRunQueueNotifier implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private static JIPipeRunQueueNotifier INSTANCE;
    private final NotificationUISettings settings;

    private JIPipeRunQueueNotifier() {
        this.settings = NotificationUISettings.getInstance();
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
        JIPipeRunnerQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);
    }

    /**
     * Installs the notifier. Can be called multiple times (singleton)
     */
    public static void install() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeRunQueueNotifier();
        }
    }

    /**
     * Triggered when a worker is finished
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (settings.isShowRunNotifications()) {
            if (!canShowNotification(event.getWorker()))
                return;
            UIUtils.sendTrayNotification("Run finished", "The run \"" + event.getRun().getTaskLabel() + "\" finished in "
                            + DurationFormatUtils.formatDurationHMS(event.getWorker().getRuntimeMillis()),
                    TrayIcon.MessageType.INFO);
        }
    }

    public boolean canShowNotification(JIPipeRunWorker worker) {
        if (settings.isShowRunOnlyIfInactive()) {
            if (JIPipeProjectWindow.getOpenWindows().stream().anyMatch(Window::isActive))
                return false;
        }
        if (settings.getShowAfterMinRuntime().isEnabled()) {
            long minutes = worker.getRuntimeMillis() / 1000 / 60;
            if (minutes < settings.getShowAfterMinRuntime().getContent()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Triggered when a worker is interrupted
     *
     * @param event Generated event
     */
    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (settings.isShowRunNotifications()) {
            if (!canShowNotification(event.getWorker()))
                return;
            UIUtils.sendTrayNotification("Run failed", "The run '" + event.getRun().getTaskLabel() + "' failed.",
                    TrayIcon.MessageType.ERROR);
        }
    }
}
