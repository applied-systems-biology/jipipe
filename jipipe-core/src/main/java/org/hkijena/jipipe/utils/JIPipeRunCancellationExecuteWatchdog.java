package org.hkijena.jipipe.utils;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Watchdog;
import org.apache.commons.exec.util.DebugUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * Based on {@link org.apache.commons.exec.ExecuteWatchdog}. Adapted to listed to {@link JIPipeProgressInfo} cancellation.
 */
public class JIPipeRunCancellationExecuteWatchdog extends ExecuteWatchdog {

    private final JIPipeProgressInfo progressInfo;
    private final JIPipeRunCancellationWatchdog cancellationWatchdog;

    /**
     * Creates a new watchdog with a given timeout.
     *
     * @param timeout the timeout for the process in milliseconds. It must be
     *                greater than 0 or 'INFINITE_TIMEOUT'
     */
    public JIPipeRunCancellationExecuteWatchdog(long timeout, JIPipeProgressInfo progressInfo) {
        super(timeout);
        this.progressInfo = progressInfo;
        this.cancellationWatchdog = new JIPipeRunCancellationWatchdog(progressInfo);
        this.cancellationWatchdog.getEventBus().register(this);
    }

    @Subscribe
    public synchronized void onProcessCancelled(JIPipeRunCancellationWatchdog.CancelledEvent event) {
        this.timeoutOccured(null);
    }

    @Override
    public synchronized void start(Process processToMonitor) {
        super.start(processToMonitor);
        cancellationWatchdog.start();
    }

    @Override
    public synchronized void stop() {
        cancellationWatchdog.stop();
        super.stop();
    }
}
