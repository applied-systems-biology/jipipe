package org.hkijena.jipipe.utils.process;

import org.apache.commons.exec.Watchdog;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.run.JIPipeRunnable;

/**
 * A watchdog that monitors a sub-process of a {@link JIPipeRunnable}
 * and watches for the {@link JIPipeRunnable} to be cancelled.
 * Based on {@link Watchdog}
 */
public class RunCancellationWatchdog implements Runnable {
    private final CancelledEventEmitter cancelledEventEmitter = new CancelledEventEmitter();
    private final JIPipeProgressInfo progressInfo;
    private boolean stopped = false;

    public RunCancellationWatchdog(JIPipeProgressInfo progressInfo) {
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
                isWaiting = !progressInfo.isCancelled();
            }
        }

        // notify the listeners outside of the synchronized block (see EXEC-60)
        if (!isWaiting) {
            cancelledEventEmitter.emit(new CancelledEvent(this));
        }
    }

    public CancelledEventEmitter getCancelledEventEmitter() {
        return cancelledEventEmitter;
    }

    public interface CancelledEventListener {
        void onCancelled(CancelledEvent event);
    }

    public static class CancelledEvent extends AbstractJIPipeEvent {
        private final RunCancellationWatchdog watchdog;

        public CancelledEvent(RunCancellationWatchdog watchdog) {
            super(watchdog);
            this.watchdog = watchdog;
        }

        public RunCancellationWatchdog getWatchdog() {
            return watchdog;
        }
    }

    public static class CancelledEventEmitter extends JIPipeEventEmitter<CancelledEvent, CancelledEventListener> {

        @Override
        protected void call(CancelledEventListener cancelledEventListener, CancelledEvent event) {
            cancelledEventListener.onCancelled(event);
        }
    }
}
