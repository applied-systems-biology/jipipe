/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeMemoryStatus;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.service.JIPipeServiceMode;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * Service component that provides application-wide memory monitoring.
 *
 * <p>This component captures memory metrics at regular intervals and maintains
 * a history of the last 100 data points. Monitoring is only active in GUI mode.</p>
 *
 * <p>Thread safety: This class is thread-safe. All public methods can be called
 * from any thread. The monitoring task runs on a dedicated executor thread.</p>
 */
public class JIPipeMemoryMonitorServiceComponent extends JIPipeServiceComponent {

    private static final int HISTORY_SIZE = 100;
    private static final long MONITOR_INTERVAL_MS = 1000;

    private final Deque<JIPipeMemoryStatus> memoryHistory = new ArrayDeque<>(HISTORY_SIZE + 1);
    private final StampedLock historyLock = new StampedLock();

    private ScheduledExecutorService executorService;
    private volatile boolean monitoringActive = false;

    /**
     * Creates a new memory monitor service component.
     *
     * @param service the parent service
     */
    public JIPipeMemoryMonitorServiceComponent(JIPipeService service) {
        super(service);
    }

    /**
     * Gets the current memory status.
     *
     * @return Current memory status
     */
    public JIPipeMemoryStatus getCurrentStatus() {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();

        return new JIPipeMemoryStatus(usedMemory, maxMemory);
    }

    /**
     * Gets a copy of the memory history.
     *
     * <p>Returns an unmodifiable copy of the history list to prevent external modification.
     * The list is ordered from oldest to newest.</p>
     *
     * @return Unmodifiable list of memory status snapshots
     */
    public List<JIPipeMemoryStatus> getHistory() {
        long stamp = historyLock.readLock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(memoryHistory));
        } finally {
            historyLock.unlockRead(stamp);
        }
    }

    /**
     * Starts memory monitoring.
     *
     * <p>Monitoring will only start if:
     * <ul>
     *   <li>The service mode is GUI</li>
     *   <li>Monitoring is not already active</li>
     * </ul>
     * </p>
     *
     * <p>This method is typically called during postprocess() when in GUI mode.</p>
     */
    public void startMonitoring() {
        if (getService().getInitializationSettings().getMode() != JIPipeServiceMode.GUI) {
            return;
        }

        if (monitoringActive) {
            return;
        }

        synchronized (this) {
            if (monitoringActive) {
                return;
            }

            executorService = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "JIPipe-MemoryMonitor");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });

            executorService.scheduleAtFixedRate(
                    this::captureMemory,
                    MONITOR_INTERVAL_MS,
                    MONITOR_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );

            monitoringActive = true;
        }
    }

    /**
     * Stops memory monitoring.
     *
     * <p>This method safely shuts down the executor service and waits for
     * completion for up to 5 seconds.</p>
     */
    public void stopMonitoring() {
        if (!monitoringActive) {
            return;
        }

        synchronized (this) {
            if (!monitoringActive) {
                return;
            }

            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                executorService = null;
            }

            monitoringActive = false;
        }
    }

    /**
     * @return True if monitoring is currently active
     */
    public boolean isMonitoringActive() {
        return monitoringActive;
    }

    /**
     * Captures current memory metrics and adds to history.
     * This method is called by the scheduled executor.
     */
    private void captureMemory() {
        JIPipeMemoryStatus status = getCurrentStatus();

        long stamp = historyLock.writeLock();
        try {
            memoryHistory.addLast(status);

            // Remove oldest entry if we exceed history size (O(1) operation)
            if (memoryHistory.size() > HISTORY_SIZE) {
                memoryHistory.removeFirst();
            }
        } finally {
            historyLock.unlockWrite(stamp);
        }
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        super.postprocess(progressInfo);

        // Initialize history with placeholder values for smooth graph rendering
        JIPipeMemoryStatus initialStatus = getCurrentStatus();
        long stamp = historyLock.writeLock();
        try {
            for (int i = 0; i < HISTORY_SIZE; i++) {
                memoryHistory.add(initialStatus);
            }
        } finally {
            historyLock.unlockWrite(stamp);
        }

        // Start monitoring in GUI mode
        startMonitoring();
    }

    @Override
    public void dispose() {
        super.dispose();
        stopMonitoring();
    }
}
