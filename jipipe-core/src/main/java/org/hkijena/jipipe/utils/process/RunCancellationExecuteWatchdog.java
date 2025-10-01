package org.hkijena.jipipe.utils.process;

import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Watchdog;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.ProcessUtils;

/**
 * Based on {@link ExecuteWatchdog}. Adapted to listed to {@link JIPipeProgressInfo} cancellation.
 */
public class RunCancellationExecuteWatchdog extends ExecuteWatchdog implements RunCancellationWatchdog.CancelledEventListener {

    private final JIPipeProgressInfo progressInfo;
    private final RunCancellationWatchdog cancellationWatchdog;
    private final ExtendedExecutor extendedExecutor;

    /**
     * Creates a new watchdog with a given timeout.
     *
     * @param timeout          the timeout for the process in milliseconds. It must be
     *                         greater than 0 or 'INFINITE_TIMEOUT'
     * @param extendedExecutor the executor
     */
    public RunCancellationExecuteWatchdog(long timeout, JIPipeProgressInfo progressInfo, ExtendedExecutor extendedExecutor) {
        super(timeout);
        this.progressInfo = progressInfo;
        this.cancellationWatchdog = new RunCancellationWatchdog(progressInfo);
        this.extendedExecutor = extendedExecutor;
        this.cancellationWatchdog.getCancelledEventEmitter().subscribe(this);
    }

    @Override
    public synchronized void timeoutOccured(Watchdog w) {
        // Kill the process using the PID
        long pid = extendedExecutor.getPid();
        ProcessUtils.killProcessTree(pid, progressInfo);
        super.timeoutOccured(w);
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

    @Override
    public void onCancelled(RunCancellationWatchdog.CancelledEvent event) {
        this.timeoutOccured(null);
    }
}
