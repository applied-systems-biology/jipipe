package org.hkijena.jipipe.utils;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * A watchdog that monitors a sub-process of a {@link org.hkijena.jipipe.api.JIPipeRunnable}
 * and watches for the {@link org.hkijena.jipipe.api.JIPipeRunnable} to be cancelled.
 * Based on {@link org.apache.commons.exec.Watchdog}
 */
public class JIPipeRunCancellationWatchdog implements Runnable {
    private final EventBus eventBus = new EventBus();
    private final JIPipeProgressInfo progressInfo;
    private boolean stopped = false;

    public JIPipeRunCancellationWatchdog(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    public synchronized void start() {
        stopped = false;
        final Thread t = new Thread(this, "WATCHDOG");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop() {
        stopped = true;
        notifyAll();
    }

    public void run() {
        boolean isWaiting;
        synchronized (this) {
            isWaiting = true;
            while (!stopped && isWaiting) {
                try {
                    wait(500);
                } catch (final InterruptedException e) {
                }
                isWaiting = !progressInfo.isCancelled().get();
            }
        }

        // notify the listeners outside of the synchronized block (see EXEC-60)
        if (!isWaiting) {
            eventBus.post(new CancelledEvent(this));
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public static class CancelledEvent {
        private final JIPipeRunCancellationWatchdog watchdog;

        public CancelledEvent(JIPipeRunCancellationWatchdog watchdog) {
            this.watchdog = watchdog;
        }

        public JIPipeRunCancellationWatchdog getWatchdog() {
            return watchdog;
        }
    }
}
